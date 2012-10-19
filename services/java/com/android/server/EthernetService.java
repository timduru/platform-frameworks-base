/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.server;

import static com.android.internal.util.Preconditions.checkNotNull;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.DhcpStateMachine;
import android.net.INetworkManagementEventObserver;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkUtils;
import android.net.ProxyProperties;
import android.net.RouteInfo;
import android.net.TrafficStats;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Slog;

import com.android.internal.ethernet.EthernetInfo;
import com.android.internal.ethernet.EthernetManager;
import com.android.internal.ethernet.EthernetStateMachine;
import com.android.internal.ethernet.IEthernetManager;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.StateMachine;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.Runnable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class EthernetService extends IEthernetManager.Stub {
    private static final String TAG = "EthernetService";
    private static final boolean DBG = true;
    private static final String PERSIST_FILE = Environment.getDataDirectory() +
            "/misc/ethernet_interfaces.json";
    private static final int POLL_TRAFFIC_STATS_INTERVAL_MSECS = 1000;
    private INetworkManagementService mNetd;
    private Context mContext;
    private EthernetStateMachine mAvailableInterface;
    private HashMap<String, EthernetInfo> mUnavailableInterfaces =
            new HashMap<String, EthernetInfo>();
    private List<AsyncChannel> mClients = new ArrayList<AsyncChannel>();
    private boolean mEnableTrafficStatsPoll = false;
    private int mTrafficStatsPollToken = 0;
    private long mTxPkts;
    private long mRxPkts;
    private int mDataActivity;
    private boolean mScreenOff;

    private class AsyncServiceHandler extends Handler {

        AsyncServiceHandler(android.os.Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AsyncChannel.CMD_CHANNEL_HALF_CONNECTED: {
                    if (msg.arg1 == AsyncChannel.STATUS_SUCCESSFUL) {
                        if (DBG) Slog.d(TAG, "New client listening to asynchronous messages");
                        mClients.add((AsyncChannel) msg.obj);
                    } else {
                        Slog.e(TAG, "Client connection failure, error=" + msg.arg1);
                    }
                    break;
                }
                case AsyncChannel.CMD_CHANNEL_DISCONNECTED: {
                    if (msg.arg1 == AsyncChannel.STATUS_SEND_UNSUCCESSFUL) {
                        if (DBG) Slog.d(TAG, "Send failed, client connection lost");
                    } else {
                        if (DBG) Slog.d(TAG, "Client connection lost with reason: " + msg.arg1);
                    }
                    mClients.remove((AsyncChannel) msg.obj);
                    break;
                }
                case AsyncChannel.CMD_CHANNEL_FULL_CONNECTION: {
                    AsyncChannel ac = new AsyncChannel();
                    ac.connect(mContext, this, msg.replyTo);
                    break;
                }
                case EthernetManager.ENABLE_TRAFFIC_STATS_POLL: {
                    mEnableTrafficStatsPoll = (msg.arg1 == 1);
                    mTrafficStatsPollToken++;
                    if (mEnableTrafficStatsPoll) {
                        notifyOnDataActivity();
                        sendMessageDelayed(Message.obtain(this, EthernetManager.TRAFFIC_STATS_POLL,
                                mTrafficStatsPollToken, 0), POLL_TRAFFIC_STATS_INTERVAL_MSECS);
                    }
                    break;
                }
                case EthernetManager.TRAFFIC_STATS_POLL: {
                    if (msg.arg1 == mTrafficStatsPollToken) {
                        notifyOnDataActivity();
                        sendMessageDelayed(Message.obtain(this, EthernetManager.TRAFFIC_STATS_POLL,
                                mTrafficStatsPollToken, 0), POLL_TRAFFIC_STATS_INTERVAL_MSECS);
                    }
                    break;
                }
                default: {
                    if (DBG) Slog.d(TAG, "EthernetServicehandler.handleMessage ignoring msg=" + msg);
                    break;
                }
            }
        }
    }

    private AsyncServiceHandler mAsyncServiceHandler;

    private class NetworkManagementEventObserver extends INetworkManagementEventObserver.Stub {
        public void interfaceAdded(String iface) {
            if(DBG) Slog.d(TAG, "interfaceAdded: " + iface);
            addInterface(iface);
        }
        public void interfaceRemoved(String iface) {
            if(DBG) Slog.d(TAG, "interfaceRemoved: " + iface);
            removeInterface(iface);
        }
        public void limitReached(String limitName, String iface) {}
        public void interfaceClassDataActivityChanged(String label, boolean active) {}
        public void interfaceLinkStateChanged(String iface, boolean up) {
            if(DBG) Slog.d(TAG, "interfaceLinkStateChanged for " + iface + ", up = " + up);
            if (mAvailableInterface != null && up) {
                //sendMessage(mAvailableInterface, 
                //EthernetStateMachine.CMD_LINK_UP);
            }
        }
        public void interfaceStatusChanged(String iface, boolean up) {
            if(DBG) Slog.d(TAG, "interfaceStatusChanged for " + iface + ", up = " + up);
            //addInterface(iface);
        }
    }

    private String getUtilityInterface() {
        String retval = SystemProperties.get("persist.sys.utility_iface");
        if(DBG) Slog.d(TAG, "getUtilityInterface: "  + retval);
        return retval;
    }

    private class InterfaceStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(EthernetManager.INTERFACE_STATE_CHANGED_ACTION)) {
                EthernetInfo ei = intent.getParcelableExtra(
                        EthernetManager.EXTRA_ETHERNET_INFO);
                String name = ei.getName();
                if(DBG) Slog.d(TAG, "INTERFACE_STATE_CHANGED_ACTION: "  + name);

                // If it's the utility interface or not our active interface,
                // do nothing:
                if (name.equals(getUtilityInterface())
                        || mAvailableInterface == null
                        || ! name.equals(mAvailableInterface.getInfo().getName())) {
                    return;
                }

                // Forward it up to ConnectivityService (via EthernetManager)
                // Don't forward events for inactive interface
                Intent newIntent = new Intent(EthernetManager.NETWORK_STATE_CHANGED_ACTION);
                newIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                newIntent.putExtra(EthernetManager.EXTRA_ETHERNET_INFO, ei);
                if(DBG) Slog.d(TAG, "Sending EthernetManager.NETWORK_STATE_CHANGED_ACTION");
                mContext.sendBroadcast(newIntent);

                saveConfig();
                evaluateTrafficStatsPolling();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                mScreenOff = false;
                evaluateTrafficStatsPolling();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                mScreenOff = true;
                evaluateTrafficStatsPolling();
            }
        }
    }

    private void removeInterface(String iface) {
        if(DBG) Slog.d(TAG, "removeInterface: " + iface);

        if ( ! iface.equals(mAvailableInterface.getInfo().getName())) {
            if(DBG) Slog.d(TAG, "removeInterface: won't remove " + iface);
            return;
        }

        if (mAvailableInterface == null) {
            if(DBG) Slog.d(TAG, "removeInterface: won't remove " + iface + " because mAvailableInterface == null");
            return;
        }

        EthernetInfo info = mAvailableInterface.getInfo();
        // info.isEnabled() is still true here and that is what we want. On
        // device suspend, USB ethernet devices will come through this
        // path. We want any enabled device to remain enabled after
        // resuming from suspend and thus we leave its EthernetInfo object
        // in the enabled state when putting it into
        // mUnavailableInterfaces.
        sendMessage(mAvailableInterface, EthernetStateMachine.CMD_INTERFACE_GONE, info);
        sendMessage(mAvailableInterface, EthernetStateMachine.CMD_SHUTDOWN, info);
        mUnavailableInterfaces.put(info.getHwAddress(), info);
        mAvailableInterface = null;

        saveConfig();

        Intent newIntent = new Intent(EthernetManager.INTERFACE_REMOVED_ACTION);
        newIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        newIntent.putExtra(EthernetManager.EXTRA_INTERFACE_NAME, iface);
        mContext.sendBroadcast(newIntent);
    }

    private void addInterface(String iface) {
        if(DBG) Slog.d(TAG, "addInterface: " + iface);

        String eth_regex = mContext.getResources().getString(
            com.android.internal.R.string.config_ethernet_iface_regex);

        // If it's not an ethernet interface, or it is the utility interface,
        // do nothing:
        if ( ! iface.matches(eth_regex)
                || iface.equals(getUtilityInterface())) {
            if(DBG) Slog.d(TAG, "addInterface: I cannot manage " + iface);
            return;
        }

        String hwAddr = getHwAddr(iface);
        EthernetStateMachine sm;

        if (mUnavailableInterfaces.containsKey(hwAddr)) {
            // We've seen this interface before
            if(DBG) Slog.d(TAG, "Found " + iface + " in mUnavailableInterfaces");
            if (mAvailableInterface == null) { // Nothing currently active
                EthernetInfo ei = mUnavailableInterfaces.get(hwAddr);

                // Interface name may have changed since last time due to
                // device insertion order, etc.:
                ei.setName(iface);

                ei.setInterfaceStatus(EthernetInfo.InterfaceStatus.ENABLED);
                sm = new EthernetStateMachine(mContext, ei);
                mUnavailableInterfaces.remove(hwAddr);
                mAvailableInterface = sm;
            }
        } else {
            // This is a new interface
            if(DBG) Slog.d(TAG, "Found new interface " + iface + " at " + hwAddr);
            EthernetInfo ei = new EthernetInfo(iface, hwAddr);

            if (mAvailableInterface == null) {
                mAvailableInterface = new EthernetStateMachine(mContext, ei);
                if (mUnavailableInterfaces.isEmpty()) {
                    if(DBG) Slog.d(TAG, "New interface " + iface
                            + " is first interface we've ever seen.");
                    ei.enable();
                    ei.setIpAssignment(EthernetInfo.IpAssignment.DHCP);
                    sendMessage(mAvailableInterface,
                            EthernetStateMachine.CMD_UPDATE_INTERFACE,
                            ei);
                }
            } else {
                mUnavailableInterfaces.put(hwAddr, ei);
            }
        }

        saveConfig();
    }

    private void saveConfig() {
        if(DBG) Slog.d(TAG, "Storing Ethernet interface configs to disk.");
        JsonWriter writer = null;
        try {
            FileOutputStream fos = new FileOutputStream(PERSIST_FILE);
            writer = new JsonWriter(new OutputStreamWriter(fos, "UTF-8"));
            writer.setIndent("  ");
            writer.beginArray();
            if (mAvailableInterface != null) {
                mAvailableInterface.getInfo().write(writer);
            }
            for (EthernetInfo info : mUnavailableInterfaces.values()) {
                info.write(writer);
            }
            writer.endArray();
        } catch (Exception e) {
            Slog.e(TAG, "Failed to store ethernet config: " + e);
        } finally {
            try { writer.close(); } catch(Exception e) {}
        }
    }

    public EthernetService(Context context) {
        mContext = context;
        mNetd = INetworkManagementService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE)
        );

        try {
            mNetd.registerObserver(new NetworkManagementEventObserver());
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote NetworkManagementService error: " + e);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(EthernetManager.INTERFACE_STATE_CHANGED_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(new InterfaceStateReceiver(), filter);

        HandlerThread ethernetThread = new HandlerThread("EthernetService");
        ethernetThread.start();
        mAsyncServiceHandler = new AsyncServiceHandler(ethernetThread.getLooper());

        try {
            if(DBG) Slog.d(TAG, "Reading " + PERSIST_FILE);
            FileInputStream fis = new FileInputStream(PERSIST_FILE);
            JsonReader reader = new JsonReader(new InputStreamReader(fis, "UTF-8"));
            reader.beginArray();
            while (reader.hasNext()) {
                EthernetInfo newInfo = new EthernetInfo(reader);
                if(DBG) Slog.d(TAG, "Found " + newInfo.getName() + " in " + PERSIST_FILE);
                mUnavailableInterfaces.put(newInfo.getHwAddress(), newInfo);
            }
            reader.endArray();
            reader.close();
        } catch (IOException e) {
            Slog.i(TAG, "While reading interface config: " + e.toString());
        } catch (IllegalStateException e) {
            Slog.e(TAG, "Invalid JSON in " + PERSIST_FILE + ": " + e);
            File f = new File(PERSIST_FILE);
            try { f.delete(); } catch (Exception ex) {}
        }

        for (String iface : getInterfaceNames()) {
            addInterface(iface);
        }

        if(DBG) Slog.d(TAG, "EthernetService initialized");
    }

    public EthernetInfo getCurrentInterface() {
        return mAvailableInterface == null ? null : mAvailableInterface.getInfo();
    }

    public boolean teardown() {
        if(DBG) Slog.d(TAG, "Teardown requested.");

        if (mAvailableInterface == null) {
            return true;
        }

        EthernetInfo info = mAvailableInterface.getInfo();
        if(DBG) Slog.d(TAG, "Requesting interface shut down for " + info.getName());
        sendMessage(mAvailableInterface, EthernetStateMachine.CMD_SHUTDOWN, info);
        return true;
    }

    public boolean isEnabled() {
        if (mAvailableInterface == null) {
            return false;
        }
        return mAvailableInterface.isEnabled();
    }

    public boolean reconnect() {
        if (isEnabled()) {
            return true;
        }
        if (mAvailableInterface == null) {
            return false;
        }
        if(DBG) Slog.d(TAG, "Reconnecting " + mAvailableInterface.getInfo().getName());
        EthernetInfo newInfo = mAvailableInterface.getInfo();
        newInfo.enable();
        sendMessage(mAvailableInterface,
                EthernetStateMachine.CMD_UPDATE_INTERFACE,
                newInfo);
        return true;
    }

    public void updateInterface(EthernetInfo newInfo) {
        if (newInfo == null) {
            Slog.e(TAG, "Null EthernetInfo");
            return;
        }
        if (mAvailableInterface == null) {
            Slog.e(TAG, "Unable to find statemachine for interface " + newInfo.getName());
            return;
        }

        sendMessage(mAvailableInterface,
                EthernetStateMachine.CMD_UPDATE_INTERFACE,
                newInfo);

        if(DBG) Slog.d(TAG, newInfo.getName() + " updateInterface done");
    }

    private void sendMessage(StateMachine sm, int what, Parcelable obj) {
        Message msg = sm.obtainMessage(what);
        msg.obj = obj;
        sm.sendMessage(msg);
    }

    private void sendMessage(StateMachine sm, int what) {
        Message msg = sm.obtainMessage(what);
        sm.sendMessage(msg);
    }

    private String getHwAddr(String ifaceName) {
        try {
            InterfaceConfiguration ic = mNetd.getInterfaceConfig(ifaceName);
            return ic.getHardwareAddress();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote NetworkManagementService error: " + e);
        }
        return "";
    }

    private Set<String> getInterfaceNames() {
        // () Call the NetworkManagementService to list interfaces that exist
        //    now
        // () filter these to the set of ones matching the ethernet interface
        //    regular expression
        Set<String> sNames = new HashSet<String>();
        String eth_regex = mContext.getResources().getString(
            com.android.internal.R.string.config_ethernet_iface_regex);
        String[] ifaces;
        try {
            ifaces = mNetd.listInterfaces();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote NetworkManagementService error " + e);
            ifaces = new String[0];
        }
        for (String iface : ifaces) {
            if (iface.matches(eth_regex)) {
                if(DBG) Slog.d(TAG, "Found " + iface + " in getInterfaceNames.");
                sNames.add(iface);
            } else {
                if(DBG) Slog.d(TAG, "Found and filtered " + iface + " in getInterfaceNames.");
            }
        }
        return sNames;
    }

    public Messenger getEthernetServiceMessenger() {
        return new Messenger(mAsyncServiceHandler);
    }

    private void notifyOnDataActivity() {
        long sent, received;
        long preTxPkts = mTxPkts, preRxPkts = mRxPkts;
        int dataActivity = EthernetManager.DATA_ACTIVITY_NONE;

        if (mAvailableInterface != null) {
            mTxPkts = TrafficStats.getTxPackets(mAvailableInterface.getInfo().getName());
            mRxPkts = TrafficStats.getRxPackets(mAvailableInterface.getInfo().getName());
        }

        if (preTxPkts > 0 || preRxPkts > 0) {
            sent = mTxPkts - preTxPkts;
            received = mRxPkts - preRxPkts;
            if (sent > 0) {
                dataActivity |= EthernetManager.DATA_ACTIVITY_OUT;
            }
            if (received > 0) {
                dataActivity |= EthernetManager.DATA_ACTIVITY_IN;
            }

            if (dataActivity != mDataActivity && !mScreenOff) {
                mDataActivity = dataActivity;
                for (AsyncChannel client : mClients) {
                    client.sendMessage(EthernetManager.DATA_ACTIVITY_NOTIFICATION, mDataActivity);
                }
            }
        }
    }

    private DetailedState getDetailedState() {
        if (mAvailableInterface != null) {
            return mAvailableInterface.getInfo().getDetailedState();
        }
        if (DBG) Slog.d(TAG, "getDetailedState: state is DISCONNECTED");
        return DetailedState.DISCONNECTED;
    }

    private void evaluateTrafficStatsPolling() {
        Message msg;
        if (getDetailedState() == DetailedState.CONNECTED && !mScreenOff) {
            msg = Message.obtain(mAsyncServiceHandler,
                    EthernetManager.ENABLE_TRAFFIC_STATS_POLL, 1, 0);
        } else {
            msg = Message.obtain(mAsyncServiceHandler,
                    EthernetManager.ENABLE_TRAFFIC_STATS_POLL, 0, 0);
        }
        msg.sendToTarget();
    }
}

