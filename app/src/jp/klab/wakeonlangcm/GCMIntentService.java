/*
 * Copyright (C) 2013 KLab Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.klab.wakeonlangcm;

import static jp.klab.wakeonlangcm.Constant.MYACTION_NOTIFY;
import static jp.klab.wakeonlangcm.Constant.MYEXTRA_EVENT_ERROR;
import static jp.klab.wakeonlangcm.Constant.MYEXTRA_EVENT_REGISTERED;
import static jp.klab.wakeonlangcm.Constant.MYEXTRA_EVENT_UNREGISTERED;
import static jp.klab.wakeonlangcm.Constant.MYEXTRA_NAME_ERRORID;
import static jp.klab.wakeonlangcm.Constant.MYEXTRA_NAME_EVENT;
import static jp.klab.wakeonlangcm.Constant.MYEXTRA_NAME_REGID;
import static jp.klab.wakeonlangcm.Constant.SENDER_ID;

import java.util.List;

import jp.klab.wakeonlangcm.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {
    private final String TAG = "WOL";
    private MyCcommunicateThread mCommThread;
    private MyNetUty mNetUty = null;
    
    public GCMIntentService() {
        super(SENDER_ID);
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        if (mNetUty == null) {
            mNetUty = new MyNetUty(getApplicationContext());
        }
        if (!mNetUty.RunningOnWifiLanEnvironment()) {
            _Log.i(TAG, "GCMIntentService: onMessage: not running on Wifi Lan Environment..");
            return;
        }
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String action = (String) extras.get("action");
            String target = (String) extras.get("target");
            _Log.d(TAG, "GCMIntentService: onMessage: action=" + action + " target=" + target);
            if (action != null && action.equals("boot") && target.length() > 0) {
                mCommThread = new MyCcommunicateThread(getApplicationContext());
                mCommThread.start();
                // target -> "pc1,pc2,pc3,"...
                String [] targetNames = target.split(","); 
                MyPref pref = new MyPref(context);
                List<MyListItem> items = pref.loadEntries();
                int num = items.size();
                // search entry
                for (int i = 0; i < targetNames.length; i++) {
                    _Log.d(TAG, "GCMIntentService: onMessage: name=[" + targetNames[i] + "]");
                    for (int j = 0; j < num; j++) {
                        MyListItem li = items.get(j); 
                        if (li.getEntryName().equals(targetNames[i])) {
                            int port = Integer.parseInt(li.getPortNumber());
                            String [] ar = li.getMacAddress().split(":");
                            byte [] mac = new byte[6];
                            for (int n = 0; n < ar.length; n++) {
                                mac[n] = (byte)Integer.parseInt(ar[n], 16);
                            }
                            // fill 0xFF first 6 bytes
                            byte [] data = new byte[6+6*16];
                            for (int n = 0; n < 6; n++) {
                                data[n] = (byte)0xFF;
                            }
                            // repeat MAC Address 16 times
                            for (int n = 0; n < 16; n++) {
                                int ofs = n * 6 + 6;
                                data[ofs+0] = mac[0];
                                data[ofs+1] = mac[1];
                                data[ofs+2] = mac[2];
                                data[ofs+3] = mac[3];
                                data[ofs+4] = mac[4];
                                data[ofs+5] = mac[5];
                            }                            
                            Message msg = Message.obtain(mCommThread.getHandler(), R.id.do_send);
                            msg.obj = data;
                            msg.arg1 = port;
                            msg.sendToTarget();
                        }
                    }
                }
                Message msg = Message.obtain(mCommThread.getHandler(), R.id.quit);
                msg.sendToTarget();
            }
        }
    }

    @Override
    public void onRegistered(Context context, String registrationId) {
        _Log.d(TAG, "GCMIntentService: onRegistered id=" + registrationId);
        Intent it = new Intent(MYACTION_NOTIFY);
        it.putExtra(MYEXTRA_NAME_EVENT, MYEXTRA_EVENT_REGISTERED);
        it.putExtra(MYEXTRA_NAME_REGID, registrationId);
        sendBroadcast(it);
    }

    @Override
    public void onUnregistered(Context context, String registrationId) {
        _Log.d(TAG, "GCMIntentService: onUnregistered");
        Intent it = new Intent(MYACTION_NOTIFY);
        it.putExtra(MYEXTRA_NAME_EVENT, MYEXTRA_EVENT_UNREGISTERED);
        sendBroadcast(it);
    }

    @Override
    public void onError(Context context, String errorId) {
        _Log.d(TAG, "GCMIntentService: onError errid=" + errorId);
        Intent it = new Intent(MYACTION_NOTIFY);
        it.putExtra(MYEXTRA_NAME_EVENT, MYEXTRA_EVENT_ERROR);
        it.putExtra(MYEXTRA_NAME_ERRORID, errorId);
        sendBroadcast(it);
    }
}
