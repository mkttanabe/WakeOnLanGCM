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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

public class MyPref {
    private static final String TAG = "WOL";
    private static final String PREFKEY_APP = "App";
    private static final String PREFKEY_ENTRY = "Entry";
    private SharedPreferences mPref = null;
    private Context mCtx;
    
    public MyPref (Context context) {
        mCtx = context;
        init();
    }
    private void init() {
        mPref = mCtx.getSharedPreferences(mCtx.getString(R.string.pref_name), Activity.MODE_PRIVATE);
        mPref.edit().putString(PREFKEY_APP, mCtx.getString(R.string.pref_name)).commit(); // dummy
    }
    
    // Check if the entry name is in use or not 
    public int entryNameIsInUse(String name) {
        List<MyListItem> items = loadEntries();
        int len = items.size();
        for (int i = 0; i < len; i++) {
            MyListItem li = items.get(i);
            if (name.equals(li.getEntryName())) {
                return i; // record position
            }
        }
        return -1; // not in use
    }
    
    // Load all entries
    public List<MyListItem> loadEntries() {
        List<MyListItem> items = new ArrayList<MyListItem>();
        // load data in descending order
        for (int i = Constant.ENTRY_MAX-1; i >= 0; i--) {
            String data = mPref.getString(PREFKEY_ENTRY + String.format("%02d", i), null);
            if (data == null) {
                continue;
            }
            String [] ar = data.split("\t");
            MyListItem li = new MyListItem(ar[0]);
            li.setMacAddress(ar[1]);
            li.setPortNumber(ar[2]);
            items.add(li);
        }
        return items;
    }

    // Get record number of an entry by position
    // -> record number of "Entry01" is 1 
    private int getRecordNumberByItemPos(int itemPos) {
        List<Integer> recNumbers = new ArrayList<Integer>();
        for (int i = Constant.ENTRY_MAX-1; i >= 0; i--) {
            String data = mPref.getString(PREFKEY_ENTRY + String.format("%02d", i), null);
            if (data == null) {
                continue;
            }
            recNumbers.add(i);
        }
        int number = -1;
        try {
            number = recNumbers.get(itemPos);
        } catch (Exception e) {
        }
        return number;
    }
    
    // Save an entry
    public boolean saveEntry(int itemPos, String entryName, String macAddress, String port) {
        String data = entryName + "\t" + macAddress + "\t" + port;
        int num;
        if (itemPos == -1) { // new entry
            // get new record number
            for (num = 0; num < Constant.ENTRY_MAX; num++) {
                String keyStr = PREFKEY_ENTRY + String.format("%02d", num);
                if (mPref.getString(keyStr, null) == null) {
                    break;
                }
            }
            if (num == Constant.ENTRY_MAX) {
                return false; // too many entries
            }
        } else { // existing entry
            num = getRecordNumberByItemPos(itemPos);
        }
        mPref.edit().putString(PREFKEY_ENTRY + String.format("%02d", num), data).commit();        
        return true;
    }
    
    // Remove an entry
    public boolean RemoveEntry(int itemPos) {
        // remove a specified entry 
        int recordNumber = getRecordNumberByItemPos(itemPos);
        if (recordNumber == -1) {
            return false;
        }
        mPref.edit().remove(PREFKEY_ENTRY + String.format("%02d", recordNumber)).commit();
        List<MyListItem> items = loadEntries();
        // remove all entries, and re-create
        for (int i = 0; i < Constant.ENTRY_MAX; i++) {
            String keyStr = PREFKEY_ENTRY + String.format("%02d", i);
            if (mPref.getString(keyStr, null) != null) {
                mPref.edit().remove(keyStr).commit();
            }
        }
        int num = items.size();
        for (int i = num-1; i >= 0; i--) {
            MyListItem li = items.get(i);
            saveEntry(-1, li.getEntryName(), li.getMacAddress(), li.getPortNumber());
        }
        return true;
    }
    
    public String sharedPrefFileName() {
        String pkg = mCtx.getPackageName();
        String prefFileName = mCtx.getString(R.string.pref_name) + ".xml";
        String path1 = "/data/data/" + pkg + "/shared_prefs/";
        String path2 = "/dbdata/databases/" + pkg + "/shared_prefs/"; // Galaxy S ?
        if (new File(path1).exists()) {
            return path1 +  prefFileName;
        } else if (new File(path2).exists()) {
            return path2 + prefFileName;
        }
        return null; // unknown..
    }
    
    public String exportPrefFileName() {
        return Environment.getExternalStorageDirectory() + File.separator +
            mCtx.getString(R.string.exportdata_name); 
    }
    
    public boolean exportPref() {
        return copyFile(sharedPrefFileName(), exportPrefFileName());
    }

    public boolean importPref() {
        boolean ret = copyFile(exportPrefFileName(), sharedPrefFileName());
        if (ret) {
            init();
        }
        return ret;
    }
    
    public boolean copyFile(String srcFileName, String dstFileName) {
        FileChannel fcSrc = null, fcDst = null;
        boolean ret = true;
        try {
            fcSrc = new FileInputStream(srcFileName).getChannel();
            fcDst = new FileOutputStream(dstFileName).getChannel();
            fcSrc.transferTo(0, fcSrc.size(), fcDst);
        } catch (Exception e) {
            ret = false;
            _Log.e(TAG, "pref: copyFile: err=" + e.toString());
        }
        try {
            if (fcSrc != null) {
                fcSrc.close();
            }
            if (fcDst != null) {
                fcDst.close();
            }
        } catch (IOException e) {
        }
        return ret;
    }
}
