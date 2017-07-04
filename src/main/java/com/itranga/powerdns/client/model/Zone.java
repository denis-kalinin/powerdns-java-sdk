package com.itranga.powerdns.client.model;

import java.util.List;

import com.itranga.powerdns.client.IZoneConfig;
import com.itranga.powerdns.client.IZoneConfig.Kind;
import com.itranga.powerdns.client.IZoneConfig.SoaEditApi;

public class Zone {
	private String account;
	private boolean dnssec;
	private String id;
	private Kind kind;
	private long last_check;
	private List<String> masters;
	private String name;
	private String notified_serial;
	private List<RRset> rrsets;
	private long serial;
	private String soa_edit;
	private SoaEditApi soa_edit_api;
	private String url;
	private String[] nameservers = new String[]{};
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
	public IZoneConfig.Kind getKind() {
		return kind;
	}
	public void setKind(IZoneConfig.Kind kind) {
		this.kind = kind;
	}
	public long getLast_check() {
		return last_check;
	}
	public void setLast_check(long last_check) {
		this.last_check = last_check;
	}
	public List<String> getMasters() {
		return masters;
	}
	public void setMasters(List<String> masters) {
		this.masters = masters;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getNotified_serial() {
		return notified_serial;
	}
	public void setNotified_serial(String notified_serial) {
		this.notified_serial = notified_serial;
	}
	public List<RRset> getRrsets() {
		return rrsets;
	}
	public void setRrsets(List<RRset> rrsets) {
		this.rrsets = rrsets;
	}
	public long getSerial() {
		return serial;
	}
	public void setSerial(long serial) {
		this.serial = serial;
	}
	public String getSoa_edit() {
		return soa_edit;
	}
	public void setSoa_edit(String soa_edit) {
		this.soa_edit = soa_edit;
	}
	public SoaEditApi getSoa_edit_api() {
		return soa_edit_api;
	}
	public void setSoa_edit_api(SoaEditApi soa_edit_api) {
		this.soa_edit_api = soa_edit_api;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String[] getNameservers() {
		return nameservers;
	}
	public void setNameservers(String[] nameservers) {
		this.nameservers = nameservers;
	}
	
}
