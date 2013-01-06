/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.service.application;

/**
 *
 * @author useruser
 */
public class FileSharingRequest implements java.io.Serializable {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1328809791835230307L;
	
	private String fileName;
    private int clientPort = -1;
    private byte[] file = null;
    private boolean get;
    
    public FileSharingRequest(boolean get, String fileName, int clientPort) {
    	this.get = get;
        this.fileName = fileName;
        this.clientPort = clientPort;
    }

    public int getClientPort() {
        return clientPort;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getFile() {
        return file;
    }

	public boolean isGet() {
		return get;
	}
    
}
