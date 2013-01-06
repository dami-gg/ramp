/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.internode;

import it.unibo.deis.lia.ramp.core.e2e.RampPacketsProtos;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author useruser
 */
public class HeartbeatRequest extends it.unibo.deis.lia.ramp.core.e2e.GenericPacket implements java.io.Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6877831114267103030L;
	public static transient final byte PACKET_ID = (byte)4;
	private final transient RampPacketsProtos.HeartbeatRequest hrProtobuf = RampPacketsProtos.HeartbeatRequest.newBuilder().build();
	
	@Override
	public byte[] toProtosByteArray() {
		return hrProtobuf.toByteArray();
	}

	@Override
	public byte getPacketId() {
		return PACKET_ID;
	}

	@Override
	public void writeToProtos(OutputStream os) throws IOException {
		hrProtobuf.writeTo(os);
	}

}
