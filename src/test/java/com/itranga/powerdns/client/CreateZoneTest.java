package com.itranga.powerdns.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.nullValue;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.itranga.powerdns.client.IZoneConfig.Kind;
import com.itranga.powerdns.client.IZoneConfig.SoaEditApi;
import com.itranga.powerdns.client.model.RRset;
import com.itranga.powerdns.client.model.RRset.ChangeType;
import com.itranga.powerdns.client.model.RRset.Record;
import com.itranga.powerdns.client.model.Zone;
import com.itranga.powerdns.client.model.ZoneConfig;
import com.itranga.powerdns.client.model.ZonePatch;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class CreateZoneTest {
	
	private static PowerDnsClient dnsClient;
	private static Logger LOG;
	
	@BeforeClass
	public static void setSystem(TestContext context) throws MalformedURLException{
		//we have slf4j in test evnironment, so configure Vert.x to use it
		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
		//PowerDnsClient pdsc = new PowerDnsClient("https://ns1a.1capp.com/api", "cXj2fsQBfuuQRpjVGqSMvANHePBfjWGs");
		
		RRset nsRrset = new RRset();
		nsRrset.setTtl(172800);
		nsRrset.setType("NS");
		nsRrset.setRecords(Arrays.asList(new Record("xns1.example.com."), new Record("xns2.example.com.")));
		
		RRset soaRrset = new RRset();
		soaRrset.setTtl(172800);
		soaRrset.setType("SOA");
		soaRrset.setRecords(Arrays.asList(new Record("ns1.example.com. hostmaster.example.com. 2016011902 10800 3600 604800 86400")));
		
		Zone zoneTemplate = new Zone();
		zoneTemplate.setRrsets( Arrays.asList( nsRrset ) );
		zoneTemplate.setKind(Kind.Master);
		//zoneTemplate.setSoa_edit_api(SoaEditApi.EPOCH);
		
		PowerDnsClient pdsc =new PowerDnsClientBuilder().setApiUrl("https://localhost/api")
			.setApiKey("cXj2fsQBfuuQRpjVGqSMvANHePBfjWGs")
			.setApiVersion("1")
			.setZoneTemplate(zoneTemplate)
			.build();
		Vertx.vertx().deployVerticle(pdsc, context.asyncAssertSuccess( h -> {
			LOG = LoggerFactory.getLogger(VertxTest.class);
			dnsClient = pdsc;
			LOG.debug("DNS client initialzied: {}", h);
		}));
	}
	
	@Test
	public void create_then_delete_zone(){
		String domain = "itran.tak.";
		Zone newZone = dnsClient.getZoneTemplate().get();
		newZone.setName(domain);
		newZone.setId(domain);
		newZone.getRrsets().forEach( rrset -> {
			rrset.setName(domain);
		});
		String testZone = dnsClient.createZone(newZone).thenApply(zone -> {
			LOG.info("{} SOA API: {}", zone.getId(), zone.getSoa_edit_api());
			return zone;
		}).thenCompose( zone -> {
			return dnsClient.deleteZone(zone.getId());
		}).thenApply( zoneId -> {
			LOG.info("Zone {} deleted", zoneId);
			return zoneId;
		}).join();
		assertThat(testZone).as("Create and delete %s", domain).isEqualTo(domain);
	}
	@Test
	public void delete_not_existing_zone(){
		String domain = "some-not-existing.domain.test.";
		Zone newZone = dnsClient.getZoneTemplate().get();
		newZone.setName(domain);
		newZone.setId(domain);
		newZone.getRrsets().forEach( rrset -> {
			rrset.setName(domain);
		});
		Throwable thrown = catchThrowable(() -> { dnsClient.deleteZone(domain).join(); });
		assertThat(thrown).hasRootCauseExactlyInstanceOf(PowerDnsApiException.class);
		assertThat(thrown.getCause()).hasMessageMatching(".*"+domain+".*");
			
	}
	
	//@Test
	public void patchZone(TestContext context) throws InterruptedException, ExecutionException, TimeoutException{
		String domain = "link.test.";
		ZonePatch patch = new ZonePatch();
		RRset rrset = new RRset();
		rrset.setChangetype(ChangeType.REPLACE);
		rrset.setName("denis."+domain);
		rrset.setRecords(Arrays.asList(new Record("google.com.")));
		rrset.setType("CNAME");
		rrset.setTtl(500);
		patch.setRrsets(Arrays.asList(rrset));
		LOG.debug("Current thread: {}", Thread.currentThread().getName());
		dnsClient.patchZone(domain, patch)
			/* for Void is not working, as v is null then
			.whenComplete( (v, ex) -> {
				if( v != null){
					LOG.info("PATCH applied");
				}else{
					LOG.error("Failed", ex);
				}
			}).get();
			*/
			//.join();
			
		
			.thenAcceptAsync( v ->{
				LOG.info("Patch applied in {}", Thread.currentThread().getName());
				context.async().complete();
			})
			.exceptionally( t -> {
				LOG.error(Thread.currentThread());
				context.async().complete();
				throw (RuntimeException) t;
			})			
			.join();
			//.get(5, TimeUnit.SECONDS);
		
		
			/*
			.handle( (result, ex) -> {
				if(result!=null){
					LOG.info("PATCHED");
				}else{
					LOG.error("ERROR while patching", ex);
				}
				return Optional.empty();
			}).get(5, TimeUnit.SECONDS);
			*/
		
	}
	
	//@Test
	public void multiPatch(TestContext context){
		Map<String, String> arecs = new HashMap<>();
		arecs.put("www-w.itran.ga.", "127.0.0.1");
		arecs.put("ww&-w.itran.ga.", "127.0.0.1");
		arecs.put("www.podru.ga.", "127.0.0.1");
		//arecs.put("test.", "127.0.0.1");
		//arecs.put("@.www.truba.", "127.0.0.1");
		arecs.put("@.trubadur.", "127.0.0.1");
		//arecs.put("www.1c.link.", "127.0.0.1");
		
		Map<String, Set<ZonePatch>> domainsMap = arecs.entrySet().stream()
		/*
		.map( entry -> {
			ZonePatch zp = new ZonePatch();
			RRset rrset = new RRset();
			rrset.setChangetype(ChangeType.REPLACE);
			rrset.setName(entry.getKey());
			rrset.setRecords(Arrays.asList(new Record(entry.getValue())));
			rrset.setType("A");
			rrset.setTtl(500);
			zp.setRrsets(Arrays.asList(rrset));
			return zp;
		})
		*/
		.collect( Collectors.groupingBy( 
				e -> {
					//return host.replaceFirst("^[a-zA-Z0-9-@]*\\.", "");
					String domain = e.getKey().replaceFirst("[^\\.]*\\.", "");
					if (domain.length()==0) domain = e.getKey();
					return domain;
				},
				Collectors.mapping(
					entry -> {
						ZonePatch zp = new ZonePatch();
						RRset rrset = new RRset();
						rrset.setChangetype(ChangeType.REPLACE);
						String host = entry.getKey();
						if(host.startsWith("@.")){
							host = host.replaceFirst("^@\\.", "");
						}
						rrset.setName(host);
						rrset.setRecords(Arrays.asList(new Record(entry.getValue())));
						rrset.setType("A");
						rrset.setTtl(500);
						zp.setRrsets(Arrays.asList(rrset));
						return zp;
					},
					Collectors.toSet())
			) );
		

		Map<String,ZonePatch> domains = new HashMap<>();
		domainsMap.forEach( (domain, patches) -> {
			ZonePatch target = new ZonePatch();
			target.setRrsets(new ArrayList<RRset>());
			ZonePatch value = patches.stream().reduce(target, (zp1, zp2) -> {
				List<RRset> rrsets = new ArrayList<>(zp1.getRrsets());
				rrsets.addAll(zp2.getRrsets());
				zp1.setRrsets(rrsets);
				return zp1;
			});
			domains.put(domain, value);
		});
		
		domains.entrySet().stream().forEach( entry -> {
			String domain = entry.getKey();
			ZonePatch patch = entry.getValue();
			LOG.info("Domain: {}", domain);
			
			CompletableFuture<Zone> zoneGetFuture = dnsClient.getZone(domain);			
			CompletableFuture<Zone> zoneFuture = zoneGetFuture.exceptionally( ex -> {
				Zone newZone = dnsClient.getZoneTemplate()
					.orElseThrow( () -> new IllegalStateException("zone template is undefined in dns client: see " + PowerDnsClient.class.getName()));
				//Zone newZone = dnsClient.zoneTemplate.get();
				newZone.setName(domain);
				newZone.setId(domain);
				newZone.getRrsets().forEach( rrset -> {
					rrset.setName(domain);
				});
				LOG.debug("Get zone ends exceptionally: {}", ex.getMessage());
				return newZone;
			})
			.thenCompose( zone -> {				
				if(zoneGetFuture.isCompletedExceptionally()){
					LOG.info("Creating zone {}", zone.getId());
					return dnsClient.createZone(zone);
				}
				return CompletableFuture.completedFuture(zone);
			});
			CompletableFuture<Collection<String>> patchFuture = zoneFuture
				.thenCompose( zone -> dnsClient.patchZone(zone.getId(), patch));			
			
			try{
			patchFuture
				.thenAccept( nsset -> {
					LOG.info("Nameservers for {}: {}", domain, nsset);
				})
				.join();
			}catch(Exception e){
				LOG.error(e.getMessage());
			}
						
			
			/*
			zoneGetFuture.applyToEither(zoneCreateFuture, zone -> zone)
				.thenCompose( zone -> dnsClient.patchZone(zone.getId(), patch))
				.join();
			*/
			
			/*
			CompletableFuture.anyOf(zoneGetFuture, zoneCreateFuture).thenCompose( zone -> 
				dnsClient.patchZone(((Zone)zone).getId(), patch)
			).join();
			*/
			
			/*
				.handleAsync( (zone, ex) -> {
					if(zone!=null){
						return zone;
					}else{
						Zone newZone = dnsClient.zoneTemplate.get();
						newZone.setName(domain);
						newZone.setId(domain);
						newZone.getRrsets().forEach( rrset -> {
							rrset.setName(domain);
						});
						LOG.debug(ex.getMessage());
						try {
							return dnsClient.createZone(newZone).join();
						}catch(RuntimeException e){ 
							throw e;
						}catch (Exception e1) {
							throw new RuntimeException(e1);
						}
					}
				});
			*/
			/*
			try {
				zoneFuture.thenCompose( zone -> {
					return dnsClient.patchZone(zone.getId(), patch);
				}).join();
			} catch (Exception e1) {
				LOG.error("Failed to patch", e1);
			}
			*/
			
		});
	}
}
