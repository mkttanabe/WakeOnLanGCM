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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

public class MainActivity extends ListActivity implements AdapterView.OnItemLongClickListener {
    private static final String TAG = "WOL";
    private List<MyListItem> mItems = null;
    private ListAdapter mAdapter = null; 
    private ListView mListView;
    private MyPref mPref;
    private Context mCtx;
    private MyCcommunicateThread mCommThread;
    private String mVersion;
    private MyNetUty mNetUty;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        _Log.d(TAG, "MainActivity: onCreate" );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);
        mCtx = this;
        mNetUty = new MyNetUty(getApplicationContext());
        mPref = new MyPref(getApplicationContext());
        
        // set LongClickListener to ListView
        mListView = getListView();
        mListView.setOnItemLongClickListener(this);
        
        // get version string
        try {
            String pkg = getPackageName();
            mVersion = getPackageManager().getPackageInfo(pkg, 0).versionName;
        } catch (NameNotFoundException e) {
            mVersion = "";
        }
    }

    @Override
    protected void onStart() {
        _Log.d(TAG, "MainActivity: onStart");
        super.onStart();
        if (!mNetUty.RunningOnWifiLanEnvironment()) {
            showDialogMessage(getString(R.string.MsgRequirePrivateWifiNetwork), true);
        }
        buildItemList();
    }
    
    @Override
    protected  void onListItemClick(ListView lv, View view, int pos,  long id) {
        _Log.d(TAG, "MainActivity: onListItemClick: pos=" + pos + " item num=" + mItems.size());
        if (pos == 0) {
            if (mItems.size() >= Constant.ENTRY_MAX) {
                showDialogMessage(getString(R.string.MsgMaxEntry), false);
                return;
            }
            openEntryDialog(null, null, null, -1);
            return;
        }
        MyListItem li = mItems.get(pos);
        String name = li.getEntryName();
        final String mac = li.getMacAddress();
        final String port = li.getPortNumber();
        new AlertDialog.Builder(mCtx).setTitle(R.string.app_name)
        .setIcon(R.drawable.icon).setMessage("[" + name + "]\n" + getString(R.string.MsgSendPacket))
        .setPositiveButton(R.string.WordYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                sendMagickPacket(mac, Integer.parseInt(port));
            }
        })
        .setNegativeButton(R.string.WordNo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        })
        .show();
    }    
    
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int pos, long id) {
        _Log.d(TAG, "MainActivity: onItemLongClick: pos=" + pos);
        if (pos == 0) { // first line is used for creating a new entry
            Uri uri = Uri.parse(getString(R.string.UrlPrivacyPolicy));
            Intent it = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(it);
            return true;
        }
        final View curView = view;
        final int curIdx = pos;
        final long curId = id;
        String[] choices = {getString(R.string.WordSendPacket),
                            getString(R.string.WordEditEntry),
                            getString(R.string.WordRemoveEntry)};

        AlertDialog.Builder dlg = new AlertDialog.Builder(this);
        dlg.setIcon(R.drawable.icon);
        dlg.setTitle(R.string.MsgActionSelect);
        dlg.setItems(choices, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                MyListItem li = mItems.get(pos);
                String name = li.getEntryName();
                String mac = li.getMacAddress();
                String port = li.getPortNumber();
                switch (which) {
                case 0: // send packet
                    onListItemClick(mListView, curView, curIdx, curId);
                    break;
                case 1: // edit
                    openEntryDialog(name, mac, port, pos - 1);
                    break;
                case 2: // remove
                    new AlertDialog.Builder(mCtx)
                            .setTitle(R.string.app_name)
                            .setIcon(R.drawable.icon)
                            .setMessage("[" + name + "]\n" + getString(R.string.MsgRemoveEntry))
                            .setPositiveButton(R.string.WordYes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,int whichButton) {
                                            mPref.RemoveEntry(pos - 1);
                                            buildItemList();
                                        }
                            })
                            .setNegativeButton(R.string.WordNo, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                        }
                            }).show();
                    break;
                }
            }
        });
        dlg.show();
        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 0, Menu.NONE, R.string.WordGotoRegisterUI).
            setIcon(android.R.drawable.ic_media_play);
        // prepare for the unknown path specification
        if (mPref.sharedPrefFileName() != null) {
            menu.add(0, 1, Menu.NONE, R.string.WordExport).
                setIcon(android.R.drawable.ic_menu_upload);
            menu.add(0, 2, Menu.NONE, R.string.WordImport).
                setIcon(android.R.drawable.ic_menu_revert);
        }
        menu.add(0, 3, Menu.NONE, R.string.WordAbout).
            setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, 4, Menu.NONE, R.string.WordExit).
            setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case 0: // Open GCM Registration UI
            Intent it = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(it);
            return true;
        case 1: // Export
            List<MyListItem> data = mPref.loadEntries();
            if (data.size() <= 0) {
                showDialogMessage(getString(R.string.MsgNoData), false);
                return true;
            }
            if (mPref.exportPref() == true) {
                showDialogMessage("[" + mPref.exportPrefFileName() + "]\n" +
                        getString(R.string.MsgExportOK), false);
            } else {
                showDialogMessage(getString(R.string.MsgExportNG), false);
            }
            return true;
            
        case 2: // Import
            String fileName = mPref.exportPrefFileName();
            if (!new File(fileName).exists()) {
                showDialogMessage("[" + fileName + "]\n" + getString(R.string.MsgFileNotFound), false);
                return true;
            }
            new AlertDialog.Builder(mCtx).setTitle(R.string.app_name)
            .setIcon(R.drawable.icon).setMessage(getString(R.string.MsgQueryImport))
            .setPositiveButton(R.string.WordYes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if (mPref.importPref() == true) {
                        buildItemList();
                        showDialogMessage(getString(R.string.MsgImportOK), false);
                    } else {
                        showDialogMessage(getString(R.string.MsgImportNG), false);
                    }
                }
            })
            .setNegativeButton(R.string.WordNo, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .show();
            return true;
        case 3: // About
            showDialogMessage(getString(R.string.app_name) +
                            " " + mVersion + "\r\n\r\n" +
                            getString(R.string.CopyRightString) , false);
            return true;
        case 4: // exit
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }    
    
    private void SetAdapter() {
        mAdapter = new MyArrayAdapter(this, R.layout.listitem, mItems, Constant.SizeExtraLarge);
        setListAdapter(mAdapter);        
    }    
    
    private int buildItemList() {
        if (mItems == null) {
            mItems = new ArrayList<MyListItem>();
        }
        if (mItems.size() > 0) {
            mItems.clear();
        }
        // first line -> used for creating new entry
        MyListItem item = new MyListItem(getString(R.string.WordNewEntry));
        item.setMacAddress(getString(R.string.MsgNewEntry));
        item.setPortNumber("");
        mItems.add(item);
        // after the second line -> user data
        List<MyListItem> data = mPref.loadEntries();
        mItems.addAll(1, data);
        SetAdapter();
        return mItems.size();
    }
    
    private void sendMagickPacket(String macAddr, int port) {
        mCommThread = new MyCcommunicateThread(getApplicationContext());
        mCommThread.start();
        String [] ar = macAddr.split(":");
        byte [] mac = new byte[6];
        for (int i = 0; i < ar.length; i++) {
            mac[i] = (byte)Integer.parseInt(ar[i], 16);
        }
        // fill 0xFF first 6 bytes
        byte [] data = new byte[6+6*16];
        for (int i = 0; i < 6; i++) {
            data[i] = (byte)0xFF;
        }
        // repeat MAC Address 16 times
        for (int i = 0; i < 16; i++) {
            int ofs = i * 6 + 6;
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
        msg = Message.obtain(mCommThread.getHandler(), R.id.quit);
        msg.sendToTarget();        
    }
    
    private void openEntryDialog(String name, String mac, String port, int itemPos) {
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View entryLayout = inflater.inflate(R.layout.entry, (ViewGroup)findViewById(R.id.entry));        
        final int pos = itemPos;
        final EditText nameFld = (EditText)entryLayout.findViewById(R.id.entryName);
        final EditText mac0Fld = (EditText)entryLayout.findViewById(R.id.mac0);
        final EditText mac1Fld = (EditText)entryLayout.findViewById(R.id.mac1);
        final EditText mac2Fld = (EditText)entryLayout.findViewById(R.id.mac2);
        final EditText mac3Fld = (EditText)entryLayout.findViewById(R.id.mac3);
        final EditText mac4Fld = (EditText)entryLayout.findViewById(R.id.mac4);
        final EditText mac5Fld = (EditText)entryLayout.findViewById(R.id.mac5);
        final EditText portFld = (EditText)entryLayout.findViewById(R.id.port);
        nameFld.setText("");
        mac0Fld.setText("");
        mac1Fld.setText("");
        mac2Fld.setText("");
        mac3Fld.setText("");
        mac4Fld.setText("");
        mac5Fld.setText("");
        portFld.setText("9");

        if (name != null) {
            nameFld.setText(name);
        }
        if (mac != null) {
            String [] ar = mac.split(":");
            try {
                mac0Fld.setText(ar[0]);
                mac1Fld.setText(ar[1]);
                mac2Fld.setText(ar[2]);
                mac3Fld.setText(ar[3]);
                mac4Fld.setText(ar[4]);
                mac5Fld.setText(ar[5]);
            } catch (Exception e) {
            }
        }
        if (port != null) {
            portFld.setText(port);
        }

        // custom dialog box
        ViewGroup vg = (ViewGroup) entryLayout.getParent();
        if (vg != null) {
            vg.removeView(entryLayout);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(entryLayout);
        builder.setIcon(R.drawable.icon);
        builder.setPositiveButton(R.string.WordOK, null);
        builder.setNegativeButton(R.string.WordCancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        final AlertDialog dlg = builder.show();
        
        // Builder#setPositiveButton() automatically close dialog box.
        // Button#setOnClickListener() is not so.
        Button buttonOK = dlg.getButton(DialogInterface.BUTTON_POSITIVE);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            public void onClick( View v ) {
                String entryName = nameFld.getText().toString();
                String macAddress =
                    mac0Fld.getText().toString().toUpperCase() + ":" +
                    mac1Fld.getText().toString().toUpperCase() + ":" +
                    mac2Fld.getText().toString().toUpperCase() + ":" +
                    mac3Fld.getText().toString().toUpperCase() + ":" +
                    mac4Fld.getText().toString().toUpperCase() + ":" +
                    mac5Fld.getText().toString().toUpperCase();
                String portNumber = portFld.getText().toString();
                
                if (!entryName.matches("[A-Za-z0-9]+")) {
                    showDialogMessage(getString(R.string.MsgSpecifyValidName), false);
                    return;
                }
                // the name is NOT in use -> OK
                // the name is in use && that record is being edited -> OK
                // the name is in use && that record is NOT being edited -> NG
                int matchedItemPos = mPref.entryNameIsInUse(entryName);
                if (matchedItemPos != -1 && matchedItemPos != pos) {
                       showDialogMessage(getString(R.string.MsgNameInUse), false);
                       return;
                }
                if (!portNumber.matches("[1-9][0-9]*")) {
                    showDialogMessage(getString(R.string.MsgSpecifyValidPort), false);
                    return;
                    
                }
                int num = Integer.parseInt(portNumber);
                if (num < 0 || num > 65535) {
                    showDialogMessage(getString(R.string.MsgPortOutOfRange), false);
                    return;
                    
                }
                if (!macAddress.matches("[A-F0-9]{2}:[A-F0-9]{2}:[A-F0-9]{2}:[A-F0-9]{2}:[A-F0-9]{2}:[A-F0-9]{2}")) {
                    showDialogMessage(getString(R.string.MsgSpecifyValidMac), false);
                    return;
                }
                mPref.saveEntry(pos, nameFld.getText().toString(), macAddress, portFld.getText().toString());
                buildItemList();
                dlg.dismiss();
            }
        } );        
    }

    private void showDialogMessage(String msg, final boolean bFinish) {
        new AlertDialog.Builder(this).setTitle(R.string.app_name)
                .setIcon(R.drawable.icon).setMessage(msg)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (bFinish) {
                            finish();
                        }
                    }
                }).show();
    }
    
}
