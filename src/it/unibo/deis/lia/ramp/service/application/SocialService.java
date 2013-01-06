
package it.unibo.deis.lia.ramp.service.application;

import java.io.File;
import java.util.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.reasoner.*;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.util.FileManager;

import it.unibo.deis.lia.ramp.core.e2e.*;
import it.unibo.deis.lia.ramp.core.internode.*;
import it.unibo.deis.lia.ramp.service.management.ServiceManager;

// Spontaneous Local Social Networking Service

public class SocialService extends Thread{
	
	// TODO dynamically retrieved from the local foaf ???
	//private Set<String> activeTopicInterests = new HashSet<String>();
	
	private int socialBeatTtl = 3;
	
	private DataSource dataSource = null;
	private String myMboxSha1sum = null;
	private Reasoner foafRreasoner;
	
	private static SocialService instance = null;
	private SocialPresenceBroadcaster socialBroadcaster = null;
	private SocialListener socialListener = null;
	private boolean active = true;
	
	// mbox_sha1 ==> RAMP address
	private Hashtable<Person, String[]> friendsLocation = new Hashtable<Person, String[]>();
	private Hashtable<Person, String[]> peopleLocation = new Hashtable<Person, String[]>();
	
	// interest name ==> receiver
	private Hashtable<String, SocialInterestCenterReceiver> interestNameReceivers = new Hashtable<String, SocialInterestCenterReceiver>();
	// content type ==> receiver
	private Hashtable<String, SocialInterestCenterReceiver> contentTypeReceivers = new Hashtable<String, SocialInterestCenterReceiver>();
	
	synchronized public static SocialService getInstance(){
		if(instance == null){
			System.out.println("SocialService START");
			instance = new SocialService();
			instance.start();
		}
		return instance;
	}
	
	synchronized public void stopService(){
		System.out.println("SocialService ENDING");
		active = false;
		if(socialBroadcaster != null){
			socialBroadcaster.stopSocialBroadcaster();
			socialBroadcaster = null;
		}
		if(socialListener != null){
			Dispatcher.getInstance(false).removePacketForwardingListener(socialListener);
//			socialListener.stopSocialListener();
			socialListener = null;
		}
		instance = null;
		ServiceManager.getInstance(false).removeService("Social");
		System.out.println("SocialService FINISHED");
	}
	
	// private SpontLocalSocialNetService(){ }
	
	public void run(){
		System.out.println("SocialService START");

		// TODO 0) username provided by the user
		Scanner keyboard = new Scanner(System.in);
		System.out.println("firstName");
		String firstName = keyboard.nextLine();
		System.out.println("surname");
		String surname = keyboard.nextLine();

		// 1) retrieve the local user mbox_sha1 via plain xml
		try{
			//File file = new File("./data/social/foaf_CarloGiannelli.rdf");
			File file = new File("./data/social/foaf_"+firstName+surname+".rdf");
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
	  
			Element root = doc.getDocumentElement();
			//System.out.println("Root element " + root.getNodeName());
	  
			Element person = null;
			Node child = root.getFirstChild();
			while (child!=null && person==null) {
				//System.out.println("child " + child);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					//System.out.println("(Element)child " + (Element)child);
					if( ((Element)child).getNodeName().equals("foaf:Person") ){
						person = (Element)child;
					}
				}
				child = child.getNextSibling();
			}
			//System.out.println("person " + person.getNodeName());
		  
			Node childOfPerson = person.getFirstChild();
			while (childOfPerson!=null  && myMboxSha1sum==null) {
				//System.out.println("child " + child);
				if ( childOfPerson.getNodeType() == Node.ELEMENT_NODE ) {
					//System.out.println("(Element)childOfPerson " + (Element)childOfPerson);
					Element el = (Element)childOfPerson;
					String elName = el.getNodeName();
					//System.out.println("elName " + elName);
					if(elName.equals("foaf:mbox_sha1sum")){
						myMboxSha1sum = el.getFirstChild().getNodeValue();
					}
				}
				childOfPerson = childOfPerson.getNextSibling();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		System.out.println("foaf:mbox_sha1sum " + myMboxSha1sum);
		
		// 2) setup the datasource
		Model defaultModel = FileManager.get().loadModel("./data/social/foaf_"+firstName+surname+".rdf");
		
		Statement st = defaultModel.listStatements(null, FOAF.mbox_sha1sum, myMboxSha1sum).next();
		Resource subject = st.getSubject();
		Property property = defaultModel.createProperty(SocialService.RAMP_URI, "localMboxSha1");
		Statement localMBoxSha1Statement = defaultModel.createStatement(subject, property, myMboxSha1sum);
		defaultModel.add(localMBoxSha1Statement);
		//Model modelCG = FileManager.get().loadModel("./data/social/foaf_CarloGiannelli.rdf");
        //Model modelFP = FileManager.get().loadModel("./data/social/foaf_FrancescaPrati.rdf");
        dataSource = DatasetFactory.create(defaultModel);
        Model groupsModel = FileManager.get().loadModel("./data/social/foaf_groups.rdf");
        dataSource.addNamedModel("groups", groupsModel);
        
        // 3) setup the reasoner
        Model ontModel = ModelFactory.createDefaultModel();
        ontModel.read("file:./data/social/foaf_ontology.owl");
        Reasoner owlRreasoner = ReasonerRegistry.getOWLReasoner();
        foafRreasoner = owlRreasoner.bindSchema(ontModel);
        
        // 4) start SocialBroadcaster
		socialBroadcaster = new SocialPresenceBroadcaster();
		socialBroadcaster.start();
		
		// 5) start the listener
		socialListener = new SocialListener();
		Dispatcher.getInstance(false).addPacketForwardingListener(socialListener);
//		socialListener.start();
		
		try {
			// 6) publish the service
			BoundReceiveSocket serviceSocket;
			serviceSocket = E2EComm.bindPreReceive(E2EComm.TCP);
			ServiceManager.getInstance(false).registerService("Social", serviceSocket.getLocalPort(), E2EComm.TCP);
			
			while(active){
				try {
					// wait for service requests
					GenericPacket gp = E2EComm.receive(serviceSocket);
					new SocialRequestHandler(gp).start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		System.out.println("SocialService.run FINISHED");
	}
	
	private class SocialRequestHandler extends Thread {
		
		private GenericPacket gp;
		
		private SocialRequestHandler(GenericPacket gp){
			this.gp = gp;
		}
		
		public void run(){
            // check packet type
			try {
	            if( gp instanceof UnicastPacket){
	                // check payload
	                UnicastPacket up = (UnicastPacket)gp;
	                Object payload;
						payload = E2EComm.deserialize(up.getBytePayload());
	                if(payload instanceof SocialRequest){
	                    System.out.println("SocialService SocialRequest");
	                	SocialRequest request = (SocialRequest)payload;
	
	                	// requiring the local user profile
	                	if(friendsLocation.containsKey(request.getMboxSha1())){
	                		// TODO provide friends and local user interests 
	                	}
	                	else{
	                		// TODO provide only local user interests tagged as public
	                	}
	                }
	            }
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class SocialPresenceBroadcaster extends Thread {
		
		private boolean activeSocialBroadcaster = true;
		private long socialBroadcasterPeriod = 5000;
		
		@Override
		public void run() {
			System.out.println("SocialBroadcaster.run START");
			
			while(activeSocialBroadcaster){
				try {
					// TODO modify socialBeat to include even public user interests
					// TODO either UDP or TCP???
					
					SocialBeat socialBeat = null;
					do{
						try {
							sleep(1000);
							socialBeat = new SocialBeat(
								myMboxSha1sum
								//, interestCenterCoordinators.keySet().toArray(new String[0])
							);
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
					while(socialBeat == null);
					
					E2EComm.sendBroadcast(
						socialBeatTtl, 
						-1, // invalid port 
						E2EComm.UDP, 
						E2EComm.serialize(socialBeat)
					);
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					sleep(socialBroadcasterPeriod);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("SocialBroadcaster.run FINISHED");
		}

		private void stopSocialBroadcaster(){
			activeSocialBroadcaster = false;
		}
		
	}
	
	
	private class SocialListener implements PacketForwardingListener {
		
		@Override
		public void receivedTCPBroadcastPacket(BroadcastPacket bp) {
            // check packet type
            //System.out.println("SocialListener: received BroadcastPacket hops = " + bp.getSource().length);
			try{
	            if( bp.getSource().length > 0 ){
	                // check payload
	                Object payload = E2EComm.deserialize(bp.getBytePayload());
	                if(payload instanceof SocialBeat){
	                    System.out.println("SocialListener: received SocialBeat");
	                	SocialBeat socialBeat = (SocialBeat)payload;
	
		                Person person = new Person(null, null, ""+myMboxSha1sum);
		                peopleLocation.put(person, bp.getSource());
	                	
	                	String remoteUserMbox = socialBeat.getMboxSha1sum();
	                    System.out.println("SocialListener: received SocialBeat from "+remoteUserMbox+" (hops = "+bp.getSource().length+")");
	                	//System.out.println( "queryFriendOfMine: " + getQueryFriendOfMine(myMbox_sha1sum, remoteUserMbox) );
						InfModel infModel = ModelFactory.createInfModel(foafRreasoner, dataSource.getDefaultModel());
						
						
						// --------------------------------------------
		                // check if the remote user is a friend of mine
	                	
						QueryExecution qExecFriendsList = QueryExecutionFactory.create(
								getQueryIsFriendOfMine(
										//myMboxSha1sum, 
										remoteUserMbox
								), 
								infModel
						);
						try {
				            ResultSet results = qExecFriendsList.execSelect();
				            while ( results.hasNext() ){
				                QuerySolution soln = results.nextSolution() ;
				                System.out.println( "il SocialBeat ricevuto e' relativo ad un mio amico " + soln );
				                RDFNode firstName = soln.get("firstName");
				                //System.out.println("firstName = "+firstName);
				                Literal surname = soln.getLiteral("surname");
				                //System.out.println("surname = "+surname);
				                //Literal mbox = soln.getLiteral("mbox");
				                //System.out.println("mbox = "+mbox);
				                Person friend = new Person(""+firstName, ""+surname, ""+myMboxSha1sum);
				                friendsLocation.put(friend, bp.getSource());
				            }
						} 
						finally {
							qExecFriendsList.close();
						}
						//System.out.println("SocialListener: peopleLocation "+peopleLocation);
						Iterator<Person> peopleIt = friendsLocation.keySet().iterator();
						while(peopleIt.hasNext()){
							Person p = peopleIt.next();
							System.out.println("SocialListener: "+p+" is in "+Arrays.toString(friendsLocation.get(p)));
						}
						

						// TODO check if the remote user has similar interests
						
						
	                }
	                else if(payload instanceof SocialInterestCenterAdvertise){
	                    System.out.println("SocialListener: received SocialInterestCenterAdvertise");
	                    SocialInterestCenterAdvertise sicAdv = (SocialInterestCenterAdvertise)payload;
						
						// ------------------------------------------------------------
		                // check if the remote user is an "interesting" interest center
	                	
	                	InfModel infModel = ModelFactory.createInfModel(foafRreasoner, dataSource.getDefaultModel());
	                	
						String sicAdvQuery = sicAdv.getSparqlQuery();
						//for(String topicInterest : interestCenters){
							QueryExecution qExecSicAdvList = QueryExecutionFactory.create(
									sicAdvQuery, 
									infModel
							);

							try {
					            ResultSet results = qExecSicAdvList.execSelect();
					            while ( results.hasNext() ){
					                QuerySolution sol = results.nextSolution() ;
					                System.out.println( "io sono interessato a " + sicAdv.getInterestName() );
					                System.out.println( "sol = " + sol );
					                //RDFNode firstName = soln.get("firstName");
					                //System.out.println("firstName = "+firstName);
					                //Literal surname = soln.getLiteral("surname");
					                //System.out.println("surname = "+surname);
					                //Literal mbox = soln.getLiteral("mbox");
					                //System.out.println("mbox = "+mbox);
					                //Person friend = new Person(""+firstName, ""+surname, ""+myMboxSha1sum);
					                //friendsLocation.put(friend, bp.getSource());
					                
					                // TODO chiedo all'utente se e' interessato a registrarsi a questo topic
					                
					                // TODO se no ==> memorizzo il no e non richiedo piu' all'utente
					                
					                // se si' ==> invio SicRegistration
					                BoundReceiveSocket receiveSocket = E2EComm.bindPreReceive(sicAdv.getProtocol());
					                SocialInterestCenterRegistration iscReg = new SocialInterestCenterRegistration(receiveSocket.getLocalPort());
					                E2EComm.sendUnicast(
					                		E2EComm.ipReverse(bp.getSource()), 
					                		sicAdv.getSiccPort(), 
					                		sicAdv.getProtocol(), 
				                            E2EComm.serialize(iscReg)
				                		);
					                

					                //	TODO istanzio un receiver per l'interestName, se esiste 
					                // interestNameReceivers
					                
					                //			altrimenti per il content type (txt, a/v,...), se esiste
					                // contentTypeReceivers
					                
					                //			altrimenti errore
					                
					                //			il receiver si mette in ascolto
					            }
							} 
							finally {
								qExecSicAdvList.close();
							}
						}
	                //}
	            }
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void receivedUDPBroadcastPacket(BroadcastPacket bp) {
			receivedTCPBroadcastPacket(bp);
		}

		@Override
		public void receivedTCPUnicastPacket(UnicastPacket up) {
			// do nothing
		}
		@Override
		public void receivedTCPUnicastHeader(UnicastHeader uh) {
			// do nothing
		}
		@Override
		public void receivedTCPPartialPayload(UnicastHeader uh, byte[] payload,
				int off, int len, boolean lastChunk) {
			// do nothing
		}
		@Override
		public void receivedUDPUnicastPacket(UnicastPacket up) {
			// do nothing
		}
		@Override
		public void sendingTCPUnicastPacketException(UnicastPacket up,
				Exception e) {
			// do nothing
		}
		@Override
		public void sendingTCPUnicastHeaderException(UnicastHeader uh,
				Exception e) {
			// do nothing
		}
		
	}
	
	
	
	// -------------------------------------- //
	// interest center coordinator            //
	// -------------------------------------- //
	
	// interestName ==> center coordinator
	Hashtable<String, SocialInterestCenterCoordinator> interestCenterCoordinators = new Hashtable<String, SocialInterestCenterCoordinator>();
	
	public void sendMulticast(String interestName, Object payload) throws Exception{
		if( interestCenterCoordinators.containsKey(interestName) ){
			throw new Exception("SocialInterestCenterCoordinator does not exist: "+interestName);
		}
		interestCenterCoordinators.get(interestName).sendMulticast(payload);
	}
	
	public SocialInterestCenterCoordinator createInterestCenterCoordinator(String interestName, 
			String contentType, String sparqlQuery, int protocol) throws Exception{
		if( interestCenterCoordinators.containsKey(interestName) ){
			throw new Exception("SocialInterestCenterCoordinator already exists: "+interestName);
		}
		SocialInterestCenterCoordinator sicc = new SocialInterestCenterCoordinator(
				interestName, 
				contentType, 
				sparqlQuery, 
				protocol);
		sicc.start();
		interestCenterCoordinators.put(interestName, sicc);
		return sicc;
	}
	public SocialInterestCenterCoordinator getInterestCenterCoordinator(String interestName){
		return interestCenterCoordinators.get(interestName);
	}
	public void removeInterestCenterCoordinator(String interestName){
		interestCenterCoordinators.remove(interestName);
	}
	
	
	
	
	
	
	// -------------------------------------- //
	// interest center client                 //
	// -------------------------------------- //
	
	// interest name ==> local port
	/*private Hashtable<String, EndPoint> localMulticastClients = new Hashtable<String, EndPoint>();
	
	public void addMulicastClient(String interestName, int localPort, int protocol){
		localMulticastClients.put(interestName, new EndPoint(localPort, null, null, protocol));
	}
	public void removeMulicastClient(String interestName, int localPort, int protocol){
		localMulticastClients.put(interestName, new EndPoint(localPort, null, null, protocol));
	}*/
	
	
	
	// -------------------------------------- //
	// interest center receivers              //
	// -------------------------------------- //
	
	// receiver type ==> receiver class
	private Hashtable<String, EndPoint> localMulticastClients = new Hashtable<String, EndPoint>();
	// TODO addNewReceiver(String type, Object Receiver);
	
	
	
	// -------------------------------------- //
	// utility classes, methods and variables //
	// -------------------------------------- //
	
	private static Model dataSource2Model(DataSource dataSource){
		Model modelFromDataSource = dataSource.getDefaultModel();
        Iterator<String> it = dataSource.listNames();
        while(it.hasNext()){
            modelFromDataSource.add(dataSource.getNamedModel(it.next()));
        }
        return modelFromDataSource;
	}
	
	// query to retrieve firstName, surname and mbox_sha1sum of my friends
	/*private String getQueryFriendsList(String myMbox_sha1sum){ 
		return "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
        "SELECT " +
        "DISTINCT " +
        "?firstName ?surname ?mbox " + 
        "WHERE " +
        "{ " +
        
        	"?friend foaf:firstName ?firstName . " +
        	"?friend foaf:surname ?surname . " +
        	"?friend foaf:mbox_sha1sum ?mbox . " +
        	
        	"?person foaf:mbox_sha1sum \"" + myMbox_sha1sum + "\" . " +
        	"?person foaf:knows ?friend . " +
        	
       "} " ;
	}*/
	
	// query to check if a user is a friend of mine
	private String getQueryIsFriendOfMine(/*String myMbox_sha1sum,*/ String remoteMbox_sha1sum){
		return "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
        "SELECT " +
        "DISTINCT " +
        "?firstName ?surname " + 
        "WHERE " +
        "{ " +
        
        	"?friend foaf:firstName ?firstName . " +
        	"?friend foaf:surname ?surname . " +
        	
        	"?person ramp:localMboxSha1 ?localMbox_sha1sum . " +
        	//"?person foaf:mbox_sha1sum \"" + myMbox_sha1sum + "\" . " +
        	"?person foaf:knows ?friend . " +
        	"?friend foaf:mbox_sha1sum \"" + remoteMbox_sha1sum + "\" . " +
        	
       "} " ;
	}

	// query to check if a user has a given topic_interest
	private String getQueryHasTopicInterest(/*String myMbox_sha1sum,*/ String topicInterest){
		return "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
		"PREFIX ramp: <"+SocialService.RAMP_URI+"> " +
        "SELECT * " +
        //"DISTINCT " +
        "topicInterest " + 
        "WHERE " +
        "{ " +
        	"?person ramp:localMboxSha1 ?localMbox_sha1sum . " +
        	"?person foaf:topic_interest ?topicInterest . " +
        	"FILTER regex(str(?topicInterest), \"" + topicInterest + "\")"+
       "} " ;
	}
	
	private class Person {
	    private String firstName;
	    private String lastName;
	    private String mbox_sha1sum;

	    public Person(String firstName, String lastName, String mbox_sha1sum) {
	        this.firstName = firstName;
	        this.lastName = lastName;
	        this.mbox_sha1sum = mbox_sha1sum;
	    }

	    public String getFirstName() {
	        return firstName;
	    }

	    public String getLastName() {
	        return lastName;
	    }

	    public String getMbox_sha1sum() {
	        return mbox_sha1sum;
	    }

	    @Override
	    public boolean equals(Object obj) {
	    	if (obj == null) return false;
	        else if( ! ( obj instanceof Person ) ) return false;
	        else if ( this.mbox_sha1sum == null ) return false;
	        else if ( ((Person)obj).getMbox_sha1sum() == null ) return false;
	        else if ( !((Person)obj).getMbox_sha1sum().equals(this.mbox_sha1sum) ) return false;
	        else return true;
	    }

	    @Override
	    public int hashCode() {
	        int hash = 7;
	        hash = 47 * hash + (this.mbox_sha1sum != null ? this.mbox_sha1sum.hashCode() : 0);
	        return hash;
	    }

	    @Override
	    public String toString() {
	        return firstName+" "+lastName+" "+mbox_sha1sum;
	    }

	}

	private static String RAMP_URI = "http://lia.deis.unibo.it/Research/RAMP/";
	
}
