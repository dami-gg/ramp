/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.internode;

/**
 *
 * @author useruser
 */
class ResolverRequest implements java.io.Serializable{

    /**
	 * 
	 */
	private static final long serialVersionUID = -4230422813484349160L;
	
	//private String nodeId;
	private int nodeId;

    protected ResolverRequest(int nodeId) {
        this.nodeId = nodeId;
    }

    protected int getNodeId() {
        return nodeId;
    }
    
}
