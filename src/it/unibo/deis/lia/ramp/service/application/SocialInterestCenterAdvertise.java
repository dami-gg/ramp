
package it.unibo.deis.lia.ramp.service.application;

public class SocialInterestCenterAdvertise implements java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 154413224961131058L;
	
	private String interestName;
	private String contentType; // e.g., "text", "a/v", ...
	private String sparqlQuery;
	
	private int siccPort;
	private int protocol;
	
	public SocialInterestCenterAdvertise(String interestName, String contentType,
			String sparqlQuery, int siccPort, int protocol) {
		super();
		this.interestName = interestName;
		this.contentType = contentType;
		this.sparqlQuery = sparqlQuery;
		this.siccPort = siccPort;
		this.protocol = protocol;
	}
	
	public String getInterestName() {
		return interestName;
	}
	public String getContentType() {
		return contentType;
	}
	public String getSparqlQuery() {
		return sparqlQuery;
	}
	public int getSiccPort() {
		return siccPort;
	}
	public int getProtocol() {
		return protocol;
	}
	
}
