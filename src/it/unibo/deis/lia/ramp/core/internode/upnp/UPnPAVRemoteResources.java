package it.unibo.deis.lia.ramp.core.internode.upnp;

import java.io.Serializable;
import java.util.Vector;

public class UPnPAVRemoteResources {

	private static UPnPAVRemoteResources uPnPRemoteAVResources = null;
	private static Vector<UPnPAVRes> avRemoteRes = null;

	public static synchronized UPnPAVRemoteResources getInstance() {
		try {
			if (uPnPRemoteAVResources == null) {
				uPnPRemoteAVResources = new UPnPAVRemoteResources();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return uPnPRemoteAVResources;
	}

	private UPnPAVRemoteResources() {
		avRemoteRes = new Vector<UPnPAVRemoteResources.UPnPAVRes>();
	}

	public void addRes(int masterID, int masterPort, String protocolInfo, int size, String duration, int bitRate, int sampleFrequency, int bitPerSample, int nrAudioChannel, String resolution,
			int colorDepth, String protection, String importUri, String realAdd) {
		boolean newRes = true;
		for (UPnPAVRes tempRes : avRemoteRes) {
			if (tempRes.getRealAdd().equalsIgnoreCase(realAdd))
				newRes = false;
		}

		if (newRes) {
			avRemoteRes.add(new UPnPAVRes(masterID, masterPort, protocolInfo, size, duration, bitRate, sampleFrequency, bitPerSample,nrAudioChannel, resolution,
					 colorDepth, protection, importUri, realAdd));
		}
	}

	public UPnPAVRes getRes(int masterId, String add){
		for (UPnPAVRes tempRes : avRemoteRes) {
			if (tempRes.masterID==masterId&&tempRes.getRealAdd().substring(tempRes.getRealAdd().indexOf('/', 7)).equalsIgnoreCase(add.substring(add.indexOf('/', 7))));
				return tempRes;
		}
		return null;
	}
	
	public UPnPAVRes getRes(String realAdd) {
		
		for (UPnPAVRes tempRes : avRemoteRes) {
			if (tempRes.getRealAdd().equalsIgnoreCase(realAdd))
				return tempRes;
		}
		return null;
	}

	public void removeRes(String realAdd) {
		avRemoteRes.remove(getRes(realAdd));
	}

	public void removeResMasterId(int masterID) {
		for (UPnPAVRes tempRes : avRemoteRes) {
			if (tempRes.getMasterID() == masterID)
				avRemoteRes.remove(tempRes);
		}
	}

	public class UPnPAVRes implements Serializable{

	
		/**
		 * 
		 */
		private static final long serialVersionUID = 4224169470051796514L;
		int masterID;
		int masterPort;
		String protocolInfo;
		int size;
		String duration;
		int bitRate;
		int sampleFrequency;
		int bitPerSample;
		int nrAudioChannel;
		String resolution;
		int colorDepth;
		String protection;
		String importUri;
		String realAdd;

		public UPnPAVRes(int masterID, int masterPort, String protocolInfo, int size, String duration, int bitRate, int sampleFrequency, int bitPerSample, int nrAudioChannel, String resolution,
				int colorDepth, String protection, String importUri, String realAdd) {
			super();
			this.masterID = masterID;
			this.masterPort = masterPort;
			this.protocolInfo = protocolInfo;
			this.size = size;
			this.duration = duration;
			this.bitRate = bitRate;
			this.sampleFrequency = sampleFrequency;
			this.bitPerSample = bitPerSample;
			this.nrAudioChannel = nrAudioChannel;
			this.resolution = resolution;
			this.colorDepth = colorDepth;
			this.protection = protection;
			this.importUri = importUri;
			this.realAdd = realAdd;
		}

		public int getMasterID() {
			return masterID;
		}
		
		

		public int getMasterPort() {
			return masterPort;
		}

		public String getProtocolInfo() {
			return protocolInfo;
		}

		public int getSize() {
			return size;
		}

		public String getDuration() {
			return duration;
		}

		public int getBitRate() {
			return bitRate;
		}

		public int getSampleFrequency() {
			return sampleFrequency;
		}

		public int getBitPerSample() {
			return bitPerSample;
		}

		public int getNrAudioChannel() {
			return nrAudioChannel;
		}

		public String getResolution() {
			return resolution;
		}

		public int getColorDepth() {
			return colorDepth;
		}

		public String getProtection() {
			return protection;
		}

		public String getImportUri() {
			return importUri;
		}

		public String getRealAdd() {
			return realAdd;
		}

	}
}
