/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.core.internode;

import it.unibo.deis.lia.ramp.core.e2e.*;
import it.unibo.deis.lia.ramp.service.management.*;
import it.unibo.deis.lia.ramp.service.application.*;

import java.util.*;

/**
 *
 * @author Dami
 */
public class TrafficAnalyser implements PacketForwardingListener {

    public static final int HOURS_TO_SAVE = 0;
    public static final int MINUTES_TO_SAVE = 19;
    public static final int SECONDS_TO_SAVE = 59;

    private TrafficTrace trace = TrafficTrace.getInstance();

    private ArrayList <Communication> pending = new ArrayList();

    private static TrafficAnalyser trafficAnalyser = null;

    public static synchronized TrafficAnalyser getInstance (boolean forceStart){

        if(forceStart) {

            if (trafficAnalyser == null) {

                trafficAnalyser = new TrafficAnalyser();
                Dispatcher.getInstance(false).addPacketForwardingListener(trafficAnalyser);
                System.out.println("TrafficAnalyser ENABLED");
            }
        }

        return trafficAnalyser;
    }

    public static void deactivate(){

        if (trafficAnalyser != null) {

            System.out.println("TrafficAnalyser DISABLED");
            Dispatcher.getInstance(false).removePacketForwardingListener(trafficAnalyser);
            trafficAnalyser = null;
        }
    }

    @Override
    public void receivedUDPUnicastPacket(UnicastPacket up) {

        registerUnicastPacket (up);
    }

    @Override
    public void receivedUDPBroadcastPacket(BroadcastPacket bp) {

        registerBroadcastPacket (bp);
    }

    @Override
    public void receivedTCPUnicastPacket(UnicastPacket up) {

        registerUnicastPacket (up);
    }

    @Override
    public void receivedTCPBroadcastPacket(BroadcastPacket bp) {

        registerBroadcastPacket (bp);
    }

    @Override
    public void receivedTCPUnicastHeader(UnicastHeader uh) { }

    @Override
    public void receivedTCPPartialPayload (UnicastHeader uh, byte[] payload, int off, int len, boolean lastChunk) {

        try {

            int source = uh.getSourceNodeId();

            int dest = uh.getDestNodeId();

            int port = uh.getDestPort();
        
            boolean putFile = true;

            String service = "";

            Communication get = new Communication (source, dest, port, false);

            int pos = posCommunication (get, "ShareFile");

            if (pos != -1) {

                service = "FileSharing";

                // Client is getting a file from the server

                putFile = false;
            }

            else {

                Communication put = new Communication (source, dest, port, true);
                
                pos = posCommunication (put, "ShareFile");

                if (pos == -1) {

                    Communication inet = new Communication (dest, source, "getInternet", port);

                    pos = posCommunication (inet, "SendWebPage");

                    if (pos == -1)  return;

                    else  service = "Internet";
                }

                else {

                    service = "FileSharing";

                    // Client is putting a file on the server

                    putFile = true;
                }
            }

            Communication communication = (Communication) pending.get(pos).clone();

            int nParamSize;
            
            if (service.equals("Internet")) nParamSize = 10;
            
            else nParamSize = (putFile) ? 12 : 11;

            double value;
                
            if (communication.howManyParameters() != nParamSize) // First packet sent
                    
                value = (double) len/1024;

            else  value = ((double) len/1024) + communication.getSize(); // Kb

            communication.setSize (value); // Kb

            // If it is the last chunk, update the trace and remove it from pending list

            if (lastChunk) {

                store (communication, pos);
            }

            else {

                pending.add (pos, communication);

                pending.remove (pos+1);
            }
        }

        catch(Exception e) {

            e.printStackTrace();
        }
    }

    @Override
    public void sendingTCPUnicastPacketException(UnicastPacket up, Exception e) {

        sendingTCPUnicastHeaderException(up.getHeader(), e);
    }

    @Override
    public void sendingTCPUnicastHeaderException(UnicastHeader uh, Exception e) { }

    public void registerUnicastPacket (UnicastPacket up) {

        try { 

            int source = up.getSourceNodeId();

            int dest = up.getDestNodeId();

            byte[] bytePayload = up.getBytePayload();

            Object payload = null;
            
            try {

                payload = E2EComm.deserialize(bytePayload);
            }
            
            catch (Exception e) {
            
                if (payload == null) {

                    String service = "";

                    Communication aux = new Communication (dest, source, "getStream", up.getDestPort());

                    int pos = posCommunication (aux, "SendStream");

                    if (pos == -1) {

                        aux = new Communication (dest, source, "getInternet", up.getDestPort());

                        pos = posCommunication (aux, "SendWebPage");

                        if (pos == -1) {

                            Communication get = new Communication (source, dest, up.getDestPort(), false);

                            pos = posCommunication (get, "ShareFile");

                            if (pos != -1) service = "FileSharing";

                            else {

                                Communication put = new Communication (source, dest, up.getDestPort(), true);

                                pos = posCommunication (put, "ShareFile");

                                if (pos == -1) return;

                                else service = "FileSharing";
                            }
                        }

                        else  service = "Internet";
                    }

                    else  service = "Stream";

                    Communication communication = (Communication) pending.get(pos).clone();

                    if (service.equals("Stream")) {

                        // If it is the end of the stream, update the trace and remove it from pending list

                        boolean endStream = (bytePayload.length == 0) ? true : false;

                        if (endStream) store (communication, pos);

                        else {

                            double value;

                            if (communication.howManyParameters() != 10) // First packet sent

                                value = (double) bytePayload.length/1024; // Kb

                            else  value = ((double) bytePayload.length/1024) + communication.getSize(); // Kb

                                communication.setSize(value);

                            pending.add (pos, communication);

                            pending.remove (pos+1);
                        }
                    }

                    else { // Internet request or FileSharing request

                        communication.setSize((double) bytePayload.length/1024); // Kb

                        store (communication, pos);
                    }
                }
                    
                return;     
            }

            if(payload instanceof ServiceResponse) { // All the ServiceResponse packets are unicast packets

                ServiceResponse res = (ServiceResponse) payload;

                String service = res.getServiceName();

                if (service.equals ("FileSharing") || service.equals ("Internet") || service.equals ("Stream")) {

                    Communication aux = new Communication (dest, service);
                    
                    int pos = posCommunication (aux, "ServiceResponse");

                    if (pos == -1) return; // An error occured

                    Communication communication = (Communication) pending.get(pos);

                    communication.setDest(source);
                }

                else return;
            }

            else if (payload instanceof FileSharingRequest) {

                FileSharingRequest fsr = (FileSharingRequest) payload;

                Communication aux = new Communication (source, "FileSharing", dest);
                
                int pos = posCommunication (aux, "ServiceRequest");

                if (pos == -1) return; // An error occured

                Communication communication = (Communication) pending.get(pos).clone();

                if (fsr.isGet()) {

                    if (fsr.getFileName().equals("list")) { // Getting list of files --> Selfish

                        // Coger el tamaño en la respuesta con payload.size();

                        communication.setAction ("getList");

                        communication.setBehaviour ("selfish");
                    }

                    else { // Getting file --> Selfish

                        communication.setAction ("getFile");

                        communication.setBehaviour ("selfish");

                        communication.setFileName (fsr.getFileName());

                        communication.setClientPort (fsr.getClientPort());
                    }
                }

                else { // Putting file --> Collaborative

                    communication.setAction ("putFile");

                    communication.setBehaviour ("collaborative");

                    communication.setFileName (fsr.getFileName());

                    communication.setClientPort (fsr.getClientPort());
                }

                pending.add (pos, communication);
            }

            else if (payload instanceof StreamRequest) {

                StreamRequest sr = (StreamRequest) payload;

                Communication aux = new Communication (source, "Stream", dest);

                int pos = posCommunication (aux, "ServiceRequest");

                if (pos == -1) return; // An error occured

                Communication communication = (Communication) pending.get(pos).clone();

                if (sr.getStreamName().equals("list")) // Getting stream list

                    communication.setAction ("getList");

                else { // Getting stream

                    communication.setAction ("getStream");
                    
                    communication.setClientPort(sr.getClientPort());
                }

                communication.setBehaviour ("selfish"); // All the possible Stream requests are selfish

                pending.add (pos, communication);
            }

            else if (payload instanceof InternetRequest) {

                InternetRequest ir = (InternetRequest) payload;

                Communication aux = new Communication (source, "Internet", dest);

                int pos = posCommunication (aux, "ServiceRequest");

                if (pos == -1) return; // An error occured

                Communication communication = (Communication) pending.get(pos).clone();

                communication.setAction("getInternet");

                communication.setClientPort(ir.getClientPort());

                communication.setBehaviour ("selfish"); // All the possible Internet requests are selfish

                pending.add (pos, communication);
            }

            else if (payload instanceof Integer) {

                int port = up.getDestPort();
                
                Communication aux = new Communication (source, dest, port, false);

                int pos = posCommunication (aux, "ShareFile");

                if (pos != -1) {

                    int portServ = (Integer) payload;

                    Communication communication = (Communication) pending.get(pos).clone();

                    communication.setServerPort (portServ);

                    pending.add (pos, communication);

                    pending.remove (pos+1);
                }

                System.out.println("TrafficAnalyser: required ServiceResponse, received "+ up.getClass().getName());
            }

            else if (payload instanceof String[]) {

                Communication aux = new Communication (dest, source, "getList");

                int pos = posCommunication (aux, "SendList");

                if (pos == -1) return; // An error occured

                Communication communication = (Communication) pending.get(pos).clone();

                String[] str = (String[]) payload;

                double size = (double) str.length;

                communication.setSize(size);

                store (communication, pos);
            }
        }

        catch(Exception e){

            e.printStackTrace();
        }
    }

    public void registerBroadcastPacket (BroadcastPacket bp) {

        try {

            int source = bp.getSourceNodeId();

            Object payload = E2EComm.deserialize(bp.getBytePayload());

            if (payload instanceof ServiceRequest) { // All the ServiceRequest packets are broadcast packets

                ServiceRequest req = (ServiceRequest) payload;

                String service = req.getServiceName();

                if (service.equals ("FileSharing") || service.equals ("Internet") || service.equals ("Stream")) {

                    Calendar cal = Calendar.getInstance();
                    
                    Communication communication = new Communication (cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), source, service);

                    pending.add (communication);
                }
                
                else return;
            }

            else {

                System.out.println("TrafficAnalyser: required ServiceRequest, received "+ bp.getClass().getName());
            }
        }

        catch(Exception e){

            e.printStackTrace();
        }
    }

    private int posCommunication (Communication c, String step) {

        if (step.equals ("ServiceResponse")) {

            if (pending.size() > 0) {

                int source = c.getSource();

                String service = c.getService();

                Communication aux, com = new Communication (source, service);

                for (int i = 0; i < pending.size(); i++) {

                    aux = pending.get(i);

                    if (aux.equals(com) && aux.howManyParameters() == 5)

                        return i;
                }
            }

            return -1;
        }

        else if (step.equals ("ServiceRequest")) {

            if (pending.size() > 0) {

                int source = c.getSource();

                String service = c.getService();

                int dest = c.getDest();

                Communication aux, com = new Communication (source, service, dest);

                for (int i = 0; i < pending.size(); i++) {

                    aux = pending.get(i);

                    if (aux.equals(com) && aux.howManyParameters() == 6)

                        return i;
                }
            }

            return -1;
        }

        else if (step.equals ("ShareFile")) {

            if (pending.size() > 0) {

                int source = c.getSource();

                int dest = c.getDest();

                if (c.getClientPort() != -1) {

                    int port = c.getClientPort();

                    Communication aux, com = new Communication (dest, source, port, "client");

                    for (int i = 0; i < pending.size(); i++) {

                        aux = pending.get(i);

                        if (aux.equals(com) && aux.getService().equals("FileSharing") && (aux.howManyParameters() == 10 || aux.howManyParameters() == 11))

                            return i;
                    }
                }

                else if (c.getServerPort() != -1) {

                    int port = c.getServerPort();

                    Communication aux, com = new Communication (dest, source, port, "server");

                    for (int i = 0; i < pending.size(); i++) {

                        aux = pending.get(i);

                        if (aux.equals(com) && aux.getService().equals("FileSharing") && (aux.howManyParameters() == 11 || aux.howManyParameters() == 12))

                            return i;
                    }
                }
            }

            return -1;
        }

        else if (step.equals("SendList")) {

            if (pending.size() > 0) {

                int source = c.getSource();

                int dest = c.getDest();

                String action = c.getAction();

                Communication aux, com = new Communication (source, dest, action);

                for (int i = 0; i < pending.size(); i++) {

                    aux = pending.get(i);

                    if (aux.equals(com) && aux.howManyParameters() == 8)

                    return i;
                }
            }

            return -1;
        }

        else if (step.equals("SendStream") || step.equals("SendWebPage")) {

            if (pending.size() > 0) {

                int source = c.getSource();

                int dest = c.getDest();

                String action = c.getAction();

                int clientPort = c.getClientPort();

                Communication aux, com = new Communication (source, dest, action, clientPort);

                for (int i = 0; i < pending.size(); i++) {

                    aux = pending.get(i);

                    if (aux.equals(com) && (aux.howManyParameters() == 9 || aux.howManyParameters() == 10) && (aux.getService().equals("Stream") || aux.getService().equals("Internet")))

                        return i;
                }
            }

            return -1;
        }
        
        return -1;
    }

    private String inverse (String behaviour) {

        if (behaviour.equals ("selfish"))

            return "collaborative";

        return "selfish";
    }

    private void refreshData () {

        Calendar cal = Calendar.getInstance();

        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        int s = cal.get(Calendar.SECOND);

        Communication aux;

        int i = 0;

        for (; i < pending.size(); i++) {

            aux = pending.get(i);
            
            if (aux.getHour() >= h - HOURS_TO_SAVE && aux.getMinute() >= m - MINUTES_TO_SAVE && aux.getSecond() >= s - SECONDS_TO_SAVE)

                break;
        }

        for (int j = i - 1; j > 0; j--) {

            pending.remove(j);
        }
    }

    private void store (Communication c, int pos) {

        Calendar cal = Calendar.getInstance();

        // Client

        trace.register (cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), c.getSource(), c.getSize(), c.getBehaviour());

        // Server

        trace.register (cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), c.getDest(), c.getSize(), inverse(c.getBehaviour()));

        pending.remove (pos);

        // Refresh the information for deleting old registers

        refreshData();
    }
}
