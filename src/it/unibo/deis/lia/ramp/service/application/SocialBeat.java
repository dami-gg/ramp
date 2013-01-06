
package it.unibo.deis.lia.ramp.service.application;

import it.unibo.deis.lia.ramp.service.management.*;

public class SocialBeat implements java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4345824496775043504L;
	
	private String mboxSha1sum;
	private int socialServicePort;
	//private String[] interestCenters;
	
	//public SocialBeat(String mboxSha1sum, String[] interestCenters) {
	public SocialBeat(String mboxSha1sum) {
		this.mboxSha1sum = mboxSha1sum;
		this.socialServicePort = ServiceManager.getInstance(false).getService("Social").getServerPort();
		//this.interestCenters = interestCenters;
	}

	public String getMboxSha1sum() {
		return mboxSha1sum;
	}
	
	public int getSocialServicePort() {
		return socialServicePort;
	}

	/*public String[] getInterestCenters() {
		return interestCenters;
	}*/
	
}
