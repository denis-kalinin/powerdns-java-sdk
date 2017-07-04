package com.itranga.powerdns.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.itranga.powerdns.client.IZoneConfig.Kind;
import com.itranga.powerdns.client.model.RRset;
import com.itranga.powerdns.client.model.Zone;
import com.itranga.powerdns.client.model.RRset.Record;

public class PowerDnsClientBuilder {
	private static final String DEFAULT_API_KEY_HEADER = IDnsClient.DEFAULT_API_KEY_HEADER;
	private static final String DEFAULT_API_VERSION = "1";
	private URL apiUrl;
	private String apiKey;
	private String apiVersion;
	private boolean isCanonical = false;
	private Zone zoneTemplate;
	private String apiKeyHeader;
	private int implicitRecordTtl = -1;
	private Set<String> nsServers;
	/**
	 * To automatically create zone while adding record you need to define zone's template (or default
	 * one will be applied). If <code>zoneTemplate</code> doesn't have <code>NS</code> RRset,
	 * then {@linkplain #setDefaultNS(String...)} is mandatory for zone auto-creation, otherwise
	 * you will have to create zones yourself with {@linkplain IDnsClient#createZone(String, Zone)}.
	 * @param zoneTemplate a template that is used by default to create new zone
	 * @return itself
	 * @see #setDefaultNS(String...)
	 */
	public PowerDnsClientBuilder setZoneTemplate(Zone zoneTemplate){
		this.zoneTemplate = zoneTemplate;
		return this;
	}
	/**
	 * Set default nameservers for zones that doesn't contain NS-RRset
	 * @param nsServer nameservers
	 * @return itself
	 */
	public PowerDnsClientBuilder setDefaultNS(String ... nsServer){
		this.nsServers = Arrays.asList(nsServer).stream()
				.map( ns -> PowerDnsUtils.toCanonical(ns))
				.collect( Collectors.toSet() );
		return this;
	}
	/**
	 * 
	 * @param apiUrl for example <code>http://pdns.example.com/api</code>
	 * @return itself
	 * @throws MalformedURLException if <code>apiUrl</code> is not valid.
	 */
	public PowerDnsClientBuilder setApiUrl(String apiUrl) throws MalformedURLException{
		this.apiUrl = new URL( apiUrl );
		return this;
	}
	/**
	 * Sets PowerDNS API version to use.
	 * @param apiVersion, default is {@value #DEFAULT_API_VERSION}
	 * @return itself
	 */
	public PowerDnsClientBuilder setApiVersion(String apiVersion){
		this.apiVersion = apiVersion;
		return this;
	}
	/**
	 * Sets API key, the secret token to access PowerDNS API
	 * @param apiKey a secret key to access API
	 * @return itself
	 */
	public PowerDnsClientBuilder setApiKey(String apiKey){
		this.apiKey = apiKey;
		return this;
	}
	/**
	 * Sets HTTP header name where to put <code>apiKey</code>. Default will be {@value #DEFAULT_API_KEY_HEADER}
	 * @param apiKeyHeader HTTP header name to carry <code>{@linkplain #setApiKey(String) apiKey}</code>
	 * @return itself
	 */
	public PowerDnsClientBuilder setApiKeyHeader(String apiKeyHeader){
		this.apiKeyHeader = apiKeyHeader;
		return this;
	}
	
	public PowerDnsClientBuilder setCanonical(boolean isCanonical){
		this.isCanonical = isCanonical;
		return this;
	}
	/**
	 * Sets default <code>ttl</code> for simple record adding with {@linkplain IDnsClient#addRecordToBatch(String, String)}.
	 * <p>If not set, then by default is {@value IDnsClient#DEFAULT_IMPLICIT_RECORD_TTL} seconds.
	 * @param seconds the time (in seconds) a record will live in end-users cache
	 * @return itself
	 */
	public PowerDnsClientBuilder setImplicitRecordTtl(int seconds){
		this.implicitRecordTtl = seconds;
		return this;
	}

	/**
	 * Creates DNS client instance
	 * @return API client instance for PowerDNS
	 * @throws MalformedURLException if <code>apiUrl</code> is invalid URL
	 * @throws IllegalStateException if {@linkplain #setApiUrl(String) API URL} is not set
	 */
	public PowerDnsClient build() throws MalformedURLException{
		if(apiUrl==null) throw new IllegalStateException("API URL is mandatory. Use setApiUrl() function!");
		String apiVer = apiVersion != null ? apiVersion : DEFAULT_API_VERSION;
		PowerDnsClient pdns = new PowerDnsClient(apiUrl, apiKey, apiVer, isCanonical, implicitRecordTtl);
		pdns.apiKeyHeader = apiKeyHeader!=null ? apiKeyHeader : DEFAULT_API_KEY_HEADER;
		pdns.zoneTemplate = Optional.ofNullable(zoneTemplate);
		if(nsServers!=null){
			if(!pdns.zoneTemplate.isPresent()){
				Zone zoneTpl = new Zone();
				zoneTpl.setKind(Kind.Master);
				pdns.zoneTemplate = Optional.of(zoneTpl);
			}
			pdns.zoneTemplate.ifPresent( zone -> {
				if(zone.getRrsets() != null){
					long nsRrsetsNum = zone.getRrsets().stream()
						.filter( rrset -> rrset.getType()!=null && rrset.getType().equals("NS"))
						.count();
					if(nsRrsetsNum>0) return;
				}else{
					zone.setRrsets(new ArrayList<>());
				}
				//zone.setNameservers(nsServers);
				RRset nsRrset = new RRset();
				nsRrset.setTtl(172800);
				nsRrset.setType("NS");
				List<Record> nsRecords = nsServers.stream()
					.map( ns -> new Record(ns))
					.collect( Collectors.toList() );
				nsRrset.setRecords(nsRecords);
				zone.getRrsets().add(nsRrset);
			});
		}
		return pdns;
	}
}
