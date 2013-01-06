/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.service.application;

import it.unibo.deis.lia.ramp.core.e2e.*;

import java.io.*;
import java.net.*;

/**
 *
 * @author useruser
 */
public class InternetUtil extends Thread{

    // shared methods by InternetService and InternetClient
    protected static String readLine(InputStream is) throws Exception{
        String res="";

        int temp=is.read();
        while( temp!=0x0D ){
            res+=(char)temp;
            temp=is.read();
        }
        is.read(); // (char)0x0A
        return res;
    }

    protected static byte[] performInternetConnection(InternetRequest internetRequest) throws Exception{// connecting to the renote server
        String serverAddress=internetRequest.getServerAddress();
        int serverPort=internetRequest.getServerPort();
        int serverProtocol=internetRequest.getLayer4Protocol();
        //System.out.println("\tInternetUtil.performInternetConnection to "+serverAddress+":"+serverPort);

        byte[] res = null;
        if(serverProtocol == InternetRequest.UDP){
            DatagramSocket destS=new DatagramSocket();
            DatagramPacket destDp=new DatagramPacket(
                    internetRequest.internetPayload,
                    internetRequest.internetPayload.length,
                    InetAddress.getByName(serverAddress),
                    serverPort
            );

            destS.send(destDp);

            byte[] buffer = new byte[GenericPacket.MAX_UDP_PACKET];
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
            destS.receive(dp);

            res=dp.getData();
        }
        else if (serverProtocol == InternetRequest.TCP){
            Socket s = null;
            try{
                s = new Socket(
                    serverAddress,
                    serverPort
                );
            }
            catch(UnknownHostException uhe){
                res="InternetUtil.performInternetConnection: unknown host".getBytes();
            }
            if(res==null){
                OutputStream os = s.getOutputStream();
                InputStream is=s.getInputStream();

                // send data to the remote server
                byte[] internetPayload = internetRequest.getInternetPayload();
                os.write(internetPayload);
                os.flush();
                //System.out.println("InternetUtil internetPayload sent to remote server");

                // receive HEADERS from remote server
                String line = readLine(is); // first line
                String text="";
                int contentLength=-1;
                boolean chunked=false;
                boolean connectionClose=false;
                //System.out.println("InternetUtil line "+line);
                while(line!=null && !line.equals("")){
                    text+=line+(char)0x0D+(char)0x0A;
                    //System.out.println("\tInternetUtil.performInternetConnection line "+line);
                    if(line.contains("Content-Length")){
                        String length=line.split(" ")[1];
                        //System.out.println("InternetUtil length "+length);
                        contentLength=Integer.parseInt(length);
                    }
                    else if(line.contains("Transfer-Encoding: chunked")){
                        chunked=true;
                    }
                    else if(line.contains("Connection: close")){
                        connectionClose=true;
                    }
                    line = readLine(is);
                    //System.out.println("InternetUtil line "+line);
                }
                if(!connectionClose){
                    text+="Connection: close"+(char)0x0D+(char)0x0A;
                }
                text+=""+(char)0x0D+(char)0x0A;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if(chunked){
                    line = readLine(is);
                    //System.out.println("\tInternetUtil.performInternetConnection chunked line "+line);
                    int lineLength = Integer.decode("0x"+line);
                    //System.out.println("\tInternetUtil.performInternetConnection chunked lineLength "+lineLength);
                    for(int i=0; i<line.length(); i++){
                        baos.write(line.charAt(i));
                    }
                    baos.write(0x0D); // (char)0x0D
                    baos.write(0x0A); // (char)0x0A
                    while(!line.equals("0")){
                        for(int i=0; i<lineLength; i++){
                            int temp=is.read();
                            //System.out.print((char)temp);
                            baos.write(temp);
                        }
                        //System.out.println();
                        //System.out.println("InternetUtil chunked buf "+new String(buf));
                        baos.write(is.read()); // (char)0x0D
                        baos.write(is.read()); // (char)0x0A

                        line = readLine(is);
                        //System.out.println("\tInternetUtil.performInternetConnection chunked line "+line);
                        lineLength = Integer.decode("0x"+line);
                        //System.out.println("\tInternetUtil.performInternetConnection chunked lineLength "+lineLength);
                        for(int i=0; i<line.length(); i++){
                            baos.write(line.charAt(i));
                        }
                        baos.write(0x0D); // (char)0x0D
                        baos.write(0x0A); // (char)0x0A
                    }
                    baos.write(0x0D); // (char)0x0D
                    baos.write(0x0A); // (char)0x0A
                }

                byte[] chunkedArray = baos.toByteArray();
                if(contentLength==-1 && chunkedArray.length==0){
                    res=text.getBytes();
                }
                else if(chunkedArray.length!=0){
                    res=new byte[text.length()+chunkedArray.length];
                    System.arraycopy(text.getBytes(), 0, res, 0, text.length());
                    System.arraycopy(chunkedArray, 0, res, text.length(), chunkedArray.length);
                }
                else{
                    // receiving DATA from remote server
                    res=new byte[text.length()+contentLength];
                    System.arraycopy(text.getBytes(), 0, res, 0, text.length());

                    int temp=0;
                    for(int i=text.length(); temp!=-1 && i<res.length; i++){
                        temp=is.read();
                        res[i]=(byte)temp;
                        //System.out.print(""+(char)temp);
                    }
                }
                s.close();
            }
        }
        else{
            System.out.println("\tInternetUtil.performInternetConnection: unsupported layer-4 protocol: "+internetRequest.getLayer4Protocol());
            res="InternetUtil.performInternetConnection: unknown protocol ".getBytes();
        }
        return res;
    }
    
}
