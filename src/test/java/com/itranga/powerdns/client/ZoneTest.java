package com.itranga.powerdns.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itranga.powerdns.client.model.ZoneConfig;

import io.vertx.core.json.Json;

import static org.assertj.core.api.Assertions.*;

public class ZoneTest {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ZoneTest.class);
	
	//ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testSoaEditApi() throws IOException{
		ZoneConfig zone = new ZoneConfig("linker.test.");
		zone.setNameservers(new String[]{"ns1.example.com"});
		String jsonZone = Json.encode(zone);
		LOG.debug("{}", jsonZone);
		ZoneConfig zone2 = Json.decodeValue(jsonZone, ZoneConfig.class);
		LOG.debug("soa_edit_api: {}", zone2.getSoaEditApi());
		assertThat(zone2.getSoaEditApi()).isEqualTo(zone.getSoaEditApi());
		assertThat(zone2.getName()).isEqualTo(zone.getName());
		assertThat(zone2.getKind()).isEqualTo(zone.getKind());
	}
	
	@Test
	public void testURLappend() throws MalformedURLException, URISyntaxException{
		URL url = new URL("https://localhost/api/v1");
		URI uri = url.toURI();
		LOG.debug("Servers URL: {}", uri.resolve("/api/v1").resolve("servers"));
	}
	
}
