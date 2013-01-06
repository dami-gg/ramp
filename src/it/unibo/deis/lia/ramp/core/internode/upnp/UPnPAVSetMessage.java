package it.unibo.deis.lia.ramp.core.internode.upnp;

import java.io.Serializable;

public class UPnPAVSetMessage implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 72418027396003342L;
	private int aVmasterID;
	private int aVMasterPort;
	private String realAdd;
	private byte[] msg;


	public UPnPAVSetMessage(int aVmasterID, int aVMasterPort, String realAdd, byte[] msg) {
		super();
		this.aVmasterID = aVmasterID;
		this.aVMasterPort = aVMasterPort;
		this.realAdd = realAdd;
		this.msg = msg;
	}

	public int getaVmasterID() {
		return aVmasterID;
	}

	public int getRealPort() {
		return aVMasterPort;
	}

	public String getRealAdd() {
		return realAdd;
	}
	
	public byte[] getMsg() {
		return msg;
	}
	
}
