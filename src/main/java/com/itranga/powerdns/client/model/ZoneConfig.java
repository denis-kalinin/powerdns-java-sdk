package com.itranga.powerdns.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.itranga.powerdns.client.IZoneConfig;

public class ZoneConfig implements IZoneConfig {
	///not implemented yet
	//private boolean dnssec;
	//private boolean nsec3narrow;
	//private boolean presigned;
	//private String nsec3param;
	//private String active_keys;
	
	////
	
	// {"name":"link.test.", "kind": "Master", "soa_edit_api": "EPOCH", "nameservers":[]}
	
	private String name;
	private Kind kind;
	//maybe empty if NS record is provided with zone. Must be canonical (e.g. with trailing dot)
	private String[] nameservers;
	//soa_edit_api for authoritative DNS servers	
	private SoaEditApi soa_edit_api;
	
	public ZoneConfig(){
		this.kind = Kind.Master;
		this.soa_edit_api = SoaEditApi.OFF;
	}
	/**
	 * Creates {@linkplain Kind#Master Master zone} with {@linkplain SoaEditApi#EPOCH SOA as EPOCH}
	 * @param name zone ID
	 */
	public ZoneConfig(String name){
		this();
		this.name = name;
	}
	

	public String getName() {
		return name;
	}
	public Kind getKind() {
		return kind;
	}
	public String[] getNameservers() {
		return nameservers;
	}
	@JsonProperty(value="soa_edit_api")
	public SoaEditApi getSoaEditApi() {
		return soa_edit_api;
	}
	
	/**
	 * Sets your domain name in the format of name.tld (eg. powerdns-admin.com). 
	 * You can also enter sub-domains to create a sub-root zone (eg. sub.powerdns-admin.com) in case you want to
	 * delegate sub-domain management to specific users.
	 * @param name as <code>name.tld</code>
	 */
	public void setName(String name) {
		this.name = name;
	}
	public void setKind(Kind kind) {
		this.kind = kind;
	}
	public void setNameservers(String[] nameservers) {
		this.nameservers = nameservers;
	}
	public void setSoaEditApi(SoaEditApi soa_edit_api) {
		this.soa_edit_api = soa_edit_api;
	}
}
