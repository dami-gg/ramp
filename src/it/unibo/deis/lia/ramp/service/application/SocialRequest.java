
package it.unibo.deis.lia.ramp.service.application;

public class SocialRequest implements java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2058141001457542677L;
	
	private String mboxSha1;
	
	public SocialRequest(String mboxSha1) {
		this.mboxSha1 = mboxSha1;
	}
	
	public String getMboxSha1() {
		return mboxSha1;
	}
	
}
