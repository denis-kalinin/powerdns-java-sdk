package com.itranga.powerdns.client.model;

public class ZoneMetainfo {
	private String account;
	private boolean dnssec;
	private String id;
	private String kind;
	private long last_check;
	private String[] masters;
	private String name;
	private long notified_serial;
	private long serial;
	private String url;
	public String getAccount() {
		return account;
	}
	public void setAccount(String account) {
		this.account = account;
	}
	public boolean isDnssec() {
		return dnssec;
	}
	public void setDnssec(boolean dnssec) {
		this.dnssec = dnssec;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getKind() {
		return kind;
	}
	public void setKind(String kind) {
		this.kind = kind;
	}
	public long getLast_check() {
		return last_check;
	}
	public void setLast_check(long last_check) {
		this.last_check = last_check;
	}
	public String[] getMasters() {
		return masters;
	}
	public void setMasters(String[] masters) {
		this.masters = masters;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getNotified_serial() {
		return notified_serial;
	}
	public void setNotified_serial(long notified_serial) {
		this.notified_serial = notified_serial;
	}
	public long getSerial() {
		return serial;
	}
	public void setSerial(long serial) {
		this.serial = serial;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	

}
