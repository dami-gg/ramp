/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.e2e;

//import java.io.*;

/**
 *
 * @author useruser
 */

public abstract class GenericPacket{
	
	public static final transient byte UNUSED_FIELD = -1;
    public static final transient int MAX_UDP_PAYLOAD = 60*1024;
    public static final transient int MAX_UDP_PACKET = 65535;
    
    public abstract byte[] toProtosByteArray();
    public abstract byte getPacketId();
    public abstract void writeToProtos(java.io.OutputStream os) throws java.io.IOException;
    
    static public int s2i(String ipS){
    	int temp;
    	int ipI = 0;
    	String[] tokens = ipS.split("[.]");
    	
    	temp = Integer.parseInt(tokens[0]); 
    	ipI += temp*256*256*256;
    	//ipI = ipI << 8;
    	
    	temp = Integer.parseInt(tokens[1]); 
    	ipI += temp*256*256;
    	//ipI = ipI << 8;
    	
    	temp = Integer.parseInt(tokens[2]); 
    	ipI += temp*256;
    	//ipI = ipI << 8;
    	
    	temp = Integer.parseInt(tokens[3]); 
    	ipI += temp;
    	
    	return ipI;
    }
    
    static public String i2s(int ipI){
    	String ipS = "";
    	ipS += ( ( ipI >> 24 ) & 0xFF );
    	ipS += ".";
    	ipS += ( ( ipI >> 16 ) & 0xFF );
    	ipS += ".";
    	ipS += ( ( ipI >> 8 ) & 0xFF );
    	ipS += ".";
    	ipS += ( ( ipI >> 0 ) & 0xFF );
    	return ipS;
    }
    
}
