/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.internode;

import android.content.*;
import android.net.wifi.*;

import java.util.*;
import java.net.*;

import it.unibo.deis.lia.ramp.*;

/**
 *
 * @author useruser
 */
public class Dispatcher {
    
    final static public int DISPATCHER_PORT=5000;
    
    private static Vector<String> localNetworkAddresses = null;
    private static long lastLocalNetworkAddresses=0;

    private UdpDispatcher udpDispatcher;
    private TcpDispatcher tcpDispatcher;
    
    private Vector<PacketForwardingListener> packetForwardingListeners = new Vector<PacketForwardingListener>();

    private Dispatcher(){
        //localIdString = createLocalId();
        //localId = localIdString.hashCode();
    	Dispatcher.setLocalNodeId(createLocalId());
        udpDispatcher = new UdpDispatcher();
        udpDispatcher.start();
        tcpDispatcher = new TcpDispatcher();
        tcpDispatcher.start();
    }

    private static Dispatcher dispatcher=null;
    public synchronized static Dispatcher getInstance(boolean forceStart){
        if(forceStart && Dispatcher.dispatcher==null){
            Dispatcher.dispatcher = new Dispatcher();
        }
        return Dispatcher.dispatcher;
    }
    
    public void stopDispatcher(){
        System.out.println("Dispatcher.stopDispatcher");
        udpDispatcher.stopUdpDisptacher();
        udpDispatcher = null;
        tcpDispatcher.stopTcpDisptacher();
        tcpDispatcher = null;
        Dispatcher.dispatcher = null;
    }

    //private static String localId = null;
    private static int localId = "".hashCode();
    private static String localIdString = "";
    public static int getLocalId(){
        return localId;
    }
    public static String getLocalIdString(){
        return localIdString;
    }
    public static void setLocalNodeId(String newLocalNodeIdString){
        if(newLocalNodeIdString==null || newLocalNodeIdString.equals("")){
            Dispatcher.localId = "".hashCode(); //null;
            Dispatcher.localIdString = null;
        }
        else{
            Dispatcher.localId = newLocalNodeIdString.hashCode();
            Dispatcher.localIdString = newLocalNodeIdString;
        }
        System.out.println("Dispatcher: localId="+localId+" localIdString="+localIdString);
    }
    private String createLocalId(){
        String nodeId = null;
        if(RampEntryPoint.getAndroidContext() != null){
            WifiManager wifi = (WifiManager)RampEntryPoint.getAndroidContext().getSystemService(Context.WIFI_SERVICE);
            nodeId = wifi.getConnectionInfo().getMacAddress();
        }
        else{
            try{
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements() && (nodeId==null || nodeId.equals("")); ) {
                    NetworkInterface intf = en.nextElement();
                    //System.out.println("Dispatcher.getInternalLocalNetworkAddresses intf: "+intf);
                    byte[] mac = intf.getHardwareAddress();
                    if(mac != null){
                        nodeId = "";
                        for(int i=0; i<mac.length; i++){
                            if(((int) mac[i] & 0xff) < 0x10){
                                nodeId += "0";
                            }
                            nodeId += Long.toString((int) mac[i] & 0xff, 16);
                            if(i<mac.length-1){
                                nodeId += ":";
                            }
                        }
                    }
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        if(nodeId==null){
            Random r = new Random();
            float number = r.nextFloat();
            nodeId = "fakeNodeId_"+Math.round(number*1000);
        }

        return nodeId;
    }

    // register/remove/get listener
    public void addPacketForwardingListener(PacketForwardingListener pfw){
        if(!packetForwardingListeners.contains(pfw)){
        	System.out.println("Dispatcher registering listener: "+pfw.getClass());
            packetForwardingListeners.addElement(pfw);
        }
    }
    public void removePacketForwardingListener(PacketForwardingListener pfw){
    	System.out.println("Dispatcher removing listener: "+pfw.getClass());
        packetForwardingListeners.remove(pfw);
    }
    PacketForwardingListener[] getPacketForwardingListeners(){
        PacketForwardingListener[] resArray = new PacketForwardingListener[packetForwardingListeners.size()];
        return (PacketForwardingListener[])(packetForwardingListeners.toArray(resArray));
    }

    public static Vector<String> getLocalNetworkAddresses(boolean force) throws Exception{
        if( !force && (System.currentTimeMillis()-lastLocalNetworkAddresses<10000) ){
            // do nothing
        }
        else{
            lastLocalNetworkAddresses = System.currentTimeMillis();
            Vector<String> newLocalNetworkAddresses = new Vector<String>();
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if ( ! inetAddress.isLoopbackAddress() ) {
                        String ip = inetAddress.getHostAddress().toString();
                        if( ! ip.contains(":") ){ // do not consider IPv6 addresses
                            //System.out.println("Dispatcher.getInternalLocalNetworkAddresses ip: "+ip);
                        	newLocalNetworkAddresses.addElement(ip);
                        }
                    }
                }
            }
            localNetworkAddresses = newLocalNetworkAddresses;
        }
        return localNetworkAddresses;
    }
}
