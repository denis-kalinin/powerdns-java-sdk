package com.itranga.powerdns.client;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.itranga.powerdns.client.model.RRset.Record;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@RunWith(VertxUnitRunner.class)
public class VertxTest {
	
	private static IDnsClient dnsClient;
	private static Logger LOG;
	
	@BeforeClass
	public static void setSystem(TestContext context) throws MalformedURLException{
		//we have slf4j in test evnironment, so configure Vert.x to use it
		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
		PowerDnsClient pdsc = new PowerDnsClient("https://localhost/api", "yourApiKey");
		Vertx.vertx().deployVerticle(pdsc, context.asyncAssertSuccess( h -> {
			LOG = LoggerFactory.getLogger(VertxTest.class);
			dnsClient = pdsc;
			LOG.debug("DNS client initialzied: {}", h);
		}));
	}
	
	@Test
	public void getApi(TestContext context) throws InterruptedException, ExecutionException, TimeoutException{
		//context.asyncAssertSuccess( a -> {});
		LOG.debug("Getting API");
		dnsClient.getApi().thenAccept( collection -> {
			LOG.debug("API collection: {}", collection.size());
			if(LOG.isDebugEnabled()){
				collection.stream().forEach( v -> {
					LOG.debug("version: {} API URL: {}", v.getVersion(), v.getUrl());
				});
			}
			context.async().complete();
		}).get(5, TimeUnit.SECONDS);
		
	}
	@Test
	public void getZones(TestContext context) throws InterruptedException, ExecutionException, TimeoutException{
		dnsClient.getZones().thenAccept( collection -> {
			if(LOG.isDebugEnabled()){
				collection.stream().forEach( v -> {
					LOG.debug("Domain Id: {}, url: {}", v.getId(), v.getUrl());
				});
			}
			context.async().complete();
		}).get(5, TimeUnit.SECONDS);
		
	}
	
	@Test
	public void getZone(TestContext context) throws InterruptedException, ExecutionException, TimeoutException{
		dnsClient.getZone("list24.info.")
		.whenComplete( (zone, ex) -> {
			if(zone!=null){
				if(LOG.isDebugEnabled()){
					LOG.debug("Zone {}", zone.getName() );
					zone.getRrsets().stream()
						.forEach( rrset -> {
							LOG.debug("- {}  {}  {}", rrset.getType(), rrset.getName(), rrset.getTtl());
							for(Record rec : rrset.getRecords()){
								LOG.debug("  - {} {}", rec.getContent(), rec.isDisabled()?"[DISABLED]":"");
							}
						});
				}
			}else{
				LOG.error("Failed", ex);
			}
			
		}).get(5, TimeUnit.SECONDS);
/*
		.thenAccept( zone -> {
			if(LOG.isDebugEnabled()){
				LOG.debug("Zone {}", zone.getName() );
				zone.getRrsets().stream()
					.forEach( rrset -> {
						LOG.debug("- {}  {}  {}", rrset.getType(), rrset.getName(), rrset.getTtl());
						for(Record rec : rrset.getRecords()){
							LOG.debug("  - {} {}", rec.getContent(), rec.isDisabled()?"[DISABLED]":"");
						}
					});
			}
		});
*/
	}

}
