/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.internode;

import it.unibo.deis.lia.ramp.core.e2e.*;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author useruser
 */
public class TcpDispatcher extends Thread{

    public static int TCP_CONNECT_TIMEOUT = 1500;
    public static int SERVER_SOCKET_BACKLOG = 200;
    private static int MAX_ATTEMPTS = 3;  // attempts for non-delay tolerant packets and without ContinuityManager
    
    private ServerSocket ss;

    private boolean active = true;
    protected void stopTcpDisptacher(){
        active = false;
        try {
            ss.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        this.interrupt();
    }

    @Override
    public void run(){
        try{
            System.out.println("TcpDispatcher START");
            ss = new ServerSocket(Dispatcher.DISPATCHER_PORT, TcpDispatcher.SERVER_SOCKET_BACKLOG);
            while(active){
                //System.out.println("TcpDispatcher accept");
                // receive
                Socket s = ss.accept();
                new TcpDispatcherHandler(s).start();
            }
        }
        catch(java.net.BindException be){
            //be.printStackTrace();
            System.out.println("TcpDispatcher port "+Dispatcher.DISPATCHER_PORT+" already in use; exiting");
            System.exit(0);
        }
        catch(java.net.SocketException se){
            //se.printStackTrace();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("TcpDispatcher END");
    }

    protected class TcpDispatcherHandler extends Thread{
        private Socket s;
        private TcpDispatcherHandler(Socket s){
            this.s=s;
        }
        @Override
        public void run(){
            //System.out.println("TcpDispatcherHandler run start");
            try{
            	InputStream is = s.getInputStream();
            	
            	Object receivedObject = E2EComm.readObjectPacket(is);

                InetSocketAddress remoteAddress = (InetSocketAddress)s.getRemoteSocketAddress();
                String remoteAddressString = remoteAddress.toString().replaceAll("/", "").split(":")[0];
                final byte[] ip = remoteAddress.getAddress().getAddress();
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

                boolean firstHop = false;
                if(remoteAddressString.equals("127.0.0.1")
                        || remoteAddressString.equals("127.0.1.1")){
                    firstHop = true;
                    //System.out.println("TcpDispatcherHandler firstHop = true A");
                }
                else{
                    Vector<String> localAddresses = Dispatcher.getLocalNetworkAddresses(false);
                    //System.out.println("TcpDispatcherHandler localAddresses = "+localAddresses);
                    for(int i=0; (firstHop==false) && (i<localAddresses.size()); i++){
                        if(localAddresses.elementAt(i).equals(remoteAddressString)){
                            firstHop = true;
                            //System.out.println("TcpDispatcherHandler firstHop = true B");
                        }
                    }
                }
                //System.out.println("TcpDispatcherHandler firstHop="+firstHop+" remoteAddressString="+remoteAddressString);

                if(receivedObject instanceof UnicastHeader){
                    //System.out.println("TcpDispatcherHandler UnicastHeader");
                    UnicastHeader uh = (UnicastHeader)receivedObject;

                    if(!firstHop ||
                            // loopback
                            //(uh.getDest().size()==1 && uh.getDest().firstElement().equals(InetAddress.getLocalHost()) ) ){
                            ( uh.getDest()!=null && uh.getDest().length==1 && uh.getDest()[0].equals(InetAddress.getLocalHost().getHostAddress().replaceAll("/", "")) ) ){
                        // update source
                        uh.addSource(remoteAddressString);

                        // update current hop
                        uh.setCurrentHop((byte)(uh.getCurrentHop()+1));
                    }

                    // invoke listeners and pass them UnicastHeader
                    PacketForwardingListener[] listeners = Dispatcher.getInstance(false).getPacketForwardingListeners();
                    for(int i=0; i<listeners.length; i++){
                        listeners[i].receivedTCPUnicastHeader(uh);
                    }

                    int currentHop = uh.getCurrentHop();

                    String ipDest = null;
                    int portDest = -1;

                    // 1) open an outgoing socket with the next dest,
                    //      either a remote Dispatcher or a local receive()
                    
                    Socket destS = null;
                    SocketAddress socketAddress = null;
                    boolean retry;
                    int attempts=0;
                    boolean missedDeadline;
                    int packetRetry;
                    do{
                        retry = true;
                        packetRetry = uh.getRetry();
                        //System.out.println("TcpDispatcherHandler UnicastHeader packetRetry="+packetRetry);
                        Exception ex =null;
                        if(packetRetry==0){
                            missedDeadline=true;
                        }
                        else{
                            missedDeadline=false;
                            try{
                                String[] dest = uh.getDest();
                                int destNodeId = uh.getDestNodeId();
                                if( ( destNodeId!="".hashCode() && destNodeId==Dispatcher.getLocalId() )
                                        || currentHop==dest.length ){
                                    // a) localhost is the destination
                                    ipDest = InetAddress.getLocalHost().getHostAddress().replaceAll("/", "");
                                    portDest = uh.getDestPort();
                                }
                                else{
                                    // b) send to the following dispatcher
                                    ipDest = dest[currentHop];
                                    portDest = Dispatcher.DISPATCHER_PORT;
                                }

                                socketAddress = new InetSocketAddress(ipDest, portDest);
                                destS = new Socket();
                                destS.setReuseAddress(true);
                                int connectTimeout = uh.getConnectTimeout();
                                if(connectTimeout == GenericPacket.UNUSED_FIELD){
                                	connectTimeout = TcpDispatcher.TCP_CONNECT_TIMEOUT;
                                }
                                destS.connect(socketAddress, connectTimeout);
                                retry = false;
                            }
                            catch(Exception e){
                            	if( destS != null ){
                                	destS.close();
                                	destS = null;
                            	}
                                ex = e;

                                System.out.println("TcpDispatcherHandler UnicastHeader exception: "+e.getMessage()+" to "+ipDest+":"+portDest);
                            }
                        }
                        if(missedDeadline==false && retry==true){
                            if( packetRetry!=GenericPacket.UNUSED_FIELD ){
                                uh.setRetry((byte)(packetRetry-1));
                            }
                            attempts++;
                            // invoke listeners and pass them UnicastHeader
                            listeners = Dispatcher.getInstance(false).getPacketForwardingListeners();
                            for(int i=0; i<listeners.length; i++){
                                listeners[i].sendingTCPUnicastHeaderException(uh, ex);
                            }
                        }
                    }
                    while( 
                        ! (
                            (packetRetry!=GenericPacket.UNUSED_FIELD && missedDeadline==true) ||
                            retry==false ||
                            (packetRetry==GenericPacket.UNUSED_FIELD && attempts>=TcpDispatcher.MAX_ATTEMPTS) 
                        )
                    );


                    if( destS!=null && destS.isConnected()){
                        // 2) send the header
                    	OutputStream destOs = destS.getOutputStream();
                    	E2EComm.writeObjectPacket(uh, destOs);
                        //System.out.println("TcpDispatcherHandler unicast header sent to "+ipDest+":"+portDest);

                        // 3) while payload not completely received
                        //      i) read bufferSize bytes from ingoing socket
                        //      ii) write bufferSize bytes to outgoing socket
                        byte[] buffer = new byte[uh.getBufferSize()];

                        BufferedOutputStream destBos = new BufferedOutputStream(destS.getOutputStream(), uh.getBufferSize());
                        BufferedInputStream bis = new BufferedInputStream(s.getInputStream(),uh.getBufferSize());
                        
                        int count=0;
                        int readBytes;
                        boolean finished=false;
                        while(!finished){
                            // attempt to read enough bytes to fulfill the buffer
                            readBytes = bis.read(buffer, count, buffer.length-count);
                            System.out.println("TcpDispatcherHandler partial payload read (partial): readBytes "+readBytes);
                            if(readBytes==-1){
                                finished = true;
                            }
                            else{
                                count += readBytes;
                                if(count == buffer.length){
                                    // write only if the buffer is full
                                    destBos.write(buffer, 0, buffer.length);
                                    destBos.flush();
                                    System.out.println("TcpDispatcherHandler partial payload written (partial): buffer.length "+buffer.length);
                                    count=0;
                                    // invoke listeners and pass them UnicastHeader + Partial Payload
                                    listeners = Dispatcher.getInstance(false).getPacketForwardingListeners();
                                    for(int i=0; i<listeners.length; i++){
                                        listeners[i].receivedTCPPartialPayload(uh, buffer, 0, buffer.length, false);
                                    }
                                }
                            }
                        }
                        if(count>0){
                            // write remaining bytes
                            destBos.write(buffer, 0, count);
                            destBos.flush();
                            System.out.println("TcpDispatcherHandler partial payload written (final): count = "+count);
                        }
                        // invoke listeners and pass them UnicastHeader + Partial Payload
                        listeners = Dispatcher.getInstance(false).getPacketForwardingListeners();
                        for(int i=0; i<listeners.length; i++){
                            listeners[i].receivedTCPPartialPayload(uh, buffer, 0, count, true);
                        }
                        
                    }
                    else{
                        System.out.println("TcpDispatcher unicast header: failed to send to the next hop");
                    }

                    if( destS != null ){
                    	destS.close();
                    	destS = null;
                    }
                    //System.out.println("TcpDispatcherHandler partial payload finished");
                }
                else if(receivedObject instanceof UnicastPacket){
                    //System.out.println("TcpDispatcherHandler UnicastPacket");
                    UnicastPacket up = (UnicastPacket)receivedObject;

                    if( !firstHop ||
                            // loopback
                            //(up.getDest().size()==1 && up.getDest().firstElement().equals(InetAddress.getLocalHost()) ) ){
                            ( up.getDest()!=null && up.getDest().length==1 && up.getDest()[0].equals(InetAddress.getLocalHost().getHostAddress().replaceAll("/", "") )) ) {

                        //System.out.println("TcpDispatcherHandler UnicastPacket update source & currentHop");
                        
                        // update source
                        up.addSource(remoteAddressString);

                        // update current hop
                        up.setCurrentHop((byte)(up.getCurrentHop()+1));
                    }

                    // invoke listeners and pass them UnicastPacket
                    PacketForwardingListener[] listeners = Dispatcher.getInstance(false).getPacketForwardingListeners();
                    for(int i=0; i<listeners.length; i++){
                        listeners[i].receivedTCPUnicastPacket(up);
                    }

                    int currentHop = up.getCurrentHop();

                    String ipDest = null;
                    int portDest = -1;
                    
                    Socket destS = null;
                    SocketAddress socketAddress = null;
                    boolean retry;
                    int attempts=0;
                    boolean missedDeadline;
                    int packetRetry;
                    do{
                        retry = true;
                        packetRetry=up.getRetry();
                        //System.out.println("TcpDispatcherHandler UnicastPacket packetRetry="+packetRetry);
                        Exception ex=null;
                        if(packetRetry==0){
                            missedDeadline=true;
                        }
                        else{
                            missedDeadline=false;
                            String[] dest=up.getDest();
                            if(dest!=null){
                                try{
                                    int destNodeId = up.getDestNodeId();
                                    if( ( destNodeId!="".hashCode() && destNodeId==Dispatcher.getLocalId() )
                                            || currentHop==dest.length ){
                                        // a) localhost is the destination
                                        ipDest = InetAddress.getLocalHost().getHostAddress().replaceAll("/", "");
                                        portDest = up.getDestPort();
                                    }
                                    else{
                                        // b) send to the following dispatcher
                                        ipDest = dest[currentHop];
                                        portDest = Dispatcher.DISPATCHER_PORT;
                                    }

                                    //System.out.println("TcpDispatcherHandler unicast packet sending to "+ipDest+":"+portDest);
                                    socketAddress = new InetSocketAddress(ipDest, portDest);
                                    destS = new Socket();
                                    destS.setReuseAddress(true);
                                    int connectTimeout = up.getConnectTimeout();
                                    if(connectTimeout == GenericPacket.UNUSED_FIELD){
                                    	connectTimeout = TcpDispatcher.TCP_CONNECT_TIMEOUT;
                                    }
                                    destS.connect(socketAddress, connectTimeout);
                                    retry=false;
                                }
                                catch(Exception e){
                                	if( destS != null ){
                                    	destS.close();
                                    	destS = null;
                                	}
                                    ex=e;
                                    System.out.println("TcpDispatcherHandler UnicastPacket exception: "+e.getMessage()+" to "+ipDest+":"+portDest);
                                }
                            }
                        }
                        if( missedDeadline==false && retry==true ){
                            if(packetRetry!=GenericPacket.UNUSED_FIELD){
                                up.setRetry((byte)(packetRetry-1));
                            }
                            attempts++;
                            // invoke listeners and pass them UnicastPacket
                            listeners = Dispatcher.getInstance(false).getPacketForwardingListeners();
                            for(int i=0; i<listeners.length; i++){
                                listeners[i].sendingTCPUnicastPacketException(up, ex);
                                //System.out.println("POST TcpDispatcherHandler unicast listener ipDest="+ipDest+" portDest="+portDest);
                            }
                        }
                    }
                    while( 
                        ! (
                            (packetRetry!=GenericPacket.UNUSED_FIELD && missedDeadline==true) ||
                            retry==false ||
                            (packetRetry==GenericPacket.UNUSED_FIELD && attempts>=TcpDispatcher.MAX_ATTEMPTS)
                        )
                    ); 
                    
                    if( destS!=null && destS.isConnected() ){
                        //System.out.println("TcpDispatcher unicast packet: destS.getRemoteSocketAddress() "+destS.getRemoteSocketAddress());
                    	OutputStream destOs = destS.getOutputStream();
                    	E2EComm.writeObjectPacket(up, destOs);
                    }
                    else{
                        System.out.println("TcpDispatcher unicast packet: failed to send to the next hop: socketAddress = "+socketAddress);
                        //throw new Exception();
                    }

                    if( destS != null ){
                        destS.close();
                        destS = null;
                    }
                }
                else if(receivedObject instanceof BroadcastPacket){
                    //System.out.println("TcpDispatcherHandler BroadcastPacket");
                    BroadcastPacket bp=(BroadcastPacket)receivedObject;
                    
                    if(bp.alreadyTraversed(Dispatcher.getLocalId())){
                    	System.out.println("TcpDispatcher broadcast packet: dropping to avoid loop");
                    }
                    else{
                    	bp.addTraversedId(Dispatcher.getLocalId());
	                    
                    	if(!firstHop) {
	                        // update source
	                        bp.addSource(remoteAddressString);
	
	                        // update TTL
	                        bp.setTtl((byte)(bp.getTtl()-1));
	                    }
	
	                    // invoke listeners and pass them BroadcastPacket
	                    PacketForwardingListener[] listeners = Dispatcher.getInstance(false).getPacketForwardingListeners();
	                    for(int i=0; i<listeners.length; i++){
	                        listeners[i].receivedTCPBroadcastPacket(bp);
	                    }
	
	                    if(!firstHop){
	                        // 1) send to the localhost (may fail)
	                        InetAddress ipDest = InetAddress.getLocalHost();
	                        int portDest = bp.getDestPort();
	                        Socket destS = new Socket();
	                        try{
	                            SocketAddress socketAddress = new InetSocketAddress(ipDest, portDest);
	                            destS.setReuseAddress(true);
	                            destS.connect(socketAddress, portDest);
	                            //System.out.println("TcpDispatcherHandler sending broadcast to localhost: "+ipDest+":"+portDest);
	                            
	                            OutputStream destOs = destS.getOutputStream();
	                            E2EComm.writeObjectPacket(bp, destOs);
	                        }
	                        catch(Exception e){
	                            // no problem...
	                            //System.out.println("TcpDispatcherHandler.broadcast: send failed to local port " + portDest);
	                        }
	                        finally{
	                            destS.close();
	                            destS = null;
	                        }
	                    }
	
	                    // 2) send to neighbors (if TTL is not 0)
	                    if(bp.getTtl()>0){
	                        Vector<InetAddress> neighbors = Heartbeater.getInstance(false).getNeighbors();
	                        if(neighbors.size()==0){
	                            //System.out.println("TcpDispatcherHandler sending broadcast ERROR!!! neighbors.size() == 0 !!!");
	                        }
	
	                        for(int i=0; i<neighbors.size(); i++){
	                            // do not send to the previous node/network
	                            String neighborString = neighbors.elementAt(i).getHostAddress().replaceAll("/", "");
	                            String[] neigh = neighborString.split("[.]");
	                            String[] rem = remoteAddressString.split("[.]");
	                            if( !firstHop &&
	                                    (rem[0].equals(neigh[0]) && rem[1].equals(neigh[1]) && rem[2].equals(neigh[2]))
	                                ){
	                                //System.out.println("TcpDispatcherHandler broadcast "+neighbors.elementAt(i)+" same subnet of "+remoteAddress);
	                            }
	                            else{
	                                //System.out.println("TcpDispatcherHandler sending broadcast to neighbors["+i+"]: "+neighbors.elementAt(i)+":"+Dispatcher.DISPATCHER_PORT);
	                                Socket destS = new Socket();
	                                try{
	                                    // may fail if the remote node has leaved
	                                    SocketAddress socketAddress = new InetSocketAddress(neighbors.elementAt(i), Dispatcher.DISPATCHER_PORT);
	
	                                    destS.setReuseAddress(true);
	                                    destS.connect(socketAddress, TcpDispatcher.TCP_CONNECT_TIMEOUT);
	                                    
	                                    OutputStream destOs = destS.getOutputStream();
	                                    E2EComm.writeObject(receivedObject, destOs);
	                                    
	                                    //destS.close();
	                                }
	                                catch(Exception e){
	                                    // no problem...
	                                    //System.out.println("TcpDispatcherHandler.broadcast: send failed to local port " + portDest);
	                                }
	                                finally{
	                                    destS.close();
	                                    destS = null;
	                                }
	                            }
	                        }
	                    }
                    }
                }
                else{
                    throw new Exception("Unknown packet type: "+receivedObject.getClass().getName());
                }
                s.close();
                //System.out.println("TcpDispatcherHandler finished");
            }
            catch(Exception e){
                System.out.println("TcpDispatcherHandler exception at "+System.currentTimeMillis()+": RemoteSocketAddress = "+s.getRemoteSocketAddress());
                e.printStackTrace();
            }
            
            try {
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
        }
        
    }

}
