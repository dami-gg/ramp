package it.unibo.deis.lia.ramp.core.internode.upnp;

import java.io.Serializable;

public class UPnPUnicastMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1631268072934214389L;

	private int senderID;
	private String serviceAdd;
	private int servicePort;
	private int masterPort;
	private String uuid;
	private Object message;

	public UPnPUnicastMessage(int senderID, String serviceAdd, int servicePort, int masterPort, String uuid, Object message) {
		super();
		this.senderID = senderID;
		this.serviceAdd = serviceAdd;
		this.servicePort = servicePort;
		this.masterPort = masterPort;
		this.uuid = uuid;
		this.message = message;
	}

	public String getServiceAdd() {
		return serviceAdd;
	}

	public int getServicePort() {
		return servicePort;
	}

	public String getUuid() {
		return uuid;
	}

	public Object getMessage() {
		return message;
	}

	public int getSenderPort() {
		return masterPort;
	}

	public int getSenderID() {
		return senderID;
	}

}
