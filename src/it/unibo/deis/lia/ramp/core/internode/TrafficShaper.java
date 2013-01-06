/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.internode;

import java.util.*;
import java.net.*;
import java.lang.Object.*;
import it.unibo.deis.lia.ramp.core.e2e.*;

/**
 *
 * @author Dami
 */
public class TrafficShaper implements PacketForwardingListener {

    private static TrafficShaper dispatchListener = null;

    private TrafficTrace trace = TrafficTrace.getInstance();

    public static synchronized TrafficShaper getInstance(boolean forceStart){
        
        if(forceStart){

            if (dispatchListener == null){
            
                dispatchListener = new TrafficShaper();
                Dispatcher.getInstance(false).addPacketForwardingListener(dispatchListener);
                System.out.println("DispatchListener ENABLED");
            }
        }
        
        return dispatchListener;
    }
    public static void deactivate(){
        
        if (dispatchListener != null){
            
            System.out.println("DispatchListener DISABLED");
            Dispatcher.getInstance(false).removePacketForwardingListener(dispatchListener);
            dispatchListener=null;
        }
    }

    @Override
    public void receivedTCPUnicastHeader(UnicastHeader uh) {

    }

    @Override
    public void receivedUDPUnicastPacket(UnicastPacket up) {

        manageUnicastPacket (up);
    }

    @Override
    public void receivedUDPBroadcastPacket(BroadcastPacket bp) {

        manageBroadcastPacket (bp);
    }

    @Override
    public void receivedTCPUnicastPacket(UnicastPacket up) {

        manageUnicastPacket (up);
    }

    @Override
    public void receivedTCPBroadcastPacket(BroadcastPacket bp) {

        manageBroadcastPacket (bp);
    }

    @Override
    public void receivedTCPPartialPayload(UnicastHeader uh, byte[] payload, int off, int len, boolean lastChunk) {

    }

    @Override
    public void sendingTCPUnicastPacketException(UnicastPacket up, Exception e) {

    }

    @Override
    public void sendingTCPUnicastHeaderException(UnicastHeader uh, Exception e) {
        
    }

    public void manageUnicastPacket (UnicastPacket up) {

        try {
            
            int source = up.getSourceNodeId();

            String behaviour = trace.getBehaviour (source);

            if (behaviour.equals("Collaborative") || behaviour.equals("NoInformationYet")) return; // I don't need to delay the packet

            double rate = trace.getRate (source);

            long delay = (long) Math.abs(rate) * (long) 2;

            Thread.sleep (delay); // ms
        }

        catch(Exception e) {

            e.printStackTrace();
        }
    }

    public void manageBroadcastPacket (BroadcastPacket bp) {

        try {

            int source = bp.getSourceNodeId();

            String behaviour = trace.getBehaviour (source);

            if (behaviour.equals("Collaborative") || behaviour.equals("NoInformationYet")) return; // I don't need to delay the packet

            double rate = trace.getRate (source);

            long delay = (long) Math.abs(rate) * (long) 2;

            Thread.sleep (delay); // ms
        }

        catch(Exception e) {

            e.printStackTrace();
        }
    }
}
