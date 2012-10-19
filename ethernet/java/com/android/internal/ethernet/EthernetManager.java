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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.LinkCapabilities;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkStateTracker;
import android.net.NetworkUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.ethernet.EthernetInfo;
import com.android.internal.ethernet.IEthernetManager;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.net.InetAddress;

/**
 * EthernetManager provides access to ethernet to system apps (such as
 * settings).

 * They can query for and configure any interfaces available on the system. In
 * addition, EthernetManager implements NetworkStateTracker, the generic
 * abstraction of a network type used by ConnectivityService.
 */
public class EthernetManager implements NetworkStateTracker {
    private IEthernetManager mService;
    private static final String TAG = "EthernetManager";
    private static final String PACKAGE = "com.android.internal.ethernet";

    /**
     * Broadcast when the NetworkInfo representing Ethernet as a whole changes.
     */
    public static final String NETWORK_STATE_CHANGED_ACTION =
                   PACKAGE + ".NETWORK_STATE_CHANGED_ACTION";

    /**
     * Broadcast when the NetworkInfo or EthernetInfo representing an
     * individual interface changes.
     */
    public static final String INTERFACE_STATE_CHANGED_ACTION =
                   PACKAGE + ".INTERFACE_STATE_CHANGED_ACTION";

    /**
     * Broadcast when an interface is removed.
     */
    public static final String INTERFACE_REMOVED_ACTION = PACKAGE+".INTERFACE_REMOVED_ACTION";

    public static final String EXTRA_LINK_PROPERTIES   = "EXTRA_LINK_PROPERTIES";
    public static final String EXTRA_ETHERNET_INFO     = "EXTRA_ETHERNET_INFO";
    public static final String EXTRA_INTERFACE_NAME    = "EXTRA_INTERFACE_NAME";
    public static final int DATA_ACTIVITY_NOTIFICATION = 0;
    public static final int DATA_ACTIVITY_NONE         = 1;
    public static final int DATA_ACTIVITY_IN           = 2;
    public static final int DATA_ACTIVITY_OUT          = 3;
    public static final int DATA_ACTIVITY_INOUT        = 4;
    public static final int ENABLE_TRAFFIC_STATS_POLL  = 5;
    public static final int TRAFFIC_STATS_POLL         = 6;

    public EthernetInfo getCurrentInterface() {
        try {
            return mService.getCurrentInterface();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to communicate with EthernetService: " + e.getMessage());
            return null;
        }
    }

    public void updateInterface(EthernetInfo info) {
        try {
            mService.updateInterface(info);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to communicate with EthernetService: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        try {
            return mService.isEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to communicate with EthernetService: " + e.getMessage());
        }
        return false;
    }

    /**
     * Sends callbacks to ConnectivityService.
     */
    private Handler mCsHandler;
    private Context mContext;
    private NetworkInfo mNetworkInfo;
    private LinkProperties mLinkProperties;
    private AtomicBoolean mPrivateDnsRouteSet = new AtomicBoolean(false);
    private AtomicBoolean mDefaultRouteSet = new AtomicBoolean(false);
    private AtomicBoolean mTeardownRequested = new AtomicBoolean(false);

    private class EthernetStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(NETWORK_STATE_CHANGED_ACTION)) {
                EthernetInfo ei = (EthernetInfo) intent.getParcelableExtra(
                        EthernetManager.EXTRA_ETHERNET_INFO);
                mLinkProperties = ei.getLinkProperties();
                mNetworkInfo = ei.getNetworkInfo();
                Message msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED,
                        new NetworkInfo(mNetworkInfo));
                msg.sendToTarget();
            }
        }
    }

    public void startMonitoring(Context context, Handler target) {
        mCsHandler = target;
        mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(NETWORK_STATE_CHANGED_ACTION);
        mContext.registerReceiver(new EthernetStateReceiver(), filter);
    }

    public boolean teardown() {
        try {
            return mService.teardown();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to communicate with EthernetService: " +
            e.getMessage());
        }
        return false;
    }

    @Override
    public void captivePortalCheckComplete() {
        // not implemented
    }

    public boolean reconnect() {
        try {
            return mService.reconnect();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to communicate with EthernetService: " +
            e.getMessage());
        }
        return false;
    }

    public boolean isAvailable() { return mNetworkInfo.isAvailable(); }
    public NetworkInfo getNetworkInfo() { return new NetworkInfo(mNetworkInfo); }
    public LinkProperties getLinkProperties() {
        return new LinkProperties(mLinkProperties);
    }
    // Wifi does the same thing:
    public LinkCapabilities getLinkCapabilities() { return new LinkCapabilities(); }
    public String getTcpBufferSizesPropName() { return "net.tcp.buffersize.ethernet"; }
    public boolean setRadio(boolean turnOn) { return true; }
    public void setUserDataEnable(boolean enabled) { }
    public void setPolicyDataEnable(boolean enabled) { }
    public boolean isPrivateDnsRouteSet() { return mPrivateDnsRouteSet.get(); }
    public void privateDnsRouteSet(boolean enabled) { mPrivateDnsRouteSet.set(enabled); }
    public boolean isDefaultRouteSet() { return mDefaultRouteSet.get(); }
    public void defaultRouteSet(boolean enabled) { mDefaultRouteSet.set(enabled); }
    public boolean isTeardownRequested() { return mTeardownRequested.get(); }
    public void setTeardownRequested(boolean enabled) { mTeardownRequested.set(enabled); }
    public void setDependencyMet(boolean met) { }

    /**
     * Create a new EthernetManager instance.
     * Applications will almost always want to use
     * {@link android.content.Context#getSystemService Context.getSystemService()} to retrieve
     * the standard {@link android.content.Context#ETHERNET_SERVICE Context.ETHERNET_SERVICE}.
     * @param service the Binder interface
     * @hide - hide this because it takes in a parameter of type
     * IEthernetManager, which
     * is a system private class.
     */
    public EthernetManager(IEthernetManager service) {
        mService = service;
        mNetworkInfo = EthernetInfo.newNetworkInfo();
        mLinkProperties = new LinkProperties();
    }

    public Messenger getEthernetServiceMessenger() {
        try {
            return mService.getEthernetServiceMessenger();
        } catch (RemoteException e) {
            return null;
        }
    }
}
