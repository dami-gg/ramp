/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.service.application;

import it.unibo.deis.lia.ramp.RampEntryPoint;
import it.unibo.deis.lia.ramp.service.management.ServiceManager;
import it.unibo.deis.lia.ramp.core.e2e.*;

import java.io.*;
import java.net.*;

/**
 *
 * @author useruser
 */
public class FileSharingService extends Thread{

    private boolean open=true;

    private String sharedDirectory = "./temp/fsService";
    private int bufferSize = 0;
    private boolean bestBufferSize = false;

    private int protocol = E2EComm.TCP;

    private static BoundReceiveSocket serviceSocket;
    private static FileSharingService fileSharing=null;
    private static FileSharingServiceJFrame fssjf=null;
    
	
	public static boolean isActive(){
        return FileSharingService.fileSharing != null;
    }
    public static synchronized FileSharingService getInstance(){
        try{
        	
            if(FileSharingService.fileSharing==null){
                FileSharingService.fileSharing = new FileSharingService(true);
                FileSharingService.fileSharing.start();
                
            }
            if(fssjf!=null){
                fssjf.setVisible(true);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return FileSharingService.fileSharing;
    }
    public static synchronized FileSharingService getInstanceNoShow(){
        try{
            if(FileSharingService.fileSharing==null){
                FileSharingService.fileSharing = new FileSharingService(false);
                FileSharingService.fileSharing.start();
            }
            if(fssjf!=null){
                
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return FileSharingService.fileSharing;
    }
    private FileSharingService(boolean gui) throws Exception{
        serviceSocket = E2EComm.bindPreReceive(protocol);
        
        ServiceManager.getInstance(false).registerService("" +
        		"FileSharing", 
        		serviceSocket.getLocalPort(), 
        		protocol
    		);
        
        if(gui&&RampEntryPoint.getAndroidContext()==null){
        	fssjf = new FileSharingServiceJFrame(this);
        }
        else{
            if(RampEntryPoint.getAndroidContext() != null){
            	sharedDirectory = android.os.Environment.getExternalStorageDirectory()+"/ramp"; //"/sdcard/ramp";
        	}
        }
    }

    public void setBestBufferSize(boolean bestBufferSize) {
        this.bestBufferSize = bestBufferSize;
    }
    public int getBufferSize() {
        return bufferSize;
    }
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public String getSharedDirectory(){
        return sharedDirectory;
    }
    public void setSharedDirectory(String sharedDirectory){
        this.sharedDirectory=sharedDirectory;
    }

    public String[] getFileList(){
    	String[] res = new String[0];
    	try{
	        File dir = new File(sharedDirectory);
	        res = dir.list();
	        // filter the list of returned files
	        // to not return any files that start with '.'.
	        FilenameFilter filter = new FilenameFilter() {
	            @Override
	            public boolean accept(File dir, String name) {
	                return !name.startsWith(".");
	            }
	        };
	        res = dir.list(filter);
        }
        catch(Exception e){
        	e.printStackTrace();
        }
        return res;
    }

    public void stopService(){
    	if(fssjf!=null)
    		fssjf.setVisible(false);
        System.out.println("FileSharingService close");
        ServiceManager.getInstance(false).removeService("FileSharing");
        open=false;
        try {
            serviceSocket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run(){
        try{
            System.out.println("FileSharingService START");
            System.out.println("FileSharingService START "+serviceSocket.getLocalPort()+" "+protocol);
            while(open){
                try{
                    // receive
                    GenericPacket gp = E2EComm.receive(serviceSocket, 5*1000);
                    System.out.println("FileSharingService new request");
                    new FileSharingHandler(gp).start();
                }
                catch(SocketTimeoutException ste){
                    //System.out.println("FileSharingService SocketTimeoutException");
                }
            }
            serviceSocket.close();
        }
        catch(SocketException se){
            
        }
        catch(Exception e){
            e.printStackTrace();
        }
        FileSharingService.fileSharing = null;
        System.out.println("FileSharingService FINISHED");
    }

    private class FileSharingHandler extends Thread{
        private GenericPacket gp;
        private FileSharingHandler(GenericPacket gp){
            this.gp=gp;
        }
        @Override
        public void run(){
            try{
                if( gp instanceof UnicastPacket){
                    // 1) payload
                    UnicastPacket up = (UnicastPacket)gp;
                    Object payload = E2EComm.deserialize(up.getBytePayload());
                    if(payload instanceof FileSharingRequest){
                        System.out.println("FileSharingService FileSharingRequest");
                        FileSharingRequest request=(FileSharingRequest)payload;
                        String fileName = request.getFileName();
                        long startReceiving=System.currentTimeMillis();
                        if( ! request.isGet() ){
                            // receiving a file
                            System.out.println("FileSharingService: receiving "+fileName+"...");

                            BoundReceiveSocket receiveFileSocket = E2EComm.bindPreReceive(protocol);
                            
                            // sending local port waiting for file
                            String[] newDest=E2EComm.ipReverse(up.getSource());
                            E2EComm.sendUnicast(
                                    newDest,
                                    up.getSourceNodeId(),
                                    request.getClientPort(),
                                    protocol,
                                    false,
                                    GenericPacket.UNUSED_FIELD,
                                    E2EComm.DEFAULT_BUFFERSIZE,
                                    GenericPacket.UNUSED_FIELD, // packetDeliveryTimeout
                                    (short)GenericPacket.UNUSED_FIELD, // packetTimeoutConnect
                                    E2EComm.serialize(receiveFileSocket.getLocalPort())
                            );
                            
                            File f = new File(sharedDirectory+"/"+fileName);
                            FileOutputStream fos = new FileOutputStream(f);
                            E2EComm.receive(receiveFileSocket, 5*1000, fos);
                            long endReceiving=System.currentTimeMillis();
                            fos.close();
                            float receivingTime = (endReceiving-startReceiving) / 1000.0F;
                            System.out.println("FileSharingService: "+fileName+" received in "+receivingTime+"s" );
                        }
                        else{
                            String[] newDest = E2EComm.ipReverse(up.getSource());
                            Object res=null;
                            int sendingBufferSize = bufferSize;
                            if(fileName.equals("list")){
                                System.out.println("FileSharingService list");
                                // 2a) send file list
                                res = getFileList();
                                sendingBufferSize = E2EComm.DEFAULT_BUFFERSIZE;
                                
                                // send unicast with bufferSize
                                E2EComm.sendUnicast(
                                        newDest,
                                        up.getSourceNodeId(),
                                        request.getClientPort(),
                                        protocol,
                                        false,
                                        GenericPacket.UNUSED_FIELD,
                                        sendingBufferSize,
                                        GenericPacket.UNUSED_FIELD, // packetDeliveryTimeout
                                        (short)GenericPacket.UNUSED_FIELD, // packetTimeoutConnect
                                        E2EComm.serialize(res)
                                );
                            }
                            else{
                                // 2b) send a specific file
                                System.out.println("FileSharingService fileName: "+fileName);
                                File f = new File(sharedDirectory+"/"+fileName);
                                FileInputStream fis = new FileInputStream(f);
                                if(bestBufferSize==true){
                                    sendingBufferSize = E2EComm.bestBufferSize(newDest.length, f.length());
                                }
                                
                                // send unicast with bufferSize
                                E2EComm.sendUnicast(
                                        newDest,
                                        up.getSourceNodeId(),
                                        request.getClientPort(),
                                        protocol,
                                        false,
                                        GenericPacket.UNUSED_FIELD,
                                        sendingBufferSize,
                                        GenericPacket.UNUSED_FIELD,
                                        fis
                                );
                                fis.close();
                            }
                        }
                    }
                    else{
                        // received payload is not FileSharingRequest: do nothing...
                        System.out.println("FileSharingService wrong payload: "+payload);
                    }
                }
                else{
                    // received packet is not UnicastPacket: do nothing...
                    System.out.println("FileSharingService wrong packet: "+gp.getClass().getName());
                }
                
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
