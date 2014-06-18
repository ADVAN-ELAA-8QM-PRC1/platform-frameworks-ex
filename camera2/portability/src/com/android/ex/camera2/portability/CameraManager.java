/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.ex.camera2.portability;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.OnZoomChangeListener;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;

/**
 * An interface which provides possible camera device operations.
 *
 * The client should call {@code CameraManager.openCamera} to get an instance
 * of {@link CameraManager.CameraProxy} to control the camera. Classes
 * implementing this interface should have its own one unique {@code Thread}
 * other than the main thread for camera operations. Camera device callbacks
 * are wrapped since the client should not deal with
 * {@code android.hardware.Camera} directly.
 *
 * TODO: provide callback interfaces for:
 * {@code android.hardware.Camera.ErrorCallback},
 * {@code android.hardware.Camera.OnZoomChangeListener}, and
 */
public interface CameraManager {
    public static final long CAMERA_OPERATION_TIMEOUT_MS = 2500;

    public static class CameraStartPreviewCallbackForward
            implements CameraStartPreviewCallback {
        private final Handler mHandler;
        private final CameraStartPreviewCallback mCallback;

        public static CameraStartPreviewCallbackForward getNewInstance(
                Handler handler, CameraStartPreviewCallback cb) {
            if (handler == null || cb == null) {
                return null;
            }
            return new CameraStartPreviewCallbackForward(handler, cb);
        }

        private CameraStartPreviewCallbackForward(Handler h,
                CameraStartPreviewCallback cb) {
            mHandler = h;
            mCallback = cb;
        }

        @Override
        public void onPreviewStarted() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onPreviewStarted();
                }
            });
        }
    }

    /**
     * A callback helps to invoke the original callback on another
     * {@link android.os.Handler}.
     */
    public static class CameraOpenCallbackForward implements CameraOpenCallback {
        private final Handler mHandler;
        private final CameraOpenCallback mCallback;

        /**
         * Returns a new instance of {@link FaceDetectionCallbackForward}.
         *
         * @param handler The handler in which the callback will be invoked in.
         * @param cb The callback to be invoked.
         * @return The instance of the {@link FaceDetectionCallbackForward}, or
         *         null if any parameter is null.
         */
        public static CameraOpenCallbackForward getNewInstance(
                Handler handler, CameraOpenCallback cb) {
            if (handler == null || cb == null) {
                return null;
            }
            return new CameraOpenCallbackForward(handler, cb);
        }

        private CameraOpenCallbackForward(Handler h, CameraOpenCallback cb) {
            // Given that we are using the main thread handler, we can create it
            // here instead of holding onto the PhotoModule objects. In this
            // way, we can avoid memory leak.
            mHandler = new Handler(Looper.getMainLooper());
            mCallback = cb;
        }

        @Override
        public void onCameraOpened(final CameraProxy camera) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onCameraOpened(camera);
                }
            });
        }

        @Override
        public void onCameraDisabled(final int cameraId) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onCameraDisabled(cameraId);
                }
            });
        }

        @Override
        public void onDeviceOpenFailure(final int cameraId, final String info) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onDeviceOpenFailure(cameraId, info);
                }
            });
        }

        @Override
        public void onDeviceOpenedAlready(final int cameraId, final String info) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onDeviceOpenedAlready(cameraId, info);
                }
            });
        }

        @Override
        public void onReconnectionFailure(final CameraManager mgr, final String info) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onReconnectionFailure(mgr, info);
                }
            });
        }
    }

    /**
     * A handler for all camera api runtime exceptions.
     * The default behavior is to throw the runtime exception.
     */
    public interface CameraExceptionCallback {
        public void onCameraException(RuntimeException e);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.ErrorCallback}
     */
    public interface CameraErrorCallback {
        public void onError(int error, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.AutoFocusCallback}.
     */
    public interface CameraAFCallback {
        public void onAutoFocus(boolean focused, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.AutoFocusMoveCallback}.
     */
    public interface CameraAFMoveCallback {
        public void onAutoFocusMoving(boolean moving, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.ShutterCallback}.
     */
    public interface CameraShutterCallback {
        public void onShutter(CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.PictureCallback}.
     */
    public interface CameraPictureCallback {
        public void onPictureTaken(byte[] data, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.PreviewCallback}.
     */
    public interface CameraPreviewDataCallback {
        public void onPreviewFrame(byte[] data, CameraProxy camera);
    }

    /**
     * An interface which wraps
     * {@link android.hardware.Camera.FaceDetectionListener}.
     */
    public interface CameraFaceDetectionCallback {
        /**
         * Callback for face detection.
         *
         * @param faces   Recognized face in the preview.
         * @param camera  The camera which the preview image comes from.
         */
        public void onFaceDetection(Camera.Face[] faces, CameraProxy camera);
    }

    /**
     * An interface to be called when the camera preview has started.
     */
    public interface CameraStartPreviewCallback {
        /**
         * Callback when the preview starts.
         */
        public void onPreviewStarted();
    }

    /**
     * An interface to be called for any events when opening or closing the
     * camera device. This error callback is different from the one defined
     * in the framework, {@link android.hardware.Camera.ErrorCallback}, which
     * is used after the camera is opened.
     */
    public interface CameraOpenCallback {
        /**
         * Callback when camera open succeeds.
         */
        public void onCameraOpened(CameraProxy camera);

        /**
         * Callback when {@link com.android.camera.CameraDisabledException} is
         * caught.
         *
         * @param cameraId The disabled camera.
         */
        public void onCameraDisabled(int cameraId);

        /**
         * Callback when {@link com.android.camera.CameraHardwareException} is
         * caught.
         *
         * @param cameraId The camera with the hardware failure.
         * @param info The extra info regarding this failure.
         */
        public void onDeviceOpenFailure(int cameraId, String info);

        /**
         * Callback when trying to open the camera which is already opened.
         *
         * @param cameraId The camera which is causing the open error.
         */
        public void onDeviceOpenedAlready(int cameraId, String info);

        /**
         * Callback when {@link java.io.IOException} is caught during
         * {@link android.hardware.Camera#reconnect()}.
         *
         * @param mgr The {@link CameraManager}
         *            with the reconnect failure.
         */
        public void onReconnectionFailure(CameraManager mgr, String info);
    }

    /**
     * Opens the camera of the specified ID asynchronously. The camera device
     * will be opened in the camera handler thread and will be returned through
     * the {@link CameraManager.CameraOpenCallback#
     * onCameraOpened(com.android.camera.cameradevice.CameraManager.CameraProxy)}.
     *
     * @param handler The {@link android.os.Handler} in which the callback
     *                was handled.
     * @param callback The callback for the result.
     * @param cameraId The camera ID to open.
     */
    public void openCamera(Handler handler, int cameraId, CameraOpenCallback callback);

    /**
     * Closes the camera device.
     *
     * @param camera The camera to close. {@code null} means all.
     * @param synced Whether this call should be synchronous.
     */
    public void closeCamera(CameraProxy camera, boolean synced);

    /**
     * Sets a callback for handling camera api runtime exceptions on
     * a handler.
     */
    public void setCameraDefaultExceptionCallback(CameraExceptionCallback callback,
            Handler handler);

    /**
     * Recycles the resources used by this instance. CameraManager will be in
     * an unusable state after calling this.
     */
    public void recycle();

    /**
     * @return The camera devices info.
     */
    public CameraDeviceInfo getCameraDeviceInfo();

    /**
     * An interface that takes camera operation requests and post messages to the
     * camera handler thread. All camera operations made through this interface is
     * asynchronous by default except those mentioned specifically.
     */
    public interface CameraProxy {

        /**
         * Returns the underlying {@link android.hardware.Camera} object used
         * by this proxy. This method should only be used when handing the
         * camera device over to {@link android.media.MediaRecorder} for
         * recording.
         */
        @Deprecated
        public android.hardware.Camera getCamera();

        /**
         * @return The camera ID associated to by this
         * {@link CameraManager.CameraProxy}.
         */
        public int getCameraId();

        /**
         * @return The camera capabilities.
         */
        public CameraCapabilities getCapabilities();

        /**
         * Reconnects to the camera device. On success, the camera device will
         * be returned through {@link CameraManager
         * .CameraOpenCallback#onCameraOpened(com.android.camera.cameradevice.CameraManager
         * .CameraProxy)}.
         * @see android.hardware.Camera#reconnect()
         *
         * @param handler The {@link android.os.Handler} in which the callback
         *                was handled.
         * @param cb The callback when any error happens.
         */
        public void reconnect(Handler handler, CameraOpenCallback cb);

        /**
         * Unlocks the camera device.
         *
         * @see android.hardware.Camera#unlock()
         */
        public void unlock();

        /**
         * Locks the camera device.
         * @see android.hardware.Camera#lock()
         */
        public void lock();

        /**
         * Sets the {@link android.graphics.SurfaceTexture} for preview.
         *
         * @param surfaceTexture The {@link SurfaceTexture} for preview.
         */
        public void setPreviewTexture(final SurfaceTexture surfaceTexture);

        /**
         * Blocks until a {@link android.graphics.SurfaceTexture} has been set
         * for preview.
         *
         * @param surfaceTexture The {@link SurfaceTexture} for preview.
         */
        public void setPreviewTextureSync(final SurfaceTexture surfaceTexture);

        /**
         * Sets the {@link android.view.SurfaceHolder} for preview.
         *
         * @param surfaceHolder The {@link SurfaceHolder} for preview.
         */
        public void setPreviewDisplay(final SurfaceHolder surfaceHolder);

        /**
         * Starts the camera preview.
         */
        public void startPreview();

        /**
         * Starts the camera preview and executes a callback on a handler once
         * the preview starts.
         */
        public void startPreviewWithCallback(Handler h, CameraStartPreviewCallback cb);

        /**
         * Stops the camera preview synchronously.
         * {@code stopPreview()} must be synchronous to ensure that the caller can
         * continues to release resources related to camera preview.
         */
        public void stopPreview();

        /**
         * Sets the callback for preview data.
         *
         * @param handler    The {@link android.os.Handler} in which the callback was handled.
         * @param cb         The callback to be invoked when the preview data is available.
         * @see  android.hardware.Camera#setPreviewCallback(android.hardware.Camera.PreviewCallback)
         */
        public void setPreviewDataCallback(Handler handler, CameraPreviewDataCallback cb);

        /**
         * Sets the one-time callback for preview data.
         *
         * @param handler    The {@link android.os.Handler} in which the callback was handled.
         * @param cb         The callback to be invoked when the preview data for
         *                   next frame is available.
         * @see  android.hardware.Camera#setPreviewCallback(android.hardware.Camera.PreviewCallback)
         */
        public void setOneShotPreviewCallback(Handler handler, CameraPreviewDataCallback cb);

        /**
         * Sets the callback for preview data.
         *
         * @param handler The handler in which the callback will be invoked.
         * @param cb      The callback to be invoked when the preview data is available.
         * @see android.hardware.Camera#setPreviewCallbackWithBuffer(android.hardware.Camera.PreviewCallback)
         */
        public void setPreviewDataCallbackWithBuffer(Handler handler, CameraPreviewDataCallback cb);

        /**
         * Adds buffer for the preview callback.
         *
         * @param callbackBuffer The buffer allocated for the preview data.
         */
        public void addCallbackBuffer(byte[] callbackBuffer);

        /**
         * Starts the auto-focus process. The result will be returned through the callback.
         *
         * @param handler The handler in which the callback will be invoked.
         * @param cb      The auto-focus callback.
         */
        public void autoFocus(Handler handler, CameraAFCallback cb);

        /**
         * Cancels the auto-focus process.
         */
        public void cancelAutoFocus();

        /**
         * Sets the auto-focus callback
         *
         * @param handler The handler in which the callback will be invoked.
         * @param cb      The callback to be invoked when the preview data is available.
         */
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        public void setAutoFocusMoveCallback(Handler handler, CameraAFMoveCallback cb);

        /**
         * Instrument the camera to take a picture.
         *
         * @param handler   The handler in which the callback will be invoked.
         * @param shutter   The callback for shutter action, may be null.
         * @param raw       The callback for uncompressed data, may be null.
         * @param postview  The callback for postview image data, may be null.
         * @param jpeg      The callback for jpeg image data, may be null.
         * @see android.hardware.Camera#takePicture(
         *         android.hardware.Camera.ShutterCallback,
         *         android.hardware.Camera.PictureCallback,
         *         android.hardware.Camera.PictureCallback)
         */
        public void takePicture(
                Handler handler,
                CameraShutterCallback shutter,
                CameraPictureCallback raw,
                CameraPictureCallback postview,
                CameraPictureCallback jpeg);

        /**
         * Sets the display orientation for camera to adjust the preview orientation.
         *
         * @param degrees The rotation in degrees. Should be 0, 90, 180 or 270.
         */
        public void setDisplayOrientation(int degrees);

        /**
         * Sets the listener for zoom change.
         *
         * @param listener The listener.
         */
        public void setZoomChangeListener(OnZoomChangeListener listener);

        /**
         * Sets the face detection listener.
         *
         * @param handler  The handler in which the callback will be invoked.
         * @param callback The callback for face detection results.
         */
        public void setFaceDetectionCallback(Handler handler, CameraFaceDetectionCallback callback);

        /**
         * Starts the face detection.
         */
        public void startFaceDetection();

        /**
         * Stops the face detection.
         */
        public void stopFaceDetection();

        /**
         * Registers an error callback.
         *
         * @param handler  The handler on which the callback will be invoked.
         * @param cb The error callback.
         * @see android.hardware.Camera#setErrorCallback(android.hardware.Camera.ErrorCallback)
         */
        public void setErrorCallback(Handler handler, CameraErrorCallback cb);

        /**
         * Sets the camera parameters.
         *
         * @param params The camera parameters to use.
         */
        @Deprecated
        public void setParameters(Camera.Parameters params);

        /**
         * Gets the current camera parameters synchronously. This method is
         * synchronous since the caller has to wait for the camera to return
         * the parameters. If the parameters are already cached, it returns
         * immediately.
         */
        @Deprecated
        public Camera.Parameters getParameters();

        /**
         * Gets the current camera settings synchronously.
         * <p>This method is synchronous since the caller has to wait for the
         * camera to return the parameters. If the parameters are already
         * cached, it returns immediately.</p>
         */
        public CameraSettings getSettings();

        /**
         * Applies the settings to the camera device.
         *
         * @param settings The settings to use on the device.
         * @return Whether the settings can be applied.
         */
        public boolean applySettings(CameraSettings settings);

        /**
         * Forces {@code CameraProxy} to update the cached version of the camera
         * settings regardless of the dirty bit.
         */
        public void refreshSettings();

        /**
         * Enables/Disables the camera shutter sound.
         *
         * @param enable   {@code true} to enable the shutter sound,
         *                 {@code false} to disable it.
         */
        public void enableShutterSound(boolean enable);
    }
}
