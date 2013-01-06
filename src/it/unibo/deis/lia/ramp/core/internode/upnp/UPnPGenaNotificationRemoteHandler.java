package it.unibo.deis.lia.ramp.core.internode.upnp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import it.unibo.deis.lia.ramp.core.e2e.BoundReceiveSocket;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.e2e.UnicastPacket;

public class UPnPGenaNotificationRemoteHandler extends Thread{
	
	String uuid;
	BoundReceiveSocket receiveSocket;
	boolean open;
	
	public UPnPGenaNotificationRemoteHandler(String uuid, BoundReceiveSocket receiveSocket) {
		super();
		this.uuid = uuid;
		this.receiveSocket = receiveSocket;
		open=true;
	}
	
	public void deactivate(){
		open=false;
		try {
			receiveSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run(){
		while(open){
			try {
				UnicastPacket up = (UnicastPacket) E2EComm.receive(receiveSocket);
				Object payload = E2EComm.deserialize(up.getBytePayload());
				
				if(payload instanceof UPnPUnicastMessage){
						//System.out.println("Received remote notification");
						UPnPUnicastMessage mess=(UPnPUnicastMessage)payload;
						Socket s=new Socket(mess.getServiceAdd(), mess.getServicePort());
						OutputStream os=s.getOutputStream();
						os.write((byte[])mess.getMessage());
						os.flush();
						//System.out.println("Sent "+ new String((byte[])mess.getMessage()));
						InputStream is = s.getInputStream();
						String text="";
						
						String line = readLine(is);
						
						while (line != null && !line.equals("")) {
							text += line + (char) 0x0D + (char) 0x0A;
			
							line = readLine(is);
						}
						text += "" + (char) 0x0D + (char) 0x0A;
						E2EComm.sendUnicast(E2EComm.ipReverse(up.getSource()), mess.getSenderPort(), E2EComm.TCP, E2EComm.serialize(text.getBytes()));
						s.close();
				}
			
			} catch (Exception e) {
				e.printStackTrace();
			}
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
	public String getUuid() {
		return uuid;
	}

	public BoundReceiveSocket getReceiveSocket() {
		return receiveSocket;
	}

}
