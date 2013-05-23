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

import android.graphics.drawable.Drawable;

public class MyListItem {
    private String mMacAddr;
    private String mPortNumber;
    private String mEntryName;
    private Drawable mIcon;

    public MyListItem(String entryName){
        mEntryName = entryName;
    }
    public void setMacAddress(String macAddr) {
        mMacAddr = macAddr;
    }
    public void setPortNumber(String portNumber) {
        mPortNumber = portNumber;
    }
    public void setIcon(Drawable Icon) {
        mIcon = Icon;
        mIcon.setCallback(null);
    }
    public String getEntryName() {
        return mEntryName;
    }
    public String getMacAddress() {
        return mMacAddr;
    }
    public String getPortNumber() {
        return mPortNumber;
    }
    public Drawable getIcon() {
        return mIcon;
    }
    
}
