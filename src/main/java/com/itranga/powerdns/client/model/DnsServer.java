package com.itranga.powerdns.client.model;

public class DnsServer {
	
	private String type;
	private String daemon_type;
	private String id;
	private String url;
	private String version;
	private String config_url;
	private String zones_url;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getDaemon_type() {
		return daemon_type;
	}
	public void setDaemon_type(String daemon_type) {
		this.daemon_type = daemon_type;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getConfig_url() {
		return config_url;
	}
	public void setConfig_url(String config_url) {
		this.config_url = config_url;
	}
	public String getZones_url() {
		return zones_url;
	}
	public void setZones_url(String zones_url) {
		this.zones_url = zones_url;
	}
	

}
