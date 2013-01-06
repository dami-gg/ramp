/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.service.application;

//import java.net.*;

import java.util.Arrays;

//import java.util.*;

/**
 *
 * @author useruser
 */
public class EndPoint {
    private int clientPort;
    private int nodeId;
    private String[] address;
    private int protocol = -1;

    public EndPoint(int clientPort, int nodeId, String[] address) {
        this.clientPort = clientPort;
        this.nodeId = nodeId;
        this.address = address;
    }
    public EndPoint(int clientPort, int nodeId, String[] address, int protocol) {
        this.clientPort = clientPort;
        this.nodeId = nodeId;
        this.address = address;
        this.protocol = protocol;
    }

    public String[] getAddress() {
        return address;
    }

    public int getNodeId() {
        return nodeId;
    }
    
    public int getClientPort() {
        return clientPort;
    }
    
    public int getProtocol(){
    	return protocol;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EndPoint other = (EndPoint) obj;
        if (this.clientPort != other.clientPort) {
            return false;
        }
        if (!Arrays.equals(this.address, other.address)) {
            return false;
        }
        //if ((this.nodeId == null) ? (other.nodeId != null) : !this.nodeId.equals(other.nodeId)) {
        if ( this.nodeId != other.nodeId ) {
            return false;
        }
        if (this.protocol != other.protocol) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + this.clientPort;
        hash = 67 * hash + this.nodeId;
        hash = 67 * hash + Arrays.deepHashCode(this.address);
        hash = 67 * hash + this.protocol;
        return hash;
    }

    @Override
    public String toString() {
        /*String stringAddress = "";
        for(String s : address){
            stringAddress += s+" ";
        }*/
    	if(protocol!=-1){
    		return nodeId+" "+Arrays.toString(address)+":"+clientPort+" via "+protocol;
    	}
    	else{
    		return nodeId+" "+Arrays.toString(address)+":"+clientPort;
    	}
    }

}
