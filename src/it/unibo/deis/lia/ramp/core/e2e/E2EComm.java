/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.e2e;

import it.unibo.deis.lia.ramp.RampEntryPoint;
import it.unibo.deis.lia.ramp.core.internode.*;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author useruser
 */
public class E2EComm {

    public static final int UDP = 0;
    public static final int TCP = 1;

    public static final int DEFAULT_BUFFERSIZE = 50*1024;
    
    public static BoundReceiveSocket bindPreReceive(int protocol) throws Exception{
        BoundReceiveSocket res = null;
        if(protocol==E2EComm.UDP){
            DatagramSocket ds = new DatagramSocket();
            ds.setReuseAddress(true);
            res = new BoundReceiveSocket(ds);
        }
        else if(protocol==E2EComm.TCP){
            ServerSocket ss = new ServerSocket(0, TcpDispatcher.SERVER_SOCKET_BACKLOG);
            ss.setReuseAddress(true);
            res = new BoundReceiveSocket(ss);
        }
        else{
            throw new Exception("Unknown protocol: must be either E2EComm.UDP or E2EComm.TCP");
        }
        return res;
    }
    public static BoundReceiveSocket bindPreReceive(int localPort, int protocol) throws Exception{
        BoundReceiveSocket res = null;
        if(protocol==E2EComm.UDP){
            DatagramSocket ds = new DatagramSocket(localPort);
            ds.setReuseAddress(true);
            res = new BoundReceiveSocket(ds);
        }
        else if(protocol==E2EComm.TCP){
            ServerSocket ss = new ServerSocket(localPort, TcpDispatcher.SERVER_SOCKET_BACKLOG);
            ss.setReuseAddress(true);
            res = new BoundReceiveSocket(ss);
        }
        else{
            throw new Exception("Unknown protocol: must be either E2EComm.UDP or E2EComm.TCP");
        }
        return res;
    }

    public static GenericPacket receive(BoundReceiveSocket socket) throws Exception{
        // blocking receive
        return receive(socket, 0, null);
    }
    public static GenericPacket receive(BoundReceiveSocket socket, OutputStream payloadDestination) throws Exception{
        // blocking receive
        return receive(socket, 0, payloadDestination);
    }
    public static GenericPacket receive(BoundReceiveSocket socket, int timeout) throws Exception{
    	return receive(socket, timeout, null);
    }
    public static GenericPacket receive(BoundReceiveSocket socket, int timeout, OutputStream payloadDestination) throws Exception{
        GenericPacket gp = null;
        if(socket==null){
            throw new Exception("Unbound socket");
        }
        else{
            DatagramSocket ds = socket.getDatagramSocket();
            ServerSocket ss = socket.getServerSocket();
            if(ds!=null){
                ds.setSoTimeout(timeout);
                byte[] udpBuffer=new byte[GenericPacket.MAX_UDP_PACKET];
                DatagramPacket dp = new DatagramPacket(udpBuffer, udpBuffer.length);

                // receive
                try{
                    // throws SocketTimeoutException in case of timeout
                    ds.receive(dp);
                }
                catch(SocketTimeoutException ste){
                    //ds.close();
                    throw ste;
                }

                // process payload
            	byte[] data = dp.getData();
                gp = (GenericPacket)E2EComm.deserializePacket(data,0,dp.getLength());
            }
            else if(ss!=null){
                ss.setSoTimeout(timeout);

                // receive
                Socket s;
                try{
                    // throws SocketTimeoutException in case of timeout
                    s = ss.accept();
                }
                catch(SocketTimeoutException ste){
                    throw ste;
                }
                s.setReuseAddress(true);
                InputStream is = s.getInputStream();
            	
                // process payload
                Object o1 = E2EComm.readObjectPacket(is);

                // if received only header,
                // then wait for the payload
                Object payload = null;
                if( ! (o1 instanceof UnicastHeader) ){
                    s.close();
                }
                else{
                    //System.out.println("E2EComm.receive tcp header received");
                    // the received object is an Unicast Header
                    // receiving the payload...
                	if(payloadDestination==null){
                		payload = E2EComm.readObject(is);
                        s.close();
                	}
                	else{
                		UnicastHeader receivedUnicastHeader = (UnicastHeader)o1;

                		BufferedOutputStream destBos = new BufferedOutputStream(payloadDestination, receivedUnicastHeader.getBufferSize());
    	                BufferedInputStream bis = new BufferedInputStream(s.getInputStream(),receivedUnicastHeader.getBufferSize());
                		
                		int count=0;
    	                int readBytes;
    	                boolean finished=false;
    	                byte[] buffer = new byte[receivedUnicastHeader.getBufferSize()];
    	                while(!finished){
    	                    // attempt to read enough bytes to fulfill the buffer
    	                    readBytes = bis.read(buffer, count, buffer.length-count);
    	                    
    	                    //System.out.println("E2EComm partial payload read (partial): readBytes "+readBytes);
    	                	if(readBytes==-1){
    	                        finished = true;
    	                    }
    	                    else{
    	                    	count += readBytes;
    	                        if(count == buffer.length){
    	                            // write only if the buffer is full
    	                        	destBos.write(buffer, 0, buffer.length);
    	                            destBos.flush();
    	                        	
    	                            //System.out.println("E2EComm partial payload written (partial): buffer.length "+buffer.length);
    	                            count=0;
    	                        }
    	                    }
    	                }
    	                if(count>0){
    	                    // write remaining bytes
    	                    destBos.write(buffer, 0, count);
    	                    destBos.flush();
    	                    //System.out.println("E2EComm partial payload written (final): count = "+count);
    	                }
    	                s.close();
                	}
                }

                if(payload!=null){
                    // unicast header + payload
                    gp = new UnicastPacket((UnicastHeader)o1, payload);
                }
                else if(payloadDestination!=null){
                	gp=(UnicastHeader)o1;
                }
                else{
                    // either unicast packet or broadcast packet or 
                    gp=(GenericPacket)o1;
                }
            }
            else{
                throw new Exception("Wrong socket");
            }

            // now __gp__ is correctly initialized,
            // either UnicastPacket or BroadcastPacket

            // 2) ACK
            if (gp instanceof UnicastPacket){
                UnicastPacket up = (UnicastPacket)gp;
                boolean ack=up.isAck();
                if(ack==true){
                    //System.out.println("E2EComm.receive UnicastPacket ack requested");
                    // ACK requested
                    int sourcePortAck = up.getSourcePortAck();

                    String[] newDest = E2EComm.ipReverse(up.getSource());

                    // send "ack" to the sender eploiting the same path
                    // (it could be possible even to exploit sourceNodeId)
                    String payloadAck = new String("ack");
                    int protocol;
                    if(ds!=null){
                        protocol = E2EComm.UDP;
                    }
                    else{
                        protocol = E2EComm.TCP;
                    }
                    sendUnicast(
                            newDest,
                            sourcePortAck,
                            protocol,
                            E2EComm.serialize(payloadAck)
                    );
                }
            }
        }
        return gp;
    }
    
    public static void sendBroadcast(int TTL, int destPort, int protocol, byte[] payload) throws Exception{
        //System.out.println("E2EComm.sendBroadcast START");

        // 1) check maximum payload size
        int payloadSize;
        if (payload != null ){
	        payloadSize = payload.length;
        }
        else{
        	payloadSize = 0;
        }
        if(payloadSize > BroadcastPacket.MAX_BROADCAST_PAYLOAD){
            throw new Exception("Maximum payload size of BroadcastPacket is "+BroadcastPacket.MAX_BROADCAST_PAYLOAD+" but current payloadSize is "+payloadSize);
        }
        
        // 2) setup packet
        int localNodeId = Dispatcher.getLocalId();
        BroadcastPacket bp=new BroadcastPacket( 
                (byte)TTL,
                destPort,
                localNodeId,
                payload
        );

        // 3) send packet to local Dispatcher
        if(protocol==E2EComm.UDP){
            //System.out.println("E2EComm.sendBroadcast udp");
            // from object to byte[]
            byte[] buffer = E2EComm.serializePacket(bp);
            
            // send unicast packet to local dispatcher
            //System.out.println("E2EComm.sendBroadcast sending to "+InetAddress.getLocalHost()+":"+Dispatcher.DISPATCHER_PORT);
            DatagramSocket ds = new DatagramSocket();
            DatagramPacket dp= new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), Dispatcher.DISPATCHER_PORT);
            ds.send(dp);
            //System.out.println("E2EComm.sendBroadcast udp end");
        }
        else if(protocol==E2EComm.TCP){
            //System.out.println("E2EComm.sendBroadcast tcp");
            // send broadcast packet to local dispatcher
            InetAddress localhost = InetAddress.getLocalHost();
            SocketAddress socketAddress = new InetSocketAddress(localhost, Dispatcher.DISPATCHER_PORT);
            Socket s = new Socket();
            s.setReuseAddress(true);
            try{
                s.setSoTimeout(TcpDispatcher.TCP_CONNECT_TIMEOUT);
                s.connect(socketAddress, TcpDispatcher.TCP_CONNECT_TIMEOUT);
                OutputStream os = s.getOutputStream();
                E2EComm.writeObjectPacket(bp, os);
            }
            catch(SocketTimeoutException ste){
                System.out.println("E2EComm.sendBroadcast tcp SocketTimeoutException to "+localhost+":"+Dispatcher.DISPATCHER_PORT+" "+System.currentTimeMillis());
                ste.printStackTrace();
            }
            catch(ConnectException ce){
                System.out.println("E2EComm.sendBroadcast tcp ConnectException to "+localhost+":"+Dispatcher.DISPATCHER_PORT+" probably due to JDK/JRE bug "+System.currentTimeMillis());
                ce.printStackTrace();
            }

            s.close();
            s = null;
            
            //System.out.println("E2EComm.sendBroadcast tcp end");
        }
        else{
            throw new Exception("Unknown protocol: must be either E2EComm.UDP or E2EComm.TCP: "+protocol);
        }

    }

    
    // sendUnicast byte[] payload ---------------------------------------------
    public static boolean sendUnicastDestNodeId(int destNodeId, int destPort, int protocol, byte[] payload) throws Exception{
        // destNodeId!!! static sender-side version
        boolean res;
        boolean ack = false;
        int bufferSize = E2EComm.DEFAULT_BUFFERSIZE; 
        res = sendUnicastDestNodeId(
                destNodeId,
                destPort,
                protocol,
                ack,
                GenericPacket.UNUSED_FIELD, // timeoutAck
                bufferSize,
                GenericPacket.UNUSED_FIELD, // packetDeliveryTimeout
                GenericPacket.UNUSED_FIELD, // packetTimeoutConnect
                payload
        );
        return res;
    }
    public static boolean sendUnicastDestNodeId(int destNodeId, int destPort, int protocol, boolean ack, int timeoutAck, int bufferSize, int packetDeliveryTimeout, short packetTimeoutConnect, byte[] payload) throws Exception{
        // destNodeId!!! static sender-side version
        boolean res;
        Vector<ResolverPath> paths = Resolver.getInstance(false).resolveBlocking(destNodeId);
        if(paths!=null && paths.size()>0){
            ResolverPath mostRecent = paths.elementAt(0);
            for(int i=1; i<paths.size(); i++){
                ResolverPath current = paths.elementAt(i);
                if(current.getLastUpdate()>mostRecent.getLastUpdate()){
                    mostRecent = current;
                }
            }
            res = sendUnicast(
                    mostRecent.getPath(),
                    "".hashCode(), // destNodeId
                    destPort,
                    protocol,
                    ack,
                    timeoutAck,
                    bufferSize,
                    packetDeliveryTimeout,
                    packetTimeoutConnect,
                    payload
            );
        }
        else{
            res = false;
        }
        return res;
    }

    public static boolean sendUnicast(String[] dest, int destPort, int protocol, byte[] payload) throws Exception{
        boolean res;
        boolean ack = false;
        int bufferSize = E2EComm.DEFAULT_BUFFERSIZE;
        res = sendUnicast(
                dest,
                "".hashCode(), // destNodeId
                destPort,
                protocol,
                ack,
                GenericPacket.UNUSED_FIELD, // timeoutAck
                bufferSize,
                GenericPacket.UNUSED_FIELD, // packetDeliveryTimeout
                GenericPacket.UNUSED_FIELD, // packetTimeoutConnect
                payload
        );
        return res;
    }
    /*public static boolean sendUnicast(String[] dest, int destPort, int protocol, int bufferSize, byte[] payload) throws Exception{
        boolean res;
        boolean ack = false;
        res = sendUnicast(
                dest,
                "".hashCode(), // destNodeId
                destPort,
                protocol,
                ack,
                GenericPacket.UNUSED_FIELD, // timeoutAck
                bufferSize,
                GenericPacket.UNUSED_FIELD, // packetDeliveryTimeout
                GenericPacket.UNUSED_FIELD, // packetTimeoutConnect
                payload
        );
        return res;
    }*/
    /*public static boolean sendUnicast(String[] dest, int destPort, int protocol, boolean ack, int timeoutAck, int bufferSize, byte[] payload) throws Exception{
        boolean res;
        res = sendUnicast(
                dest,
                "".hashCode(), // destNodeId
                destPort,
                protocol,
                ack,
                timeoutAck,
                bufferSize,
                GenericPacket.UNUSED_FIELD, // packetDeliveryTimeout
                GenericPacket.UNUSED_FIELD, // packetTimeoutConnect
                payload
        );
        return res;
    }*/
    public static boolean sendUnicast(
    		String[] dest, 
    		int destNodeId, 
    		int destPort, 
    		int protocol, 
    		boolean ack, int timeoutAck, 
    		int bufferSize, 
    		int packetDeliveryTimeout, // delay-tolerant messaging
    		short packetTimeoutConnect, // inter-node socket connect timeout (only TCP) 
    		byte[] payload
    		) throws Exception{
    	
    	//System.out.println("E2EComm.sendUnicast byte[] payload.length="+payload.length);
    	
        boolean res = true;

        // check parameters
        if( bufferSize!=GenericPacket.UNUSED_FIELD && bufferSize!=0 && (bufferSize<5*1024 || bufferSize>1024*1024)){
            throw new Exception("bufferSize must be in the [5KB,1MB] range but current bufferSize = "+bufferSize);
        }
        if( (ack==true) && (timeoutAck<0)){
            throw new Exception("timeoutAck must be equal to or greater than 0 but current timeoutAck = "+timeoutAck);
        }
        if( packetDeliveryTimeout!=GenericPacket.UNUSED_FIELD && packetDeliveryTimeout<=0 ){
            throw new Exception("packetDeliveryTimeout must be greater than 0 but current packetDeliveryTimeout = "+packetDeliveryTimeout);
        }
        if( packetDeliveryTimeout>0 && protocol!=E2EComm.TCP ){
            throw new Exception("packetDeliveryTimeout is greater than 0 but protocol is not E2EComm.TCP (protocol="+protocol+")");
        }
        if( packetDeliveryTimeout>0 && destNodeId=="".hashCode() ){//(destNodeId==null || destNodeId.equals(""))){
            throw new Exception("packetDeliveryTimeout is greater than 0 but destNodeId is empty");//either null or empty");
        }
        if( packetDeliveryTimeout>0 && bufferSize!=GenericPacket.UNUSED_FIELD ){
            throw new Exception("packetDeliveryTimeout is greater than 0 but bufferSize is enabled: bufferSize="+bufferSize);
        }
        //if( (destNodeId==null || destNodeId.equals("")) && (dest==null || dest.length==0) ){
        if( (destNodeId=="".hashCode()) && (dest==null || dest.length==0) ){
            throw new Exception("both dest and destNodeId are incorrect: dest="+dest+" destNodeId="+destNodeId);
        }

        if( (bufferSize!=GenericPacket.UNUSED_FIELD) && (bufferSize==0)){
            bufferSize = E2EComm.DEFAULT_BUFFERSIZE;
        }

        int payloadSize;
        if (payload != null ){
	        payloadSize = payload.length;
        }
        else{
        	payloadSize = 0;
        }
        
        // packetDeliveryTimeout (seconds)
        int retry = GenericPacket.UNUSED_FIELD;
        int timeWait = GenericPacket.UNUSED_FIELD;
        if( packetDeliveryTimeout != GenericPacket.UNUSED_FIELD ){
            // delay-tolerant
            if(payloadSize >= UnicastPacket.MAX_DELAY_TOLERANT_PAYLOAD){
                throw new Exception("Maximum payload size of Delay-Tolerant UnicastPacket is "+UnicastPacket.MAX_DELAY_TOLERANT_PAYLOAD+" but current payloadSize is "+payloadSize);
            }
            if(payloadSize<100*1024){
                retry=15;
            }
            else{
                retry=8;
            }
            timeWait=(packetDeliveryTimeout*1000)/retry;
        }

        // 1) setup packet
        int localNodeId = Dispatcher.getLocalId();
        UnicastHeader uh = new UnicastHeader(
                dest,
                destPort,
                destNodeId,
                localNodeId,
                ack,
                timeoutAck,
                (byte)0, // currentHop set to 0
                bufferSize,
                (byte)retry,
                timeWait,
                packetTimeoutConnect
                );
        
        UnicastPacket up = new UnicastPacket(uh, payload);

        // 2) send packet to local Dispatcher
        if( protocol == E2EComm.UDP ){

            // 1) maximum payload size
            if(payloadSize > GenericPacket.MAX_UDP_PAYLOAD){
            	System.out.println("E2EComm: Maximum payload size of UDP-baseed UnicastPacket is "+GenericPacket.MAX_UDP_PAYLOAD+" but current payloadSize is "+payloadSize);
                throw new Exception("Maximum payload size of UDP-baseed UnicastPacket is "+GenericPacket.MAX_UDP_PAYLOAD+" but current payloadSize is "+payloadSize);
            }

            // check 255.255.255.255 is not in dest[]
            if(dest!=null){
                for(int i=0; i<dest.length; i++){
                    if(dest[i].equals("255.255.255.255")){
                        throw new Exception("E2EComm.sendUnicast: 255.255.255.255 not allowed");
                    }
                }
            }

            DatagramSocket ds = new DatagramSocket();
            ds.setReuseAddress(true);
            if(ack){
                up.setSourcePortAck(ds.getLocalPort());
            }
            
            // from object to byte[]
            byte[] buffer = E2EComm.serializePacket(up);
            //System.out.println("E2EComm: UDP buffer.length "+buffer.length);
            
            // send unicast packet to local dispatcher
            DatagramPacket dp= new DatagramPacket(
            		buffer,
            		buffer.length, 
            		InetAddress.getLocalHost(), 
            		Dispatcher.DISPATCHER_PORT);
            ds.send(dp);
            ds.close();
        }
        else if(protocol==E2EComm.TCP){
            //System.out.println("E2EComm.sendUnicast tcp start");
            
            // use bufferSize to decide if sending
            // the whole packet
            // or
            // first only the header and then only the payload
            boolean split=false;
            if(bufferSize != GenericPacket.UNUSED_FIELD){
                int packetSize = E2EComm.objectSizePacket(up);
                if(packetSize > bufferSize){
                    split=true;
                }
            }

            // send unicast packet to the local Dispatcher
            SocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), Dispatcher.DISPATCHER_PORT);
            Socket socketToLocalhost = new Socket();
            socketToLocalhost.setReuseAddress(true);
            socketToLocalhost.bind(null);
            
            if(ack){
                uh.setSourcePortAck(socketToLocalhost.getLocalPort());
            }
            
            try{
                //System.out.println("E2EComm.sendUnicast TCP: localPort = "+socketToLocalhost.getLocalPort());
                socketToLocalhost.connect(socketAddress, TcpDispatcher.TCP_CONNECT_TIMEOUT);
            }
            catch(Exception e){
                System.out.println("E2EComm.sendUnicast TCP Exception: localPort = "+socketToLocalhost.getLocalPort());
                socketToLocalhost.close();
                socketToLocalhost = null;
                throw e;
            }
            //System.out.println("E2EComm.sendUnicast tcp s.getSendBufferSize() = "+socketToLocalhost.getSendBufferSize());
            /*

             MS Windows issue!!!
             In case of "Address already in use: connect"
             there could be two issues...

             1) first of all reduce the TIME_WAIT windows socket value
                a) Start Registry Editor.   
                b) locate the following subkey, and then click Parameters:
                    HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters
                c) On the Edit menu, click New, and then add the following registry entry:
                    Value Name: TcpTimedWaitDelay
                    Value Type: DWORD
                    Value data: 0x1E or 30 (i.e., 30 seconds)
                    Valid Range: 30–300 seconds (decimal)
                    Default: 0xF0 ( 240 seconds = 4 minutes )

             2) if it does not resolve the problem
                see the following topic Microsoft Knowledge Base Article 196271:
                "When you try to connect from TCP ports greater than 5000
                you receive the error 'WSAENOBUFS (10055)'"

                 a) Start Registry Editor.
                 b) Locate the following subkey in the registry, and then click Parameters:
                    HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters
                 c) On the Edit menu, click New, and then add the following registry entry:
                        Value Name: MaxUserPort
                        Value Type: DWORD
                        Value data: 65534
                        Valid Range: 5000-65534 (decimal)
                        Default: 0x1388 (5000 decimal)
             */
            
            OutputStream os = socketToLocalhost.getOutputStream();
            if(!split){
                // send the whole packet
            	E2EComm.writeObjectPacket(up, os);
            }
            else{
                // send only the header
                //System.out.println("E2EComm.sendUnicast header tcp "+InetAddress.getLocalHost()+":"+Dispatcher.DISPATCHER_PORT);
            	E2EComm.writeObjectPacket(uh, os);
            	
            	// send the payload
            	//System.out.println("E2EComm.sendUnicast tcp sending payload to "+InetAddress.getLocalHost()+":"+Dispatcher.DISPATCHER_PORT);
            	E2EComm.writeObject(payload, os);
                
            }
            os.flush();
            socketToLocalhost.close();
            socketToLocalhost = null;
        }
        else{
            throw new Exception("Unknown protocol: must be either E2EComm.UDP or E2EComm.TCP");
        }

        // 3) ack
        if(ack==true){
            // receive ack
            GenericPacket gpAck = null;
            try{
                if(protocol==E2EComm.UDP){
                    System.out.println("E2EComm.sendUnicast udp ack localPort = "+uh.getSourcePortAck());
                    byte[] udpBuffer = new byte[GenericPacket.MAX_UDP_PACKET];
                    DatagramSocket ds = new DatagramSocket(uh.getSourcePortAck());
                    DatagramPacket dp = new DatagramPacket(udpBuffer, udpBuffer.length);
                    ds.setSoTimeout(timeoutAck);

                    // receive and close
                    ds.receive(dp); // throws SocketTimeoutException in case of timeout
                    ds.close();

                    // process payload
                    // from byte to object
                    gpAck = (GenericPacket)E2EComm.deserializePacket(dp.getData());
                }
                else if(protocol==E2EComm.TCP){
                    //System.out.println("E2EComm.sendUnicast tcp ack localPort = "+up.getSourcePortAck());
                    ServerSocket ssAck = new ServerSocket(up.getSourcePortAck(), TcpDispatcher.SERVER_SOCKET_BACKLOG);
                    ssAck.setReuseAddress(true);
                    ssAck.setSoTimeout(timeoutAck);

                    // receive and close
                    Socket s = ssAck.accept(); // throws SocketTimeoutException in case of timeout
                    ssAck.close();

                    // process payload
                    InputStream is = s.getInputStream();
                    gpAck = (GenericPacket)E2EComm.readObjectPacket(is);
                    s.close();
                }
            }
            catch(java.net.SocketTimeoutException ste){
                System.out.println("E2EComm.sendUnicast receive ack: ste = "+ste);
                res = false;
            }

            if(res==true){
                // check it is actually an ack
                if(gpAck instanceof UnicastPacket){
                    UnicastPacket upAck=(UnicastPacket)gpAck;

                    // XXX ??? check the sender of the ack is the same node in dest (an id in the ack???)

                    Object ackPayload = E2EComm.deserialize(upAck.getBytePayload());
                    if(ackPayload instanceof java.lang.String){
                        String ackPayloadString = (String)ackPayload;
                        if( ! ackPayloadString.equals("ack") ){
                            System.out.println("E2EComm.sendUnicast receive ack FALSE: ackPayloadString = "+ackPayloadString);
                            res = false;
                        }
                    }
                    else{
                        System.out.println("E2EComm.sendUnicast receive ack FALSE: ackPayload class = "+ackPayload.getClass().getName());
                        System.out.println("E2EComm.sendUnicast receive ack FALSE: ackPayload class = "+ackPayload.getClass().getSimpleName());
                        res = false;
                    }
                }
                else{
                    System.out.println("E2EComm.sendUnicast receive ack FALSE: gpAck class = "+gpAck.getClass().getName());
                    res = false;
                }
            }
        }

        return res;
    }
    
    // InputStream -------------------------------------------
    public static boolean sendUnicastDestNodeId(int destNodeId, int destPort, int protocol, InputStream payload) throws Exception{
        // destNodeId!!! static sender-side version
        boolean res;
        boolean ack = false;
        int bufferSize = E2EComm.DEFAULT_BUFFERSIZE;
        res = sendUnicastDestNodeId(
                destNodeId,
                destPort,
                protocol,
                ack,
                GenericPacket.UNUSED_FIELD, // timeoutAck
                bufferSize,
                GenericPacket.UNUSED_FIELD, // packetTimeoutConnect
                payload
        );
        return res;
    }
    public static boolean sendUnicastDestNodeId(int destNodeId, int destPort, int protocol, boolean ack, int timeoutAck, int bufferSize, /*int packetDeliveryTimeout,*/ short packetTimeoutConnect, InputStream payload) throws Exception{
        // destNodeId!!! static sender-side version
        boolean res;
        Vector<ResolverPath> paths = Resolver.getInstance(false).resolveBlocking(destNodeId);
        if(paths!=null && paths.size()>0){
            ResolverPath mostRecent = paths.elementAt(0);
            for(int i=1; i<paths.size(); i++){
                ResolverPath current = paths.elementAt(i);
                if(current.getLastUpdate()>mostRecent.getLastUpdate()){
                    mostRecent = current;
                }
            }
            res = sendUnicast(
                    mostRecent.getPath(),
                    "".hashCode(), // destNodeId
                    destPort,
                    protocol,
                    ack,
                    timeoutAck,
                    bufferSize,
                    packetTimeoutConnect,
                    payload
            );
        }
        else{
            res = false;
        }
        return res;
    }

    public static boolean sendUnicast(String[] dest, int destPort, int protocol, InputStream payload) throws Exception{
        boolean res;
        boolean ack = false;
        int bufferSize = E2EComm.DEFAULT_BUFFERSIZE;
        res = sendUnicast(
                dest,
                "".hashCode(), // destNodeId
                destPort,
                protocol,
                ack,
                GenericPacket.UNUSED_FIELD, // timeoutAck
                bufferSize,
                GenericPacket.UNUSED_FIELD, // packetTimeoutConnect
                payload
        );
        return res;
    }
    /*public static boolean sendUnicast(String[] dest, int destPort, int protocol, int bufferSize, InputStream payload) throws Exception{
        boolean res;
        boolean ack = false;
        res = sendUnicast(
                dest,
                "".hashCode(), // destNodeId
                destPort,
                protocol,
                ack,
                GenericPacket.UNUSED_FIELD, // timeoutAck
                bufferSize,
                GenericPacket.UNUSED_FIELD, // packetTimeoutConnect
                payload
        );
        return res;
    }*/
    /*public static boolean sendUnicast(String[] dest, int destPort, int protocol, boolean ack, int timeoutAck, int bufferSize, InputStream payload) throws Exception{
        boolean res;
        res = sendUnicast(
                dest,
                "".hashCode(), // destNodeId
                destPort,
                protocol,
                ack,
                timeoutAck,
                bufferSize,
                GenericPacket.UNUSED_FIELD, // packetTimeoutConnect
                payload
        );
        return res;
    }*/
    public static boolean sendUnicast(
    		String[] dest, 
    		int destNodeId, 
    		int destPort, 
    		int protocol, 
    		boolean ack, int timeoutAck, 
    		int bufferSize, 
    		short packetTimeoutConnect, // inter-node socket connect timeout (only TCP) 
    		InputStream payload
    		) throws Exception{

    	//System.out.println("E2EComm.sendUnicast InputStream");
    	
        boolean res = true;

        // check parameters
        if( bufferSize!=GenericPacket.UNUSED_FIELD && bufferSize!=0 && (bufferSize<5*1024 || bufferSize>1024*1024)){
            throw new Exception("bufferSize must be in the [5KB,1MB] range but current bufferSize = "+bufferSize);
        }
        if( (ack==true) && (timeoutAck<0)){
            throw new Exception("timeoutAck must be equal to or greater than 0 but current timeoutAck = "+timeoutAck);
        }
        if( (destNodeId=="".hashCode() ) && (dest==null || dest.length==0) ){
            throw new Exception("both dest and destNodeId are incorrect: dest="+dest+" destNodeId="+destNodeId);
        }
        if( protocol!=E2EComm.TCP ){
            throw new Exception("InputStream but protocol is not E2EComm.TCP (protocol="+protocol+")");
        }

        if( (bufferSize!=GenericPacket.UNUSED_FIELD) && (bufferSize==0)){
            bufferSize = E2EComm.DEFAULT_BUFFERSIZE;
        }
        
        // 1) setup packet
        int localNodeId = Dispatcher.getLocalId();
        UnicastHeader uh = new UnicastHeader(
                dest,
                destPort,
                destNodeId,
                localNodeId,
                ack,
                timeoutAck,
                (byte)0, // currentHop set to 0
                bufferSize,
                (byte)GenericPacket.UNUSED_FIELD, 	// (byte)retry,
                GenericPacket.UNUSED_FIELD,			// timeWait,
                packetTimeoutConnect
            );

        // send unicast header to the local Dispatcher
        SocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), Dispatcher.DISPATCHER_PORT);
        Socket socketToLocalhost = new Socket();
        socketToLocalhost.setReuseAddress(true);
        socketToLocalhost.bind(null);
        
        if(ack){
            uh.setSourcePortAck(socketToLocalhost.getLocalPort());
        }
        try{
            //System.out.println("E2EComm.sendUnicast TCP: localPort = "+socketToLocalhost.getLocalPort());
            socketToLocalhost.connect(socketAddress, TcpDispatcher.TCP_CONNECT_TIMEOUT);
        }
        catch(Exception e){
            System.out.println("E2EComm.sendUnicast TCP Exception: localPort = "+socketToLocalhost.getLocalPort());
            throw e;
        }
        
        OutputStream os = socketToLocalhost.getOutputStream();
        
        // send only the header
        //System.out.println("E2EComm.sendUnicast header tcp "+InetAddress.getLocalHost()+":"+Dispatcher.DISPATCHER_PORT);
    	E2EComm.writeObjectPacket(uh, os);
        
    	// while payload not completely received
        //      i) read bufferSize bytes from ingoing socket
        //      ii) write bufferSize bytes to outgoing socket
    	InputStream payloadInputStream = (InputStream)payload;

        BufferedOutputStream destBos = new BufferedOutputStream(socketToLocalhost.getOutputStream(), uh.getBufferSize());
    	BufferedInputStream bis = new BufferedInputStream(payloadInputStream, bufferSize);
        
        int count=0;
        int readBytes;
        boolean finished=false;
        byte[] buffer = new byte[uh.getBufferSize()];
        while(!finished){
            // attempt to read enough bytes to fulfill the buffer
        	readBytes = bis.read(buffer, count, buffer.length-count);
            //System.out.println("E2EComm partial payload read (partial): readBytes "+readBytes);
            if(readBytes==-1){
                finished = true;
            }
            else{
                count += readBytes;
                if(count == buffer.length){
                    // write only if the buffer is full
                    destBos.write(buffer, 0, buffer.length);
                    destBos.flush();
                	
                    //System.out.println("E2EComm partial payload written (partial): buffer.length "+buffer.length);
                    count=0;
                }
            }
        }
        if(count>0){
            // write remaining bytes
            destBos.write(buffer, 0, count);
            destBos.flush();
            //System.out.println("E2EComm partial payload written (final): count = "+count);
        }
        
        os.flush();
        socketToLocalhost.close();
        socketToLocalhost = null;

        // 3) ack
        if(ack==true){
            // receive ack
            GenericPacket gpAck = null;
            try{
                //System.out.println("E2EComm.sendUnicast tcp ack localPort = "+up.getSourcePortAck());
                ServerSocket ssAck = new ServerSocket(uh.getSourcePortAck(), TcpDispatcher.SERVER_SOCKET_BACKLOG);
                ssAck.setReuseAddress(true);
                ssAck.setSoTimeout(timeoutAck);

                // receive and close
                Socket s = ssAck.accept(); // throws SocketTimeoutException in case of timeout
                ssAck.close();

                // process payload
                InputStream is = s.getInputStream();
                gpAck = (GenericPacket)E2EComm.readObjectPacket(is);
            }
            catch(java.net.SocketTimeoutException ste){
                System.out.println("E2EComm.sendUnicast receive ack: ste = "+ste);
                res = false;
            }

            if(res==true){
                // check it is actually an ack
                if(gpAck instanceof UnicastPacket){
                    UnicastPacket upAck=(UnicastPacket)gpAck;

                    // XXX ??? check the sender of the ack is the same node in dest (an id in the ack???)

                    Object ackPayload = E2EComm.deserialize(upAck.getBytePayload());
                    if(ackPayload instanceof java.lang.String){
                        String ackPayloadString = (String)ackPayload;
                        if( ! ackPayloadString.equals("ack") ){
                            System.out.println("E2EComm.sendUnicast receive ack FALSE: ackPayloadString = "+ackPayloadString);
                            res = false;
                        }
                    }
                    else{
                        System.out.println("E2EComm.sendUnicast receive ack FALSE: ackPayload class = "+ackPayload.getClass().getName());
                        System.out.println("E2EComm.sendUnicast receive ack FALSE: ackPayload class = "+ackPayload.getClass().getSimpleName());
                        res = false;
                    }
                }
                else{
                    System.out.println("E2EComm.sendUnicast receive ack FALSE: gpAck class = "+gpAck.getClass().getName());
                    res = false;
                }
            }
        }

        return res;
    }


    public static String[] ipReverse(String[] dest){
        String[] res = new String[dest.length];
        for(int i=0; i<dest.length; i++){
            res[i] = dest[dest.length-i-1];
        }
        return res;
    }

    public static int bestBufferSize(int pathLength, long packetSize){
        int res = E2EComm.DEFAULT_BUFFERSIZE;
        if(pathLength==1){
            // single-hop path
            packetSize=GenericPacket.UNUSED_FIELD;
        }
        else{
            // multi-hop path
            if(packetSize>=5*1024*1024){
                // large file (>=5MB)
                res=100*1024;
            }
            else if(packetSize<=100*1024){
                // small file (<=100KB)
                if(pathLength<=2){
                    // short path
                    res=10*1024;
                }
                else{
                    // long path
                    res=5*1024;
                }
            }
        }
        return res;
    }
    
    public static int objectSize(Object object) {
        int objectSize = -1;
        try{
        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        	
        	//com.caucho.hessian.io.Hessian2Output out = new com.caucho.hessian.io.Hessian2Output(baos);
        	ObjectOutputStream out = new ObjectOutputStream(baos);
        	
            out.writeObject(object);
            out.close();
            objectSize = baos.toByteArray().length;
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return objectSize;
    }
    public static int objectSizePacket(GenericPacket gp) {
    	int objectSize = -1;
        if(RampEntryPoint.protobuf){
        	objectSize = gp.toProtosByteArray().length;
    	}
    	else{
    		objectSize = E2EComm.objectSize(gp);
    	}
        return objectSize;
    }

    public static byte[] serialize(Object obj) throws Exception {
    	byte[] serialized;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	
    	//com.caucho.hessian.io.Hessian2Output out = new com.caucho.hessian.io.Hessian2Output(baos);
    	ObjectOutputStream out = new ObjectOutputStream(baos);
    	
    	out.writeObject(obj);
        out.close();
        serialized = baos.toByteArray();
        return serialized;
    }
    public static byte[] serializePacket(GenericPacket gp) throws Exception{
    	byte[] serialized;
    	if(RampEntryPoint.protobuf){
        	byte[] protoBytes = gp.toProtosByteArray();
        	serialized = new byte[protoBytes.length+1];
        	serialized[0] = gp.getPacketId();
        	System.arraycopy(protoBytes, 0, serialized, 1, protoBytes.length);
        }
        else{
        	serialized = E2EComm.serialize(gp);
        }
    	return serialized;
    }
    public static Object deserialize(byte[] bytes) throws Exception {
    	return E2EComm.deserialize(bytes, 0, bytes.length);
    }
    public static Object deserialize(byte[] bytes, int offset, int length) throws Exception {
    	//System.out.println("E2EComm.deserialize bytes.length="+bytes.length);
    	Object deserialized;
    	ByteArrayInputStream bin = new ByteArrayInputStream(bytes, offset, length);
    	//ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
    	
    	//com.caucho.hessian.io.Hessian2Input in = new com.caucho.hessian.io.Hessian2Input(bin);
    	ObjectInputStream in = new ObjectInputStream(bin);
    	
    	deserialized = in.readObject();    
    	in.close();
    	bin.close();
    	return deserialized;
    }
    public static GenericPacket deserializePacket(byte[] bytes) throws Exception {
    	return E2EComm.deserializePacket(bytes, 0, bytes.length);
    }
    public static GenericPacket deserializePacket(byte[] bytes, int offset, int length) throws Exception {
    	GenericPacket res;
    	if(RampEntryPoint.protobuf){
    		byte packetId = bytes[offset];
    		if( packetId == UnicastHeader.PACKET_ID ){
    			res = UnicastHeader.parseFromProtos(bytes, offset+1, length-1);
    		}
    		else if( packetId == UnicastPacket.PACKET_ID ){
    			res = UnicastPacket.parseFromProtos(bytes, offset+1, length-1);
    		}
    		else if( packetId == BroadcastPacket.PACKET_ID ){
    			res = BroadcastPacket.parseFromProtos(bytes, offset+1, length-1);
    		}
    		else if( packetId == HeartbeatRequest.PACKET_ID ){
    			res = new HeartbeatRequest();
    		}
    		else if( packetId == HeartbeatResponse.PACKET_ID ){
    			res = new HeartbeatResponse();
    		}
    		else{
    			throw new Exception("E2EComm.deserializePacket: unknown packetId "+packetId);
    		}
    	}
    	else{
    		res = (GenericPacket)E2EComm.deserialize(bytes, 0, bytes.length);
    	}
    	return res;
    }

    public static Object readObject(InputStream is) throws Exception {
    	Object obj;
		
		//com.caucho.hessian.io.Hessian2Input in = new com.caucho.hessian.io.Hessian2Input(is);
		ObjectInputStream in = new ObjectInputStream(is);
		
		obj = in.readObject();
    	return obj;
    }
    public static GenericPacket readObjectPacket(InputStream is) throws Exception {
    	GenericPacket res;

    	if(RampEntryPoint.protobuf){
    		byte packetId = (byte)is.read();
    		if( packetId == UnicastHeader.PACKET_ID ){
    			res = UnicastHeader.parseFromProtos(is);
    		}
    		else if( packetId == UnicastPacket.PACKET_ID ){
    			res = UnicastPacket.parseFromProtos(is);
    		}
    		else if( packetId == BroadcastPacket.PACKET_ID ){
    			res = BroadcastPacket.parseFromProtos(is);
    		}
    		else if( packetId == HeartbeatRequest.PACKET_ID ){
    			res = new HeartbeatRequest();
    		}
    		else if( packetId == HeartbeatResponse.PACKET_ID ){
    			res = new HeartbeatResponse();
    		}
    		else{
    			throw new Exception("E2EComm.deserializePacket: unknown packetId "+packetId);
    		}
    	}
    	else{
    		return (GenericPacket)E2EComm.readObject(is);
    	}
    	
    	return res;
    }
    
    public static void writeObject(Object obj, OutputStream os) throws Exception{
    	//com.caucho.hessian.io.Hessian2Output out = new com.caucho.hessian.io.Hessian2Output(os);
    	ObjectOutputStream out = new ObjectOutputStream(os);
    	
    	out.writeObject(obj);
        //out.close();
    }
    public static void writeObjectPacket(GenericPacket gp, OutputStream os) throws Exception{
    	if(RampEntryPoint.protobuf){
    		//System.out.println("E2EComm.writeObjectPacket gp.getPacketId()="+gp.getPacketId()+" (int)gp.getPacketId()="+(int)gp.getPacketId());
    		os.write(gp.getPacketId());
    		gp.writeToProtos(os);
    	}
    	else{
    		E2EComm.writeObject(gp, os);
    	}
    }
        
}

