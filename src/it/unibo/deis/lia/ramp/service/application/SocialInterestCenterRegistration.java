
package it.unibo.deis.lia.ramp.service.application;

public class SocialInterestCenterRegistration implements java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2310440966210464046L;
	
	//private String interestName;
	private int clientPort;
	
	public SocialInterestCenterRegistration(
			//String interestName, 
			int clientPort) {
		//this.interestName = interestName;
		this.clientPort = clientPort;
	}

	/*public String getInterestName(){
		return interestName;
	}*/
	
	public int getClientPort(){
		return clientPort;
	}
	
}
