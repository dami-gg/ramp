package it.unibo.deis.lia.ramp.core.internode.upnp;

import it.unibo.deis.lia.ramp.core.e2e.BoundReceiveSocket;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.e2e.UnicastPacket;
import it.unibo.deis.lia.ramp.core.internode.Dispatcher;
import it.unibo.deis.lia.ramp.core.internode.upnp.UPnPAVRemoteResources.UPnPAVRes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UPnPSOAPRemoteServiceHandler extends Thread {

	private String remoteAdd;
	private int remotePort;
	private String localAdd;
	private int localPort;
	private String uuid;
	private int masterId;
	private int masterPort;
	private String[] pathToMaster;
	private ServerSocket localServerSocket;
	private boolean open;
	private UPnPAVRemoteResources aVresources;
	private Vector<UPnPGenaNotificationRemoteHandler> notificationsRemoteHandlers;

	public UPnPSOAPRemoteServiceHandler(String remoteAdd, int remotePort, String localAdd, int localPort, String uuid, int masterId, int masterPort, String[] pathToMaster, ServerSocket localServerSocket) {
		super();
		this.remoteAdd = remoteAdd;
		this.remotePort = remotePort;
		this.localAdd = localAdd;
		this.localPort = localPort;
		this.uuid = uuid;
		this.masterId = masterId;
		this.masterPort = masterPort;
		this.pathToMaster = E2EComm.ipReverse(pathToMaster);
		this.localServerSocket = localServerSocket;
		aVresources = UPnPAVRemoteResources.getInstance();
		notificationsRemoteHandlers=new Vector<UPnPGenaNotificationRemoteHandler>();
		this.open = true;
	}

	public void stopRemoteService() {
		open = false;
		try {
			localServerSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public String getRemoteAdd() {
		return remoteAdd;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public String getLocalAdd() {
		return localAdd;
	}

	public int getLocalPort() {
		return localPort;
	}

	public String getUuid() {
		return uuid;
	}

	public int getMasterId() {
		return masterId;
	}

	public int getMasterPort() {
		return masterPort;
	}

	public String[] getPathToMaster() {
		return pathToMaster;
	}

	public ServerSocket getLocalServerSocket() {
		return localServerSocket;
	}
	private UPnPGenaNotificationRemoteHandler getHandler(String uuid){
		for(UPnPGenaNotificationRemoteHandler temp:notificationsRemoteHandlers){
			if(temp.getUuid().equalsIgnoreCase(uuid))
				return temp;
		}
		return null;
	}

	public void run() {

		System.out.println("Remote service handler started for device with " + uuid);
		while (open) {
			try {
				localServerSocket.setSoTimeout(30 * 1000);
				//System.out.println("Remote Service handler is waiting on" + localServerSocket.getLocalSocketAddress());
				Socket clientSocket = localServerSocket.accept();
				//System.out.println("Local client connected");
				new localClientHandler(clientSocket).start();
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}

	}

	public class localClientHandler extends Thread {
		private Socket socket;

		public localClientHandler(Socket socket) {
			super();
			this.socket = socket;
		}

		public void run() {
			try {
				InputStream is = socket.getInputStream();

				int contentLength = -1;
				boolean connectionClose = false;
				boolean chunked = false;
				String soapAction = "";
				String text = "";
				String line = readLine(is); // first line

				if (line.startsWith("GET")) {
					System.err.println("Received description request for device with uuid " + uuid);
					while (line != null && !line.equals("")) {
						if (line.contains("Host") || line.contains("HOST") || line.contains("host")) {

							final Pattern pattern = Pattern.compile(localAdd + ":" + localPort);
							final Matcher matcher = pattern.matcher(line);
							line = matcher.replaceAll(remoteAdd + ":" + remotePort);
						}

						// line.replace(localAdd + ":" + localPort, remoteAdd
						// + ":" + remotePort);
						text += line + (char) 0x0D + (char) 0x0A;

						line = readLine(is);
					}
					text += "" + (char) 0x0D + (char) 0x0A;
					BoundReceiveSocket receiveSocket = E2EComm.bindPreReceive(E2EComm.TCP);
					UPnPUnicastMessage messageUnicast = new UPnPUnicastMessage(Dispatcher.getLocalId(), remoteAdd, remotePort, receiveSocket.getLocalPort(), uuid, text.getBytes());
					//System.out.println(pathToMaster.length);
					if (pathToMaster.length > 0)
						E2EComm.sendUnicast(pathToMaster, masterPort, E2EComm.TCP, E2EComm.serialize(messageUnicast));// send request

					
					// else if (pathToMaster.length == 0) {
					
					// //
					//System.out.println("this is the socket"+("" + localServerSocket.getInetAddress()).substring(1));
//					for (Master tempManager : UPnPmanager.getManagerList()) {
//						if (tempManager.getLocalInterface().equalsIgnoreCase(tempManager.getManagerAdd())
//							 && !tempManager.getLocalInterface().equalsIgnoreCase(("" + localServerSocket.getInetAddress()).substring(1))) {
//							String[] add = { tempManager.getManagerAdd() };
//							try {
//								E2EComm.sendUnicast(add, masterPort, E2EComm.TCP, E2EComm.serialize(messageUnicast));
//							} catch (Exception e) {
//								// e.printStackTrace();
//							}
//						}
//					}
					// }

					System.out.println("Sent description request to " + uuid);

					UnicastPacket up = (UnicastPacket) E2EComm.receive(receiveSocket, 30 * 1000);// waiting answer

					System.out.println("Description reived " + uuid);

					receiveSocket.close();
					Object payload = E2EComm.deserialize(up.getBytePayload());
					byte[] tosend = (byte[]) payload;

					OutputStream os = socket.getOutputStream();
					os.write(tosend);
					os.flush();
					socket.close();

				}

				if (line.startsWith("POST") || line.startsWith("M-POST")) {
					//System.err.println("Soap action request for " + uuid);
					byte[] res = null;
					while (line != null && !line.equals("")) {

						if (line.contains("Content-Length")) {
							String length = line.split(" ")[1];

							contentLength = Integer.parseInt(length);
						} else if (line.contains("Transfer-Encoding: chunked")) {
							chunked = true;
						}
						if (line.contains("SOAPACTION")) {
							String[] linePeaces = line.split("" + (char) 0x23);// carattere
																				// #
							soapAction = linePeaces[1].replaceAll("" + (char) 0x22, "");

						}
						if (line.contains("Host") || line.contains("HOST") || line.contains("host")) {

							final Pattern pattern = Pattern.compile(localAdd + ":" + localPort);
							final Matcher matcher = pattern.matcher(line);
							line = matcher.replaceAll(remoteAdd + ":" + remotePort);
						}

						text += line + (char) 0x0D + (char) 0x0A;
						line = readLine(is);

					}

					text += "" + (char) 0x0D + (char) 0x0A;

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					if (chunked) {
						line = readLine(is);

						int lineLength = Integer.decode("0x" + line);

						for (int i = 0; i < line.length(); i++) {
							baos.write(line.charAt(i));
						}
						baos.write(0x0D);
						baos.write(0x0A);
						while (!line.equals("0")) {
							for (int i = 0; i < lineLength; i++) {
								int temp = is.read();

								baos.write(temp);
							}

							baos.write(is.read());
							baos.write(is.read());

							line = readLine(is);

							lineLength = Integer.decode("0x" + line);

							for (int i = 0; i < line.length(); i++) {
								baos.write(line.charAt(i));
							}
							baos.write(0x0D);
							baos.write(0x0A);
						}
						baos.write(0x0D);
						baos.write(0x0A);
					}

					byte[] chunkedArray = baos.toByteArray();
					if (contentLength == -1 && chunkedArray.length == 0) {
						res = text.getBytes();
					} else if (chunkedArray.length != 0) {
						res = new byte[text.length() + chunkedArray.length];
						System.arraycopy(text.getBytes(), 0, res, 0, text.length());
						System.arraycopy(chunkedArray, 0, res, text.length(), chunkedArray.length);
					} 
					else {
						res = new byte[text.length() + contentLength];
						System.arraycopy(text.getBytes(), 0, res, 0, text.length());

						int temp = 0;
						for (int i = text.length(); temp != -1 && i < res.length; i++) {
							temp = is.read();
							res[i] = (byte) temp;
						}
					}

					BoundReceiveSocket receiveSocket = E2EComm.bindPreReceive(E2EComm.TCP);
		
					Object messageUnicast = null;
					if (soapAction.equalsIgnoreCase("SetAVTransportURI")) {

						System.err.println("SetAvTransport");
						String resToParse = new String(res);
						int startAddPosition = resToParse.indexOf("<CurrentURI>") + "<CurrentURI>".length();
						int endAddPosition = resToParse.indexOf("</CurrentURI>");
						String addAV = resToParse.substring(startAddPosition, endAddPosition);
						
						UPnPAVRes avRes = aVresources.getRes(addAV);
						UPnPAVSetMessage toSend = null;
						if (avRes != null)
							toSend = new UPnPAVSetMessage(avRes.getMasterID(), avRes.getMasterPort(), addAV, res);
						
						messageUnicast = new UPnPUnicastMessage(Dispatcher.getLocalId(), remoteAdd, remotePort, receiveSocket.getLocalPort(), uuid, E2EComm.serialize(toSend));
					} 
					else {
						messageUnicast = new UPnPUnicastMessage(Dispatcher.getLocalId(), remoteAdd, remotePort, receiveSocket.getLocalPort(), uuid, res);
					}

					if (pathToMaster.length > 0)
						E2EComm.sendUnicast(pathToMaster, masterPort, E2EComm.TCP, E2EComm.serialize(messageUnicast));

					
					// else if (pathToMaster.length == 0) {
					
//					for (Master tempManager : UPnPmanager.getManagerList()) {
//						if (tempManager.getLocalInterface().equalsIgnoreCase(tempManager.getManagerAdd())
//							 && !tempManager.getLocalInterface().equalsIgnoreCase(("" + localServerSocket.getInetAddress()).substring(1))) {
//							String[] add = { tempManager.getManagerAdd() };
//							try {
//								E2EComm.sendUnicast(add, masterPort, E2EComm.TCP, E2EComm.serialize(messageUnicast));
//								
//							} catch (Exception e) {
//								// e.printStackTrace();
//							}
//						}
//					}
					// }

					UnicastPacket up = (UnicastPacket) E2EComm.receive(receiveSocket, 30 * 1000);
					//System.out.println("Answer received  for" + uuid);

					receiveSocket.close();
					Object payload = E2EComm.deserialize(up.getBytePayload());
					byte[] tosend = (byte[]) payload;
					if (soapAction.equalsIgnoreCase("Browse")) {
						//System.out.println("Received request for directoryBrose AV");
						tosend = parseRemoteAVResources(tosend, up.getSourceNodeId(), masterPort);

					}

					/*
					 * 
					 * if soap browse action record remote AV resources
					 */
					OutputStream os = socket.getOutputStream();
					os.write(tosend);
					os.flush();
					socket.close();

				}

				if (line.startsWith("SUBSCRIBE")) {
					
					String sidUuid="";
					UPnPGenaNotificationRemoteHandler handler=null;
					BoundReceiveSocket receiveSocket=null;
					while (line != null && !line.equals("")) {
						if (line.contains("Host") || line.contains("HOST") || line.contains("host")) {

							final Pattern pattern = Pattern.compile(localAdd + ":" + localPort);
							final Matcher matcher = pattern.matcher(line);
							line = matcher.replaceAll(remoteAdd + ":" + remotePort);
						}
if(line.contains("CALLBACK: <http://")){
	sidUuid=line.substring(line.indexOf('/', "http://".length()+1)+1, line.indexOf('/', "http://".length()+1)+37);
}
					
						text += line + (char) 0x0D + (char) 0x0A;

						line = readLine(is);
					}
					text += "" + (char) 0x0D + (char) 0x0A;
					if(!sidUuid.equalsIgnoreCase("")){
						handler=getHandler(sidUuid);
					}
					if(handler==null)
					receiveSocket = E2EComm.bindPreReceive(E2EComm.TCP);
					else
						receiveSocket=handler.getReceiveSocket();
					UPnPUnicastMessage messageUnicast = new UPnPUnicastMessage(Dispatcher.getLocalId(), remoteAdd, remotePort, receiveSocket.getLocalPort(), uuid, text.getBytes());
					System.out.println(pathToMaster.length);
					if (pathToMaster.length > 0)
						E2EComm.sendUnicast(pathToMaster, masterPort, E2EComm.TCP, E2EComm.serialize(messageUnicast));
					
					
					UnicastPacket up = (UnicastPacket) E2EComm.receive(receiveSocket, 30 * 1000);// attende risposta
	
	
					
					Object payload = E2EComm.deserialize(up.getBytePayload());
					
					
					byte[] tosend = (byte[]) payload;
				
					String answer=new String(tosend);
					if(answer.contains("uuid")&&!sidUuid.equalsIgnoreCase("")){
						int beginUuid=answer.indexOf("uuid:")+"uuid:".length();
						int endUuid=beginUuid+36;
						sidUuid=answer.substring(beginUuid, endUuid);}
					if(handler==null){
						handler=new UPnPGenaNotificationRemoteHandler(sidUuid, receiveSocket);
					System.out.println("GENA remote handler started for connection with uuid "+sidUuid);
					notificationsRemoteHandlers.add(handler);
					handler.start();}
					
					OutputStream os = socket.getOutputStream();
					os.write(tosend);
					os.flush();
					socket.close();
				}
				
				if (line.startsWith("UNSUBSCRIBE")) {
					// modello ad eventi
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private byte[] parseRemoteAVResources(byte[] tosend, int masterId, int masterPort) {

			String allMessage = new String(tosend);
			
			Vector<String> lines = convertToLines(tosend);

			ServerSocket localSock = null;

			try {
				localSock = new ServerSocket(0, -1, InetAddress.getLocalHost());
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			int masterID = masterId;

			String protocolInfo = "";
			int size = -1;
			String duration = "";
			int bitRate = -1;
			int sampleFrequency = -1;
			int bitPerSample = -1;
			int nrAudioChannel = -1;
			String resolution = "";
			int colorDepth = -1;
			String protection = "";
			String importUri = "";
			String realAdd = "";
			for (String line : lines) {

				if (line.contains("<res") || line.contains("&lt;res") || line.contains((char) 0x3C + "res")) {
					// System.out.println(line);

					try {
						if (line.contains("protocolInfo")) {
							String linepeace[] = line.split("protocolInfo=\"");
							linepeace = linepeace[1].split("\"");
							protocolInfo = linepeace[0];
						}
						if (line.contains("size")) {
							String linepeace[] = line.split("size=\"");
							linepeace = linepeace[1].split("\"");
							size = Integer.parseInt(linepeace[0]);
						}
						if (line.contains("duration")) {
							String linepeace[] = line.split("duration=\"");
							linepeace = linepeace[1].split("\"");
							duration = linepeace[0];
						}
						if (line.contains("bitrate")) {
							String linepeace[] = line.split("bitrate=\"");
							linepeace = linepeace[1].split("\"");
							bitRate = Integer.parseInt(linepeace[0]);
						}
						if (line.contains("sampleFrequency")) {
							String linepeace[] = line.split("sampleFrequency=\"");
							linepeace = linepeace[1].split("\"");
							sampleFrequency = Integer.parseInt(linepeace[0]);
						}
						if (line.contains("bitsPerSample")) {
							String linepeace[] = line.split("bitsPerSample=\"");
							linepeace = linepeace[1].split("\"");
							bitPerSample = Integer.parseInt(linepeace[0]);
						}
						if (line.contains("nrAudioChannels")) {
							String linepeace[] = line.split("nrAudioChannels=\"");
							linepeace = linepeace[1].split("\"");
							nrAudioChannel = Integer.parseInt(linepeace[0]);
						}
						if (line.contains("resolution")) {
							String linepeace[] = line.split("resolution=\"");
							linepeace = linepeace[1].split("\"");
							resolution = linepeace[0];
						}
						if (line.contains("colorDepth")) {
							String linepeace[] = line.split("colorDepth=\"");
							linepeace = linepeace[1].split("\"");
							colorDepth = Integer.parseInt(linepeace[0]);
						}
						if (line.contains("protection")) {
							String linepeace[] = line.split("protection=\"");
							linepeace = linepeace[1].split("\"");
							protection = linepeace[0];
						}
						if (line.contains("importUri")) {
							String linepeace[] = line.split("importUri=\"");
							linepeace = linepeace[1].split("\"");
							importUri = linepeace[0];
						}
						if (line.contains(">")) {
							String linepeace[] = line.split(">");
							linepeace = linepeace[1].split("<");
							realAdd = linepeace[0];
						}
						if (line.contains("&gt;")) {
							String linepeace[] = line.split("&gt;");
							linepeace = linepeace[1].split("&lt;");
							realAdd = linepeace[0];
						}
						if (line.contains("" + (char) 0x3E)) {
							String linepeace[] = line.split("" + (char) 0x3E);
							linepeace = linepeace[1].split("" + (char) 0x3C);
							realAdd = linepeace[0];
						}

					} catch (Exception e) {
						e.printStackTrace();
					}

					if (!realAdd.contains("127.0.0.1")) {
						aVresources.addRes(masterID, masterPort, protocolInfo, size, duration, bitRate, sampleFrequency, bitPerSample, nrAudioChannel, resolution, colorDepth, protection, importUri,
								realAdd);
						//System.out.println("Updated resource with this master " + masterID + " with this real add " + realAdd);
						// UPnPAVRes res=aVresources.getRes(masterID, realAdd);
						UPnPAVLocalMediaServerHandler local = new UPnPAVLocalMediaServerHandler(localSock, masterID, masterPort, realAdd);
						local.start();

						final Pattern pattern = Pattern.compile(realAdd.substring("http:/".length(), realAdd.indexOf('/', 7)));
						final Matcher matcher = pattern.matcher(allMessage);

						allMessage = matcher.replaceAll(("" + localSock.getLocalSocketAddress()).substring(("" + localSock.getLocalSocketAddress()).indexOf('/')));

					}
					if (realAdd.contains("127.0.0.1")) {

						try {
							//System.out.println("Resource binded to this local address " + localSock.getInetAddress());
							UPnPAVRes res = aVresources.getRes(masterID, realAdd);
							UPnPAVLocalMediaServerHandler local = new UPnPAVLocalMediaServerHandler(localSock, masterID, masterPort, res.getRealAdd());
							local.start();

							final Pattern pattern = Pattern.compile(realAdd.substring("http:/".length(), realAdd.indexOf('/', 7)));
							final Matcher matcher = pattern.matcher(allMessage);
							String realAdd2 = res.getRealAdd();
							allMessage = matcher.replaceAll(realAdd2.substring("http:/".length(), realAdd2.indexOf('/', 7)));

						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				}
			}
			int newContenentLenght = allMessage.indexOf("/s:Envelope>") + "/s:Envelope>".length() - allMessage.indexOf("<?xml");
			int numberPosition = allMessage.indexOf("Content-Length:") + "Content-Length:".length();
			if (allMessage.charAt(numberPosition) == ' ') {
				numberPosition++;
			}
			allMessage = allMessage.substring(0, numberPosition) + newContenentLenght + allMessage.substring(allMessage.indexOf((char) (0x0D), numberPosition));

			return allMessage.getBytes();
		}

		private String readLine(InputStream is) throws Exception {
			String res = "";

			int temp = is.read();
			while (temp != 0x0D) {
				res += (char) temp;
				temp = is.read();
			}
			is.read(); // (char)0x0A
			return res;
		}

		private Vector<String> convertToLines(byte[] buf) {
			Vector<String> res = new Vector<String>();

			boolean jump = false;
			StringBuilder sb = new StringBuilder(buf.length);
			for (byte b : buf)
				sb.append((char) b);

			String line = sb.toString();

			if (!(line.contains("<?xml") && (line.contains("<res") || line.contains("&lt;res") || line.contains((char) 0x3C + "res"))))
				res.add(line);
			else {

				boolean repeat = true;
				while (repeat) {
					int indexOpenRes = line.indexOf("&lt;res");
					int indexCloseRes = line.indexOf("/res&gt;") + "/res&gt;".length();
					res.add(line.substring(indexOpenRes, indexCloseRes));

					line = line.substring(indexCloseRes);
					if (!line.contains("&lt;res"))
						repeat = false;
				}
			}
			sb = new StringBuilder(buf.length);

			return res;
		}
	}
}
