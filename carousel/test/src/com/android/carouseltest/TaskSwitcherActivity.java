/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.android.carouseltest;

import java.util.ArrayList;
import java.util.List;
import com.android.carouseltest.R;

import com.android.ex.carousel.CarouselRS.CarouselCallback;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.IThumbnailReceiver;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;

public class TaskSwitcherActivity extends Activity {
    private static final String TAG = "TaskSwitcherActivity";
    private static final int CARD_SLOTS = 56;
    private static final int MAX_TASKS = 20;
    private static final int VISIBLE_SLOTS = 7;
    protected static final boolean DBG = false;
    private ActivityManager mActivityManager;
    private List<RunningTaskInfo> mRunningTaskList;
    private boolean mPortraitMode = true;
    private ArrayList<ActivityDescription> mActivityDescriptions
            = new ArrayList<ActivityDescription>();
    private MyCarouselView mView;
    private Bitmap mBlankBitmap = Bitmap.createBitmap(128, 128, Config.RGB_565);

    static class ActivityDescription {
        int id;
        Bitmap thumbnail;
        Drawable icon;
        String label;
        String description;
        Intent intent;
        Matrix matrix;

        public ActivityDescription(Bitmap _thumbnail,
                Drawable _icon, String _label, String _desc, int _id)
        {
            thumbnail = _thumbnail;
            icon = _icon;
            label = _label;
            description = _desc;
            id = _id;
        }

        public void clear() {
            icon = null;
            thumbnail = null;
            label = null;
            description = null;
            intent = null;
            matrix = null;
            id = -1;
        }
    };

    private ActivityDescription findActivityDescription(int id) {
        for (int i = 0; i < mActivityDescriptions.size(); i++) {
            ActivityDescription item = mActivityDescriptions.get(i);
            if (item != null && item.id == id) {
                return item;
            }
        }
        return null;
    }

    final CarouselCallback mCarouselCallback = new CarouselCallback() {

        public void onAnimationFinished() {

        }

        public void onAnimationStarted() {

        }

        public void onCardSelected(int n) {
            if (n < mActivityDescriptions.size()) {
                ActivityDescription item = mActivityDescriptions.get(n);
                // prepare a launch intent and send it
                if (item.intent != null) {
                    item.intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
                    try {
                        Log.v(TAG, "Starting intent " + item.intent);
                        startActivity(item.intent);
                        overridePendingTransition(R.anim.zoom_enter, R.anim.zoom_exit);
                    } catch (ActivityNotFoundException e) {
                        Log.w("Recent", "Unable to launch recent task", e);
                    }
                    finish();
                }
            }
        }

        public void onInvalidateTexture(int n) {

        }

        public void onRequestDetailTexture(int n) {
            if (DBG) Log.v(TAG, "onRequestDetailTexture(" + n + ")" );
            //mDetailTextureHandler.removeMessages(n);
            //Message message = mDetailTextureHandler.obtainMessage(n, n, 0);
            //mDetailTextureHandler.sendMessageDelayed(message, HOLDOFF_DELAY);
        }

        public void onInvalidateDetailTexture(int n) {
            if (DBG) Log.v(TAG, "onInvalidateDetailTexture(" + n + ")");
            //mDetailTextureHandler.removeMessages(n);
        }

        public void onRequestGeometry(int n) {

        }

        public void onInvalidateGeometry(int n) {

        }

        public void onRequestTexture(final int n) {
            Log.v(TAG, "onRequestTexture(" + n + ")");
            if (n < mActivityDescriptions.size()) {
                mView.post(new Runnable() {
                    public void run() {
                        ActivityDescription desc = mActivityDescriptions.get(n);
                        if (desc != null) {
                            Log.v(TAG, "FOUND ACTIVITY THUMBNAIL " + desc.thumbnail);
                            Bitmap bitmap = desc.thumbnail == null ? mBlankBitmap : desc.thumbnail;
                            mView.setTextureForItem(n, bitmap);
                        } else {
                            Log.v(TAG, "FAILED TO GET ACTIVITY THUMBNAIL FOR ITEM " + n);
                        }
                    }
                });
            }
        }

        public void onReportFirstCardPosition(int n) {

        }
    };

    private final IThumbnailReceiver mThumbnailReceiver = new IThumbnailReceiver.Stub() {

        public void finished() throws RemoteException {

        }

        public void newThumbnail(final int id, final Bitmap bitmap, CharSequence description)
                throws RemoteException {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            Log.v(TAG, "New thumbnail for id=" + id + ", dimensions=" + w + "x" + h
                    + " description '" + description + "'");
            ActivityDescription info = findActivityDescription(id);
            if (info != null) {
                info.thumbnail = bitmap;
                final int thumbWidth = bitmap.getWidth();
                final int thumbHeight = bitmap.getHeight();
                if ((mPortraitMode && thumbWidth > thumbHeight)
                        || (!mPortraitMode && thumbWidth < thumbHeight)) {
                    Matrix matrix = new Matrix();
                    matrix.setRotate(90.0f, (float) thumbWidth / 2, (float) thumbHeight / 2);
                    info.matrix = matrix;
                } else {
                    info.matrix = null;
                }
            } else {
                Log.v(TAG, "Can't find view for id " + id);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Resources res = getResources();
        final View decorView = getWindow().getDecorView();

        mView = new MyCarouselView(this);
        mView.setSlotCount(CARD_SLOTS);
        mView.setVisibleSlots(VISIBLE_SLOTS);
        mView.createCards(1);
        mView.setStartAngle((float) -(2.0f*Math.PI * 5 / CARD_SLOTS));
        mView.setDefaultBitmap(BitmapFactory.decodeResource(res, R.drawable.wait));
        mView.setLoadingBitmap(BitmapFactory.decodeResource(res, R.drawable.wait));
        mView.setCallback(mCarouselCallback);

        mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        mPortraitMode = decorView.getHeight() > decorView.getWidth();

        refresh();

        setContentView(mView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mPortraitMode = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
        Log.v(TAG, "CONFIG CHANGE, mPortraitMode = " + mPortraitMode);
        refresh();
    }

    void updateRunningTasks() {
        mRunningTaskList = mActivityManager.getRunningTasks(MAX_TASKS + 2, 0, mThumbnailReceiver);
        Log.v(TAG, "Portrait: " + mPortraitMode);
        for (RunningTaskInfo r : mRunningTaskList) {
            if (r.thumbnail != null) {
                int thumbWidth = r.thumbnail.getWidth();
                int thumbHeight = r.thumbnail.getHeight();
                Log.v(TAG, "Got thumbnail " + thumbWidth + "x" + thumbHeight);
                ActivityDescription desc = findActivityDescription(r.id);
                if (desc != null) {
                    desc.thumbnail = r.thumbnail;
                    desc.label = r.topActivity.flattenToShortString();
                    if ((mPortraitMode && thumbWidth > thumbHeight)
                            || (!mPortraitMode && thumbWidth < thumbHeight)) {
                        Matrix matrix = new Matrix();
                        matrix.setRotate(90.0f, (float) thumbWidth / 2, (float) thumbHeight / 2);
                        desc.matrix = matrix;
                    }
                } else {
                    Log.v(TAG, "Couldn't find ActivityDesc for id=" + r.id);
                }
            } else {
                Log.v(TAG, "*** RUNNING THUMBNAIL WAS NULL ***");
            }
        }
        // HACK refresh carousel
        mView.createCards(mActivityDescriptions.size());
    }

    private void updateRecentTasks() {
        final PackageManager pm = getPackageManager();
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        final List<ActivityManager.RecentTaskInfo> recentTasks =
                am.getRecentTasks(MAX_TASKS + 2, ActivityManager.RECENT_IGNORE_UNAVAILABLE);

        ActivityInfo homeInfo = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .resolveActivityInfo(pm, 0);

        //IconUtilities iconUtilities = new IconUtilities(this);

        int numTasks = recentTasks.size();
        mActivityDescriptions.clear();
        for (int i = 1, index = 0; i < numTasks && (index < MAX_TASKS + 2); ++i) {
            final ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(i);

            Intent intent = new Intent(recentInfo.baseIntent);
            if (recentInfo.origActivity != null) {
                intent.setComponent(recentInfo.origActivity);
            }

            // Skip the current home activity.
            if (homeInfo != null
                    && homeInfo.packageName.equals(intent.getComponent().getPackageName())
                    && homeInfo.name.equals(intent.getComponent().getClassName())) {
                continue;
            }

            intent.setFlags((intent.getFlags()&~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            final ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
            if (resolveInfo != null) {
                final ActivityInfo info = resolveInfo.activityInfo;
                final String title = info.loadLabel(pm).toString();
                Drawable icon = info.loadIcon(pm);

                int id = recentTasks.get(i).id;
                if (id != -1 && title != null && title.length() > 0 && icon != null) {
                    //icon = iconUtilities.createIconDrawable(icon);
                    ActivityDescription item = new ActivityDescription(null, icon, title, null, id);
                    item.intent = intent;
                    mActivityDescriptions.add(item);
                    Log.v(TAG, "Added item[" + index + "], id=" + item.id);
                    ++index;
                } else {
                    Log.v(TAG, "SKIPPING item " + id);
                }
            }
        }
    }

    private void refresh() {
        updateRecentTasks();
        updateRunningTasks();
        mView.createCards(mActivityDescriptions.size());
    }
}
