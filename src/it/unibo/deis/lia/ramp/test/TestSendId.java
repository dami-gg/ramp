package it.unibo.deis.lia.ramp.test;

import it.unibo.deis.lia.ramp.RampEntryPoint;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.e2e.GenericPacket;
import it.unibo.deis.lia.ramp.core.internode.ContinuityManager;

public class TestSendId {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		RampEntryPoint.getInstance(true, null);
		ContinuityManager.getInstance(true);
		Thread.sleep(3000);
		
		System.out.println("TestSendId");
		String[] dest = {"137.204.57.183"};
		E2EComm.sendUnicast(
				dest,
				1182992612, // destNodeId
                250, // destPort,
                E2EComm.TCP, //protocol,
                false, //ack,
                GenericPacket.UNUSED_FIELD, // timeoutAck
                GenericPacket.UNUSED_FIELD, //bufferSize,
                GenericPacket.UNUSED_FIELD, // packetDeliveryTimeout
                GenericPacket.UNUSED_FIELD, // packetTimeoutConnect
                new byte[0] // payload
		);

		Thread.sleep(5000);
		System.exit(0);
	}

}
