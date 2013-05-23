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

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class MyNetUty {
    private static final String TAG = "WOL";
    private static Context mContext = null;

    public MyNetUty(Context ctx) {
        mContext = ctx;
    }

    private String addrIntToStr(int addr) {
        return (addr & 0xFF) + "." + ((addr >> 8) & 0xFF) + "."
                + ((addr >> 16) & 0xFF) + "." + ((addr >> 24) & 0xFF);
    }

    // Get my IP address in the Wi-fi network
    public String getWifiMyAddress() {
        WifiManager wm = (WifiManager) mContext
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo wi = wm.getConnectionInfo();
        int ip = wi.getIpAddress();
        return addrIntToStr(ip);
    }

    // Get the broadcast address of the Wi-fi network
    public String getWifiBcastAddress() {
        WifiManager wm = (WifiManager) mContext
                .getSystemService(Context.WIFI_SERVICE);
        DhcpInfo di = wm.getDhcpInfo();
        if (di == null) {
            return null;
        }
        int bcast = (di.ipAddress & di.netmask) | ~di.netmask;
        return addrIntToStr(bcast);
    }

    // Send UDP data
    public int sendData(String targetAddr, int port, String data) {
        InetSocketAddress isa = new InetSocketAddress(targetAddr, port);
        byte[] buf = data.getBytes();
        DatagramPacket packet = null;
        try {
            packet = new DatagramPacket(buf, buf.length, isa);
            new DatagramSocket().send(packet);
        } catch (Exception e) {
            return -1;
        }
        return 0;
    }

    // Send UDP data
    public int sendData(String targetAddr, int port, byte [] data) {
        InetSocketAddress isa = new InetSocketAddress(targetAddr, port);
        DatagramPacket packet = null;
        try {
            packet = new DatagramPacket(data, data.length, isa);
            new DatagramSocket().send(packet);
        } catch (Exception e) {
            return -1;
        }
        return 0;
    }

    // Checks to see if the given address is a firewalled private address.
    public static boolean isPrivateIpAddress(String addr) {
        if (addr == null || addr.length() <= 0) {
            return false;
        }
        String[] ar = addr.split("\\.");
        int f, s;
        if (ar.length != 4) {
            return false;
        }
        f = Integer.parseInt(ar[0]);
        s = Integer.parseInt(ar[1]);
        if (f == 10) { // 10.0.0.0 - 10.255.255.255
            return true;
        } else if (f == 172) { // 172.16.0.0 - 172.31.255.255
            if (s >= 16 && s <= 31) {
                return true;
            }
        } else if (f == 192 && s == 168) { // 192.168.0.0 - 192.168.255.255
            return true;
        } else if (f == 169 && s == 254) { // 169.254.0.0 - 169.254.255.255
            return true;
        }
        return false;
    }
    
    public boolean RunningOnWifiLanEnvironment() { 
        ConnectivityManager cm =
            (ConnectivityManager)mContext.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected() ||ni.getType() != ConnectivityManager.TYPE_WIFI) {
            return false;
        }
        String myAddr = getWifiMyAddress();
        if (myAddr.equals("0.0.0.0")) {
            return false;
        }
        else if (!isPrivateIpAddress(myAddr)) {
            return false;
        }
        return true;
    }    
    
    // Get the IP addresses of the nodes that have responded the expected response
    // to the broadcast message
    // - broadCastAddr, port: The broadcast address and the port number
    // - queryString: Message to broadcast 
    // - expectAnswer: The expected response message
    // - msecTimeOut: Timeout for waiting response (msecs)
    // - retryCount: Broadcast retry count
    public String[] searchNode(String broadCastAddr, int port, String queryString,
            String expectAnswer, int msecTimeOut, int retryCount) {
        InetSocketAddress isa;
        HashSet<String> serverHash;
        DatagramPacket packet;
        DatagramSocket socket;

        serverHash = new HashSet<String>();
        isa = new InetSocketAddress(broadCastAddr, port);
        try {
            socket = new DatagramSocket(port);
            socket.setReuseAddress(true);
            socket.setSoTimeout(msecTimeOut);
        } catch (Exception e) {
            _Log.e(TAG, "searchNode: DatagramSocket err=+" + e.toString());
            return null;
        }
        byte rBuf[] = new byte[1024];
        DatagramPacket rPacket = new DatagramPacket(rBuf, rBuf.length);

        for (int i = 0; i < retryCount; i++) {
            byte[] query = queryString.getBytes();
            try {
                packet = new DatagramPacket(query, query.length, isa);
                new DatagramSocket().send(packet); // broadcast message
            } catch (Exception e) {
                _Log.e(TAG, "searchNode: DatagramPacket err=" + e.toString());
                continue;
            }
            // collect the responses
            while (true) {
                try {
                    socket.receive(rPacket);
                } catch (Exception e) {
                    _Log.i(TAG, "searchNode: recv err=" + e.toString());
                    break;
                }
                int len = rPacket.getLength();
                String recvData = "";
                try {
                    recvData = new String(rPacket.getData(), 0, len, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                }
                InetAddress ia = rPacket.getAddress();
                //_Log.d(TAG, "searchNode: recv [" + rData + "] from: " + ia.getHostAddress());
                // copy the IP address of the node if the response matches the expected value 
                if (recvData.equals(expectAnswer)) {
                    serverHash.add(ia.getHostAddress());
                }
            }
            // break if node is found  
            if (serverHash.size() > 0) {
                break;
            }
        }
        socket.close();
        
        // return IP addresses of the nodes that have responded
        String[] servers = null;
        int num = serverHash.size();
        if (num > 0) {
            Iterator it = serverHash.iterator();
            servers = new String[num];
            int cnt = 0;
            while (it.hasNext()) {
                servers[cnt++] = (String) it.next();
            }
        }
        return servers;
    }
}
