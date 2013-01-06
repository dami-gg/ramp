/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.e2e;

import java.io.*;

//import java.io.*;

/**
 *
 * @author useruser
 */
public class BroadcastPacket extends GenericPacket implements Externalizable {
	
	public BroadcastPacket(){} // only for Externalizable

    /**
	 * 
	 */
	private static final long serialVersionUID = -3283510252549941617L;
	public static final transient byte PACKET_ID = (byte)2;

	public static final transient int MAX_BROADCAST_PAYLOAD = 60*1024;

	private int[] source;
    private int[] traversedIds;
    private byte ttl;
    private short destPort;
    private int sourceNodeId;
    private byte[] bytePayload;
    
    public BroadcastPacket(
    		byte ttl, 
            int destPort,
            int sourceNodeId,
            byte[] payload
        ) throws Exception  {
	
    this.ttl = ttl;
    this.destPort = (short)(destPort + Short.MIN_VALUE);
    this.source = new int[0];
    this.traversedIds = new int[0];
    this.sourceNodeId = sourceNodeId;
    this.bytePayload = payload;
}

    public byte getTtl() {
        return ttl;
    }
    public void setTtl(byte ttl) {
        this.ttl = ttl;
    }

    public int getSourceNodeId() {
        return sourceNodeId;
    }
    
    public String[] getSource() {
    	String[] resSource = new String[source.length];
    	for(int i=0; i<source.length; i++){
    		resSource[i] = i2s(source[i]);
    	}
    	return resSource;
    }
    public void addSource(String aSource) {
    	int[] newSource = new int[source.length+1];
        System.arraycopy(this.source, 0, newSource, 0, this.source.length);
        newSource[newSource.length-1] = s2i(aSource);
        this.source = newSource;
    }
    
    public int[] getTraversedIds(){
    	return this.traversedIds;
    }
    public void addTraversedId(int nodeId){
    	int[] newTraversedIds = new int[traversedIds.length+1];
        System.arraycopy(this.traversedIds, 0, newTraversedIds, 0, this.traversedIds.length);
        newTraversedIds[newTraversedIds.length-1] = nodeId;
        this.traversedIds = newTraversedIds;
    }
    public boolean alreadyTraversed(int nodeId){
    	for(int i=0; i<traversedIds.length; i++){
    		if(nodeId==traversedIds[i]){
    			return true;
    		}
    	}
    	return false;
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
        return destPort - Short.MIN_VALUE;
    }
    
    /*private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    	out.writeByte(source.length);
    	for(int i=0; i<source.length; i++)
    		out.writeInt(source[i]);
    	
    	out.writeByte(traversedIds.length);
    	for(int i=0; i<traversedIds.length; i++)
    		out.writeInt(traversedIds[i]);
    	
    	out.writeByte(TTL);
    	out.writeShort(destPort);
    	out.writeInt(sourceNodeId);
    	
    	out.writeInt(bytePayload.length);
    	out.write(bytePayload);
    	out.flush();
    }
	private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
		byte sourceLength = in.readByte();
		this.source = new int[sourceLength];
    	for(int i=0; i<this.source.length; i++)
    		this.source[i] = in.readInt();
    	
    	byte traversedIdsLength = in.readByte();
		this.traversedIds = new int[traversedIdsLength];
    	for(int i=0; i<this.traversedIds.length; i++)
    		this.traversedIds[i] = in.readInt();
    	
    	this.TTL = in.readByte();
    	this.destPort = in.readShort();
    	this.sourceNodeId = in.readInt();
    	
		int payloadLength = in.readInt();
		this.bytePayload = new byte[payloadLength];
		in.readFully(this.bytePayload,0,payloadLength);
	}*/

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		byte sourceLength = in.readByte();
		this.source = new int[sourceLength];
    	for(int i=0; i<this.source.length; i++)
    		this.source[i] = in.readInt();
    	
    	byte traversedIdsLength = in.readByte();
		this.traversedIds = new int[traversedIdsLength];
    	for(int i=0; i<this.traversedIds.length; i++)
    		this.traversedIds[i] = in.readInt();
    	
    	this.ttl = in.readByte();
    	this.destPort = in.readShort();
    	this.sourceNodeId = in.readInt();
    	
		int payloadLength = in.readInt();
		this.bytePayload = new byte[payloadLength];
		in.readFully(this.bytePayload,0,payloadLength);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(source.length);
    	for(int i=0; i<source.length; i++)
    		out.writeInt(source[i]);
    	
    	out.writeByte(traversedIds.length);
    	for(int i=0; i<traversedIds.length; i++)
    		out.writeInt(traversedIds[i]);
    	
    	out.writeByte(ttl);
    	out.writeShort(destPort);
    	out.writeInt(sourceNodeId);
    	
    	out.writeInt(bytePayload.length);
    	out.write(bytePayload);
    	out.flush();
	}
	
	public void writeToProtos(java.io.OutputStream os) throws java.io.IOException {
    	createProtosBroadcastPacket().writeDelimitedTo(os);
    }
	public byte[] toProtosByteArray(){
		return createProtosBroadcastPacket().toByteArray();
	}
	private RampPacketsProtos.BroadcastPacket createProtosBroadcastPacket(){
		RampPacketsProtos.BroadcastPacket.Builder bpProtobufBuilder = RampPacketsProtos.BroadcastPacket.newBuilder();
		
    	for(int i=0; i<source.length; i++)
    		bpProtobufBuilder.addSource(source[i]);

    	for(int i=0; i<traversedIds.length; i++)
    		bpProtobufBuilder.addTraversedIds(traversedIds[i]);
    	
    	bpProtobufBuilder.setTtl(ttl);
    	bpProtobufBuilder.setDestPort(destPort);
    	bpProtobufBuilder.setSourceNodeId(sourceNodeId);
    	
    	if(bytePayload==null){
    		bytePayload = new byte[0];
    	}
		bpProtobufBuilder.setPayload(com.google.protobuf.ByteString.copyFrom(bytePayload));
		
		RampPacketsProtos.BroadcastPacket bpProtobuf = bpProtobufBuilder.build();
		return bpProtobuf;
	}
	
	public static BroadcastPacket parseFromProtos(InputStream is) throws IOException{
		RampPacketsProtos.BroadcastPacket bpProtobuf = RampPacketsProtos.BroadcastPacket.parseDelimitedFrom(is);
		return createBroadcastPacket(bpProtobuf);
	}
	public static BroadcastPacket parseFromProtos(byte[] bytes, int offset, int length) throws IOException{
		RampPacketsProtos.BroadcastPacket bpProtobuf = RampPacketsProtos.BroadcastPacket.newBuilder().mergeFrom(bytes,offset,length).build();
		return createBroadcastPacket(bpProtobuf);
	}
	private static BroadcastPacket createBroadcastPacket(RampPacketsProtos.BroadcastPacket bpProtobuf){
		BroadcastPacket bp = new BroadcastPacket();
		
		bp.source = new int[bpProtobuf.getSourceCount()];
		for(int i=0; i<bpProtobuf.getSourceCount(); i++)
			bp.source[i] = bpProtobuf.getSource(i);
		
		bp.traversedIds = new int[bpProtobuf.getTraversedIdsCount()];
		for(int i=0; i<bpProtobuf.getTraversedIdsCount(); i++)
			bp.traversedIds[i] = bpProtobuf.getTraversedIds(i);
		
		bp.ttl = (byte)bpProtobuf.getTtl();
		bp.destPort = (short)bpProtobuf.getDestPort();
    	bp.sourceNodeId = bpProtobuf.getSourceNodeId();
    	
    	bp.bytePayload = bpProtobuf.getPayload().toByteArray();
    	
		return bp;
	}
	
	@Override
	public byte getPacketId() {
		return PACKET_ID;
	}
	
}
