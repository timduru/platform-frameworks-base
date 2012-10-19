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

import android.net.ConnectivityManager;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.ProxyProperties;
import android.net.RouteInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ServiceManager;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Slog;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class EthernetInfo implements Parcelable, Comparable<EthernetInfo> {
    private static final String TAG = "EthernetInfo";

    public enum IpAssignment {
        /* Use statically configured IP settings. Configuration can be accessed
         * with linkProperties */
        STATIC,
        /* Use dynamically configured IP settigns */
        DHCP
    }

    private IpAssignment mIpAssignment;

    public enum ProxySettings {
        /* No proxy is to be used. Any existing proxy settings
         * should be cleared. */
        NONE,
        /* Use statically configured proxy. Configuration can be accessed
         * with linkProperties */
        STATIC,
        /* no proxy details are assigned, this is used to indicate
         * that any existing proxy settings should be retained */
        UNASSIGNED
    }

    private ProxySettings mProxySettings;
    public enum InterfaceStatus { DISABLED, ENABLED }
    private InterfaceStatus mInterfaceStatus;
    private LinkProperties mLinkProperties;
    private NetworkInfo mNetworkInfo;
    private String mHwAddress;

    public EthernetInfo() {
        mInterfaceStatus = InterfaceStatus.DISABLED;
        mIpAssignment = IpAssignment.DHCP;
        mProxySettings = ProxySettings.UNASSIGNED;
        mLinkProperties = new LinkProperties();
        mNetworkInfo = newNetworkInfo();
        mHwAddress = "";
    }

    public static NetworkInfo newNetworkInfo() {
        return new NetworkInfo(ConnectivityManager.TYPE_ETHERNET,
                                       0, /*subtype*/
                                       "Ethernet", /*name*/
                                       "" /*subtype name*/
                                       );
    }

    public EthernetInfo(String ifaceName, String hwAddress) {
        mInterfaceStatus = InterfaceStatus.DISABLED;
        mIpAssignment = IpAssignment.DHCP;
        mProxySettings = ProxySettings.UNASSIGNED;
        mLinkProperties = new LinkProperties();
        mLinkProperties.setInterfaceName(ifaceName);
        mNetworkInfo = newNetworkInfo();
        mHwAddress = hwAddress;
    }

    public EthernetInfo(EthernetInfo source) {
        mInterfaceStatus = source.mInterfaceStatus;
        mIpAssignment = source.mIpAssignment;
        mProxySettings = source.mProxySettings;
        mLinkProperties = new LinkProperties(source.mLinkProperties);
        mNetworkInfo = new NetworkInfo(source.mNetworkInfo);
        mHwAddress = source.mHwAddress;
    }

    public EthernetInfo(JsonReader reader) throws IOException {
        mInterfaceStatus = InterfaceStatus.DISABLED;
        mIpAssignment = IpAssignment.DHCP;
        mProxySettings = ProxySettings.UNASSIGNED;
        mLinkProperties = new LinkProperties();
        mNetworkInfo = newNetworkInfo();
        mHwAddress = "";
        mLinkProperties.setInterfaceName("foobar");
        String proxy_host = null;
        int proxy_port = -1;
        String proxy_exclusion = null;
        String ip_address = null;
        int netmask = -1;
        String dns1 = null;
        String dns2 = null;
        String default_route = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            if (key.equals("interface_status")) {
                setInterfaceStatus(InterfaceStatus.values()[reader.nextInt()]);
            } else if (key.equals("ip_assignment")) {
                setIpAssignment(IpAssignment.values()[reader.nextInt()]);
            } else if (key.equals("proxy_settings")) {
                setProxySettings(ProxySettings.values()[reader.nextInt()]);
            } else if (key.equals("interface_name")) {
                mLinkProperties.setInterfaceName(reader.nextString());
            } else if (key.equals("proxy_host")) {
                proxy_host = reader.nextString();
            } else if (key.equals("proxy_port")) {
                proxy_port = reader.nextInt();
            } else if (key.equals("proxy_exclusion")) {
                proxy_exclusion = reader.nextString();
            } else if (key.equals("ip_address")) {
                ip_address = reader.nextString();
            } else if (key.equals("netmask")) {
                netmask = reader.nextInt();
            } else if (key.equals("dns1")) {
                dns1 = reader.nextString();
            } else if (key.equals("dns2")) {
                dns2 = reader.nextString();
            } else if (key.equals("default_route")) {
                default_route = reader.nextString();
            } else if (key.equals("hw_address")) {
                mHwAddress = reader.nextString();
            }
        }
        reader.endObject();

        if (proxy_host != null && proxy_port > 0 && proxy_exclusion != null) {
            mLinkProperties.setHttpProxy(
                    new ProxyProperties(proxy_host, proxy_port, proxy_exclusion));
        }
        if (ip_address != null && netmask != -1) {
            InetAddress inetaddr = InetAddress.getAllByName(ip_address)[0];
            mLinkProperties.addLinkAddress( new LinkAddress( inetaddr, netmask));
        }
        if (dns1 != null) {
            mLinkProperties.addDns(InetAddress.getAllByName(dns1)[0]);
        }
        if (dns2 != null) {
            mLinkProperties.addDns(InetAddress.getAllByName(dns2)[0]);
        }
        if (default_route != null) {
            InetAddress gw_addr = InetAddress.getAllByName(default_route)[0];
            mLinkProperties.addRoute(new RouteInfo(gw_addr));
        }
    }

    public String getHwAddress() { return mHwAddress; }
    public void setHwAddress(String addr) { mHwAddress = addr; }
    public IpAssignment getIpAssignment() { return mIpAssignment; }
    public void setIpAssignment(IpAssignment ipAssignment) { mIpAssignment = ipAssignment; }
    public boolean isStaticIpAssignment() { return mIpAssignment == IpAssignment.STATIC; }
    public ProxySettings getProxySettings() { return mProxySettings; }
    public void setProxySettings(ProxySettings proxySettings) { mProxySettings = proxySettings; }
    public InterfaceStatus getInterfaceStatus() { return mInterfaceStatus; }
    public void setInterfaceStatus(InterfaceStatus is) { mInterfaceStatus = is; }
    public void enable() { mInterfaceStatus = InterfaceStatus.ENABLED; }
    public void disable() { mInterfaceStatus = InterfaceStatus.DISABLED; }
    public boolean isEnabled() { return mInterfaceStatus == InterfaceStatus.ENABLED; }
    public DetailedState getDetailedState() { return mNetworkInfo.getDetailedState(); }

    public void setDetailedState(DetailedState ds, String reason, String extraInfo) {
        mNetworkInfo.setDetailedState(ds, reason, extraInfo);
    }

    public void setIsAvailable(boolean flag) { mNetworkInfo.setIsAvailable(flag); }
    public LinkProperties getLinkProperties() { return mLinkProperties; }
    public void setLinkProperties(LinkProperties lp) { mLinkProperties = lp; }
    public void setDefaultGateway(InetAddress gw) { mLinkProperties.setDefaultGateway(gw); }
    public NetworkInfo getNetworkInfo() { return new NetworkInfo(mNetworkInfo); }
    public void setNetworkInfo(NetworkInfo ni) { mNetworkInfo = ni; }
    public void setLinkAddress(LinkAddress addr) { mLinkProperties.setLinkAddress(addr); }
    public void setHttpProxy(ProxyProperties props) { mLinkProperties.setHttpProxy(props); }
    public ProxyProperties getHttpProxy() { return mLinkProperties.getHttpProxy(); }

    public InetAddress getDefaultGateway() {
        boolean foundDefault = false;
        InetAddress retval = null;
        for (RouteInfo route : mLinkProperties.getRoutes()) {
            if (route.isDefaultRoute()) {
                if (!foundDefault) {
                    retval = route.getGateway();
                    foundDefault = true;
                } else {
                    Slog.e(TAG, "Consistency error: Multiple default routes");
                    break;
                }
            }
        }
        return retval;
    }

    public InetAddress getDNS1() {
        Iterator<InetAddress> dnsIterator = mLinkProperties.getDnses().iterator();
        if (dnsIterator.hasNext()) {
            return dnsIterator.next();
        }
        return null;
    }

    public void setDNS(InetAddress dns1, InetAddress dns2) {
        Collection<InetAddress> addrs = new ArrayList<InetAddress>();
        if (dns1 != null) {
            addrs.add(dns1);
        }
        if (dns2 != null) {
            addrs.add(dns2);
        }
        mLinkProperties.setDnses(addrs);
    }

    public InetAddress getDNS2() {
        Iterator<InetAddress> dnsIterator = mLinkProperties.getDnses().iterator();
        if (dnsIterator.hasNext()) {
            dnsIterator.next();
            if (dnsIterator.hasNext()) {
                return dnsIterator.next();
            }
        }
        return null;
    }

    @Override
    public int describeContents() { return 0; }

    public String getName() {
        return mLinkProperties == null ? "none" : mLinkProperties.getInterfaceName();
    }

    public void setName(String name) { mLinkProperties.setInterfaceName(name); }

    public LinkAddress getLinkAddress() {
        Collection<LinkAddress> addresses = mLinkProperties.getLinkAddresses();
        if (addresses.size() == 0) {
            return null;
        } else if (addresses.size() > 1) {
            Slog.e(TAG, "Consistency error: Multiple link addresses");
        }
        return addresses.iterator().next();
    }

    public void write(JsonWriter writer) throws IOException {
        // NetworkInfo member is not persisted to json because
        // it is essentially a "read-only" parameter. i.e. it is ignored
        // by EthernetStateMachine.updateInterface().
        writer.beginObject();
        writer.name("interface_status").value(mInterfaceStatus.ordinal());
        writer.name("ip_assignment").value(mIpAssignment.ordinal());
        writer.name("proxy_settings").value(mProxySettings.ordinal());
        writer.name("interface_name").value( mLinkProperties.getInterfaceName());
        writer.name("hw_address").value(mHwAddress);

        ProxyProperties proxy = mLinkProperties.getHttpProxy();
        if (proxy != null) {
            Slog.d(TAG, "Persisting proxy properties.");
            writer.name("proxy_host").value(proxy.getHost());
            writer.name("proxy_port").value(proxy.getPort());
            writer.name("proxy_exclusion").value(proxy.getExclusionList());
        } else {
            Slog.d(TAG, "Not persisting proxy properties.");
        }

        if (mIpAssignment != IpAssignment.DHCP) {
            // IP Address
            Collection<LinkAddress> addresses = mLinkProperties.getLinkAddresses();
            if (addresses.size() > 1) {
                Slog.w(TAG, "Found multiple InetAddresses, only persisting first.");
            }
            if (!addresses.isEmpty()) {
                LinkAddress la = (LinkAddress) addresses.toArray()[0];
                String ip_address = la.getAddress().getHostAddress();
                writer.name("ip_address").value(ip_address);
                writer.name("netmask").value(la.getNetworkPrefixLength());
            }

            // DNS1:
            Collection<InetAddress> dnses = mLinkProperties.getDnses();
            if (dnses.size() > 0) {
                InetAddress ia = (InetAddress) dnses.toArray()[0];
                String dns1 = ia.getHostAddress();
                writer.name("dns1").value(dns1);
            }

            // DNS2:
            if (dnses.size() > 1) {
                InetAddress ia = (InetAddress) dnses.toArray()[1];
                String dns2 = ia.getHostAddress();
                writer.name("dns2").value(dns2);
            }

            // Default gateway:
            Collection<RouteInfo> routes = mLinkProperties.getRoutes();
            if (routes.size() > 1) {
                Slog.w(TAG, "Found multiple routes, only persisting first.");
            }
            if (!routes.isEmpty()) {
                RouteInfo ri = (RouteInfo) routes.toArray()[0];
                String route = ri.getGateway().getHostAddress();
                writer.name("default_route").value(route);
            }
        }
        writer.endObject();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String none = "<none>";
        String lpString = mLinkProperties == null ? none : mLinkProperties.toString();
        String niString = mNetworkInfo == null ? none : mNetworkInfo.toString();

        sb.append(" Device name: ").append(getName()).
           append(", Interface Status: ").append(mInterfaceStatus.name()).
           append(", MAC Address: ").append(mHwAddress).
           append(", IP assignment: ").append(mIpAssignment.name()).
           append(", Proxy Settings: ").append(mProxySettings.name()).
           append(", Link properties: ").append(lpString).
           append(", Network info: ").append(niString);

        return sb.toString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mInterfaceStatus.name());
        dest.writeString(mIpAssignment.name());
        dest.writeString(mProxySettings.name());
        dest.writeParcelable(mLinkProperties, flags);
        dest.writeParcelable(mNetworkInfo, flags);
        dest.writeString(mHwAddress);
    }

    @Override
    public boolean equals(Object that)
    {
        if (that instanceof EthernetInfo) {
            EthernetInfo thatEI = (EthernetInfo) that;
            if (! this.mInterfaceStatus.equals(thatEI.mInterfaceStatus)) {
                return false;
            } else if (! this.mIpAssignment.equals(thatEI.mIpAssignment)) {
                return false;
            } else if (! this.mProxySettings.equals(thatEI.mProxySettings)) {
               return false;
            } else if (! this.mLinkProperties.equals(thatEI.mLinkProperties)) {
               return false;
            } else if (! this.mNetworkInfo.equals(thatEI.mNetworkInfo)) {
                return false;
            } else if (! this.mHwAddress.equals(thatEI.mHwAddress)) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public static final Creator<EthernetInfo> CREATOR = new
            Creator<EthernetInfo>() {
                @Override
                public EthernetInfo createFromParcel(Parcel in) {
                    EthernetInfo info = new EthernetInfo();
                    info.setInterfaceStatus(InterfaceStatus.valueOf(in.readString()));
                    info.setIpAssignment(IpAssignment.valueOf(in.readString()));
                    info.setProxySettings(ProxySettings.valueOf(in.readString()));
                    info.setLinkProperties((LinkProperties)in.readParcelable(null));
                    info.setNetworkInfo((NetworkInfo)in.readParcelable(null));
                    info.setHwAddress(in.readString());
                    return info;
                }

                @Override
                public EthernetInfo[] newArray(int size) {
                    return new EthernetInfo[size];
                }
            };

    public int compareTo(EthernetInfo o) {
        return mHwAddress.compareTo(o.getHwAddress());
    }
}
