/**
 * Copyright (c) 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.ethernet;

import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.ethernet.EthernetInfo;

/**
 * Interface that allows Ethernet device discovery as well as querying
 * and setting Ethernet configuration information.
 * {@hide}
 */
interface IEthernetManager
{
    EthernetInfo getCurrentInterface();
    void updateInterface(in EthernetInfo info);
    boolean teardown();
    boolean reconnect();
    boolean isEnabled();
    Messenger getEthernetServiceMessenger();
}
