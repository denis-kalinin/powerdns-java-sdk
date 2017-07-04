package com.itranga.powerdns.client;

import io.vertx.core.Vertx;

public class DnsClientFactory {

	////////////FACTORY//////////////
	
	public static IDnsClient getClient(String apiUrl, String apiKey){
		return getClient(apiUrl, apiKey, false);
	}
	
	public static IDnsClient getClient(String apiUrl, String apiKey, String server){
		return getClient(apiUrl, apiKey, false);
	}
	/**
	 * Creates or get already created <code>PowerDNS client</code>.
	 * @param apiUrl address of PowerDNS API, e.g. <code>http://dns.example.com/api</code>
	 * @param apiKey API security key
	 * @param isCanonical if <code>true</code> client won't adjust names to canonical where it is required by protocol.
	 * For example, {@linkplain com.itranga.powerdns.client.model.Zone#setName(String) domain zone name} is required to
	 * be canonical: <code>example.org.</code>&mdash;trailing dot&mdash;and then
	 * <code>example.org</code> is invalid if <code>isCanonical=true</code>.
	 * @return dns client
	 */
	public static IDnsClient getClient(String apiUrl, String apiKey, boolean isCanonical){
		try {
			PowerDnsClient client = new PowerDnsClientBuilder().setApiKey(apiKey).setApiUrl(apiUrl)
				.setImplicitRecordTtl(180)
				.build();
			//PowerDnsClient client = new PowerDnsClient(apiUrl, apiKey, "1", isCanonical, 180);
			Vertx vertx = Vertx.vertx();			
			vertx.deployVerticle(client, asyncResult -> {});
			return client;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
