package com.itranga.powerdns.client;


import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.itranga.powerdns.client.model.ApiVersion;
import com.itranga.powerdns.client.model.Zone;
import com.itranga.powerdns.client.model.ZoneMetainfo;
import com.itranga.powerdns.client.model.ZonePatch;
import com.itranga.powerdns.client.model.RRset.ChangeType;

public interface IDnsClient {
	
	String SERVERS_API_PATH = "servers";
	String DEFAULT_SERVER = "localhost";
	static final String DEFAULT_API_KEY_HEADER = "X-API-Key";
	/**
	 * Default implicit TTL for DNS-record (RRset)
	 */
	static final int DEFAULT_IMPLICIT_RECORD_TTL = 180;
	
	/**
	 * Gets PowerDNS API versions and their URLs provided by API server
	 * @return result future with collection of available API versions.
	 */
	CompletableFuture<Collection<ApiVersion>> getApi();
	Optional<Zone> getZoneTemplate();
	///////// get Zones ///////////////
	/**
	 * Gets zones from the host where PowerDNS API resides.
	 * @return result future with zones from <code>localhost</code>,
	 * i.e. from the same server where PowerDNS API resides.
	 * @see #getZones(String) getZones("localhost")
	 */
	CompletableFuture<Collection<ZoneMetainfo>> getZones();
	/**
	 * Gets zones from the <code>server</code> residing behind PowerDNS API.
	 * @param server PowerDNS server to work with through API. Actually API endpoint can serve multiple servers. 
	 * @return result future with zones from <code>server</code>
	 */
	CompletableFuture<Collection<ZoneMetainfo>> getZones(String server);
	
	/////////////// get Zone /////////////////
	CompletableFuture<Zone> getZone(String serverId, String zoneId);
	CompletableFuture<Zone> getZone(String zoneId);

	////////////// createZone ////////////////
	CompletableFuture<Zone> createZone(String serverId, Zone zone);
	CompletableFuture<Zone> createZone(Zone zone);
	
	///////////// patchZone //////////////
	/**
	 * Patches the zone, as defined by <code>zoneId</code>.
	 * @param serverId the DNS server, residing behind PowerDNS API server, that this patch is to be applied
	 * @param zoneId the DNS zone to update
	 * @param zonePatch the patch itself
	 * @return future result with set of NS addresses responsible for the domain zone. For example, these
	 * addresses could be provided to the domain's register.
	 * <p>NS addresses are in canonical form, i.e. with trailing dot&mdash;<code>ns1.example.com.</code></p>
	 */
	CompletableFuture<Collection<String>> patchZone(String serverId, String zoneId, ZonePatch zonePatch);
	/**
	 * Patches the zone, as defined by <code>zoneId</code>, at the the same server (<code>localhost</code>)
	 * where PowerDNS API resides.
	 * @param zoneId the DNS zone to update
	 * @param zonePatch the patch itself
	 * @return future result with set of NS addresses responsible for the domain zone. For example, these
	 * addresses could be provided to the domain's register.
	 * <p>NS addresses are in canonical form, i.e. with trailing dot&mdash;<code>ns1.example.com.</code></p>
	 * @see #patchZone(String, String, ZonePatch) patchZone("localhost", zoneId, zonePatch)
	 */
	CompletableFuture<Collection<String>> patchZone(String zoneId, ZonePatch zonePatch);
	
	//////////// deleteZone ////////////////
	CompletableFuture<String> deleteZone(String serverId, String zoneId);
	CompletableFuture<String> deleteZone(String zoneId);
	
	/**
	 * Adds record to DNS.
	 * @param action either {@linkplain ChangeType#REPLACE REPLACE}(both for create and update)
	 * or {@linkplain ChangeType#DELETE DELETE}
	 * @param hostname FQDN. The first part (before a dot) is considered as <code>name</code> the remains
	 * as <code>zone</code>: <code>www.example.com.</code> &ndash; zone <code>example.com.</code> will be
	 * created if absent and the record <code>name</code> will be <code>www</code>. For root-records
	 * define <code>hostname</code> as <code>@.example.com.</code> &ndash; for zone <code>example.com.</code>.
	 * @param records a collection of string, for example IP-addresses for "A" record type
	 * @param recordType DNS record type: <code>A, AAAA, NS</code> and so on.
	 * @param ttl time-to-live on end clients (i.e. how long end-client cache it). If it is 0 or less, then
	 * IDnsClient set it's own default TTL or {@linkplain #DEFAULT_IMPLICIT_RECORD_TTL}
	 * @return future result with the nameservers' fqdn that hosts your zone&mdash;you can provide
	 * these names, for example, to your domain register. 
	 */
	CompletableFuture<Collection<String>> addRecordToBatch(ChangeType action, 
			String hostname, 
			Collection<String> records,
			String recordType,
			int ttl);
	/**
	 * <p>Adds or updates <code>A</code>-record to DNS zone, while setting.</p>
	 * <p>Equivalent to: {@linkplain #addRecordToBatch(ChangeType, String, Collection, String, int) 
	 * addRecordToBatch( ChangeType.REPLACE, hostname, [ipAddress], "A", 180 )}</p>
	 * @param hostname to add/update
	 * @param ipAddress to set/update
	 * @return future for collection of NS-records serving the zone.
	 */
	CompletableFuture<Collection<String>> addRecordToBatch(String hostname, String ipAddress);
	
	/**
	 * Triggers batch job, i.e. push all records in a batch to DNS API server.
	 * @return a future with successfully updated FQDNs - i.e. DNS API replied
	 */
	CompletableFuture<Collection<String>> doBatchJob();
	
	
	
}
