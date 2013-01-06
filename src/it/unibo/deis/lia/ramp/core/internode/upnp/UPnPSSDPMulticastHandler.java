package it.unibo.deis.lia.ramp.core.internode.upnp;


//m
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.unibo.deis.lia.ramp.core.e2e.BoundReceiveSocket;
import it.unibo.deis.lia.ramp.core.e2e.BroadcastPacket;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.e2e.GenericPacket;
import it.unibo.deis.lia.ramp.core.internode.Dispatcher;
import it.unibo.deis.lia.ramp.core.internode.upnp.UPnPmanager.Master;

public class UPnPSSDPMulticastHandler extends Thread {

	private BoundReceiveSocket receiveUPnPMulticastSocket;

	public UPnPSSDPMulticastHandler() {
		super();
		try {
			receiveUPnPMulticastSocket = E2EComm.bindPreReceive(UPnPmanager.UPNPMANAGER_PORT, UPnPmanager.UPNPMANAGER_PROTOCOL);
			UPnPmanager.openedBoundReceiveSocket.add(receiveUPnPMulticastSocket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {

		while (UPnPmanager.isOpen()) {
			GenericPacket recGP;
			try {
				recGP = E2EComm.receive(receiveUPnPMulticastSocket);
				System.out.println("Received multicast message from " + ((BroadcastPacket) recGP).getTraversedIds().length);
				new UPnPMulticastMessageHandler(recGP).start();
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}
	}

	private class UPnPMulticastMessageHandler extends Thread {
		private GenericPacket recGP;

		private UPnPMulticastMessageHandler(GenericPacket recGP) {
			this.recGP = recGP;
		}

		@Override
		public void run() {

			try {
				if (recGP instanceof BroadcastPacket) {
					BroadcastPacket recBP = (BroadcastPacket) recGP;

					Object recO = E2EComm.deserialize(recBP.getBytePayload());
					if (!(recO instanceof UPnPMulticastMessage)) {
						System.out.println("Received a not multicas message on UPnPMulticastHandler");
					} else {
						//System.out.println("Received a multicast message on UPnPMulticastHandler");
						UPnPMulticastMessage msg = (UPnPMulticastMessage) recO;
						if (msg.getMessageType().startsWith("NOTIFY")) {

							//System.out.println("Received UPnP notify message");
							for (Master tempManager : UPnPmanager.getManagerList()) {
								boolean notlocale = true;
								if (recBP.getTraversedIds().length == 0) {
									String[] senderOttetti = msg.getSenderAddress().split("\\.");
									String[] tempManagerOttetti = tempManager.getLocalInterface().split("\\.");
									if (senderOttetti[0].equalsIgnoreCase(tempManagerOttetti[0]) && senderOttetti[1].equalsIgnoreCase(tempManagerOttetti[1])
											&& senderOttetti[2].equalsIgnoreCase(tempManagerOttetti[2]))
										notlocale = false;
								}

								if (tempManager.isIm() && notlocale) {
									if (msg.getNTS().contains("alive")) {
										//System.out.println("Received alive messageand I'm the master " + tempManager.getManagerAdd());
										ServerSocket localServerSocket = null;
										UPnPSOAPRemoteServiceHandler temp = null;
										synchronized (UPnPmanager.lockNewRemoteService) {
											temp = UPnPmanager.getRemoteService(msg.getUuid());

											if (temp == null) {
												localServerSocket = new ServerSocket(0, 0, InetAddress.getByName(tempManager.getLocalInterface()));
												temp = new UPnPSOAPRemoteServiceHandler(msg.getSenderAddress(), msg.getSenderPort(), tempManager.getLocalInterface(), localServerSocket.getLocalPort(),
														msg.getUuid(), msg.getMasterID(), msg.getMasterPort(), recBP.getSource(), localServerSocket);

												UPnPmanager.remoteServiceManaged.add(temp);
												temp.start();
											}
											if (temp != null)
												localServerSocket = temp.getLocalServerSocket();
										}
										String upnpMessage = new String(msg.getMulticastUPnPmessagePayload());
										//System.out.println(msg.getSenderAddress() + ":" + msg.getSenderPort());
										//System.out.println(tempManager.getLocalInterface() + ":" + localServerSocket.getLocalPort());
										final Pattern pattern = Pattern.compile(msg.getSenderAddress() + ":" + msg.getSenderPort());
										final Matcher matcher = pattern.matcher(upnpMessage);
										upnpMessage = matcher.replaceAll(tempManager.getLocalInterface() + ":" + localServerSocket.getLocalPort());
										DatagramSocket serverSocketUnicast;
										try {
											serverSocketUnicast = new DatagramSocket(0, InetAddress.getByName(tempManager.getLocalInterface()));
											byte[] message = upnpMessage.getBytes();
											String socketadd = "";
											try {
												socketadd = "" + serverSocketUnicast.getLocalSocketAddress();
												//System.out.println("Local sender socket: " + socketadd);
												UPnPmanager.localSenderSocket.add(socketadd);
											} catch (Exception e) {
												// e.printStackTrace();
											}
											DatagramPacket sendPacket = new DatagramPacket(message, message.length, InetAddress.getByName("239.255.255.250"), 1900);

											serverSocketUnicast.send(sendPacket);
											//System.out.println("Sent alive message");
											serverSocketUnicast.close();
										} catch (Exception e) {
											// e.printStackTrace();
										}

									}

									if (msg.getNTS().contains("bye")) {
										//System.out.println("Received a BYEBYE notification from uuid: " + msg.getUuid());
										DatagramSocket serverSocketUnicast;
										try {
											serverSocketUnicast = new DatagramSocket(0, InetAddress.getByName(tempManager.getLocalInterface()));

											byte[] message = msg.getMulticastUPnPmessagePayload();
											String socketadd = "";
											try {
												socketadd = "" + serverSocketUnicast.getLocalSocketAddress();
												UPnPmanager.localSenderSocket.add(socketadd);
											} catch (Exception e) {
												// e.printStackTrace();
											}
											DatagramPacket sendPacket = new DatagramPacket(message, message.length, InetAddress.getByName("239.255.255.250"), 1900);

											serverSocketUnicast.send(sendPacket);
											//System.out.println("Sent BYE BYE message");

											serverSocketUnicast.close();
										} catch (Exception e) {
											// e.printStackTrace();
										}
										synchronized (UPnPmanager.lockNewRemoteService) {
											UPnPSOAPRemoteServiceHandler tempToRemove = UPnPmanager.getRemoteService(msg.getUuid());
											if (tempToRemove != null) {
												tempToRemove.stopRemoteService();
												UPnPmanager.remoteServiceManaged.remove(tempToRemove);

											}
										}
									}
								}
							}
						}
						
						if (msg.getMessageType().startsWith("M-SEARCH")) {// someone is doing a research
							
							System.out.println("Received a search message");
							for (Master tempManager : UPnPmanager.getManagerList()) {
								if (tempManager.isIm()) {//  For every interface that i'm the master I'll send the message and then I'll wait an answer for mx seconds
									
									System.out.println("I received a message and I'm the master of that interface");
									// if(!UPnPmanager.onlyLocalReader.equalsIgnoreCase(tempManager.getLocalInterface()))

									new MsearchSendHandler(msg, tempManager.getLocalInterface(), recBP.getSource()).start();

									// DatagramSocket datagramSocket=new
									// DatagramSocket(0,
									// InetAddress.getByName(tempManager.getLocalInterface()));
									// DatagramPacket sendPacket =
									// new
									// DatagramPacket(msg.getMulticastUPnPmessagePayload(),
									// msg.getMulticastUPnPmessagePayload().length,
									// InetAddress.getByName("239.255.255.250"),
									// 1900);
									//
									// datagramSocket.send(sendPacket);
									//
									// byte[] buf=new byte[50*1024];
									// DatagramPacket receivePacket = new
									// DatagramPacket(buf, buf.length);
								}
							}
						}

					}
				}
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}

	}

	private class MsearchSendHandler extends Thread {
		private UPnPMulticastMessage msg;
		private String localInterface;
		private String[] traversedNode;

		public MsearchSendHandler(UPnPMulticastMessage msg, String localInterface, String[] traversedNode) {
			super();
			this.msg = msg;
			this.localInterface = localInterface;
			this.traversedNode = traversedNode;
		}

		public void run() {
			DatagramSocket serverSocketUnicast;
			try {
				serverSocketUnicast = new DatagramSocket(0, InetAddress.getByName(localInterface));
				// System.out.println(new
				// String(msg.getMulticastUPnPmessagePayload()));
				// InetAddress interfaceAddress =
				// InetAddress.getByName(localInterface);
				//
				// InetAddress group = InetAddress.getByName("239.255.255.250");
				//
				// serverSocketUnicast = new MulticastSocket(1900);
				//
				// serverSocketUnicast.setNetworkInterface(NetworkInterface
				// .getByInetAddress(interfaceAddress));
				// serverSocketUnicast.setLoopbackMode(true);
				// serverSocketUnicast.joinGroup(group);
				//
				//System.err.println("" + serverSocketUnicast.getLocalSocketAddress());
				String add = "" + serverSocketUnicast.getLocalAddress();
				//System.err.println("" + add.substring(1));
				// if(UPnPmanager.onlyLocalReader.equalsIgnoreCase(add.substring(1)))

				DatagramPacket sendPacket = new DatagramPacket(msg.getMulticastUPnPmessagePayload(), msg.getMulticastUPnPmessagePayload().length, InetAddress.getByName("239.255.255.250"), 1900);
				try {
					UPnPmanager.localSenderSocket.add("" + serverSocketUnicast.getLocalSocketAddress());
				} catch (Exception e) {
					// e.printStackTrace();
				}
				serverSocketUnicast.send(sendPacket);
				System.err.println("Sent a search message from " + serverSocketUnicast.getLocalSocketAddress());
				boolean waitAnswer = true;

				byte[] buf = new byte[50 * 1024];
				DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
				long stopWaiting = System.currentTimeMillis() + msg.getmX() * 1000;// wait at most MX seconds, maybe it's a little too much because the the send/receive time is longer in multi-hop scenario
				while (waitAnswer) {

					serverSocketUnicast.setSoTimeout((int) (stopWaiting - System.currentTimeMillis()));

					try {
						serverSocketUnicast.receive(receivePacket);
						System.out.println("Received answer");
						new MsearchReceiveHandler(msg, receivePacket, buf, traversedNode).start();

						buf = new byte[50 * 1024];
						receivePacket = new DatagramPacket(buf, buf.length);

					} catch (SocketTimeoutException e) {
						if (stopWaiting - System.currentTimeMillis() < 0)
							waitAnswer = false;
					}
				}

				serverSocketUnicast.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private class MsearchReceiveHandler extends Thread {
		UPnPMulticastMessage msg;
		DatagramPacket receivePacket;
		byte[] buf;
		String[] traversedNode;

		public MsearchReceiveHandler(UPnPMulticastMessage msg, DatagramPacket receivePacket, byte[] buf, String[] traversedNode) {
			super();
			this.msg = msg;
			this.receivePacket = receivePacket;
			this.buf = buf;
			this.traversedNode = traversedNode;
		}

		public void run() {
			try {
				Vector<String> res = new Vector<String>();

				System.out.println("Received answer from a service ");
				boolean read = true;
				boolean jump = false;
				// char temp=(char)buf[0];
				res.add("SOURCE: " + receivePacket.getSocketAddress());

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

				Vector<String> mess;
				String serviceAdd = null;
				int servicePort = -1;
				int masterPort = -1;

				String uuid = null;
				BoundReceiveSocket masterSocket = null;

				for (String line : res) {
					if (line.contains("uuid")) {
						String[] peace = line.split("uuid:");
						uuid = peace[1].substring(0, 36);

					}
					if (line.contains("LOCATION")) {
						String[] peace;
						if (line.charAt(9) == (char) 0x20)
							peace = line.split("LOCATION: http://");
						else
							peace = line.split("LOCATION:http://");

						peace = peace[1].split("/");
						peace = peace[0].split(":");
						serviceAdd = peace[0];

						servicePort = Integer.parseInt(peace[1]);
					}
					if (uuid != null && serviceAdd != null && servicePort > -1) {

						break;
					}
				}
				UPnPSOAPLocalServiceHandler temp = null;
				synchronized (UPnPmanager.lockNewLocalService) {
					temp = UPnPmanager.getLocalService(uuid);
					if (temp == null) {
						try {
							masterSocket = E2EComm.bindPreReceive(E2EComm.TCP);
							masterPort = masterSocket.getLocalPort();
							// sendService(msg);
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

				sendService(msg, traversedNode, serviceAdd, servicePort, masterPort, uuid, buf);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void sendService(UPnPMulticastMessage msg2, String[] traversedNode, String serviceAdd, int servicePort, int masterPort, String uuid, byte[] messagebyte) {
		UPnPUnicastMessage unicastAnswer = new UPnPUnicastMessage(Dispatcher.getLocalId(), serviceAdd, servicePort, masterPort, uuid, messagebyte);
		try {
			if (traversedNode == null) {
				
			} else if (traversedNode.length > 0){
				E2EComm.sendUnicast(E2EComm.ipReverse(traversedNode), msg2.getMasterPort(), E2EComm.UDP, E2EComm.serialize(unicastAnswer));
				System.out.println("Sent found service");
			}
//			 for (Master tempManager : UPnPmanager.getManagerList()) {
//			 if (tempManager.getLocalInterface().equalsIgnoreCase(
//			 tempManager.getManagerAdd())) {
//			 String[] add = { tempManager.getManagerAdd() };
//			 try {
//			 //System.out.println("Sent unicast message one-hop distance");
//				 
//				 E2EComm.sendUnicast(add, msg2.getMasterPort(),
//			 E2EComm.UDP,
//			 E2EComm.serialize(unicastAnswer));
//			 } catch (Exception e) {
//			 // e.printStackTrace();
//			 }
//			 }
//			 }
			 

		} catch (Exception e) {
			e.printStackTrace();

		}
	}
	// private class SSDPReSearcher{
	// int senderNodeId=0;
	// String[] source;
	// String[] dest;
	// long timeStamp=-1;
	// public SSDPReSearcher(int senderNodeId, String[] source, String[] dest,
	// long timeStamp) {
	// super();
	// this.senderNodeId = senderNodeId;
	// this.source = source;
	// this.dest = dest;
	// this.timeStamp = timeStamp;
	// }
	// public int getSenderNodeId() {
	// return senderNodeId;
	// }
	// public String[] getSource() {
	// return source;
	// }
	// public String[] getDest() {
	// return dest;
	// }
	// public long getTimeStamp() {
	// return timeStamp;
	// }
	//
	// }
}
