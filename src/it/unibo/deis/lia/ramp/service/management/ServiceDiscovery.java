/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp.service.management;

import it.unibo.deis.lia.ramp.core.e2e.*;

import java.util.*;
import java.net.*;

/**
 *
 * @author useruser
 */
public class ServiceDiscovery extends Thread{

    final private static int DEFAULT_FIND_SERVICE_TIMEOUT = 5000;

    // look for a given service via broadcast
    public static Vector<ServiceResponse> findService(int TTL, String serviceName) throws Exception{
        return ServiceDiscovery.findServices(TTL, serviceName, ServiceDiscovery.DEFAULT_FIND_SERVICE_TIMEOUT, 1, null);
    }
    public static Vector<ServiceResponse> findServices(int TTL, String serviceName, int timeout, int serviceAmount) throws Exception{
        return ServiceDiscovery.findServices(TTL, serviceName, timeout, serviceAmount, null);
    }
    public static Vector<ServiceResponse> findServices(int TTL, String serviceName, int timeout, int serviceAmount, String qos) throws Exception{
        // serviceAmount==0 ==> wait until timeout!!!
        //System.out.println("Discovery.findService START");
        if(timeout<=0){
            throw new Exception("ServiceDiscovery: timeout must be greater than 0: "+timeout);
        }
        if(serviceAmount<0){
            throw new Exception("ServiceDiscovery: serviceAmount must be equal to or greater than 0: "+serviceAmount);
        }

        Vector<ServiceResponse> res=new Vector<ServiceResponse>();

        // check on the local node
       
        ServiceDescriptor sd = ServiceManager.getInstance(false).getService(serviceName);
        //System.out.println("ServiceManagerHandler: sd "+sd);
        if(sd!=null){
            // requested service available on the local node
            ServiceResponse localService = new ServiceResponse(serviceName, sd);
            String[] source = new String[1];
            source[0] = InetAddress.getLocalHost().getHostAddress().replaceAll("/", "");
            localService.setServerDest(source);
            res.add(localService);
        }

        BoundReceiveSocket serviceClientSocket = E2EComm.bindPreReceive(ServiceManager.SERVICEMANAGER_PROTOCOL);
        ServiceRequest servReq = new ServiceRequest(serviceName, serviceClientSocket.getLocalPort(), qos);

        //System.out.println("Discovery.findService pre sendBroadcast");
        E2EComm.sendBroadcast(
        		TTL, 
        		ServiceManager.SERVICEMANAGER_PORT, 
        		ServiceManager.SERVICEMANAGER_PROTOCOL, 
        		E2EComm.serialize(servReq)
    		);

        long preReceive=System.currentTimeMillis();
        long spentTime=System.currentTimeMillis()-preReceive;
        try{
            while(spentTime<timeout && (serviceAmount==0 || res.size()<serviceAmount)){
                // serviceAmount==0 ==> wait until timeout!!!
                GenericPacket gp = E2EComm.receive(serviceClientSocket, (int)(timeout-spentTime));
                //System.out.println("ServiceDiscovery POST (timeout-spentTime) = "+(timeout-spentTime));
                if(gp instanceof UnicastPacket){
                    UnicastPacket up = (UnicastPacket)gp;
                    Object o = E2EComm.deserialize(up.getBytePayload());
                    if(o instanceof ServiceResponse){
                        ServiceResponse servResp=(ServiceResponse)o;
                        servResp.setServerDest(E2EComm.ipReverse(up.getSource()));

                        servResp.setSourceNodeId(up.getSourceNodeId());

                        res.add(servResp);
                    }
                    else{
                        System.out.println("ServiceDiscovery: required ServiceResponse, received "+o.getClass().getName());
                    }
                }
                else{
                    System.out.println("ServiceDiscovery: required UnicastPacket, received "+gp.getClass().getName());
                }
                spentTime=System.currentTimeMillis()-preReceive;
            }
        }
        catch(SocketTimeoutException ste){
            // do nothing...
            //System.out.println("ServiceDiscovery SocketTimeoutException");
        }
        serviceClientSocket.close();

        return res;
    }


    // look for a given service via unicast
    public static ServiceResponse findService(String[] addresses, String serviceName) throws Exception{
        return ServiceDiscovery.findService(addresses, serviceName, ServiceDiscovery.DEFAULT_FIND_SERVICE_TIMEOUT, null);
    }
    public static ServiceResponse findService(String[] addresses, String serviceName, int timeout) throws Exception{
        return ServiceDiscovery.findService(addresses, serviceName, timeout, null);
    }
    public static ServiceResponse findService(String[] addresses, String serviceName, int timeout, String qos) throws Exception{
        //System.out.println("Discovery.findService START");
        if(timeout<=0){
            throw new Exception("ServiceDiscovery.serviceName: timeout must be greater than 0: "+timeout);
        }

        BoundReceiveSocket serviceClientSocket = E2EComm.bindPreReceive(ServiceManager.SERVICEMANAGER_PROTOCOL);
        ServiceRequest servReq = new ServiceRequest(
                serviceName,
                serviceClientSocket.getLocalPort(),
                qos
        );

        E2EComm.sendUnicast(
                addresses,
                ServiceManager.SERVICEMANAGER_PORT,
                ServiceManager.SERVICEMANAGER_PROTOCOL,
                E2EComm.serialize(servReq)
        );

        ServiceResponse res = null;
        GenericPacket gp = E2EComm.receive(serviceClientSocket,timeout);
        if(gp instanceof UnicastPacket){
            UnicastPacket up=(UnicastPacket)gp;
            Object o = E2EComm.deserialize(up.getBytePayload());
            if(o instanceof ServiceResponse){
                ServiceResponse servResp=(ServiceResponse)o;
                servResp.setServerDest(E2EComm.ipReverse(up.getSource()));
                res = servResp;
            }
            else{
                System.out.println("ServiceDiscovery.serviceName: required ServiceResponse, received "+o.getClass().getName());
            }
        }
        else{
            System.out.println("ServiceDiscovery.serviceName: required UnicastPacket, received "+gp.getClass().getName());
        }

        return res;
    }

    // look for every available service via unicast
    public static Vector<ServiceResponse> findService(String[] dest) throws Exception{
        return ServiceDiscovery.findService(dest, ServiceDiscovery.DEFAULT_FIND_SERVICE_TIMEOUT, null);
    }
    public static Vector<ServiceResponse> findService(String[] dest, int timeout) throws Exception{
        return ServiceDiscovery.findService(dest, timeout, null);
    }
    public static Vector<ServiceResponse> findService(String[] dest, int timeout, String qos) throws Exception{
        // TODO check: retrieve every service of a given node
        //System.out.println("Discovery.findService START");
        if(timeout<=0){
            throw new Exception("ServiceDiscovery.serviceName: timeout must be greater than 0: "+timeout);
        }

        BoundReceiveSocket serviceClientSocket = E2EComm.bindPreReceive(ServiceManager.SERVICEMANAGER_PROTOCOL);
        ServiceRequest servReq = new ServiceRequest(
                null,
                serviceClientSocket.getLocalPort(),
                qos
        );

        E2EComm.sendUnicast(
                dest,
                ServiceManager.SERVICEMANAGER_PORT,
                ServiceManager.SERVICEMANAGER_PROTOCOL,
                E2EComm.serialize(servReq)
        );

        Vector<ServiceResponse> res = null;
        GenericPacket gp = E2EComm.receive(serviceClientSocket,timeout);
        if(gp instanceof UnicastPacket){
            UnicastPacket up = (UnicastPacket)gp;
            Object o = E2EComm.deserialize(up.getBytePayload());
            if(o instanceof Vector<?>){
            	
            	@SuppressWarnings("unchecked")
                Vector<ServiceResponse> servRespVector = (Vector<ServiceResponse>)o;
            	
                if(servRespVector.size()>0){
                    if(servRespVector.elementAt(0) instanceof ServiceResponse){
                        for(int i=0; i<servRespVector.size(); i++){
                            servRespVector.elementAt(i).setServerDest(E2EComm.ipReverse(up.getSource()));
                        }
                    }
                    else{
                        System.out.println("ServiceDiscovery.serviceName: required Vector<ServiceResponse>, received Vector<"+servRespVector.elementAt(0).getClass().getName()+">");
                    }
                }
            }
            else{
                System.out.println("ServiceDiscovery.serviceName: required Vector<ServiceResponse>, received "+o.getClass().getName());
            }
        }
        else{
            System.out.println("ServiceDiscovery.serviceName: required UnicastPacket, received "+gp.getClass().getName());
        }

        return res;
    }
}
