/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.internode;

import it.unibo.deis.lia.ramp.core.e2e.*;

/**
 *
 * @author useruser
 */
public interface PacketForwardingListener {

    void receivedUDPUnicastPacket(UnicastPacket up);
    void receivedUDPBroadcastPacket(BroadcastPacket bp);

    void receivedTCPUnicastPacket(UnicastPacket up);
    void receivedTCPBroadcastPacket(BroadcastPacket bp);
    
    void receivedTCPUnicastHeader(UnicastHeader uh);
    void receivedTCPPartialPayload(UnicastHeader uh, byte[] payload, int off, int len, boolean lastChunk);

    void sendingTCPUnicastPacketException(UnicastPacket up, Exception e);
    void sendingTCPUnicastHeaderException(UnicastHeader uh, Exception e);
}
