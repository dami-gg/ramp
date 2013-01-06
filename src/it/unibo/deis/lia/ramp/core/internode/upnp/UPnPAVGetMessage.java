package it.unibo.deis.lia.ramp.core.internode.upnp;

import java.io.Serializable;

public class UPnPAVGetMessage implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -9150406371645780530L;

	
	
	private int masterID;
	private int masterPort;
	private String serviceAdd;
	private int servicePort;
	private byte[] mess;
	
	public UPnPAVGetMessage(int masterID, int masterPort, String serviceAdd, int servicePort, byte[] mess) {
		super();
		this.masterID = masterID;
		this.masterPort = masterPort;
		this.serviceAdd = serviceAdd;
		this.servicePort = servicePort;
		this.mess = mess;
	}

	public int getMasterID() {
		return masterID;
	}

	public int getMasterPort() {
		return masterPort;
	}

	public String getServiceAdd() {
		return serviceAdd;
	}

	public int getServicePort() {
		return servicePort;
	}

	public byte[] getMess() {
		return mess;
	}
	
	
	
	
}
