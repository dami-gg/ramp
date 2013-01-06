/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.service.management;


/**
 *
 * @author useruser
 */
public class ServiceResponse implements java.io.Serializable{
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 5994054093761340049L;
	
	private String serviceName;
    private String[] serverDest;
    private int serverPort;
    private int protocol;
    private String qos = null; // (optional)
    private int sourceNodeId = -1;

    public ServiceResponse(String serviceName, ServiceDescriptor sd) {
        this.serviceName = serviceName;
        this.serverPort = sd.getServerPort();
        this.protocol = sd.getProtocol();
        this.qos  = sd.getQos();
    }

    public ServiceResponse(String serviceName, int serverPort, int protocol) {
        this.serviceName = serviceName;
        this.serverPort = serverPort;
        this.protocol = protocol;
    }

    public ServiceResponse(String serviceName, int serverPort, int protocol, String qos) {
        this.serviceName = serviceName;
        this.serverPort = serverPort;
        this.protocol = protocol;
        this.qos = qos;
    }

    public ServiceResponse(String serviceName, int serverPort, int protocol, String qos, int sourceNodeId) {
        this.serviceName = serviceName;
        this.serverPort = serverPort;
        this.protocol = protocol;
        this.qos = qos;
        this.sourceNodeId = sourceNodeId;
    }
    
    public int getProtocol() {
        return protocol;
    }

    public String getQos() {
        return qos;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String[] getServerDest() {
        return serverDest;
    }

    public int getSourceNodeId() {
        return sourceNodeId;
    }

    public void setServerDest(String[] serverDest) {
        this.serverDest = serverDest;
    }

    public void setSourceNodeId (int sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    @Override
    public String toString() {
        String res = "";
        res += "[";
        if(serverDest!=null){
            for(int i=0; i<serverDest.length-1; i++){
                res += serverDest[i]+", ";
            }
            res += serverDest[serverDest.length-1];
        }
        else{
            res += "null";
        }
        res += "]:"+serverPort;
        return res;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final ServiceResponse other = (ServiceResponse) obj;

        if(this.serverDest==null || other.getServerDest()==null){
            return false;
        }

        if( this.serverDest.length != other.getServerDest().length ){
            return false;
        }

        for(int i=0; i< this.serverDest.length; i++){
            if( ! this.serverDest[i].equals(other.getServerDest()[i])){
                return false;
            }
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.serverDest != null ? this.serverDest.hashCode() : 0);
        return hash;
    }

    public String getServiceName() {
        return serviceName;
    }

}
