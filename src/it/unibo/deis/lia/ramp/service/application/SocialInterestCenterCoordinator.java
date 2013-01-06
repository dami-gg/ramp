
package it.unibo.deis.lia.ramp.service.application;

import it.unibo.deis.lia.ramp.core.e2e.*;

import java.util.*;

public class SocialInterestCenterCoordinator extends Thread {
		
	private String interestName;
	private String contentType;
	private String sparqlQuery;
	private int siccPort = -1;
	private int protocol;
	
	private boolean active = true;
	
	protected SocialInterestCenterCoordinator(String interestName, String contentType,
			String sparqlQuery, int protocol){
		this.interestName = interestName;
		this.contentType = contentType;
		this.sparqlQuery = sparqlQuery;
		this.protocol = protocol;
	}
	
	// remote node ==> last ack time
	private Hashtable<EndPoint, Long> registeredClients = new Hashtable<EndPoint, Long>();
	protected void sendMulticast(Object payload){
		Iterator<EndPoint> endpoints = registeredClients.keySet().iterator();
		while(endpoints.hasNext()){
			EndPoint endpoint = endpoints.next();
			if(registeredClients.get(endpoint) > 20*1000 ){
				endpoints.remove();
			}
			else{
				try {
					E2EComm.sendUnicast(
							endpoint.getAddress(), 
							endpoint.getClientPort(), 
							endpoint.getProtocol(),
                            E2EComm.serialize(payload)
						);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/*private String getInterestName(){
		return this.interestName;
	}*/
	
	/*private void putReceiver(EndPoint endpoint){
		remoteClients.put(endpoint, System.currentTimeMillis());
	}*/
	
	public void run(){
		try{
			BoundReceiveSocket siccSocket = E2EComm.bindPreReceive(protocol);
			this.siccPort = siccSocket.getLocalPort();
			new SiccBroadcaster().start();
			while(active){
				GenericPacket gp = E2EComm.receive(siccSocket);
				new SiccHandler(gp).start();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private class SiccBroadcaster extends Thread{
		public void run(){
			System.out.println("InterestCenterCoordinator.run: START "+interestName);
			/*while(siccPort == -1){
				try {
					sleep(250);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}*/
			SocialInterestCenterAdvertise sicAdvice = new SocialInterestCenterAdvertise(interestName,
					contentType, sparqlQuery, siccPort, protocol);
			try{
				while(active){
					E2EComm.sendBroadcast(3, -1, protocol, E2EComm.serialize(sicAdvice));
					sleep(5000);
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
			System.out.println("InterestCenterCoordinator.run: FINISHED "+interestName);
		}
	}
	
	private class SiccHandler extends Thread{
		
		private GenericPacket gp;
		
		private SiccHandler(GenericPacket gp){
			this.gp = gp;
		}
		
		public void run(){
			//System.out.println("SiccHandler: START "+interestName);
			try{
				if(gp instanceof UnicastPacket){
					UnicastPacket up = (UnicastPacket)gp;
					Object objectPayload = E2EComm.deserialize(up.getBytePayload());
					if(objectPayload instanceof SocialInterestCenterRegistration){
						SocialInterestCenterRegistration sicReg = (SocialInterestCenterRegistration)objectPayload;
	                	EndPoint endpoint = new EndPoint(
	                			sicReg.getClientPort(),
	                			up.getSourceNodeId(),
	                			E2EComm.ipReverse(up.getSource())
            			);
	                	registeredClients.put(endpoint, System.currentTimeMillis());
					}
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
}
