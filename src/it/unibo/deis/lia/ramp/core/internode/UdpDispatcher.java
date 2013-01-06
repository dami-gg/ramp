/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.internode;

//import it.unibo.deis.lia.ramp.RampEntryPoint;
import it.unibo.deis.lia.ramp.core.e2e.*;

import java.net.*;
import java.util.*;

/**
 *
 * @author useruser
 */
public class UdpDispatcher extends Thread{

    private DatagramSocket receiverDS;
    protected UdpDispatcher(){
        try {
            receiverDS = new DatagramSocket(Dispatcher.DISPATCHER_PORT);
            receiverDS.setBroadcast(active);
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
    }

    private boolean active = true;
    protected void stopUdpDisptacher(){
        active = false;
        receiverDS.close();
        this.interrupt();
    }

    @Override
    public void run(){
        try{
            System.out.println("UdpDispatcher START");
            while(true){
                byte[] buffer = new byte[GenericPacket.MAX_UDP_PACKET];
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                //System.out.println("UdpDispatcher received "+System.currentTimeMillis());

                // receive
                receiverDS.receive(dp);
                new UdpDispatcherHandler(dp).start();
            }
        }
        catch(java.net.BindException be){
            //be.printStackTrace();
            System.out.println("UdpDispatcher port "+Dispatcher.DISPATCHER_PORT+" already in use; exiting");
            System.exit(0);
        }
        catch(java.net.SocketException se){
            //se.printStackTrace();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("UdpDispatcher END");
    }

    protected class UdpDispatcherHandler extends Thread{
        private DatagramPacket dp;
        private UdpDispatcherHandler(DatagramPacket dp){
            this.dp=dp;
        }
        @Override
        public void run(){
            //System.out.println("UdpDispatcherHandler run start");
            try{
                byte[] data = dp.getData();
            	
            	// from byte[] to object
                GenericPacket gp = E2EComm.deserializePacket(data,0,dp.getLength());
                
                InetAddress remoteAddress = dp.getAddress();
                String remoteAddressString = remoteAddress.toString().replaceAll("/", "").split(":")[0];
                final byte[] ip = remoteAddress.getAddress();
                if (ip != null) {
                    final StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < ip.length; i++) {
                        sb.append((ip[i]) & 0xff); //convert byte to 0..255 by bitwise AND with 0xff
                        if (i < (ip.length - 1)) {
                            sb.append('.');
                        }
                    }
                    remoteAddressString = sb.toString();
                }

                boolean firstHop=false;
                if(remoteAddressString.equals("127.0.0.1")
                        || remoteAddressString.equals("127.0.1.1") ){
                    firstHop=true;
                }
                else{
                    Vector<String> localAddresses = Dispatcher.getLocalNetworkAddresses(false);
                    for(int i=0; (firstHop==false) && (i<localAddresses.size()); i++){
                        if(localAddresses.elementAt(i).equals(remoteAddressString)){
                            firstHop=true;
                        }
                    }
                }
                //System.out.println("UdpDispatcher firstHop "+firstHop);
                
                if(gp instanceof HeartbeatRequest){
                    if(!firstHop){
                        System.out.println("UdpDispatcher HeartbeatRequest "+System.currentTimeMillis()+" from "+remoteAddress);
                        Heartbeater.getInstance(false).addNeighbor(remoteAddress);

                        HeartbeatResponse hResp = new HeartbeatResponse();

                        // from object to byte[]
                        byte[] bufferDest = E2EComm.serializePacket(hResp);

                        DatagramSocket ds = new DatagramSocket();
                        ds.setReuseAddress(true);
                        DatagramPacket dp2 = new DatagramPacket(
                        		bufferDest, 
                        		bufferDest.length, 
                        		dp.getAddress(), 
                        		Dispatcher.DISPATCHER_PORT);

                        // random delay to avoid multiple simultaneous responses
                        Random random = new Random();
                        float f = random.nextFloat();
                        long delay = Math.round(f*1000*2);
                        Thread.sleep(delay);
                        
                        ds.send(dp2);
                        ds.close();
                    }
                }
                else if(gp instanceof HeartbeatResponse){
                    if(!firstHop){
                        System.out.println("UdpDispatcher HeartbeatResponse "+System.currentTimeMillis()+" from "+remoteAddress);
                        Heartbeater.getInstance(false).addNeighbor(remoteAddress);
                    }
                }
                else if(gp instanceof UnicastPacket){
                    //System.out.println("UdpDispatcher UnicastPacket "+System.currentTimeMillis()+" from "+remoteAddress);
                    UnicastPacket up=(UnicastPacket)gp;

                    if( !firstHop ||
                            // loopback
                            ( up.getDest()!=null && up.getDest().length==1 && up.getDest()[0].equals(InetAddress.getLocalHost().getHostAddress().replaceAll("/", "")) ) ){

                        // update source
                        up.addSource(remoteAddressString);

                        // update current hop
                        up.setCurrentHop((byte)(up.getCurrentHop()+1));
                    }

                    // invoke listeners and pass them UnicastPacket
                    PacketForwardingListener[] listeners = Dispatcher.getInstance(false).getPacketForwardingListeners();
                    for(int i=0; i<listeners.length; i++){
                        listeners[i].receivedUDPUnicastPacket(up);
                    }

                    // retrieve the next hop
                    int currentHop=up.getCurrentHop();
                    String[] dest=up.getDest();
                    String ipDest=null;
                    int portDest;

                    if(dest!=null){
                        if(currentHop==dest.length){
                            // a) the localhost is the destination
                            //System.out.println("UdpDispatcherHandler InetAddress.getLocalHost().getHostAddress(): "+InetAddress.getLocalHost().getHostAddress());
                            ipDest=InetAddress.getLocalHost().getHostAddress().replaceAll("/", "");
                            portDest=up.getDestPort();
                        }
                        else{
                            // b) send to the following dispatcher
                            //ipDest=dest.elementAt(currentHop);
                            ipDest=dest[currentHop];
                            portDest=Dispatcher.DISPATCHER_PORT;
                        }

                        // from object to byte[]
                        byte[] bufferDest = E2EComm.serializePacket(up);

                        // send
                        //System.out.println("UdpDispatcherHandler sending unicast to neighbor: "+ipDest+":"+portDest);
                        DatagramSocket destS = new DatagramSocket();
                        destS.setReuseAddress(true);
                        DatagramPacket destDp=new DatagramPacket(
                                bufferDest,
                                bufferDest.length,
                                InetAddress.getByName(ipDest),
                                portDest
                        );
                        destS.send(destDp);
                        destS.close();
                    }

                }
                else if(gp instanceof BroadcastPacket){
                	//System.out.println("UdpDispatcher BroadcastPacket "+System.currentTimeMillis()+" from "+remoteAddress);
                	BroadcastPacket bp=(BroadcastPacket)gp;
                	
                	if(bp.alreadyTraversed(Dispatcher.getLocalId())){
                    	// XXX check alreadyTraversed
                    	System.out.println("UdpDispatcher broadcast packet: dropping to avoid loop");
                    }
                    else{
                    	bp.addTraversedId(Dispatcher.getLocalId());
                    
	                    if(!firstHop){
	                        // update source
	                        bp.addSource(remoteAddressString);
	
	                        // reduce TTL only if not first hop
	                        bp.setTtl((byte)(bp.getTtl()-1));
	                    }
	
	                    // invoke listeners and pass them BroadcastPacket
	                    PacketForwardingListener[] listeners = Dispatcher.getInstance(false).getPacketForwardingListeners();
	                    for(int i=0; i<listeners.length; i++){
	                        listeners[i].receivedUDPBroadcastPacket(bp);
	                    }
	                    
	                    // from object to byte[]
	                    byte[] bufferDest = E2EComm.serializePacket(bp);
	
	                    if(!firstHop){
	                        // 1) send to the localhost (may fail)
	                        //      only if not first hop
	                        InetAddress ipDest = InetAddress.getLocalHost();
	                        int portDest = bp.getDestPort();
	                        DatagramSocket destS = new DatagramSocket();
	                        try{
	                            //System.out.println("UdpDispatcherHandler BroadcastPacket ipDest "+ipDest);
	                        	destS.setReuseAddress(true);
	                            DatagramPacket destDp = new DatagramPacket(
	                            		bufferDest, 
	                            		bufferDest.length, 
	                            		ipDest, 
	                            		portDest);
	                            destS.send(destDp);
	                        }
	                        catch(Exception e){
	                            // no problem...
	                            //System.out.println("UdpDispatcher.broadcast: send failed to local port " + portDest);
	                        }
	                        finally{
	                            destS.close();
	                        }
	                    }
	
	                    // 2a) send to neighbors (if TTL is not 0)
	                    if(bp.getTtl()>0){
	                    	
	                        Vector<InetAddress> neighbors = Heartbeater.getInstance(false).getNeighbors();
	                        if(neighbors.size()==0){
	                            //System.out.println("UdpDispatcherHandler sending broadcast ERROR!!! neighbors.size() == 0 !!!");
	                        }
	                        
	                        for(int i=0; i<neighbors.size(); i++){
	                            // do not send to the previous node/network
	                            String neighborString = neighbors.elementAt(i).getHostAddress().replaceAll("/", "");
	                            String[] neigh = neighborString.split("[.]");
	                            String[] rem = remoteAddressString.split("[.]");
	                            if( !firstHop &&
	                                    rem[0].equals(neigh[0]) && rem[1].equals(neigh[1]) && rem[2].equals(neigh[2])
	                                ){
	                                //System.out.println("UdpDispatcher "+neighbors.elementAt(i)+" same subnet of "+remoteAddress);
	                            }
	                            else{
	                                //System.out.println("UdpDispatcherHandler sending broadcast to neighbors["+i+"]: "+neighbors.elementAt(i)+":"+Dispatcher.DISPATCHER_PORT);
	                                DatagramSocket destS=new DatagramSocket();
	                                try{
	                                	destS.setReuseAddress(true);
	                                    DatagramPacket destDp=new DatagramPacket(
	                                            bufferDest,
	                                            bufferDest.length,
	                                            neighbors.elementAt(i),
	                                            Dispatcher.DISPATCHER_PORT);
	                                    destS.send(destDp);
	                                }
	                                catch(Exception e){
	                                    System.out.println("UdpDispatcherHandler: failed to send to "+neighbors.elementAt(i)+":"+Dispatcher.DISPATCHER_PORT+" ("+e.getMessage()+")");
	                                    //e.printStackTrace();
	                                }
	                                finally{
	                                    destS.close();
	                                }
	                            }
	                        }
	                    }
                    }
                }
                else{
                    throw new Exception("UdpDispatcherHandler: unknown packet type: "+gp.getClass().getName());
                }
            }
            catch(Exception e){
                System.out.println("UdpDispatcherHandler: failed DatagramPacket with remote node "+dp.getAddress()+":"+dp.getPort());
                e.printStackTrace();
            }
            //System.out.println("UdpDispatcherHandler END");
        }
    }
    
}
