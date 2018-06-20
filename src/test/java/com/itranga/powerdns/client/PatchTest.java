package com.itranga.powerdns.client;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.*;

import com.itranga.powerdns.client.IZoneConfig.Kind;
import com.itranga.powerdns.client.model.RRset;
import com.itranga.powerdns.client.model.Zone;
import com.itranga.powerdns.client.model.RRset.ChangeType;
import com.itranga.powerdns.client.model.RRset.Record;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class PatchTest {
	
	private static PowerDnsClient dnsClient;
	private static Logger LOG;
	private ExecutorService executorService = Executors.newFixedThreadPool(2);
	
	@BeforeClass
	public static void setSystem(TestContext context) throws MalformedURLException{
		//we have slf4j in test evnironment, so configure Vert.x to use it
		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");

		RRset nsRrset = new RRset();
		nsRrset.setTtl(172800);
		nsRrset.setType("NS");
		nsRrset.setRecords(Arrays.asList(new Record("xns1.example.com."), new Record("xns2.example.com.")));
		
		
		Zone zoneTemplate = new Zone();
		zoneTemplate.setRrsets( Arrays.asList( nsRrset ) );
		zoneTemplate.setKind(Kind.Master);
		//zoneTemplate.setSoa_edit_api(SoaEditApi.EPOCH);
		
		PowerDnsClient pdsc = new PowerDnsClientBuilder().setApiUrl("https://localhost/api")
			.setApiKey("yourApiKey")
			.setApiVersion("1")
			//.setZoneTemplate(zoneTemplate)
			.setDefaultNS("nx1.example.com.", "nx2.example.com.")
			.build();
		/*
		Vertx.vertx().deployVerticle(pdsc, context.asyncAssertSuccess( h -> {
			LOG = LoggerFactory.getLogger(VertxTest.class);
			dnsClient = pdsc;
			LOG.debug("DNS client initialzied: {}", h);
		}));
		*/
		
		String deploymentId = pdsc.init().join();
		LOG = LoggerFactory.getLogger(VertxTest.class);
		LOG.debug("Vertx deployment Id of the PowerDnsClient: {}", deploymentId);
		dnsClient = pdsc;
	}
	
	@Test
	public void sendPatch(TestContext context) throws InterruptedException, ExecutionException, TimeoutException{
		List<String> sampledata = Arrays.asList("www.example.com.", "www.example.org.");
		
		
		CompletableFuture<Collection<String>> nsFuture = dnsClient
					.addRecordToBatch(sampledata.get(0), "127.0.0.1")
					.exceptionally( t -> {
						t.printStackTrace();
						LOG.error(t);
						context.fail();
						return null;
					});

		
		//CompletableFuture<Collection<String>> nsFuture = future.get(); 
		LOG.info("Record added");
		Collection<String> registeredHostnames = dnsClient.doBatchJob().join();
		
		assertThat(registeredHostnames.size()).isEqualTo(1);
		String[] hostnames = registeredHostnames.toArray(new String[]{});
		assertThat(hostnames[0]).isEqualTo(sampledata.get(0));
		
		LOG.info("NS: {}", nsFuture.join());
		/*
		nsFuture.thenCompose( ns -> {
			LOG.info("NS: {}", ns);
			return dnsClient.deleteZone(PowerDnsUtils.getZone(hostname));						
		})
		.thenAccept( s -> {
			LOG.trace("{} deleted", s);
			context.async().complete();
		}).join();
		*/
	}
}
