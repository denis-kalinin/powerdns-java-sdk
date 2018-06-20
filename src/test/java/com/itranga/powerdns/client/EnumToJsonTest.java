package com.itranga.powerdns.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itranga.powerdns.client.IZoneConfig.SoaEditApi;

public class EnumToJsonTest {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EnumToJsonTest.class);
	
	@Test
	public void testSoa() throws JsonProcessingException{
		assertThat(new ObjectMapper().writeValueAsString(SoaEditApi.INCEPTION_EPOCH))
			.as("ENUM to JSON test")
			.isEqualTo("\"INCEPTION-EPOCH\"");
	}

}
