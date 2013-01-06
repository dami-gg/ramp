/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.e2e;

import java.net.*;

/**
 *
 * @author useruser
 */
public class BoundReceiveSocket {
    final private DatagramSocket ds;
    final private ServerSocket ss;

    public BoundReceiveSocket(DatagramSocket ds) {
        this.ss = null;
        this.ds = ds;
    }

    public BoundReceiveSocket(ServerSocket ss) {
        this.ss = ss;
        this.ds = null;
    }

    public DatagramSocket getDatagramSocket() {
        return ds;
    }

    public ServerSocket getServerSocket() {
        return ss;
    }

    public int getLocalPort(){
        int localPort;
        if(ds!=null){
            localPort = ds.getLocalPort();
        }
        else{
            localPort = ss.getLocalPort();
        }
        return localPort;
    }

    public void close() throws java.io.IOException{
        if(ds!=null){
            ds.close();
        }
        else{
            ss.close();
        }
    }
}
