/*
 * Copyright (C) 2013 The Android Open Source Project
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

/**
 * A factory class for {@link CameraManager}.
 */
public class CameraManagerFactory {

    private static AndroidCameraManagerImpl sAndroidCameraManager;
    private static int sAndoridCameraManagerClientCount;

    /**
     * Returns the android camera implementation of {@link com.android.camera.cameradevice.CameraManager}.
     *
     * @return The {@link CameraManager} to control the camera device.
     */
    public static synchronized CameraManager getAndroidCameraManager() {
        if (sAndroidCameraManager == null) {
            sAndroidCameraManager = new AndroidCameraManagerImpl();
            sAndoridCameraManagerClientCount = 1;
        } else {
            ++sAndoridCameraManagerClientCount;
        }
        return sAndroidCameraManager;
    }

    /**
     * Recycles the resources. Always call this method when the activity is
     * stopped.
     */
    public static synchronized void recycle() {
        if (--sAndoridCameraManagerClientCount == 0 && sAndroidCameraManager != null) {
            sAndroidCameraManager.recycle();
            sAndroidCameraManager = null;
        }
    }
}
