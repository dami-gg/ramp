/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.service.application;

import it.unibo.deis.lia.ramp.RampEntryPoint;
import it.unibo.deis.lia.ramp.service.management.*;
import it.unibo.deis.lia.ramp.core.e2e.*;

import java.util.*;
import java.io.*;

/**
 *
 * @author useruser
 */
public class FileSharingClient{

    private String sharedDirectory="./temp/fsClient";

    private static FileSharingClient fileSharingClient = null;
    private static FileSharingClientJFrame fscj = null;
    
	private FileSharingClient(boolean gui){
		if( gui && RampEntryPoint.getAndroidContext()==null ){
        	fscj = new FileSharingClientJFrame(this);
        }
        else{
            if( RampEntryPoint.getAndroidContext() != null )
            sharedDirectory =  android.os.Environment.getExternalStorageDirectory()+"/ramp";
        }
    }
    public static synchronized FileSharingClient getInstance(){
        if(fileSharingClient==null){
            fileSharingClient=new FileSharingClient(true);
        }
        if(fscj != null){
            fscj.setVisible(true);
        }
        return fileSharingClient;
    }
    public static synchronized FileSharingClient getInstanceNoShow(){
        if(fileSharingClient==null){
            fileSharingClient=new FileSharingClient(false);
        }
        return fileSharingClient;
    }

    public void stopClient(){
    	if(fscj!=null)
    		fscj.setVisible(false);
        fileSharingClient=null;
    }

    public Vector<ServiceResponse> findFileSharingService(int ttl, int timeout, int serviceAmount) throws Exception{
        long pre = System.currentTimeMillis();
        Vector<ServiceResponse> services = ServiceDiscovery.findServices(
                ttl,
                "FileSharing",
                timeout,
                serviceAmount,
                null
        );
        long post = System.currentTimeMillis();
        float elapsed = (post-pre)/(float)1000;
        System.out.println("FileSharingClient findFileSharingService elapsed="+elapsed+"    services="+services);
        return services;
    }

    public String[] getRemoteFileList(ServiceResponse service) throws Exception{
        BoundReceiveSocket clientSocket = E2EComm.bindPreReceive(service.getProtocol());
        FileSharingRequest fsr=new FileSharingRequest(true, "list", clientSocket.getLocalPort());
        E2EComm.sendUnicast(
                service.getServerDest(),
                service.getSourceNodeId(),
                service.getServerPort(),
                service.getProtocol(),
                false,
                GenericPacket.UNUSED_FIELD,
                E2EComm.DEFAULT_BUFFERSIZE,
                GenericPacket.UNUSED_FIELD, // packetDeliveryTimeout
                (short)GenericPacket.UNUSED_FIELD, // packetTimeoutConnect
                E2EComm.serialize(fsr)
        );
        System.out.println("FileSharingClient fileList service.getServerDest()[0]="+service.getServerDest()[0]+" service.getServerPort="+service.getServerPort()+" service.getProtocol="+service.getProtocol());
        
        // receive the file list
        UnicastPacket up = (UnicastPacket)E2EComm.receive(
                clientSocket,
                4*1000 // timeout
        );
        clientSocket.close();
        String[] availableFiles= (String[])E2EComm.deserialize(up.getBytePayload());
        return availableFiles;
    }

    public void getRemoteFile(ServiceResponse service, String fileName) throws Exception{
        BoundReceiveSocket socketClient = E2EComm.bindPreReceive(service.getProtocol());
        FileSharingRequest fsr = new FileSharingRequest(true, fileName, socketClient.getLocalPort());
        System.out.println("FileSharingClient requiring "+fileName);
        E2EComm.sendUnicast(
                service.getServerDest(),
                service.getSourceNodeId(),
                service.getServerPort(),
                service.getProtocol(),false,
                GenericPacket.UNUSED_FIELD,
                E2EComm.DEFAULT_BUFFERSIZE,
                GenericPacket.UNUSED_FIELD, // packetDeliveryTimeout
                (short)GenericPacket.UNUSED_FIELD, // packetTimeoutConnect
                E2EComm.serialize(fsr)
        );
        
        // receive the requested file and write it in the local filesystem
        FileOutputStream fos = new FileOutputStream(sharedDirectory+"/"+fileName);
        long pre = System.currentTimeMillis();
        UnicastHeader uh = (UnicastHeader)E2EComm.receive(
                socketClient,
                4*1000, // timeout
                fos
        );
        socketClient.close();
        
        long post = System.currentTimeMillis();
        float elapsed = (post-pre)/(float)1000;
        System.out.println("FileSharingClient getFile elapsed="+elapsed+"    bufferSize="+uh.getBufferSize());
        //byte[] fileByte= (byte[])up.getObjectPayload();
        //FileOutputStream fos=new FileOutputStream(sharedDirectory+"/"+file);
        //fos.write(fileByte);
        fos.close();
        System.out.println("FileSharingClient received "+fileName);
    }

    public void sendLocalFile(ServiceResponse service, String fileName) throws Exception{
        BoundReceiveSocket socketClient = E2EComm.bindPreReceive(service.getProtocol());
        FileSharingRequest fsr=new FileSharingRequest(false, fileName, socketClient.getLocalPort());
        System.out.println("FileSharingClient sending "+fileName);
        E2EComm.sendUnicast(
                service.getServerDest(),
                service.getServerPort(),
                service.getProtocol(),
                E2EComm.serialize(fsr)
        );
        
    	UnicastPacket upServerPort = (UnicastPacket) E2EComm.receive(socketClient, 5000);
    	
        File f = new File(sharedDirectory+"/"+fileName);
        FileInputStream fis = new FileInputStream(f);
        E2EComm.sendUnicast(
                service.getServerDest(),
                (Integer)(E2EComm.deserialize(upServerPort.getBytePayload())),
                service.getProtocol(),
                fis
        );
        fis.close();
        System.out.println("FileSharingClient: "+fileName+" sent");
    }

    public String[] getLocalFileList(){
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
    
}
