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

import java.io.File;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;

public class RegisterActivity extends Activity implements OnClickListener {
    private final String TAG = "WOL";
    private Button mButtonRegister;
    private Button mButtonUnregister;
    private Button mButtonSendMail;
    private Button mButtonNoSleep;
    private MyBroadcastReceiver mReceiver;
    private String mRegId;
    private Context mCtx;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mCtx = this;
        mButtonRegister = (Button)findViewById(R.id.buttonRegister);
        mButtonUnregister = (Button)findViewById(R.id.buttonUnregister);
        mButtonSendMail = (Button)findViewById(R.id.buttonSendMail);
        mButtonNoSleep = (Button)findViewById(R.id.buttonNoSleep);

        mButtonRegister.setOnClickListener(this);
        mButtonUnregister.setOnClickListener(this);
        mButtonSendMail.setOnClickListener(this);
        mButtonNoSleep.setOnClickListener(this);

        GCMRegistrar.checkDevice(this);
        GCMRegistrar.checkManifest(this);
        mRegId = GCMRegistrar.getRegistrationId(this);
        if (mRegId.equals("")) {
            mySetViewStatus(true);
        } else {
            mySetViewStatus(false);
        }
        mReceiver = new MyBroadcastReceiver();
        registerReceiver(mReceiver, new IntentFilter(MYACTION_NOTIFY));
    }
    
    @Override
    protected void onDestroy() {
        _Log.d(TAG, "RegisterActivity: onDestroy");
        File html = new File(HtmlForm.fileName(this));
        if (html.exists()) {
            html.delete();
        }
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        try {
            GCMRegistrar.onDestroy(getApplicationContext());
        } catch (Exception e) {
        }
        super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 0, Menu.NONE, R.string.WordGotoMainUI).
            setIcon(android.R.drawable.ic_media_play);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case 0: // Open Main UI
            Intent it = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(it);
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v == (View)mButtonRegister) {
            new AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setIcon(R.drawable.icon)
            .setMessage(R.string.MsgRegisterDevice)
            .setPositiveButton(R.string.WordYes,
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        GCMRegistrar.register(getApplicationContext(), SENDER_ID);
                    }
                })
            .setNegativeButton(R.string.WordNo,
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
            .show();
        }
        else if (v == (View)mButtonUnregister) {
            new AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setIcon(R.drawable.icon)
            .setMessage(R.string.MsgUnregisterDevice)
            .setPositiveButton(R.string.WordYes,
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        GCMRegistrar.unregister(getApplicationContext());
                    }
                })
            .setNegativeButton(R.string.WordNo,
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
            .show();
        }
        else if (v == (View)mButtonSendMail) {
            createMail(mRegId);
        }
        else if (v == (View)mButtonNoSleep) {
            Intent it = new Intent(getApplicationContext(), NoSleepActivity.class);
            startActivity(it);
            finish();
        }
    }
    
    private void createMail(final String regid) {
        // Open password dialog
        final EditText editView = new EditText(this);
        editView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.icon)
                .setTitle(R.string.MsgSpecifyPassword)
                .setCancelable(false)
                .setView(editView)
                .setPositiveButton(R.string.WordNext, null)
                .setNegativeButton(R.string.WordCancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        final AlertDialog dlg = builder.show();
        
        // Builder#setPositiveButton() automatically close dialog box.
        // Button#setOnClickListener() is not so.
        Button buttonOK = dlg.getButton(DialogInterface.BUTTON_POSITIVE);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String pass = editView.getText().toString();
                if (pass.length() <= 0) {
                    showDialogMessage(mCtx, getString(R.string.MsgNoPassword), false);
                    return;
                }
                dlg.dismiss();
                Toast.makeText(mCtx, R.string.MsgCreatingMail, Toast.LENGTH_SHORT).show();
                createMailSub(regid, pass);
            }
        });
    }

    private boolean createMailSub(String regid, String pass) {
        Account[] accounts =
            AccountManager.get(this).getAccountsByType("com.google");
        
        String cryptStr = MyCrypt.Crypt(regid, pass);
        if (cryptStr == null) {
            return false;
        }
        MyPref pref = new MyPref(this);
        List<MyListItem> items = pref.loadEntries();
        int num = items.size();
        String [] entry = new String[num];
        for (int i = 0; i < num; i++) {
            entry[i] = items.get(i).getEntryName();
        }
        if (!HtmlForm.create(cryptStr, entry, getApplicationContext())) {
            return false;
        }
        Intent it = new Intent();
        it.setAction(Intent.ACTION_SEND);
        String[] mailto = {accounts[0].name};
        it.putExtra(Intent.EXTRA_EMAIL, mailto);
        it.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.WordMailSubject));
        it.putExtra(Intent.EXTRA_TEXT, getString(R.string.MsgMailText));
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        it.setType("application/octed-stream");
        it.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + HtmlForm.fileName(this)));
        // Gmail ClassName
        it.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
        try {
            startActivity(it);
        } catch (Exception e) {
            _Log.i(TAG, "createMailSub: Gmail ClassName causes an exception:" + e.toString());
            // Gmail 4.2.1 etc. 
            it.setClassName("com.google.android.gm", "com.android.mail.compose.ComposeActivityGmail");
            startActivity(it);
        }
        return true;
    }
    
    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            WakeLock wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getString(R.string.app_name));
            wakeLock.acquire();
            Bundle extras = intent.getExtras();
            String event = extras.getString(MYEXTRA_NAME_EVENT);
            _Log.d(TAG, "MyBroadcastReceiver: event=" + event);
            if (event.equals(MYEXTRA_EVENT_REGISTERED)) {
                mySetViewStatus(false);
                final String regid = extras.getString(MYEXTRA_NAME_REGID);
                new AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setIcon(R.drawable.icon)
                .setCancelable(false) // disable back button 
                .setMessage(R.string.MsgCreateMail)
                .setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        createMail(regid);
                    }
                })
                .show();
            }
            else if (event.equals(MYEXTRA_EVENT_UNREGISTERED)) {
                mySetViewStatus(true);
                showDialogMessage(context, getString(R.string.MsgUnregistered), false);
            }
            else if (event.equals(MYEXTRA_EVENT_ERROR)) {
                String errId = extras.getString(MYEXTRA_NAME_ERRORID);
                showDialogMessage(context, errId, false);
            }
            wakeLock.release();
        }
    }

    private void showDialogMessage(Context ctx, String msg, final boolean bFinish) {
        new AlertDialog.Builder(ctx)
            .setTitle(R.string.app_name)
            .setIcon(R.drawable.icon)
            .setMessage(msg)
            .setPositiveButton("OK",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if (bFinish) {
                        finish();
                    }
                }
            })
            .show();
    }
    
    private void mySetViewStatus(boolean bMode) {
        mButtonRegister.setEnabled(bMode);
        mButtonRegister.setClickable(bMode);
        mButtonUnregister.setEnabled(!bMode);
        mButtonUnregister.setClickable(!bMode);
        mButtonSendMail.setEnabled(!bMode);
        mButtonSendMail.setClickable(!bMode);
        mButtonNoSleep.setEnabled(!bMode);
        mButtonNoSleep.setClickable(!bMode);
    }    
}