/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.e2e;

import java.io.*;

/**
 *
 * @author useruser
 */
public class UnicastPacket extends GenericPacket 
							//implements Serializable { 
							implements Externalizable{ 
	
	
	public UnicastPacket(){} // only for Externalizable

    /**
	 * 
	 */
	private static final long serialVersionUID = -5794779426316569415L;
	public static transient final byte PACKET_ID = (byte)3;
	
	private UnicastHeader header;
	private byte[] bytePayload;
    public static final transient int MAX_DELAY_TOLERANT_PAYLOAD = 500*1024;
    
    public UnicastPacket(UnicastHeader header, byte[] payload) throws Exception{
        this.header = header;
        this.bytePayload = payload;
    }

    public UnicastPacket(String[] dest, int destPort, byte[] payload) throws Exception {
    	
        this.header=new UnicastHeader(
            dest,
            destPort,
            "".hashCode(),
            "".hashCode(),
            false,
            GenericPacket.UNUSED_FIELD,
            (byte)0,
            GenericPacket.UNUSED_FIELD,
            GenericPacket.UNUSED_FIELD,
            GenericPacket.UNUSED_FIELD,
            GenericPacket.UNUSED_FIELD
        );
         this.bytePayload = payload;
    }
    
    public UnicastPacket(
		        String[] dest,
		        int portaDest,
		        int destNodeId,
		        int sourceNodeId,
		        boolean ack,
		        int sourcePortAck,
		        byte currentHop,
		        int bufferSize,
		        byte retry,
		        int timeWait,
	            short connectTimeout,
	            byte[] payload
        	) throws Exception {
    	
        this.header=new UnicastHeader(
            dest,
            portaDest,
            destNodeId,
            sourceNodeId,
            ack,
            sourcePortAck,
            currentHop,
            bufferSize,
            retry,
            timeWait,
            connectTimeout
        );
        this.bytePayload = payload;
    }

    public UnicastPacket(UnicastHeader header, Object payload) throws Exception{
        this.header = header;

        // from object to byte[]
        this.bytePayload = E2EComm.serialize(payload);
    }

    public UnicastPacket(String[] dest, int destPort, Object payload) throws Exception {
    	
        this.header=new UnicastHeader(
            dest,
            destPort,
            "".hashCode(),
            "".hashCode(),
            false,
            GenericPacket.UNUSED_FIELD,
            (byte)0,
            GenericPacket.UNUSED_FIELD,
            GenericPacket.UNUSED_FIELD,
            GenericPacket.UNUSED_FIELD,
            GenericPacket.UNUSED_FIELD
        );
        
        // from object to byte[]
         this.bytePayload = E2EComm.serialize(payload);
    }
    
    public UnicastPacket(
		        String[] dest,
		        int portaDest,
		        int destNodeId,
		        int sourceNodeId,
		        boolean ack,
		        int sourcePortAck,
		        byte currentHop,
		        int bufferSize,
		        byte retry,
		        int timeWait,
	            short connectTimeout,
		        Object payload
        	) throws Exception {
    	
        this.header=new UnicastHeader(
            dest,
            portaDest,
            destNodeId,
            sourceNodeId,
            ack,
            sourcePortAck,
            currentHop,
            bufferSize,
            retry,
            timeWait,
            connectTimeout
        );
        
        // from object to byte[]
        this.bytePayload = E2EComm.serialize(payload);
        
    }

    public UnicastHeader getHeader() {
        return header;
    }

    public boolean isAck() {
        return header.isAck();
    }

    public byte getCurrentHop() {
        return header.getCurrentHop();
    }
    public void setCurrentHop(byte currentHop) {
        header.setCurrentHop(currentHop);
        
    }

    public String[] getDest() {
        return header.getDest();
    }
    public void setDest(String[] dest) {
        header.setDest(dest);
    }

    public int getBufferSize() {
        return header.getBufferSize();
    }
    public void setBufferSize(int bufferSize) {
        header.setBufferSize(bufferSize);
    }

    public int getDestNodeId() {
        return header.getDestNodeId();
    }

    public int getSourceNodeId() {
        return header.getSourceNodeId();
    }
    
    /*public Object getObjectPayload() throws Exception {
    	// from byte[] to object
    	Object objectPayload = E2EComm.deserialize(bytePayload);
        return objectPayload;
    }*/
    public byte[] getBytePayload() {
        return bytePayload;
    }

    public int getDestPort() {
        return header.getDestPort();
    }

    public int getSourcePortAck() {
        return header.getSourcePortAck();
    }
    public void setSourcePortAck(int portaSourceAck) {
        header.setSourcePortAck(portaSourceAck);
    }

    public byte getRetry() {
        return header.getRetry();
    }
    public void setRetry(byte retry) {
        header.setRetry(retry);
    }
    public int getTimeWait() {
        return header.getTimeWait();
    }
    public void setTimeWait(int timeWait) {
        header.setTimeWait(timeWait);
    }
    
    public short getConnectTimeout() {
		return header.getConnectTimeout();
	}
    
    public String[] getSource() {
        return header.getSource();
    }
    public void addSource(String aSource) {
        header.addSource(aSource);
    }
    
    /**/private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    	out.writeByte(header.getDestInt().length);
    	for(int i=0; i<header.getDestInt().length; i++)
    		out.writeInt(header.getDestInt()[i]);
    	
    	out.writeByte(header.getSourceInt().length);
    	for(int i=0; i<header.getSourceInt().length; i++)
    		out.writeInt(header.getSourceInt()[i]);
    	
    	out.writeShort(header.getDestPortShort());
    	out.writeInt(header.getDestNodeId());
    	out.writeInt(header.getSourceNodeId());
    	out.writeBoolean(header.isAck());
    	out.writeShort(header.getSourcePortAckShort());
    	out.writeByte(header.getCurrentHop());
    	out.writeInt(header.getBufferSize());
    	out.writeByte(header.getRetry());
    	out.writeInt(header.getTimeWait());
    	out.writeShort(header.getConnectTimeout());
    	
    	//System.out.println("UnicastPacket.writeObject bytePayload.length="+bytePayload.length);
    	out.writeInt(bytePayload.length);
    	out.write(bytePayload, 0, bytePayload.length);
    	out.flush();
    }
	private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
		byte destLength = in.readByte();
		int[] dest = new int[destLength];
    	for(int i=0; i<dest.length; i++)
    		dest[i] = in.readInt();
    	
    	byte sourceLength = in.readByte();
		int[] source = new int[sourceLength];
    	for(int i=0; i<source.length; i++)
    		source[i] = in.readInt();
    	
    	short destPort = in.readShort();
    	int destNodeId = in.readInt();
    	int sourceNodeId = in.readInt();
    	boolean ack = in.readBoolean();
    	short sourcePortAck = in.readShort();
    	byte currentHop = in.readByte();
    	int bufferSize = in.readInt();
    	byte retry = in.readByte();
    	int timeWait = in.readInt();
    	short connectTimeout = in.readShort();
    	
		this.header = new UnicastHeader(
				dest,
	    	    source,
	    		destPort,
	    		destNodeId,
	    		sourceNodeId,
	    		ack,
	    		sourcePortAck,
	    	    currentHop,
	    	    bufferSize,
	    	    retry,
	    	    timeWait,
	    	    connectTimeout
    	    );
		
		int payloadLength = in.readInt();
		//System.out.println("UnicastPacket.readObject payloadLength="+payloadLength);
		this.bytePayload = new byte[payloadLength];
		in.readFully(this.bytePayload,0,payloadLength);
	}/**/

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {

		byte destLength = in.readByte();
		int[] dest = new int[destLength];
    	for(int i=0; i<dest.length; i++)
    		dest[i] = in.readInt();
    	
    	byte sourceLength = in.readByte();
		int[] source = new int[sourceLength];
    	for(int i=0; i<source.length; i++)
    		source[i] = in.readInt();
    	
    	short destPort = in.readShort();
    	int destNodeId = in.readInt();
    	int sourceNodeId = in.readInt();
    	boolean ack = in.readBoolean();
    	short sourcePortAck = in.readShort();
    	byte currentHop = in.readByte();
    	int bufferSize = in.readInt();
    	byte retry = in.readByte();
    	int timeWait = in.readInt();
    	short connectTimeout = in.readShort();
    	
		this.header = new UnicastHeader(
				dest,
	    	    source,
	    		destPort,
	    		destNodeId,
	    		sourceNodeId,
	    		ack,
	    		sourcePortAck,
	    	    currentHop,
	    	    bufferSize,
	    	    retry,
	    	    timeWait,
	    	    connectTimeout
    	    );
		
		int payloadLength = in.readInt();
		//System.out.println("Unicastpacket.readExternal payloadLength="+payloadLength);
		this.bytePayload = new byte[payloadLength];
		in.readFully(this.bytePayload,0,payloadLength);
		//System.out.println("Unicastpacket.readExternal END");
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
    	out.writeByte(header.getDestInt().length);
    	for(int i=0; i<header.getDestInt().length; i++)
    		out.writeInt(header.getDestInt()[i]);
    	
    	out.writeByte(header.getSourceInt().length);
    	for(int i=0; i<header.getSourceInt().length; i++)
    		out.writeInt(header.getSourceInt()[i]);
    	
    	out.writeShort(header.getDestPortShort());
    	out.writeInt(header.getDestNodeId());
    	out.writeInt(header.getSourceNodeId());
    	out.writeBoolean(header.isAck());
    	out.writeShort(header.getSourcePortAckShort());
    	out.writeByte(header.getCurrentHop());
    	out.writeInt(header.getBufferSize());
    	out.writeByte(header.getRetry());
    	out.writeInt(header.getTimeWait());
    	out.writeShort(header.getConnectTimeout());
    	
    	out.writeInt(bytePayload.length);
		//System.out.println("Unicastpacket.writeExternal bytePayload.length="+bytePayload.length);
    	out.write(bytePayload, 0, bytePayload.length);
    	out.flush();
	}/**/
	
	public void writeToProtos(java.io.OutputStream os) throws java.io.IOException {
    	createProtosUnicastPacket().writeDelimitedTo(os);
    }
	public byte[] toProtosByteArray(){
		return createProtosUnicastPacket().toByteArray();
	}
	private RampPacketsProtos.UnicastPacket createProtosUnicastPacket(){
		RampPacketsProtos.UnicastPacket.Builder upProtobufBuilder = RampPacketsProtos.UnicastPacket.newBuilder();

    	for(int i=0; i<header.getDestInt().length; i++)
    		upProtobufBuilder.addDest(header.getDestInt()[i]);
    	
    	for(int i=0; i<header.getSourceInt().length; i++)
    		upProtobufBuilder.addSource(header.getSourceInt()[i]);
    	
    	upProtobufBuilder.setDestPort(header.getDestPort());
    	upProtobufBuilder.setDestNodeId(header.getDestNodeId());
    	upProtobufBuilder.setSourceNodeId(header.getSourceNodeId());
    	upProtobufBuilder.setAck(header.isAck());
    	upProtobufBuilder.setSourcePortAck(header.getSourcePortAck());
    	upProtobufBuilder.setCurrentHop(header.getCurrentHop());
    	upProtobufBuilder.setBufferSize(header.getBufferSize());
    	upProtobufBuilder.setRetry(header.getRetry());
    	upProtobufBuilder.setTimeWait(header.getTimeWait());
    	upProtobufBuilder.setConnectTimeout(header.getConnectTimeout());

    	if(bytePayload==null){
    		bytePayload = new byte[0];
    	}
    	upProtobufBuilder.setPayload(com.google.protobuf.ByteString.copyFrom(bytePayload));
    	
		RampPacketsProtos.UnicastPacket uhProtobuf = upProtobufBuilder.build();
		return uhProtobuf;
	}

	public static UnicastPacket parseFromProtos(InputStream is) throws IOException{
		RampPacketsProtos.UnicastPacket upProtobuf = RampPacketsProtos.UnicastPacket.parseDelimitedFrom(is);
		return createUnicastHeader(upProtobuf);
	}
	public static UnicastPacket parseFromProtos(byte[] bytes, int offset, int length) throws IOException{
		RampPacketsProtos.UnicastPacket upProtobuf = RampPacketsProtos.UnicastPacket.newBuilder().mergeFrom(bytes,offset,length).build();
		return createUnicastHeader(upProtobuf);
	}
	private static UnicastPacket createUnicastHeader(RampPacketsProtos.UnicastPacket upProtobuf){
		UnicastHeader uh = new UnicastHeader();
		
		int[] dest = new int[upProtobuf.getDestCount()];
		for(int i=0; i<upProtobuf.getDestCount(); i++)
			dest[i] = upProtobuf.getDest(i);
		uh.setDestInt(dest);

		int[] source = new int[upProtobuf.getSourceCount()];
		for(int i=0; i<upProtobuf.getSourceCount(); i++)
			source[i] = upProtobuf.getSource(i);
		uh.setSourceInt(source);
    	
    	uh.setDestPort(upProtobuf.getDestPort());
    	uh.setDestNodeId(upProtobuf.getDestNodeId());
    	uh.setSourceNodeId(upProtobuf.getSourceNodeId());
    	uh.setAck(upProtobuf.getAck());
    	uh.setSourcePortAck(upProtobuf.getSourcePortAck());
    	uh.setCurrentHop((byte)upProtobuf.getCurrentHop());
    	uh.setBufferSize(upProtobuf.getBufferSize());
    	uh.setRetry((byte)upProtobuf.getRetry());
    	uh.setTimeWait(upProtobuf.getTimeWait());
    	uh.setConnectTimeout((short)upProtobuf.getConnectTimeout());
    	
    	UnicastPacket up = new UnicastPacket();
    	up.header = uh;
    	up.bytePayload = upProtobuf.getPayload().toByteArray();
    	
		return up;
	}

	@Override
	public byte getPacketId() {
		return PACKET_ID;
	}
	
}
