/*
 * Copyright (C) 2011 The Android Open Source Project
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

package frameworks.base.core.java.android.server.wm;

import android.os.Process;
import android.view.Display;
import android.view.InputChannel;
import android.view.WindowManager;
import com.android.server.input.InputApplicationHandle;
import com.android.server.input.InputWindowHandle;

class InputConsumerImpl {
    final WindowManagerService mService;
    final InputChannel mServerChannel, mClientChannel;
    final InputApplicationHandle mApplicationHandle;
    final InputWindowHandle mWindowHandle;

    InputConsumerImpl(WindowManagerService service, String name, InputChannel inputChannel) {
        mService = service;

        InputChannel[] channels = InputChannel.openInputChannelPair(name);
        mServerChannel = channels[0];
        if (inputChannel != null) {
            channels[1].transferTo(inputChannel);
            channels[1].dispose();
            mClientChannel = inputChannel;
        } else {
            mClientChannel = channels[1];
        }
        mService.mInputManager.registerInputChannel(mServerChannel, null);

        mApplicationHandle = new InputApplicationHandle(null);
        mApplicationHandle.name = name;
        mApplicationHandle.dispatchingTimeoutNanos =
                WindowManagerService.DEFAULT_INPUT_DISPATCHING_TIMEOUT_NANOS;

        mWindowHandle = new InputWindowHandle(mApplicationHandle, null, Display.DEFAULT_DISPLAY);
        mWindowHandle.name = name;
        mWindowHandle.inputChannel = mServerChannel;
        mWindowHandle.layoutParamsType = WindowManager.LayoutParams.TYPE_INPUT_CONSUMER;
        mWindowHandle.layer = getLayerLw(mWindowHandle.layoutParamsType);
        mWindowHandle.layoutParamsFlags = 0;
        mWindowHandle.dispatchingTimeoutNanos =
                WindowManagerService.DEFAULT_INPUT_DISPATCHING_TIMEOUT_NANOS;
        mWindowHandle.visible = true;
        mWindowHandle.canReceiveKeys = false;
        mWindowHandle.hasFocus = false;
        mWindowHandle.hasWallpaper = false;
        mWindowHandle.paused = false;
        mWindowHandle.ownerPid = Process.myPid();
        mWindowHandle.ownerUid = Process.myUid();
        mWindowHandle.inputFeatures = 0;
        mWindowHandle.scaleFactor = 1.0f;
    }

    void layout(int dw, int dh) {
        mWindowHandle.touchableRegion.set(0, 0, dw, dh);
        mWindowHandle.frameLeft = 0;
        mWindowHandle.frameTop = 0;
        mWindowHandle.frameRight = dw;
        mWindowHandle.frameBottom = dh;
    }

    private int getLayerLw(int windowType) {
        return mService.mPolicy.windowTypeToLayerLw(windowType)
                * WindowManagerService.TYPE_LAYER_MULTIPLIER
                + WindowManagerService.TYPE_LAYER_OFFSET;
    }

    void disposeChannelsLw() {
        mService.mInputManager.unregisterInputChannel(mServerChannel);
        mClientChannel.dispose();
        mServerChannel.dispose();
    }
}
