/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.internode;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import it.unibo.deis.lia.ramp.RampEntryPoint;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;

//import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author useruser
 */
public class Heartbeater extends Thread {

    private Hashtable<InetAddress, Long> neighbors = new Hashtable<InetAddress, Long>();
    private int period = 60*1000; // heartbeater period in millis
    private byte[] bufferDest;

    private static Heartbeater heartbeater = null;
    public static synchronized Heartbeater getInstance(boolean forceStart){
        if(forceStart && heartbeater==null){
            heartbeater = new Heartbeater();
            heartbeater.start();
        }
        return heartbeater;
    }

    public Heartbeater(){
        HeartbeatRequest hReq = new HeartbeatRequest();

        try{
            // from object to byte[]
        	bufferDest = E2EComm.serializePacket(hReq);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private boolean active = true;
    public void stopHeartbeater(){
        System.out.println("Heartbeater.stopHeartbeater");
        active = false;
        interrupt();
    }

    @Override
    public void run(){
        try{
            System.out.println("Heartbeater START");
            while(active){
                sendHeartbeat(false);
                sleep(period);
            }
        }
        catch(InterruptedException ie){

        }
        catch(Exception e){
            e.printStackTrace();
        }
        heartbeater = null;
        System.out.println("Heartbeater END");
    }

    public void sendHeartbeat(boolean force){
        try{
            //System.out.println("Heartbeater sending request");
            Vector<String> localInterfaces = Dispatcher.getLocalNetworkAddresses(force);
            for(int i=0; i<localInterfaces.size(); i++){
                String anInterface = localInterfaces.elementAt(i);
            	//System.out.println("Heartbeater: anInterface "+anInterface);
                try{
                    InetAddress inetA = InetAddress.getByName(anInterface);
                    NetworkInterface netA = NetworkInterface.getByInetAddress(inetA);
                    //System.out.println("Heartbeater sending request via "+netA);

                    Set<InetAddress> broadcastAddresses = new  HashSet<InetAddress>();
                    if(RampEntryPoint.getAndroidContext() != null){
                        WifiManager manager = (WifiManager)RampEntryPoint.getAndroidContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        DhcpInfo dhcp = manager.getDhcpInfo();
                        //System.out.println("Heartbeater dhcp.netmask " + dhcp.netmask);
                        //int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
                        //int broadcast = dhcp.ipAddress | ( ~dhcp.netmask );
                        
                        byte[] ipaddressQuads = new byte[4];
                        for (int k = 0; k < 4; k++){
                        	ipaddressQuads[k] = (byte) ((manager.getConnectionInfo().getIpAddress() >> k * 8) & 0xFF);
                        	//System.out.println("Heartbeater ipaddressQuads["+k+"] " + ipaddressQuads[k]);
                        }
                    	//System.out.println("Heartbeater InetAddress.getByAddress(ipaddressQuads) " + InetAddress.getByAddress(ipaddressQuads));
                        //broadcastAddresses.add(InetAddress.getByAddress(ipaddressQuads));
                        
                        byte[] netmaskQuads = new byte[4];
                        for (int k = 0; k < 4; k++){
                        	netmaskQuads[k] = (byte) ((dhcp.netmask >> k * 8) & 0xFF);
                        	//System.out.println("Heartbeater netmaskQuads["+k+"] " + netmaskQuads[k]);
                        }
                    	//System.out.println("Heartbeater InetAddress.getByAddress(netmaskQuads) " + InetAddress.getByAddress(netmaskQuads));
                        //broadcastAddresses.add(InetAddress.getByAddress(netmaskQuads));
                        
                        int broadcast = manager.getConnectionInfo().getIpAddress() | ( ~dhcp.netmask );
                    	//System.out.println("Heartbeater broadcast " + broadcast);
                        byte[] broadcastQuads = new byte[4];
                        for (int k = 0; k < 4; k++){
                        	broadcastQuads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
                        	//System.out.println("Heartbeater broadcast["+k+"] " + broadcastQuads[k]);
                        }
                    	//System.out.println("Heartbeater InetAddress.getByAddress(broadcastQuads) " + InetAddress.getByAddress(broadcastQuads));
                        
                    	broadcastAddresses.add(InetAddress.getByAddress(broadcastQuads));
                        
                    }
                    else{
                    	// NOT Android
                        List<InterfaceAddress> interfaceAddresses = netA.getInterfaceAddresses();
                        if( interfaceAddresses != null && interfaceAddresses.size()>0 ){
                            //System.out.println(anInterface+": interfaceAddresses.size() = "+interfaceAddresses.size());
                            for(int j=0; j<interfaceAddresses.size(); j++){
                            	InterfaceAddress interfaceA = netA.getInterfaceAddresses().get(j);
                        		//System.out.println("Heartbeater interfaceA " + interfaceA);
                        		if( interfaceA!=null && interfaceA.getBroadcast()!=null ){
                        			//System.out.println("Heartbeater interfaceA.getBroadcast() " + interfaceA.getBroadcast());
                            		//System.out.println("Heartbeater interfaceA.getNetworkPrefixLength() " + interfaceA.getNetworkPrefixLength());
    	                        	broadcastAddresses.add(interfaceA.getBroadcast());
	                            }
                            }
                        }
                    }
                    
                    //System.out.println(anInterface+": broadcast = "+broadcastVector);
                    DatagramSocket ds = new DatagramSocket(0, inetA);
                    ds.setReuseAddress(true);
                    ds.setBroadcast(true);
                    
                    if(anInterface.startsWith("10.")){
                    	broadcastAddresses.add(InetAddress.getByName("255.255.255.255"));
                    }
                    if(broadcastAddresses.size()==0){
                    	broadcastAddresses.add(InetAddress.getByName("255.255.255.255"));
                    	/*
                    	broadcastAddresses.add(InetAddress.getByName("192.168.180.49"));
                    	broadcastAddresses.add(InetAddress.getByName("192.168.255.255"));
                    	broadcastAddresses.add(InetAddress.getByName("192.255.255.255"));
                    	broadcastAddresses.add(InetAddress.getByName("192.168.180.255"));
                    	broadcastAddresses.add(InetAddress.getByName("192.168.181.255"));
                    	broadcastAddresses.add(InetAddress.getByName("192.168.182.255"));
                    	broadcastAddresses.add(InetAddress.getByName("192.168.183.255"));
                    	*/
                    }
                    
                    Iterator<InetAddress> it = broadcastAddresses.iterator();
                    while(it.hasNext()){
                        Thread.sleep(50);
                    	InetAddress broadcastAddress = it.next();
                    	System.out.println("Heartbeater: sending from " + inetA + " to " + broadcastAddress);
	                    DatagramPacket dp = new DatagramPacket(
	                    		bufferDest, 
	                    		bufferDest.length, 
	                    		broadcastAddress,
	                    		Dispatcher.DISPATCHER_PORT);
	                    try{
	                    	ds.send(dp);
	                    	sleep(50);
	                    }
	                    catch(SocketException se){
	                        //System.out.println("Heartbeater ds.send(dp) SocketException to "+broadcastAddress+": "+se.getMessage());
	                        //se.printStackTrace();
	                    	//System.out.println("Heartbeater: sending to 255.255.255.255 instead of " + broadcastAddress);
	                        dp = new DatagramPacket(
		                    		bufferDest, 
		                    		bufferDest.length, 
		                    		InetAddress.getByName("255.255.255.255"),
		                    		Dispatcher.DISPATCHER_PORT);
	                    	ds.send(dp);
	                    }
                    }
                    ds.close();
                    Thread.sleep(100);
                }
                catch(SocketException se){
                    System.out.println("Heartbeater SocketException from "+anInterface+": "+se.getMessage());
                    se.printStackTrace();
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Heartbeater "+e.getMessage());
            // on debian/ubuntu, remember to change the file
            // "/etc/sysctl.d/bindv6only.conf"
            // from
            // "net.ipv6.bindv6only = 1"
            // to
            // "net.ipv6.bindv6only = 0"
            // and finally invoke
            // "invoke-rc.d procps restart"
        }
    }

    protected synchronized void addNeighbor(InetAddress neighbor){
        neighbors.put(neighbor, System.currentTimeMillis());
    }
    public synchronized Vector<InetAddress> getNeighbors() throws Exception{
        //System.out.println("Heartbeater.getNeighbors start");
        Vector<InetAddress> res = new Vector<InetAddress>();
        Enumeration<InetAddress> keys = neighbors.keys();
        for(;keys.hasMoreElements();){
            InetAddress address = keys.nextElement();
            long lastUpdate = neighbors.get(address);
            if(System.currentTimeMillis()-lastUpdate > period+10){
                neighbors.remove(address);
            }
            else{
                res.add(address);
            }
        }
        return res;
    }
    
}
