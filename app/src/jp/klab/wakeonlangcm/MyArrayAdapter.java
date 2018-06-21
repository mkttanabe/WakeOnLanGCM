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

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

// ArrayAdapter for ListView
public class MyArrayAdapter extends ArrayAdapter<MyListItem> {
    private static final String TAG = "WOL";
    private Context mContext;
    private int mViewSize;
    private LayoutInflater mInflater;
    private int mTextViewResId;
    private List<MyListItem> mItems;

    public MyArrayAdapter(Context ctx, int textViewResourceId, List<MyListItem> items, int viewSize) {
        super(ctx, textViewResourceId, items);
        mContext = ctx;
        mViewSize = viewSize;
        if (mViewSize > 2) {
            mViewSize = 2;
        } else if (mViewSize < 0) {
            mViewSize = 0;
        }
        mTextViewResId = textViewResourceId;
        mItems = items;
        mInflater =
            (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent){
        View view = convertView;
        if (view == null) { // create a view if not exists
            view = mInflater.inflate(mTextViewResId, null);
        }
        // get target entry
        MyListItem item = mItems.get(pos);
        // entry name
        TextView tvEntryName = (TextView)view.findViewWithTag("entryName");
        tvEntryName.setTextSize(Constant.ListLabelSize[mViewSize]);
        tvEntryName.setTextColor(Color.WHITE);
        tvEntryName.setText(item.getEntryName());
        // MAC address
        TextView tvMacAddress = (TextView)view.findViewWithTag("macAddress");
        tvMacAddress.setTextSize(Constant.ListDescSize[mViewSize]);
        // icon
        ImageView imageView = (ImageView)view.findViewWithTag("icon");
        
        if (pos == 0) { // first line is used for creating a new entry 
            tvMacAddress.setText(item.getMacAddress());
            imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.add));
        } else {
            tvMacAddress.setText(item.getMacAddress() + "   port=" + item.getPortNumber());
            imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.pc));
        }
        return view;
    }
}
