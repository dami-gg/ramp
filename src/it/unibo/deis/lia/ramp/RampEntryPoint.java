/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package it.unibo.deis.lia.ramp;

import android.content.Context;
import it.unibo.deis.lia.ramp.core.internode.*;
import it.unibo.deis.lia.ramp.core.internode.upnp.UPnPmanager;
import it.unibo.deis.lia.ramp.service.management.ServiceManager;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;

import org.osgi.framework.BundleContext;



/**
 *
 * @author useruser
 */
public class RampEntryPoint {

    // RAMP properties
    public static boolean logging = false;
    public static final String releaseDate = "14 I 2011";
    public static final String os = System.getProperty("os.name").toLowerCase();
    public static final boolean protobuf = true;

    private Dispatcher dispatcher;
    private Heartbeater heartbeater;
    private Resolver resolver;
    private ServiceManager serviceManager;
    private TrafficAnalyser trafficListener;
    private TrafficShaper dispatchListener;

    private static Context androidContext=null;
    private static BundleContext osgiContext=null;
    
    

	private static RampEntryPoint ramp = null;
    public static boolean isActive(){
        return RampEntryPoint.ramp != null;
    }
    synchronized static public RampEntryPoint getInstance(boolean forceStart, Object context){
    	if(forceStart && RampEntryPoint.ramp==null){
        	if (context instanceof Context) {// sono su android
				RampEntryPoint.androidContext = (Context) context;
				RampEntryPoint.ramp = new RampEntryPoint();
			}

			if (context instanceof BundleContext) {// sono su osgi
				RampEntryPoint.osgiContext = (BundleContext) context;
				RampEntryPoint.ramp = new RampEntryPoint();
			}
			if (context==null)
				RampEntryPoint.ramp = new RampEntryPoint();
        }
        return RampEntryPoint.ramp;
    }
    private RampEntryPoint(){
        // Dispatcher, Heartbeater, Resolver, ServiceManager
    	this.dispatcher = Dispatcher.getInstance(true);
        this.heartbeater = Heartbeater.getInstance(true);
        this.resolver = Resolver.getInstance(true);
        this.serviceManager = ServiceManager.getInstance(true);
        this.trafficListener = TrafficAnalyser.getInstance(true);
        this.dispatchListener = TrafficShaper.getInstance(true);
        
    }
    synchronized public void stopRamp(){
        System.out.println("RampEntryPoint.stopRamp");
        if(RampEntryPoint.ramp != null){
        	
            ContinuityManager.deactivate();
            BufferSizeManager.deactivate();
            Layer3RoutingManager.deactivate();
            UPnPmanager.deactivate();
        	
            serviceManager.stopServiceManager();
            serviceManager = null;
            
            dispatcher.stopDispatcher();
            dispatcher = null;
            heartbeater.stopHeartbeater();
            heartbeater = null;
            resolver.stopResolver();
            resolver = null;
            TrafficAnalyser.deactivate();
            trafficListener = null;
            TrafficShaper.deactivate();
            dispatchListener = null;
            RampEntryPoint.ramp = null;
        }
    }

    public static Context getAndroidContext(){
        return RampEntryPoint.androidContext;
    }
    
    public static BundleContext getOsgiContext() {
		return osgiContext;
	}
    
    public void forceNeighborsUpdate(){
        Thread t = new Thread(
            new Runnable(){
                @Override
                public void run() {
                    try {
                        Heartbeater.getInstance(false).sendHeartbeat(true);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        );
        t.start();
    }
    public Vector<InetAddress> getCurrentNeighbors(){
        Vector<InetAddress> res = null;
        try {
            res = Heartbeater.getInstance(false).getNeighbors();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public ArrayList<String> getClients(){
    	ArrayList<String> res = new ArrayList<String>();
		if (osgiContext != null){
			org.osgi.framework.Bundle[] bundles = osgiContext.getBundles();
			for (int i = 0; i < bundles.length; i++){
				if(bundles[i].getSymbolicName().contains("it.unibo.deis.lia.ramp.osgi.service.application")&&bundles[i].getSymbolicName().endsWith("Client")){
				System.out.println("" + bundles[i].getSymbolicName());
				res.add(bundles[i].getSymbolicName().substring(48));
				}
			}
		} else {
		try {
			Class<?>[] classes = RampEntryPoint
					.getClasses("it.unibo.deis.lia.ramp.service.application");
			
			for (int i = 0; i < classes.length; i++) {
				String name = classes[i].getSimpleName();
				if (name.endsWith("Client")) {
					res.add(name);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}}
		return res;
    }
    public ArrayList<String> getServices(){
    	ArrayList<String> res = new ArrayList<String>();
		if (osgiContext != null) {
		
			org.osgi.framework.Bundle[] bundles = osgiContext.getBundles();
			for (int i = 0; i < bundles.length; i++){
				if(bundles[i].getSymbolicName().contains("it.unibo.deis.lia.ramp.osgi.service.application")&&bundles[i].getSymbolicName().endsWith("Service")){
				System.out.println("" + bundles[i].getSymbolicName());
				res.add(bundles[i].getSymbolicName().substring(48));
				}
			}
		} else {

			try {

				Class<?>[] classes = RampEntryPoint
						.getClasses("it.unibo.deis.lia.ramp.service.application");
				
				for (int i = 0; i < classes.length; i++) {
					
					String name = classes[i].getSimpleName();
					if (name.endsWith("Service")) {
						res.add(name);

					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return res;
    }

    public void startClient(String className){
    	if (osgiContext != null) {
			try{
				org.osgi.framework.Bundle[] bundles = osgiContext.getBundles();
				int i=0;
				while(!bundles[i].getSymbolicName().contains(className))
					i++;
				bundles[i].start();
			}
			catch (Exception e) {
				System.out.println("Impossibile avviare il bundle "+e.getMessage());
			}
		}else{
			try {
				Class<?> c = Class
						.forName("it.unibo.deis.lia.ramp.service.application."
								+ className);
				Method m = c.getMethod("getInstance");
				m.invoke(null, new Object[] {});
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
    }
    public void startService(String className){
        startClient(className);
    }
    
    public int getNodeId(){
        return Dispatcher.getLocalId();
    }
    public void setNodeId(String newNodeId){
        Dispatcher.setLocalNodeId(newNodeId);
    }
    public String getNodeIdString(){
        return Dispatcher.getLocalIdString();
    }
    private BufferSizeManager bufferSizeManager = null;
    public void startBufferSizeManager(){
        bufferSizeManager = BufferSizeManager.getInstance();
    }
    public void stopBufferSizeManager(){
        BufferSizeManager.deactivate();
        bufferSizeManager = null;
    }
    public int getBufferSize() throws Exception{
        if(bufferSizeManager == null){
            throw new Exception("BufferSizeManager not yet active");
        }
        return bufferSizeManager.getLocalBufferSize();
    }
    public void setBufferSize(int localBufferSize) throws Exception{
        if(bufferSizeManager == null){
            throw new Exception("BufferSizeManager not yet active");
        }
        bufferSizeManager.setLocalBufferSize(localBufferSize);
    }

    /*public boolean isActiveResolver(){
        // return Resolver.isActive();
        return true;
    }
    public void startResolver(){
        resolver = Resolver.getInstance();
    }
    public void stopResolver(){
        // Resolver.dactivate()
        resolver = null;
    }*/


    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private static Class<?>[] getClasses(String packageName)
            throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = "./"+packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String fileName = resource.getFile();
            fileName = fileName.replaceAll("%20", " ");
            dirs.add(new File(fileName));
        }
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

}
