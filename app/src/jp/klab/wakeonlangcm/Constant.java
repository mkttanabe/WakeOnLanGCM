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

public final class Constant {
    static final String SENDER_ID = "**SENDER_ID**";
    static final String MYACTION_NOTIFY = "jp.klab.wakeonlangcm.NOTIFICATION";
    static final String MYEXTRA_NAME_EVENT = "EVENT";    
    static final String MYEXTRA_NAME_REGID = "REGID";
    static final String MYEXTRA_NAME_ERRORID = "ERRORID";
    static final String MYEXTRA_EVENT_REGISTERED = "REGISTERED";
    static final String MYEXTRA_EVENT_UNREGISTERED = "UNREGISTERED";
    static final String MYEXTRA_EVENT_ERROR= "ERROR";
    static final int ENTRY_MAX = 100;
    
    static final int [] ListLabelSize  = {12, 16, 20, 24};
    static final int [] ListDescSize   = { 8, 12, 16, 20};
    
    static final int SizeSmall       = 0;
    static final int SizeMedium      = 1;
    static final int SizeLarge       = 2;
    static final int SizeExtraLarge  = 3;

}
