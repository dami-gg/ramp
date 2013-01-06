package it.unibo.deis.lia.ramp.core.internode.upnp;
//
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;

import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.unibo.deis.lia.ramp.core.e2e.BoundReceiveSocket;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.e2e.GenericPacket;
import it.unibo.deis.lia.ramp.core.e2e.UnicastPacket;
import it.unibo.deis.lia.ramp.core.internode.Dispatcher;

public class UPnPmanager extends Thread {

	private static boolean open = true;
	final public static int TTL = 6;
	final public static int UPNPMANAGER_PROTOCOL = E2EComm.UDP;

	final public static int UPNPMANAGER_PORT = 1902;

	// private int protocol = E2EComm.TCP;
	private final String multicastIP = "239.255.255.250";
	private final int multicastPort = 1900;
	// private Vector<String> imManager = null;
	public static Vector<Master> masterList = null;
	public static Vector<UPnPSOAPRemoteServiceHandler> remoteServiceManaged = null;
	private static Vector<UPnPSOAPLocalServiceHandler> localServiceManaged = null;
	private static Vector<MulticastSocket> openedSocket = null;
	public static Vector<BoundReceiveSocket> openedBoundReceiveSocket = null;
	private static Vector<UPnPMasterElection> electionThread = null;
	public static Vector<String> localSenderSocket = null;

	public static final Object lockNewLocalService = new Object();
	public static final Object lockNewRemoteService = new Object();
	public static final Object lockTheOnlyLoop = new Object();

	public static String onlyLocalReader = null;
	private static UPnPmanager uPnPmanager = null;

	public static synchronized UPnPmanager getInstance() {
		try {
			if (uPnPmanager == null) {
				uPnPmanager = new UPnPmanager();
				open = true;
				uPnPmanager.start();
				new UPnPSSDPMulticastHandler().start();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return uPnPmanager;
	}

	public static void deactivate() {
		if (uPnPmanager != null) {
			open = false;
			for (BoundReceiveSocket bounded : openedBoundReceiveSocket) {
				try {
					bounded.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			for (MulticastSocket sok : openedSocket) {
				// sok.disconnect();
				sok.close();
			}
			for (UPnPMasterElection thread : electionThread) {
				thread.interrupt();
			}

			uPnPmanager = null;
		}
		System.out.println("UPnP Manager CLOSE");

	}

	private UPnPmanager() {
		// imManager = new Vector<String>();
		masterList = new Vector<UPnPmanager.Master>();
		remoteServiceManaged = new Vector<UPnPSOAPRemoteServiceHandler>();
		localServiceManaged = new Vector<UPnPSOAPLocalServiceHandler>();
		openedSocket = new Vector<MulticastSocket>();
		openedBoundReceiveSocket = new Vector<BoundReceiveSocket>();
		electionThread = new Vector<UPnPmanager.UPnPMasterElection>();
		localSenderSocket = new Vector<String>();
	}

	public static Vector<Master> getManagerList() {
		return masterList;
	}

	public void run() {

		try {
			Vector<String> localInterfaces = Dispatcher.getLocalNetworkAddresses(true);
			for (int i = 0; i < localInterfaces.size(); i++) {
				String anInterface = localInterfaces.elementAt(i);

				UPnPMasterElection thread = new UPnPMasterElection(anInterface);
				electionThread.add(thread);
				thread.start();
			}

			for (int i = 0; i < localInterfaces.size(); i++) {
				String anInterface = localInterfaces.elementAt(i);

				new UPnPManagerReceiver(anInterface).start();
			}

			System.out.println("UPnPManager START");
		} 
		catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static boolean isOpen() {
		return open;
	}

	public static Master getInterfaceManager(String theInterface) {
		try {
			for (Master temp : masterList) {
				if (temp.getLocalInterface().equalsIgnoreCase(theInterface))
					return temp;
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	public static UPnPSOAPRemoteServiceHandler getRemoteService(String uuid) {
		for (UPnPSOAPRemoteServiceHandler temp : remoteServiceManaged) {
			if (temp.getUuid().equalsIgnoreCase(uuid))
				return temp;
		}
		return null;
	}

	public static UPnPSOAPLocalServiceHandler getLocalService(String uuid) {
		for (UPnPSOAPLocalServiceHandler temp : localServiceManaged) {
			if (temp.getUuid().equalsIgnoreCase(uuid))
				return temp;
		}
		return null;
	}

	public static void addLocalServiceHandler(UPnPSOAPLocalServiceHandler local) {
		localServiceManaged.add(local);
	}

	private class UPnPManagerReceiver extends Thread {
		private String anInterface;
		private InetAddress interfaceAddress;
		private MulticastSocket uPnPMulticastSocket = null;
		private InetAddress group;

		// private boolean master = true;

		public UPnPManagerReceiver(String anInterface) {
			this.anInterface = anInterface;
			try {
				interfaceAddress = InetAddress.getByName(anInterface);

				group = InetAddress.getByName(multicastIP);

				uPnPMulticastSocket = new MulticastSocket(multicastPort);
				openedSocket.add(uPnPMulticastSocket);
				uPnPMulticastSocket.setNetworkInterface(NetworkInterface.getByInetAddress(interfaceAddress));
				if (!onlyLocalReader.equalsIgnoreCase(anInterface))
					uPnPMulticastSocket.setLoopbackMode(true);// enable loop only for local reading
				uPnPMulticastSocket.joinGroup(group);

			} catch (Exception e) {

				e.printStackTrace();
			}
		}

		public void run() {

			byte[] buf = new byte[50 * 1024];
			DatagramPacket recv = new DatagramPacket(buf, buf.length);
			boolean cicle = true;

			while (cicle) {
				try {
					uPnPMulticastSocket.setSoTimeout(30 * 1000);
					uPnPMulticastSocket.receive(recv);

					// System.err.println("Received multicast message from "
					// + recv.getSocketAddress() + ":" +
					// recv.getPort()+"   "+uPnPMulticastSocket.getInetAddress());
					boolean notLocal = true;
					String socketToRemove = "";
					if (localSenderSocket.size() > 0)
						for (String sock : localSenderSocket)
							if (sock.equalsIgnoreCase("" + recv.getSocketAddress())) {
								notLocal = false;
								socketToRemove = sock;
							}

					if (notLocal)
						new MulticastMessageParser(buf, recv).start();
					localSenderSocket.remove(socketToRemove);
					if (!open)
						cicle = false;
					buf = new byte[50 * 1024];
					recv = new DatagramPacket(buf, buf.length);

				} catch (Exception e) {

					if (!open)
						cicle = false;
				}
			}

		}

		private class MulticastMessageParser extends Thread {
			private byte[] buf = null;
			private DatagramPacket recv = null;

			public MulticastMessageParser(byte[] buf, DatagramPacket packet) {
				super();
				this.buf = buf;
				this.recv = packet;
			}

			public void run() {
				Vector<String> res = new Vector<String>();
				boolean read = true;
				boolean jump = false;

				res.add("SOURCE: " + recv.getSocketAddress()); // add the source address; this information is helpful for not-RAMP nodes
																
				while (read) {
					StringBuilder sb = new StringBuilder(buf.length);
					for (byte b : buf) {
						if (jump) // salta (char)0x0A
							jump = false;
						else {
							if (b == 0) {
								read = false;
								break;
							}
							if ((char) b != (char) (0x0D))
								sb.append((char) b);
							else {
								jump = true;
								res.add(sb.toString());
								sb = new StringBuilder(buf.length);

							}
						}
					}
				}

				if (getInterfaceManager(anInterface).isIm()) {// if I'm the manager
					UPnPMulticastMessage message = null;
					String senderAddress = "";
					int senderPort = -1;
					int masterID = Dispatcher.getLocalId();
					String messageType = "";
					String uuid = null;
					String nTS = null;
					int mX = -1;
					byte[] multicastUPnPmessagePayload = buf;

					messageType = res.elementAt(1);
					if (messageType.startsWith("M-SEARCH")) {

						String tempAdd = res.elementAt(0);
						String fullAdd = tempAdd.split("/")[1];
						String[] addsplit = fullAdd.split(":");
						senderAddress = addsplit[0];
						senderPort = Integer.parseInt(addsplit[1]);
						for (int i = 2; i < res.size(); i++) {
							String line = res.elementAt(i);
							if (line.contains("MX")) {//maybe it will better to reduce MX value
								if (line.charAt(3) == (char) 0x20)
									mX = Integer.parseInt(line.substring(4));
								else
									mX = Integer.parseInt(line.substring(3));
							}
							if (mX > 0) {
								break;
							}
						}
						BoundReceiveSocket upnpMasterSocket;
						try {
							upnpMasterSocket = E2EComm.bindPreReceive(UPNPMANAGER_PROTOCOL);

							message = new UPnPMulticastMessage(senderAddress, senderPort, masterID, upnpMasterSocket.getLocalPort(), messageType, uuid, nTS, mX, multicastUPnPmessagePayload);
							serachHandeler(message, upnpMasterSocket);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					else {// notify message
						for (int i = 2; i < res.size(); i++) {
							String line = res.elementAt(i);
							if (line.contains("LOCATION")) {
								String[] peace;
								if (line.charAt(9) == (char) 0x20)
									peace = line.split("LOCATION: http://");
								else
									peace = line.split("LOCATION:http://");

								peace = peace[1].split("/");
								peace = peace[0].split(":");
								senderAddress = peace[0];

								senderPort = Integer.parseInt(peace[1]);
							}

							if (line.contains("uuid")) {
								String[] peace = line.split("uuid:");
								uuid = peace[1].substring(0, 36);

							}
							if (line.startsWith("NTS")) {
								if (line.contains("alive"))
									nTS = "alive";
								else if (line.contains("bye"))
									nTS = "bye";
							}
							if (nTS != null && uuid != null && senderPort > 0)
								break;
						}
						try {
							if (nTS.contains("alive")) {
								BoundReceiveSocket masterSocket = null;
								int masterPort = -1;
								UPnPSOAPLocalServiceHandler temp = null;
								System.out.println("Received ALIVE notification from uuid:" + uuid);
								synchronized (lockNewLocalService) {
								
									temp = UPnPmanager.getLocalService(uuid);
									if (temp == null) {
										try {
											
											masterSocket = E2EComm.bindPreReceive(E2EComm.TCP);
											masterPort = masterSocket.getLocalPort();
											 
											UPnPSOAPLocalServiceHandler handler = new UPnPSOAPLocalServiceHandler(uuid, masterSocket);
											UPnPmanager.addLocalServiceHandler(handler);
											handler.start();
										} catch (Exception e) {
											e.printStackTrace();
										}

									}
									if (temp != null) {
										
										masterSocket = temp.getMasterSocket();
										masterPort = masterSocket.getLocalPort();
									}
								}
							
								message = new UPnPMulticastMessage(senderAddress, senderPort, masterID, masterPort, messageType, uuid, nTS, mX, multicastUPnPmessagePayload);
								notifyAndAnswerHandler(message);

								//System.out.println("Sent multicast message of type " + messageType + " received from " + senderAddress + ":" + senderPort);

							}
							if (nTS.contains("bye")) {
								System.out.println("Received BYE BYE notification from uuid:" + uuid);
								synchronized (lockNewLocalService) {
									UPnPSOAPLocalServiceHandler tempToRemove = getLocalService(uuid);
									if (tempToRemove != null) {
										tempToRemove.deactivate();
										localServiceManaged.remove(tempToRemove);
									}
								}
								message = new UPnPMulticastMessage(senderAddress, senderPort, masterID, -1, messageType, uuid, nTS, mX, multicastUPnPmessagePayload);
								notifyAndAnswerHandler(message);
							}

						} catch (Exception e) {

							e.printStackTrace();
						}

					}

				} else{
					//System.out.println("Received multicast message but I'm not the master");
					}

			}

			private void serachHandeler(UPnPMulticastMessage message,
					BoundReceiveSocket upnpMasterSocket) throws Exception {
				System.err.println("received a search request from " + message.getSenderAddress());
				E2EComm.sendBroadcast(TTL, UPNPMANAGER_PORT, UPNPMANAGER_PROTOCOL, E2EComm.serialize(message));

				// code to send to other master
//				 for (Master tempManager : masterList) {
//				 if (!tempManager.getLocalInterface().equalsIgnoreCase(
//				 anInterface)
//				 && tempManager.getLocalInterface()
//				 .equalsIgnoreCase(
//				 tempManager.getManagerAdd())) {
//				
//				 BroadcastPacket packet = new BroadcastPacket((byte) 1,
//				 UPNPMANAGER_PORT, Dispatcher.getLocalId(),
//				 E2EComm.serialize(message));
//				 byte[] buffer = E2EComm.serializePacket(packet);
//				
//				 DatagramSocket ds = new DatagramSocket();
//				 DatagramPacket dp = new DatagramPacket(buffer,
//				 buffer.length,
//				 InetAddress.getByName(tempManager
//				 .getManagerAdd()), UPNPMANAGER_PORT);
//				 ds.send(dp);
//				 }
//				 }
				long preReceive = System.currentTimeMillis();
				long spentTime = 0;
				int i = 0;
				while (spentTime < message.getmX() * 1000) {
					try {
						GenericPacket gp = E2EComm.receive(upnpMasterSocket, ((int) ((message.getmX() * 1000) - spentTime))+1);
						spentTime = System.currentTimeMillis() - preReceive;
						if (gp instanceof UnicastPacket) {
							new localUpnpSearchThreadHandler((UnicastPacket) gp, message).start();
							i++;
						}
					} catch (SocketTimeoutException e) {
						spentTime = System.currentTimeMillis() - preReceive;
						if (i == 0){
							//System.out.println("Waited more than MX");
						}
					}
				}
				if (i == 0){
					//System.out.println("Waited enough");
				}
				else{
					//System.out.println("Found " + i + " devices");
				}
			}

			private class localUpnpSearchThreadHandler extends Thread {
				UnicastPacket gp;
				UPnPMulticastMessage message;

				public localUpnpSearchThreadHandler(UnicastPacket gp, UPnPMulticastMessage message) {
					super();
					this.gp = gp;
					this.message = message;
				}

				public void run() {
					try {
						UnicastPacket up = (UnicastPacket) gp;
						Object o = E2EComm.deserialize(up.getBytePayload());
						if (o instanceof UPnPUnicastMessage) {
							UPnPUnicastMessage mess = (UPnPUnicastMessage) o;
							String upnpMessage="";
							if(mess.getMessage() instanceof byte[])
							upnpMessage = new String((byte[])mess.getMessage());

							
							UPnPSOAPRemoteServiceHandler service = null;
							synchronized (lockNewRemoteService) {
								service = getRemoteService(mess.getUuid());

								if (service == null) {
									ServerSocket localServerSocket = new ServerSocket(0, 0, InetAddress.getByName(anInterface));

									System.out.println("Opened UPnP SOAP remote handler " + anInterface + localServerSocket.getLocalPort());
									service = new UPnPSOAPRemoteServiceHandler(mess.getServiceAdd(), mess.getServicePort(), anInterface, localServerSocket.getLocalPort(), mess.getUuid(),
											up.getSourceNodeId(), mess.getSenderPort(), up.getSource(), localServerSocket);
									service.start();
									remoteServiceManaged.add(service);
								}
							}

							//System.out.println("remote " + "" + mess.getServiceAdd() + ":" + mess.getServicePort() + " local " + "" + service.getLocalAdd() + ":" + service.getLocalPort());
							final Pattern pattern = Pattern.compile(mess.getServiceAdd() + ":" + mess.getServicePort());
							final Matcher matcher = pattern.matcher(upnpMessage);
							upnpMessage = matcher.replaceAll(service.getLocalAdd() + ":" + service.getLocalPort());

							// System.out.println(upnpMessage);
							//System.out.println("Received divice presence uuid:" + mess.getUuid() + " master " + up.getSourceNodeId() + " is listening on " + mess.getSenderPort()
									//+ " and the serivce add is: " + mess.getServiceAdd() + ":" + mess.getServicePort());
							byte[] sendData = new byte[50 * 1024];
							sendData = upnpMessage.getBytes();
							DatagramPacket packet2 = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(message.getSenderAddress()), message.getSenderPort());

							uPnPMulticastSocket.send(packet2);
						} else
							throw new Exception();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

		}

		private void notifyAndAnswerHandler(UPnPMulticastMessage message// gestione
		){

			try {
				E2EComm.sendBroadcast(TTL, UPNPMANAGER_PORT, UPNPMANAGER_PROTOCOL, E2EComm.serialize(message));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// code to send to other master add
//			for (Master tempMaster : masterList) {
//				 if (!tempMaster.getLocalInterface().equalsIgnoreCase(
//				 anInterface)
//				 && tempMaster.getLocalInterface()
//				 .equalsIgnoreCase(
//				 tempMaster.getManagerAdd())) {
//				
//				 BroadcastPacket packet = new BroadcastPacket((byte) 1,
//				 UPNPMANAGER_PORT, Dispatcher.getLocalId(),
//				 E2EComm.serialize(message));
//				 byte[] buffer = E2EComm.serializePacket(packet);
//				
//				 DatagramSocket ds = new DatagramSocket();
//				 DatagramPacket dp = new DatagramPacket(buffer,
//				 buffer.length,
//				 InetAddress.getByName(tempMaster
//				 .localInterface), UPNPMANAGER_PORT);
//				 ds.send(dp);
//				 //System.out.println("Sent notify message to this local master "+tempMaster.getLocalInterface());
//				 }
//				 }

		}

	}

	private class UPnPMasterElection extends Thread {

		private String thisInterface = "";
		private Master threadMaster = null;
		private MulticastSocket multicastElectionSocket = null;
		private String multicastElectionIP = "239.255.255.251";
		private int multicastElectionPort = 1901;
		private int timeout = 30000;
		private String networkip = "";
		private Thread refresh = null;

		public UPnPMasterElection(String anInterface) {
			thisInterface = anInterface;
			System.out.println(thisInterface);
			String[] ottetti = thisInterface.split("\\.");

			networkip = ottetti[0] + "." + ottetti[1] + "." + ottetti[2];
			try {
				multicastElectionSocket = new MulticastSocket(multicastElectionPort);
				openedSocket.add(multicastElectionSocket);
				multicastElectionSocket.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getByName(thisInterface)));
				multicastElectionSocket.setLoopbackMode(true);// true to disable
				multicastElectionSocket.joinGroup(InetAddress.getByName(multicastElectionIP));
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		public void run() {
			try {

				becomeManager(thisInterface);
				multicastElectionSocket.setSoTimeout(30 * 1000);
				boolean cicle = true;
				if (!open)
					cicle = false;

				while (cicle) {
					byte[] receiveData = new byte[1024];// once finished handshaking phase, it receives the other messages
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					try {
						multicastElectionSocket.receive(receivePacket);

						String receive = new String(receiveData, 0, receivePacket.getLength());
						parsemessage(receive);
					} catch (Exception e) {
						if (!open)
							cicle = false;

					}
				}

				sendByeBye(thisInterface);

				if (refresh != null)
					refresh.interrupt();

			} catch (SocketException e) {
				// e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		private void sendByeBye(String thisInterface) {
			try {

				if (getInterfaceManager(thisInterface) != null && getInterfaceManager(thisInterface).isIm()) {
					MulticastSocket byeElectionSocket = new MulticastSocket(multicastElectionPort);

					byeElectionSocket.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getByName(thisInterface)));
					byeElectionSocket.setLoopbackMode(true);// true to disable
					byeElectionSocket.joinGroup(InetAddress.getByName(multicastElectionIP));
					byte[] dataToSend = new byte[1024];
					String imTheManager = "BYEBYE:" + thisInterface;//
					dataToSend = imTheManager.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(dataToSend, dataToSend.length, InetAddress.getByName(multicastElectionIP), multicastElectionPort);
					byeElectionSocket.send(sendPacket);
					System.out.println("Sent BYEBYE message to " + thisInterface);

					byeElectionSocket.close();

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void becomeManager(String Interface) throws Exception {// Ask to become master twice at random time to avoid collision

			threadMaster = getInterfaceManager(Interface);

			if (threadMaster == null || (System.currentTimeMillis() - threadMaster.getTimeLastUpdate()) > 300 * 1000) {
				threadMaster = new Master(Interface, thisInterface, System.currentTimeMillis(), true);
				masterList.add(threadMaster);
				synchronized (lockTheOnlyLoop) {
					if (onlyLocalReader == null)
						onlyLocalReader = thisInterface;
				}
				byte[] dataToSend = new byte[1024];
				String imTheManager = "BECOMEMASTER:" + Interface;
				dataToSend = imTheManager.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(dataToSend, dataToSend.length, InetAddress.getByName(multicastElectionIP), multicastElectionPort);
				multicastElectionSocket.send(sendPacket);

				// da levare
				System.out.println("Sent first request to become master to: " + Interface);

				byte[] receiveData = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

				boolean redo = false;
				boolean cicle = true;
				multicastElectionSocket.setSoTimeout(timeout);
				long startWaiting = System.currentTimeMillis();
				try {
					while (cicle) {
						multicastElectionSocket.receive(receivePacket);
						String receive = new String(receiveData, 0, receivePacket.getLength());
						if (receive.startsWith("ALREADY:" + networkip)) {
							// da levare

							String[] peace = receive.split(":");
							System.out.println("This interface " + Interface + " has already a master with this address " + peace[1]);
							masterList.remove(threadMaster);
							threadMaster = new Master(Interface, peace[1], System.currentTimeMillis(), false);
							masterList.add(threadMaster);
							cicle = false;
						} else {
							multicastElectionSocket.setSoTimeout(timeout - (int) (System.currentTimeMillis() - startWaiting));
							parsemessage(receive);// received update message
						}

					}
				} catch (SocketTimeoutException e) {
					redo = true;
				} catch (SocketException e) {
					redo = false;
					sendByeBye(thisInterface);
				}
				if (redo) {
					try {
						try {
							Thread.sleep(new Random().nextInt(15 * 1000));// redo the same thing after a random period to avoid conflict
						} 
						catch (InterruptedException e) {
							// e.printStackTrace();
						}
						multicastElectionSocket.send(sendPacket);

						
						System.out.println("Sent second request to become master to: " + Interface);

						receiveData = new byte[1024];
						receivePacket = new DatagramPacket(receiveData, receiveData.length);

						cicle = true;
						multicastElectionSocket.setSoTimeout(timeout);
						startWaiting = System.currentTimeMillis();

						while (cicle) {
							multicastElectionSocket.receive(receivePacket);
							String receive = new String(receiveData, 0, receivePacket.getLength());
							if (receive.startsWith("ALREADY:" + networkip)) {
								String[] peace = receive.split(":");
								System.err.println("This interface " + Interface + " has already a master with this address " + peace[1]);
								masterList.remove(threadMaster);
								threadMaster = new Master(Interface, peace[1], System.currentTimeMillis(), false);
								masterList.add(threadMaster);
								cicle = false;
							} 
							else {
								multicastElectionSocket.setSoTimeout(timeout - (int) (System.currentTimeMillis() - startWaiting));
								parsemessage(receive);
							}

						}
					} catch (SocketTimeoutException e) {
						// do nothing... I'm the manager
					} catch (SocketException e) {
						sendByeBye(Interface);
					}

				}

			}
			if (threadMaster.isIm()) {
				refresh = new RefreshMasterStatus();
				refresh.start();
			}
		}

		private void parsemessage(String receive) throws Exception {
			byte[] dataToSend = new byte[1024];
			System.err.println(receive);
			if (receive.startsWith("BECOMEMASTER:")) {// receive BECOMEMASTER message

				String[] splitString = receive.split(":");
				String[] splitOttetti = splitString[1].split("\\.");
				String networkip = splitOttetti[0] + "." + splitOttetti[1] + "." + splitOttetti[2];
				for (Master temp : masterList) {
					if (temp.getLocalInterface().startsWith(networkip))
						if (temp.isIm()) {// I'm the manager
							String imTheManager = "ALREADY:" + thisInterface;
							dataToSend = imTheManager.getBytes();
							DatagramPacket sendPacket = new DatagramPacket(dataToSend, dataToSend.length, InetAddress.getByName(multicastElectionIP), multicastElectionPort);
							multicastElectionSocket.send(sendPacket);
							break;
						}
				}
			} else if (receive.startsWith("ALREADY:")) {// received an already message
				String[] splitString = receive.split(":");

				for (Master temp : masterList) {
					if (temp.getManagerAdd().equalsIgnoreCase(splitString[1])) {

						temp.setTimeLastUpdate(System.currentTimeMillis());// update the time
						break;

					}
				}
			} else if (receive.startsWith("STILLMASTER:")) {
				String[] splitString = receive.split(":");

				for (Master temp : masterList) {
					if (temp.getManagerAdd().equalsIgnoreCase(splitString[1])) {

						temp.setTimeLastUpdate(System.currentTimeMillis());// update time

					}
				}

			} else if (receive.startsWith("BYEBYE:")) {
				String[] splitString = receive.split(":");// received BYE BYE

				for (Master temp : masterList) {
					if (temp.getManagerAdd().equalsIgnoreCase(splitString[1])) {
						masterList.remove(temp);// remove master
						break;
					}
				}
				becomeManager(thisInterface);// start new master handshake phase
			} else
				throw new Exception("received incorrect message " + thisInterface);

		}

		private class RefreshMasterStatus extends Thread {

			public synchronized void run() {

				do {
					try {
						Thread.sleep(200 * 1000);

						if (open) {
							byte[] dataToSend = new byte[1024];
							String imTheManager = "STILLMASTER:" + thisInterface;
							dataToSend = imTheManager.getBytes();
							DatagramPacket sendPacket = new DatagramPacket(dataToSend, dataToSend.length, InetAddress.getByName(multicastElectionIP), multicastElectionPort);
							multicastElectionSocket.send(sendPacket);
						
							System.out.println("Sent " + imTheManager + " to " + thisInterface);
						}

					} catch (InterruptedException e) {
						// e.printStackTrace();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				} while (open);
			}
		}
	}

	protected class Master {
		private String localInterface = null;
		private String masterAdd = "";
		private long timeLastUpdate = 0;
		private boolean im = false;

		public Master(String localInterface, String managerAdd, long timeLastUpdate, boolean im) {
			super();
			this.localInterface = localInterface;
			this.masterAdd = managerAdd;
			this.timeLastUpdate = timeLastUpdate;
			this.im = im;
		}

		public boolean isIm() {
			return im;
		}

		public void setIm(boolean im) {
			this.im = im;
		}

		public String getLocalInterface() {
			return localInterface;
		}

		public String getManagerAdd() {
			return masterAdd;
		}

		public void setManagerAdd(String managerAdd) {
			this.masterAdd = managerAdd;
		}

		public long getTimeLastUpdate() {
			return timeLastUpdate;
		}

		public void setTimeLastUpdate(long timeLastUpdate) {
			this.timeLastUpdate = timeLastUpdate;
		}

	}
}
