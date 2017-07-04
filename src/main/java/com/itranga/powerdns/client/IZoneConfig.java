package com.itranga.powerdns.client;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface IZoneConfig {
	/**
	 * The type decides how the domain will be replicated across multiple DNS servers.
	 * @author Denis Kalinin
	 *
	 */
	public static enum Kind {
		/**
		 * Native - PowerDNS will not perform any replication. Use this if you only have one PowerDNS server or
		 * you handle replication via your backend (MySQL).
		 */
		Native,
		/**
		 * Master - This PowerDNS server will serve as the master and will send zone transfers (AXFRs) to other
		 * servers configured as slaves.
		 */
		Master,
		/**
		 * Slave - This PowerDNS server will serve as the slave and will request and receive zone transfers (AXFRs)
		 * from other servers configured as masters.
		 */
		Slave
	}
	/**
	 * The SOA-EDIT-API setting defines when and how the SOA serial number will be updated after a change is made to the domain.
	 * @author Denis Kalinin
	 *
	 */
	public static enum SoaEditApi {
		@JsonEnumDefaultValue
		OFF,
//		DEFAULT,
		/**
		 * INCEPTION-INCREMENT - Uses YYYYMMDDSS format for SOA serial numbers. If the SOA serial from the backend is 
		 * within two days after inception, it gets incremented by two (the backend should keep SS below 98).
		 */
		@JsonProperty("INCEPTION-INCREMENT")
		INCEPTION_INCREMENT,
		/**
		 * INCEPTION - Sets the SOA serial to the last inception time in YYYYMMDD01 format.
		 * Uses localtime to find the day for inception time. Not recomended.
		 */
		INCEPTION,
		/**
		 * INCREMENT-WEEK - Sets the SOA serial to the number of weeks since the epoch, which is the last inception
		 * time in weeks. Not recomended.
		 */
		@JsonProperty("INCREMENT-WEEK")
		INCREMENT_WEEK,
		/**
		 * INCREMENT-WEEKS - Increments the serial with the number of weeks since the UNIX epoch. This should work in
		 * every setup; but the result won't look like YYYYMMDDSS anymore.
		 */
		@JsonProperty("INCREMENT-WEEKS")
		INCREMENT_WEEKS,
		/**
		 * EPOCH - Sets the SOA serial to the number of seconds since the epoch.
		 */
		EPOCH,
		/**
		 * INCEPTION-EPOCH - Sets the new SOA serial number to the maximum of the old SOA serial number, and age in seconds of the last inception.
		 */
		@JsonProperty("INCEPTION-EPOCH")
		INCEPTION_EPOCH
		/*
		private String value;		
		SoaEditApi(){}
		SoaEditApi(String value){
			this.value = value;
		}
		@Override
	    public String toString() {
	        if(value!=null) return value;
	        else return name();	        
	    }
	    */
	}

}
