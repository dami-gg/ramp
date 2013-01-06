package it.unibo.deis.lia.ramp.core.internode.upnp;

import it.unibo.deis.lia.ramp.core.e2e.BoundReceiveSocket;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.internode.Dispatcher;
import it.unibo.deis.lia.ramp.core.internode.upnp.UPnPAVRemoteResources.UPnPAVRes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UPnPAVLocalMediaServerHandler extends Thread {

	private ServerSocket avLocalSocket;
	private int aVmasterID;
	private int aVMasterPort;
	private String realAdd;

	private Vector<Socket> sockets = null;

	public UPnPAVLocalMediaServerHandler(ServerSocket avLocalSocket, int aVmasterID, int aVMasterPort, String realAdd) {
		super();
		this.avLocalSocket = avLocalSocket;
		this.aVmasterID = aVmasterID;
		this.aVMasterPort = aVMasterPort;
		this.realAdd = realAdd;
		sockets = new Vector<Socket>();

	}

	public void run() {
		while (UPnPmanager.isOpen()) {
			try {
				Socket localResourceSocket = avLocalSocket.accept();
				sockets.add(localResourceSocket);
				// System.out.println("Multimedia receiver connected");
				new ConnectionHandler(localResourceSocket).start();
			} 
			catch (Exception e) {
				// e.printStackTrace();
			}
		}
		for (Socket s : sockets){
			try {
				s.close();
			} 
			catch (IOException e) {
				// e.printStackTrace();
			}
		}
	}

	private class ConnectionHandler extends Thread {
		Socket s;

		public ConnectionHandler(Socket s) {
			super();
			this.s = s;
		}

		public void run() {
			try {
				InputStream is = s.getInputStream();
				OutputStream os = s.getOutputStream();

				int contentLength = -1;
				boolean connectionClose = false;
				boolean chunked = false;
				String soapAction = "";
				String text = "";
				String line = readLine(is); // first line
				String remote = "";
				if (line.startsWith("GET")) {
					//System.out.println("Received multimedia resource request");
					while (line != null && !line.equals("")) {
						if (line.contains("Host") || line.contains("HOST") || line.contains("host")) {

							if (realAdd.startsWith("http")) {//for other protocols we need to add other parsers
								remote = realAdd.substring("http://".length(), realAdd.indexOf('/', 7));
							}
							
							line = "HOST:  " + remote;
						}
						text += line + (char) 0x0D + (char) 0x0A;

						line = readLine(is);
					}
					text += "" + (char) 0x0D + (char) 0x0A;

					String[] addSplit = remote.split(":");
					String serviceAdd = addSplit[0];
					int servicePort = Integer.parseInt(addSplit[1]);

					BoundReceiveSocket receiveSocket = E2EComm.bindPreReceive(E2EComm.TCP);
					UPnPAVGetMessage messToSend = new UPnPAVGetMessage(Dispatcher.getLocalId(), receiveSocket.getLocalPort(), serviceAdd, servicePort, text.getBytes());
					E2EComm.sendUnicastDestNodeId(aVmasterID, aVMasterPort, E2EComm.TCP, E2EComm.serialize(messToSend));

					E2EComm.receive(receiveSocket, os);// it receives the streaming and forward it directly

				}
			}
			catch (Exception e) {
				// e.printStackTrace();
			}
		}
	}

	public ServerSocket getAvLocalSocket() {
		return avLocalSocket;
	}

	public int getAvLocalSocketPort() {
		return avLocalSocket.getLocalPort();
	}

	public int getaVmasterID() {
		return aVmasterID;
	}

	public int getaVMasterPort() {
		return aVMasterPort;
	}

	public String getRealAdd() {
		return realAdd;
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

}
