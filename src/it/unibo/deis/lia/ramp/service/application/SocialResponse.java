
package it.unibo.deis.lia.ramp.service.application;

public class SocialResponse implements java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2058141001457542677L;
	
	private String mboxSha1;
	private byte[] interests;
	
	public SocialResponse(String mboxSha1, byte[] interests) {
		this.mboxSha1 = mboxSha1;
		this.interests = interests;
	}
	
	public String getMboxSha1() {
		return mboxSha1;
	}

	public byte[] getInterests() {
		return interests;
	}
	
}
