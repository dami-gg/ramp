package it.unibo.deis.lia.ramp.core.internode.upnp;

import java.io.Serializable;

public class UPnPMulticastMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7724873421042978605L;

	private String senderAddress;
	private int senderPort;
	private int masterID;
	private int masterPort;

	private String messageType;
	private String uuid;// null on search request
	private String nTS;// null on search request; alive or bye-bye in
						// notification messages
	private int mX;// number of second; null in notification messages

	private byte[] multicastUPnPmessagePayload;

	public UPnPMulticastMessage(String senderAddress, int senderPort, int masterID, int masterPort, String messageType, String uuid, String nTS, int mX, byte[] multicastUPnPmessagePayload) {
		super();
		this.senderAddress = senderAddress;
		this.senderPort = senderPort;
		this.masterID = masterID;
		this.masterPort = masterPort;
		this.messageType = messageType;
		this.uuid = uuid;
		this.nTS = nTS;
		this.mX = mX;
		this.multicastUPnPmessagePayload = multicastUPnPmessagePayload;
	}

	public String getSenderAddress() {
		return senderAddress;
	}

	public int getSenderPort() {
		return senderPort;
	}

	public int getMasterID() {
		return masterID;
	}

	public int getMasterPort() {
		return masterPort;
	}

	public String getMessageType() {
		return messageType;
	}

	public String getUuid() {
		return uuid;
	}

	public String getNTS() {
		return nTS;
	}

	public int getmX() {
		return mX;
	}

	public byte[] getMulticastUPnPmessagePayload() {
		return multicastUPnPmessagePayload;
	}

}
