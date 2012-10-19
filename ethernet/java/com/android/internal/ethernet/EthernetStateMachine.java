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

package com.android.internal.ethernet;

import java.net.InetAddress;
import java.util.ArrayList;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfoInternal;
import android.net.DhcpStateMachine;
import android.net.DhcpStateMachine;
import android.net.InterfaceConfiguration;
import android.net.LinkProperties;
import android.net.NetworkInfo.DetailedState;
import android.net.RouteInfo;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import com.android.internal.util.Protocol;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

public class EthernetStateMachine extends StateMachine {
    private static final String TAG = "EthernetStateMachine";
    private static final boolean DBG = true;
    public static final int BASE = Protocol.BASE_ETHERNET;
    public static final int CMD_UPDATE_INTERFACE = BASE + 1;
    public static final int CMD_SHUTDOWN = BASE + 2;
    public static final int CMD_INTERFACE_GONE = BASE + 3;
    private Context mContext;
    private EthernetInfo mEthernetInfo;
    private DhcpStateMachine mDhcpStateMachine;
    private State mRootState = new RootState();
    private State mIdleState = new IdleState();
    //private State mObtainingLinkState = new ObtainingLinkState();
    private State mObtainingIpState = new ObtainingIpState();
    private State mIPConnectedState = new IPConnectedState();
    private State mDisconnectingState = new DisconnectingState();
    private INetworkManagementService mNetd;
    private boolean mInterfaceGone = false;

    public EthernetStateMachine(Context context, String ifaceName, String hwAddr) {
        super(TAG);
        mEthernetInfo = new EthernetInfo(ifaceName, hwAddr);
        initialize(context);
    }

    public EthernetStateMachine(Context context, EthernetInfo info) {
        super(TAG);
        mEthernetInfo = info;
        initialize(context);
    }

    private void initialize(Context context) {
        mContext = context;
        mNetd = INetworkManagementService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
        addState(mRootState);
            addState(mIdleState, mRootState);
            //addState(mObtainingLinkState, mRootState);
            addState(mObtainingIpState, mRootState);
            addState(mIPConnectedState, mRootState);
            addState(mDisconnectingState, mRootState);

        setInitialState(mIdleState);

        mDhcpStateMachine = DhcpStateMachine.makeDhcpStateMachine(
                mContext, EthernetStateMachine.this,
                mEthernetInfo.getName());

        sendInterfaceStateChangedBroadcast();
        start();
    }

    public EthernetInfo getInfo() {
        return new EthernetInfo(mEthernetInfo);
    }

    private void sendInterfaceStateChangedBroadcast() {
        if (DBG) Slog.d(TAG, "Sending INTERFACE_STATE_CHANGED_ACTION for "
                + mEthernetInfo.getName());
        if (!mInterfaceGone) {
            Intent intent = new Intent(EthernetManager.INTERFACE_STATE_CHANGED_ACTION);
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            intent.putExtra(EthernetManager.EXTRA_ETHERNET_INFO, new EthernetInfo(mEthernetInfo));
            mContext.sendBroadcast(intent);
        }
    }

    private void setNetworkDetailedState(DetailedState state) {
        if (DBG) Slog.d(TAG, mEthernetInfo.getName() + " setDetailed state, old ="
                + mEthernetInfo.getDetailedState() + " and new state=" + state);
        if (state != mEthernetInfo.getDetailedState()) {
            mEthernetInfo.setDetailedState(state, null, null);
            mEthernetInfo.setIsAvailable(true);
            sendInterfaceStateChangedBroadcast();
        }
    }

    public boolean isEnabled() {
        return mEthernetInfo.isEnabled();
    }

    class RootState extends State {
        @Override
        public boolean processMessage(Message message) {
            if (DBG) Slog.d(TAG, mEthernetInfo.getName()
                    + " RootState.processMessage: " + message.toString());
            switch (message.what) {
                case CMD_UPDATE_INTERFACE:
                    if (DBG) Slog.d(TAG, "RootState: received CMD_UPDATE_INTERFACE");
                    EthernetInfo newInfo = (EthernetInfo) message.obj;
                    if (newInfo != mEthernetInfo) {
                        if (mEthernetInfo.isEnabled()) {
                            setNetworkDetailedState(DetailedState.DISCONNECTING);
                            transitionTo(mDisconnectingState);
                        } else {
                            transitionTo(mIdleState);
                        }
                        mEthernetInfo = newInfo;
                    }
                    return HANDLED;
                case CMD_SHUTDOWN:
                    shutdown();
                case CMD_INTERFACE_GONE:
                    mInterfaceGone = true;
                default:
                    Slog.e(TAG, mEthernetInfo.getName()
                            + " Unhandled message in EthernetStateMachine: "
                            + message.toString());
                    return NOT_HANDLED;
            }
        }
    }

    private void shutdown() {
        mEthernetInfo.disable();
        setNetworkDetailedState(DetailedState.DISCONNECTING);
        transitionTo(mDisconnectingState);
    }

    class IdleState extends State {
        @Override
        public void enter() {
            if (DBG) Slog.d(TAG, mEthernetInfo.getName() + " Entered IdleState");
            if (mEthernetInfo.isEnabled()) {
                if (DBG) Slog.d(TAG, mEthernetInfo.getName()
                        + " is enabled in IdleState.enter()");
                setNetworkDetailedState(DetailedState.OBTAINING_IPADDR);
                transitionTo(mObtainingIpState);
                //setNetworkDetailedState(DetailedState.SCANNING);
                //transitionTo(mObtainingLinkState);
            }
        }
        public boolean processMessage(Message message) {
            switch (message.what) {
                case CMD_SHUTDOWN:
                    return HANDLED;
                default:
                    return NOT_HANDLED;
            }
        }
    }

    /*
    class ObtainingLinkState extends State {
        @Override
        public void enter() {
            if (DBG) Slog.d(TAG, mEthernetInfo.getName() + " Entered ObtainingLinkState");
            String ifaceName = mEthernetInfo.getName();
            try {
                InterfaceConfiguration config =
                    mNetd.getInterfaceConfig(ifaceName);
                config.setInterfaceUp();
                mNetd.setInterfaceConfig(ifaceName, config);
            } catch (RemoteException re) {
                Slog.e(TAG, "Failed to bring up " + mEthernetInfo.getName()
                        + ": " + re);
            } catch (IllegalStateException e) {
                Slog.e(TAG, "Failed to bring up " + mEthernetInfo.getName()
                        + ": " + e);
            }
            shutdown();
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) Slog.d(TAG, mEthernetInfo.getName()
                    + " ObtainingLinkState.processMessage: " + message.toString());
            switch (message.what) {
                case CMD_LINK_UP:
                    setNetworkDetailedState(DetailedState.OBTAINING_IPADDR);
                    transitionTo(mObtainingIpState);
                    return HANDLED;
                default:
                    return NOT_HANDLED;
            }
        }
    }
    */

    class ObtainingIpState extends State {
        @Override
        public void enter() {
            if (DBG) Slog.d(TAG, mEthernetInfo.getName() + " Entered ObtainingIpState");
            String ifaceName = mEthernetInfo.getName();
            if (mEthernetInfo.isStaticIpAssignment()) {
                try {
                    InterfaceConfiguration config =
                        mNetd.getInterfaceConfig(ifaceName);
                    config.setLinkAddress(mEthernetInfo.getLinkAddress());
                    config.setInterfaceUp();
                    mNetd.setInterfaceConfig(ifaceName, config);
                    if (DBG) Slog.d(TAG, mEthernetInfo.getName()
                            + " Transitioning to IPConnectedState");
                    setNetworkDetailedState(DetailedState.CONNECTED);
                    transitionTo(mIPConnectedState);
                    return;
                } catch (RemoteException re) {
                    Slog.e(TAG, mEthernetInfo.getName()
                            + " Static IP configuration failed: " + re);
                } catch (IllegalStateException e) {
                    Slog.e(TAG, mEthernetInfo.getName()
                            + " Static IP configuration failed: " + e);
                }
                shutdown();
            } else {
                if (DBG) Slog.d(TAG, "Starting DhcpStateMachine for " + mEthernetInfo.getName());
                mDhcpStateMachine.sendMessage(DhcpStateMachine.CMD_START_DHCP);
            }
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) Slog.d(TAG, mEthernetInfo.getName()
                    + " ObtainingIpState.processMessage: " + message.toString());
            switch (message.what) {
                case DhcpStateMachine.CMD_POST_DHCP_ACTION:
                    if (message.arg1 == DhcpStateMachine.DHCP_SUCCESS) {
                        dhcpSuccess((DhcpInfoInternal) message.obj);
                        transitionTo(mIPConnectedState);
                    } else if (message.arg1 == DhcpStateMachine.DHCP_FAILURE) {
                        if (DBG) Slog.d(TAG, mEthernetInfo.getName() + " DHCP failed; retrying in 10s");
                        mDhcpStateMachine.sendMessageDelayed(DhcpStateMachine.CMD_START_DHCP, 10000);
                    }
                    return HANDLED;
                default: return NOT_HANDLED;
            }
        }
    }

    void dhcpSuccess(DhcpInfoInternal di) {
        if (DBG) Slog.d(TAG, mEthernetInfo.getName() + " DHCP successful");
        LinkProperties lp = di.makeLinkProperties();
        lp.setHttpProxy(mEthernetInfo.getHttpProxy());
        lp.setInterfaceName(mEthernetInfo.getName());
        if (!lp.equals(mEthernetInfo.getLinkProperties())) {
            Slog.i(TAG, "Link configuration changed for: " + mEthernetInfo.getName()
                    + " old: " + mEthernetInfo.getLinkProperties() + "new: " + lp);
            mEthernetInfo.setLinkProperties(lp);
        }
        setNetworkDetailedState(DetailedState.CONNECTED);
    }

    class IPConnectedState extends State {
        public void enter() {
            if (DBG) Slog.d(TAG, mEthernetInfo.getName() + " Entered IPConnectedState");
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) Slog.d(TAG, mEthernetInfo.getName()
                    + " IPConnectedState.processMessage: " + message.toString());
            switch (message.what) {
                case CMD_UPDATE_INTERFACE:
                    if (DBG) Slog.d(TAG, "IPConnectedState: received CMD_UPDATE_INTERFACE");
                    EthernetInfo newInfo = (EthernetInfo) message.obj;
                    if (!newInfo.equals(mEthernetInfo)) {
                        setNetworkDetailedState(DetailedState.DISCONNECTING);
                        transitionTo(mDisconnectingState);
                        mEthernetInfo = newInfo;
                    }
                    return HANDLED;

                case DhcpStateMachine.CMD_POST_DHCP_ACTION:
                    if (message.arg1 == DhcpStateMachine.DHCP_SUCCESS) {
                        dhcpSuccess((DhcpInfoInternal) message.obj);
                        setNetworkDetailedState(DetailedState.CONNECTED);
                    } else if (message.arg1 == DhcpStateMachine.DHCP_FAILURE) {
                        if (DBG) Slog.d(TAG, mEthernetInfo.getName() + " DHCP failed");
                        shutdown();
                    }
                    return HANDLED;
                default: return NOT_HANDLED;
            }
        }
    }

    class DisconnectingState extends State {
        public void enter() {
            if (DBG) Slog.d(TAG, mEthernetInfo.getName() + " Entered DisconnectingState");
            try {
                mNetd.setInterfaceDown(mEthernetInfo.getName());
            } catch (RemoteException re) {
                Slog.e(TAG, mEthernetInfo.getName() + " Failed to bring down interface: " + re);
            } catch (IllegalStateException ise) {
                // Oftentimes interface may not exist anymore:
                if (DBG) Slog.d(TAG, "Failed to bring down "
                        + mEthernetInfo.getName() + ": " + ise);
            }
            mDhcpStateMachine.sendMessage(DhcpStateMachine.CMD_STOP_DHCP);
            setNetworkDetailedState(DetailedState.DISCONNECTED);
            transitionTo(mIdleState);
        }
    }
}
