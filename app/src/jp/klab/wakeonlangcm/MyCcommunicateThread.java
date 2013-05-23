/*
 * Copyright (C) 2013 KLab Inc.
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

package jp.klab.wakeonlangcm;

import java.util.concurrent.CountDownLatch;

import jp.klab.wakeonlangcm.R;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class MyCcommunicateThread extends Thread {
    private static final String TAG = "WOL";
    private Handler handler;
    private CountDownLatch handlerInitLatch;
    private Context mCtx;

    MyCcommunicateThread(Context ctx) {
    	mCtx = ctx;
        handlerInitLatch = new CountDownLatch(1);
    }

    Handler getHandler() {
        try {
            handlerInitLatch.await(); // wait until countDown()
        } catch (InterruptedException ie) {
        }
        return handler;
    }

    @Override
    public void run() {
    	_Log.d(TAG, "MyCcommunicateHandler run");
        Looper.prepare();
        // set MyCcommunicateHandler to thread's handler
        handler = new MyCcommunicateHandler(mCtx);
        handlerInitLatch.countDown();
        Looper.loop();
    }
}

final class MyCcommunicateHandler extends Handler {
    private static final String TAG = "WOL";
    private boolean running = true;
    private MyNetUty mNetUty;
    private String mBroadcastAddr;
    private Context mCtx;
    
    MyCcommunicateHandler(Context ctx) {
    	mCtx = ctx;
        mNetUty = new MyNetUty(ctx);
        mBroadcastAddr = null;
    }

    public void handleMessage(Message msg) {
        if (!running) {
            return;
        }
        switch (msg.what) {
        // send magic packet
        case R.id.do_send:
        	mBroadcastAddr = mNetUty.getWifiBcastAddress();
            byte [] data = (byte[]) msg.obj;
            int port = msg.arg1;
            _Log.d(TAG, "MyCcommunicateHandler: do_send port=" + port);
            int sts = mNetUty.sendData(mBroadcastAddr, port, data);
            if (sts != 0) { // error
                //_Log.d(TAG, "MyCcommunicateHandler: do_send error");
            }
            break;

        // quit this thread
        case R.id.quit:
            _Log.d(TAG, "MyCcommunicateHandler exiting..");
            running = false;
            Looper.myLooper().quit();
            break;
        }
    }
}
