package com.itranga.powerdns.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class PatternTest {
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PatternTest.class);
	
	@Test
	public void parsetZones(){
		String url = PowerDnsUtils.getUrlFromPattern("/api/v1/servers{/server}/zones{/zone}", "localhost", "example.com.");
		LOG.debug("Zone url: {}", url);
		assertThat(url).as("URL parsed").isEqualTo("/api/v1/servers/localhost/zones/example.com.");
	}
	
	@Test
	public void hostname(){
		Pattern p = Pattern.compile("^(?<host>[a-zA-Z0-9-]+|[@])\\.(?<zone>.*)");
		Matcher m = p.matcher("www.example.org.");
		assertThat(m.matches()).isTrue();
		Matcher m1 = p.matcher("@.example.org");
		assertThat(m1.matches()).isTrue();
		Matcher m2 = p.matcher("sdf@.example.org");
		assertThat(m2.matches()).isFalse();
		
		LOG.debug("Groups: {}", m.groupCount());
		LOG.debug("Host: {}", m.group("host"));
		LOG.debug("Zone: {}", m.group("zone"));
		for(int i=0; i<=m.groupCount(); i++){
			LOG.debug(m.group(i));
		}
	}
	
	@Test
	public void getZone(){
		String zoneId = PowerDnsUtils.getZone("www.example.org");
		assertThat(zoneId).as("Zone id extracted").isEqualTo("example.org.");
		zoneId = PowerDnsUtils.getZone("@.example.org");
		assertThat(zoneId).as("Zone id extracted").isEqualTo("example.org.");

		Throwable thrown = catchThrowable(() -> {PowerDnsUtils.getZone(null);});
		assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
		
		thrown = catchThrowable(() -> {PowerDnsUtils.getZone("&a9#.example.org.");});
		assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
		
		thrown = catchThrowable(() -> {PowerDnsUtils.getZone(".example.org.");});
		assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
	}

}
