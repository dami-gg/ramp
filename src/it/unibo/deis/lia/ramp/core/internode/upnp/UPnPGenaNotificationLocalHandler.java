package it.unibo.deis.lia.ramp.core.internode.upnp;

import it.unibo.deis.lia.ramp.core.e2e.BoundReceiveSocket;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.e2e.UnicastPacket;
import it.unibo.deis.lia.ramp.core.internode.Dispatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class UPnPGenaNotificationLocalHandler extends Thread{
	
	private String uuid;
	private ServerSocket socket;
	private String realAdd;
	private String[] destToClient;
	private int senderPort;
	private boolean open;
	
	public UPnPGenaNotificationLocalHandler(String uuid, ServerSocket socket, String realAdd, String[] destToClient, int senderPort) {
		super();
		this.uuid = uuid;
		this.socket = socket;
		this.realAdd = realAdd;
		this.destToClient = destToClient;
		this.senderPort = senderPort;
		open=true;
	}

	public void deactivate(){
		try {
			socket.close();
			open=false;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	public void run(){
		while(open){
			try {
				Socket senderSocket = socket.accept();
				new NotifyHandler(senderSocket).start();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class NotifyHandler extends Thread{
		Socket s;
		
		public NotifyHandler(Socket s) {
			super();
			this.s = s;
		}

		public void run(){
			try {
				InputStream is = s.getInputStream();

				int contentLength = -1;
				boolean connectionClose = false;
				boolean chunked = false;
				
				String text = "";
				String line = readLine(is); // first line
				
				
				System.err.println("Received notify for "+uuid);
				byte[] res = null;
				while (line != null && !line.equals("")) {
					if (line.contains("Host") || line.contains("HOST") || line.contains("host")) {

						line= "HOST: "+realAdd;
					}
					if (line.contains("Content-Length")) {
						String length = line.split(" ")[1];

						contentLength = Integer.parseInt(length);
					} else if (line.contains("Transfer-Encoding: chunked")) {
						chunked = true;
					}

					// line.replace(localAdd + ":" + localPort, remoteAdd
					// + ":" + remotePort);
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
				} else {

					res = new byte[text.length() + contentLength];
					System.arraycopy(text.getBytes(), 0, res, 0, text.length());

					int temp = 0;
					for (int i = text.length(); temp != -1 && i < res.length; i++) {
						temp = is.read();
						res[i] = (byte) temp;

					}
				}
				
				String[] splitRealAdd=realAdd.split(":");
				BoundReceiveSocket receiveSocket = E2EComm.bindPreReceive(E2EComm.TCP);
				UPnPUnicastMessage messageUnicast = new UPnPUnicastMessage(Dispatcher.getLocalId(), splitRealAdd[0], Integer.parseInt(splitRealAdd[1]), receiveSocket.getLocalPort(), uuid, res);
				E2EComm.sendUnicast(destToClient, senderPort, E2EComm.TCP, E2EComm.serialize(messageUnicast));
				
				
				UnicastPacket up = (UnicastPacket) E2EComm.receive(receiveSocket, 30 * 1000);
				

				receiveSocket.close();
				Object payload = E2EComm.deserialize(up.getBytePayload());
				byte[] tosend = (byte[]) payload;
				
				
				OutputStream os = s.getOutputStream();
				os.write(tosend);
				os.flush();
				s.close();
			}
			catch (Exception e) {
				// e.printStackTrace();
			}
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
	
	public String getUuid() {
		return uuid;
	}


	public ServerSocket getSocket() {
		return socket;
	}


	public String getRealAdd() {
		return realAdd;
	}


	public String[] getDestToClient() {
		return destToClient;
	}


	public int getSenderPort() {
		return senderPort;
	}
	
}
