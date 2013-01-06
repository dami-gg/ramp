package it.unibo.deis.lia.ramp.core.internode.upnp;

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

import it.unibo.deis.lia.ramp.core.e2e.BoundReceiveSocket;

import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.e2e.GenericPacket;
import it.unibo.deis.lia.ramp.core.e2e.UnicastPacket;

public class UPnPSOAPLocalServiceHandler extends Thread {

	private boolean open = true;
	private String uuid = "";
	private BoundReceiveSocket masterSocket = null;
	private Vector<ServerSocket> avServerSocket = null;
	private Vector<UPnPGenaNotificationLocalHandler> genaNotificationHandlers;

	public UPnPSOAPLocalServiceHandler(String uuid, BoundReceiveSocket masterSocket) {
		super();
		this.uuid = uuid;
		this.masterSocket = masterSocket;
		avServerSocket = new Vector<ServerSocket>();
		genaNotificationHandlers = new Vector<UPnPGenaNotificationLocalHandler>();
	}

	public void deactivate() {
		open = false;
		try {
			for (ServerSocket sock : avServerSocket)
				sock.close();
			masterSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		GenericPacket gp;
		while (open) {
			try {
				gp = E2EComm.receive(masterSocket, 30 * 1000);
				new UPnPRequestHandler(gp).start();
				System.out.println("Received a message from a remote node");
			} catch (Exception e) {
				// e.printStackTrace();
			}

		}
	}

	private class UPnPRequestHandler extends Thread {
		GenericPacket gp;

		public UPnPRequestHandler(GenericPacket gp) {
			super();
			this.gp = gp;
		}

		public void run() {
			try {
				boolean sub = false;
				if (gp instanceof UnicastPacket) {
					// 1) payload
					UnicastPacket up = (UnicastPacket) gp;
					Object payload = E2EComm.deserialize(up.getBytePayload());
					if (payload instanceof UPnPAVGetMessage) {

						UPnPAVGetMessage getAVmess = (UPnPAVGetMessage) payload;
						Socket sAV = null;
						byte[] resAV = null;
						try {
							sAV = new Socket(getAVmess.getServiceAdd(), getAVmess.getServicePort());
						} catch (UnknownHostException uhe) {
							resAV = "UPnp device not found".getBytes();
						}
						if (resAV == null) {
							OutputStream os = sAV.getOutputStream();
							InputStream is = sAV.getInputStream();

							resAV = getAVmess.getMess();
							os.write(resAV);
							os.flush();
							E2EComm.sendUnicast(E2EComm.ipReverse(up.getSource()), getAVmess.getMasterPort(), E2EComm.TCP, is);
						}
					}

					if (payload instanceof UPnPUnicastMessage) {
						UPnPUnicastMessage mess = (UPnPUnicastMessage) payload;
						Socket s = null;
						byte[] res = null;
						try {
							s = new Socket(mess.getServiceAdd(), mess.getServicePort());
							System.out.println("received an unicast message");
						} catch (UnknownHostException uhe) {
							res = "UPnp device not found".getBytes();
						}
						if (res == null) {
							OutputStream os = s.getOutputStream();
							InputStream is = s.getInputStream();
							Object des = mess.getMessage();
							byte[] upnprequest = null;
							boolean setMess = true;
							try {
								des = E2EComm.deserialize((byte[]) des);
							} catch (Exception e) {
								setMess = false;
							}

							if (setMess && des instanceof UPnPAVSetMessage) {
								UPnPAVSetMessage setMessage = (UPnPAVSetMessage) des;
								ServerSocket localMediaServerSocket = new ServerSocket();
								localMediaServerSocket.bind(s.getLocalSocketAddress());
								upnprequest = setMessage.getMsg();
								String upnprequestString = new String(upnprequest);

								String oldAdd = setMessage.getRealAdd();
								if (oldAdd.startsWith("http")) {// http protocol
									String newAdd = "http:/" + localMediaServerSocket.getLocalSocketAddress() + oldAdd.substring(oldAdd.indexOf('/', 7));
									final Pattern pattern = Pattern.compile(newAdd);
									final Matcher matcher = pattern.matcher(upnprequestString);
									upnprequestString = matcher.replaceAll(oldAdd);
									int newContenentLenght = upnprequestString.indexOf("/s:Envelope>") + "/s:Envelope>".length() - upnprequestString.indexOf("<?xml");
									int numberPosition = upnprequestString.indexOf("Content-Length:") + "Content-Length:".length();
									if (upnprequestString.charAt(numberPosition) == ' ') {
										numberPosition++;
									}
									upnprequestString = upnprequestString.substring(0, numberPosition) + newContenentLenght
											+ upnprequestString.substring(upnprequestString.indexOf((char) (0x0D), numberPosition));// cambia il currentlenght

									new UPnPAVLocalMediaServerHandler(localMediaServerSocket, setMessage.getaVmasterID(), setMessage.getRealPort(), setMessage.getRealAdd()).start();

									upnprequest = upnprequestString.getBytes();
									os.write(upnprequest);
									os.flush();

								}
								// add other protocol here (rtp, stream, ecc)

							} else {

								upnprequest = (byte[]) des;
								String upnpreq = new String(upnprequest);
								if (upnpreq.contains("SUBSCRIBE")) {
									
									upnprequest = newGenaSubscribe(upnpreq, up, mess, s.getLocalAddress());
									sub = true;
									// System.out.println("Subscribe message received");
									}
								System.out.println("sending answare to controller");	
								os.write(upnprequest);
									os.flush();

								}

								String line = readLine(is); // first line
								String text = "";
								int contentLength = -1;
								boolean chunked = false;

								while (line != null && !line.equals("")) {
									text += line + (char) 0x0D + (char) 0x0A;
									if (line.contains("Content-Length")) {
										String length = line.split(" ")[1];
										contentLength = Integer.parseInt(length);
									} else if (line.contains("Transfer-Encoding: chunked")) {
										chunked = true;
									}

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
								} else {
									res = new byte[text.length() + contentLength];
									System.arraycopy(text.getBytes(), 0, res, 0, text.length());

									int temp = 0;
									for (int i = text.length(); temp != -1 && i < res.length; i++) {
										temp = is.read();
										res[i] = (byte) temp;
										// System.out.print(""+(char)temp);
									}
								}
								s.close();

								String[] destToClient = E2EComm.ipReverse(up.getSource());

								if(sub)
									System.out.println("UPnP answer "+new String(res));
								else
									System.out.println("UPnP answer ");
								E2EComm.sendUnicast(destToClient, mess.getSenderPort(), E2EComm.TCP, E2EComm.serialize(res));

							}
						}
					}
				
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	private byte[] newGenaSubscribe(String upnpreq, UnicastPacket up, UPnPUnicastMessage mess, InetAddress add) {
		String upnp = upnpreq;
		ServerSocket genaLocalSocket = null;

		String realBack = null;
		String callBackuuid = null;
		String realCallBackAdd = null;
		if (upnp.contains("CALLBACK")) {
			int beginCall = upnpreq.indexOf("CALLBACK: <") + "CALLBACK: <".length();
			int endCall = upnpreq.indexOf('>', beginCall);
			realCallBackAdd = upnpreq.substring(beginCall, endCall);
			System.out.println("" + realCallBackAdd);
		}
		if (realCallBackAdd != null && realCallBackAdd.contains("http")) {// possible other protocol
			realBack = realCallBackAdd.substring("http://".length(), realCallBackAdd.indexOf('/', "http://".length() + 1));
			callBackuuid = realCallBackAdd.substring(realCallBackAdd.indexOf('/', "http://".length() + 1) + 1, realCallBackAdd.indexOf('/', "http://".length() + 1) + 37);
			System.out.println("realback  " + realBack);

		}
		if (upnpreq.contains("uuid")) {
			int beginUuid = upnpreq.indexOf("uuid:") + "uuid:".length();
			int endUuid = beginUuid + 36;
			callBackuuid = upnpreq.substring(beginUuid, endUuid);
		}

		UPnPGenaNotificationLocalHandler handler = getHandler(callBackuuid);
		if (handler == null) {
			try {
				genaLocalSocket = new ServerSocket(0, 0, add);
			} catch (IOException e) {
				e.printStackTrace();
			}
			handler = new UPnPGenaNotificationLocalHandler(callBackuuid, genaLocalSocket, realBack, up.getSource(), mess.getSenderPort());

			genaNotificationHandlers.add(handler);
			handler.start();
		}

		else {
			genaLocalSocket = handler.getSocket();
		}
		if (realBack != null) {
			final Pattern pattern = Pattern.compile(realBack);
			final Matcher matcher = pattern.matcher(upnpreq);

			upnp = matcher.replaceAll(("" + genaLocalSocket.getLocalSocketAddress()).substring(1));
			// from now the handler is ready to return the notify message to the real service
			// System.out.println("Received subscription to service"+callBackuuid);}
			return upnp.getBytes();
		}
		return null;

	}

	private UPnPGenaNotificationLocalHandler getHandler(String uuid) {
		for (UPnPGenaNotificationLocalHandler temp : genaNotificationHandlers) {
			if (temp.getUuid().equalsIgnoreCase(uuid))
				return temp;
		}
		return null;
	}

	protected static String readLine(InputStream is) throws Exception {
		String res = "";
		int temp = is.read();
		while (temp != 0x0D) {
			res += (char) temp;
			temp = is.read();
		}
		is.read(); // (char)0x0A
		return res;
	}

	public BoundReceiveSocket getMasterSocket() {
		return masterSocket;
	}

	public String getUuid() {
		return uuid;
	}

	public boolean isOpen() {
		return open;
	}

}
