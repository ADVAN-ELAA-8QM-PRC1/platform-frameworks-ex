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

package com.android.ex.carousel;

import com.android.ex.carousel.CarouselRS.CarouselCallback;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.renderscript.FileA3D;
import android.renderscript.Mesh;
import android.renderscript.RSSurfaceView;
import android.renderscript.RenderScriptGL;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public abstract class CarouselView extends RSSurfaceView {
    private static final boolean USE_DEPTH_BUFFER = true;
    private final int DEFAULT_SLOT_COUNT = 10;
    private final Bitmap DEFAULT_BITMAP = Bitmap.createBitmap(1, 1, Config.RGB_565);
    private final float DEFAULT_RADIUS = 20.0f;
    private final float DEFAULT_SWAY_SENSITIVITY = 0.0f;
    private final float DEFAULT_FRICTION_COEFFICIENT = 10.0f;
    private final float DEFAULT_DRAG_FACTOR = 0.25f;
    private static final String TAG = "CarouselView";
    private CarouselRS mRenderScript;
    private RenderScriptGL mRS;
    private Context mContext;
    private boolean mTracking;

    // These are meant to shadow the state of the renderer in case the surface changes.
    private Bitmap mDefaultBitmap;
    private Bitmap mLoadingBitmap;
    private Bitmap mBackgroundBitmap;
    private Mesh mDefaultGeometry;
    private Mesh mLoadingGeometry;
    private int mCardCount = 0;
    private int mVisibleSlots = 0;
    private float mStartAngle;
    private float mRadius = DEFAULT_RADIUS;
    private float mCardRotation = 0.0f;
    private float mSwaySensitivity = DEFAULT_SWAY_SENSITIVITY;
    private float mFrictionCoefficient = DEFAULT_FRICTION_COEFFICIENT;
    private float mDragFactor = DEFAULT_DRAG_FACTOR;
    private int mSlotCount = DEFAULT_SLOT_COUNT;
    private float mEye[] = { 20.6829f, 2.77081f, 16.7314f };
    private float mAt[] = { 14.7255f, -3.40001f, -1.30184f };
    private float mUp[] = { 0.0f, 1.0f, 0.0f };

    public static class Info {
        public Info(int _resId) { resId = _resId; }
        public int resId; // resource for renderscript resource (e.g. R.raw.carousel)
    }

    public abstract Info getRenderScriptInfo();

    public CarouselView(Context context) {
        this(context, null);
    }

    /**
     * Constructor used when this widget is created from a layout file.
     */
    public CarouselView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        boolean useDepthBuffer = true;
        ensureRenderScript();
        // TODO: add parameters to layout
    }

    private void ensureRenderScript() {
        mRS = createRenderScript(USE_DEPTH_BUFFER);
        mRenderScript = new CarouselRS();
        mRenderScript.init(mRS, getResources(), getRenderScriptInfo().resId);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);
        //mRS.contextSetSurface(w, h, holder.getSurface());
        mRenderScript.init(mRS, getResources(), getRenderScriptInfo().resId);
        setSlotCount(mSlotCount);
        createCards(mCardCount);
        setVisibleSlots(mVisibleSlots);
        setCallback(mCarouselCallback);
        setDefaultBitmap(mDefaultBitmap);
        setLoadingBitmap(mLoadingBitmap);
        setDefaultGeometry(mDefaultGeometry);
        setLoadingGeometry(mLoadingGeometry);
        setBackgroundBitmap(mBackgroundBitmap);
        setStartAngle(mStartAngle);
        setRadius(mRadius);
        setCardRotation(mCardRotation);
        setSwaySensitivity(mSwaySensitivity);
        setFrictionCoefficient(mFrictionCoefficient);
        setDragFactor(mDragFactor);
        setLookAt(mEye, mAt, mUp);
    }

    /**
     * Loads geometry from a resource id.
     *
     * @param resId
     * @return the loaded mesh or null if it cannot be loaded
     */
    public Mesh loadGeometry(int resId) {
        Resources res = mContext.getResources();
        FileA3D model = FileA3D.createFromResource(mRS, res, resId);
        FileA3D.IndexEntry entry = model.getIndexEntry(0);
        if(entry == null || entry.getClassID() != FileA3D.ClassID.MESH) {
            return null;
        }
        return (Mesh) entry.getObject();
    }

    /**
     * Load A3D file from resource.  If resId == 0, will clear geometry for this item.
     * @param n
     * @param resId
     */
    public void setGeometryForItem(int n, Mesh mesh) {
        if (mRenderScript != null) {
            mRenderScript.setGeometry(n, mesh);
        }
    }

    public void setSlotCount(int n) {
        mSlotCount = n;
        if (mRenderScript != null) {
            mRenderScript.setSlotCount(n);
        }
    }

    public void setVisibleSlots(int n) {
        mVisibleSlots = n;
        if (mRenderScript != null) {
            mRenderScript.setVisibleSlots(n);
        }
    }

    public void createCards(int n) {
        mCardCount = n;
        if (mRenderScript != null) {
            mRenderScript.createCards(n);
        }
    }

    public void setTextureForItem(int n, Bitmap bitmap) {
        // Also check against mRS, to handle the case where the result is being delivered by a
        // background thread but the sender no longer exists.
        if (mRenderScript != null && mRS != null) {
            Log.v(TAG, "setTextureForItem(" + n + ")");
            mRenderScript.setTexture(n, bitmap);
            Log.v(TAG, "done");
        }
    }

    public void setDefaultBitmap(Bitmap bitmap) {
        mDefaultBitmap = bitmap;
        if (mRenderScript != null) {
            mRenderScript.setDefaultBitmap(bitmap);
        }
    }

    public void setLoadingBitmap(Bitmap bitmap) {
        mLoadingBitmap = bitmap;
        if (mRenderScript != null) {
            mRenderScript.setLoadingBitmap(bitmap);
        }
    }

    public void setBackgroundBitmap(Bitmap bitmap) {
        mBackgroundBitmap = bitmap;
        if (mRenderScript != null) {
            mRenderScript.setBackgroundTexture(bitmap);
        }
    }

    public void setDefaultGeometry(Mesh mesh) {
        mDefaultGeometry = mesh;
        if (mRenderScript != null) {
            mRenderScript.setDefaultGeometry(mesh);
        }
    }

    public void setLoadingGeometry(Mesh mesh) {
        mLoadingGeometry = mesh;
        if (mRenderScript != null) {
            mRenderScript.setLoadingGeometry(mesh);
        }
    }

    public void setCallback(CarouselCallback callback)
    {
        mCarouselCallback = callback;
        if (mRenderScript != null) {
            mRenderScript.setCallback(callback);
        }
    }

    public void setStartAngle(float angle)
    {
        mStartAngle = angle;
        if (mRenderScript != null) {
            mRenderScript.setStartAngle(angle);
        }
    }

    public void setRadius(float radius) {
        mRadius = radius;
        if (mRenderScript != null) {
            mRenderScript.setRadius(radius);
        }
    }

    public void setCardRotation(float cardRotation) {
        mCardRotation = cardRotation;
        if (mRenderScript != null) {
            mRenderScript.setCardRotation(cardRotation);
        }
    }

    public void setSwaySensitivity(float swaySensitivity) {
        mSwaySensitivity = swaySensitivity;
        if (mRenderScript != null) {
            mRenderScript.setSwaySensitivity(swaySensitivity);
        }
    }

    public void setFrictionCoefficient(float frictionCoefficient) {
        mFrictionCoefficient = frictionCoefficient;
        if (mRenderScript != null) {
            mRenderScript.setFrictionCoefficient(frictionCoefficient);
        }
    }

    public void setDragFactor(float dragFactor) {
        mDragFactor = dragFactor;
        if (mRenderScript != null) {
            mRenderScript.setDragFactor(dragFactor);
        }
    }

    public void setLookAt(float[] eye, float[] at, float[] up) {
        mEye = eye;
        mAt = at;
        mUp = up;
        if (mRenderScript != null) {
            mRenderScript.setLookAt(eye, at, up);
        }
    }

    public void requestFirstCardPosition() {
        if (mRenderScript != null) {
            mRenderScript.requestFirstCardPosition();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(mRS != null) {
            mRS = null;
            destroyRenderScript();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ensureRenderScript();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        final float x = event.getX();
        final float y = event.getY();

        if (mRenderScript == null) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mTracking = true;
                mRenderScript.doStart(x, y);
                break;

            case MotionEvent.ACTION_MOVE:
                if (mTracking) {
                    mRenderScript.doMotion(x, y);
                }
                break;

            case MotionEvent.ACTION_UP:
                mRenderScript.doStop(x, y);
                mTracking = false;
                break;
        }

        return true;
    }

    private final CarouselCallback DEBUG_CALLBACK = new CarouselCallback() {
        @Override
        public void onAnimationStarted() {
            Log.v(TAG, "onAnimationStarted()");
        }

        @Override
        public void onAnimationFinished() {
            Log.v(TAG, "onAnimationFinished()");
        }

        @Override
        public void onCardSelected(int n) {
            Log.v(TAG, "onCardSelected(" + n + ")");
        }

        @Override
        public void onRequestGeometry(int n) {
            Log.v(TAG, "onRequestGeometry(" + n + ")");
        }

        @Override
        public void onInvalidateGeometry(int n) {
            Log.v(TAG, "onInvalidateGeometry(" + n + ")");
        }

        @Override
        public void onRequestTexture(final int n) {
            Log.v(TAG, "onRequestTexture(" + n + ")");
        }

        @Override
        public void onInvalidateTexture(int n) {
            Log.v(TAG, "onInvalidateTexture(" + n + ")");
        }

        @Override
        public void onReportFirstCardPosition(int n) {
            Log.v(TAG, "onReportFirstCardPosition(" + n + ")");
        }
    };

    private CarouselCallback mCarouselCallback = DEBUG_CALLBACK;
}
