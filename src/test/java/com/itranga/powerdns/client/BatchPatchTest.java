package com.itranga.powerdns.client;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.*;

import com.itranga.powerdns.client.IZoneConfig.Kind;
import com.itranga.powerdns.client.model.RRset;
import com.itranga.powerdns.client.model.Zone;
import com.itranga.powerdns.client.model.RRset.ChangeType;
import com.itranga.powerdns.client.model.RRset.Record;
import com.itranga.powerdns.client.test.params.Parallelized;

import io.vertx.core.Vertx;




@RunWith(Parallelized.class)
public class BatchPatchTest {

	private static PowerDnsClient dnsClient;
	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(BatchPatchTest.class);
	//private static ExecutorService executorService;
	//private static ConcurrentHashMap<String, CompletableFuture<Collection<String>>> results = new ConcurrentHashMap<>();
	
	private static final int HOSTS_PER_DOMAIN = 7;
	
	private static Collection<String> domains = Arrays.asList("example.com.", "example.org.", "example.net."); 
	
	@Parameterized.Parameters
	public static Collection<PatchRecord> getTestData(){
		
		return createTestData(domains).get();
		/*
		Collection<PatchRecord> patches = new ArrayList<>();
		patches.add(new PatchRecord("www.example.com.", "127.0.0.1"));
		patches.add(new PatchRecord("www1.example.org.", "127.0.0.1"));
		patches.add(new PatchRecord("www2.example.com.", "127.0.0.1"));
		patches.add(new PatchRecord("www3.example.com.", "127.0.0.1"));
		patches.add(new PatchRecord("www4.example.com.", "127.0.0.1"));
		patches.add(new PatchRecord("www5.example.com.", "127.0.0.1"));
		return patches;
		*/
	}
	
	@BeforeClass
	public static void createDnsClient() throws MalformedURLException{
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
			.setApiKey("cXj2fsQBfuuQRpjVGqSMvANHePBfjWGs")
			.setApiVersion("1")
			.setZoneTemplate(zoneTemplate)
			.build();
		Vertx vertx = Vertx.vertx();
		LOG.info("Excuting blocinkg");
		/*
		vertx.executeBlocking( f -> {
			vertx.deployVerticle(pdsc, h -> {
				f.complete(h.result());
			});
			
		}, result -> {
			executorService = Executors.newFixedThreadPool(2);
			dnsClient = pdsc;
			LOG.debug("DNS client initialzied: {}", result.result());			
		});		
		LOG.info("Blocking ends");
		*/
		CompletableFuture<String> dnsClientFuture = new CompletableFuture<>();
		vertx.deployVerticle(pdsc, h -> {
			//executorService = Executors.newFixedThreadPool(5);			
			dnsClient = pdsc;
			LOG.debug("DNS client initialzied: {}", h.result());
			dnsClientFuture.complete(h.result());
		});
		dnsClientFuture.join();
	}
	
	@After
	public void doBatch(){
		dnsClient.doBatchJob().join();
	}
	
	@AfterClass
	public static void waitResults() throws InterruptedException{
		//Thread.sleep(2000);
		//Collection<String> registeredHostnames = dnsClient.doBatchJob().join();
		//LOG.debug("batch done...{}", Thread.currentThread());
		//LOG.debug("{}", registeredHostnames);
		//assertThat(registeredHostnames.size()).isEqualTo(HOSTS_PER_DOMAIN*domains.size());
		/*
		CompletableFuture<?> resultArray[] = results.values().toArray( new CompletableFuture<?>[]{} );
		CompletableFuture
			.allOf(resultArray).join();
		*/
	}
	
	
	private final PatchRecord thePatch;
	
	public BatchPatchTest(PatchRecord thePatch){
		this.thePatch = thePatch;
	}
	
	
	
	//@Test
	public void testPatch1() throws InterruptedException, ExecutionException, TimeoutException{
		sendPatch();
		dnsClient.doBatchJob()
			.thenCompose( batch1 -> {
				try {
					LOG.info("Doing batch...");
					sendPatch();
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return dnsClient.doBatchJob();
			}).join();
	}
	@Test
	public void testPatch2() throws InterruptedException, ExecutionException, TimeoutException{
		sendPatch();
	}
	
	private void sendPatch() throws InterruptedException, ExecutionException, TimeoutException{
		CompletableFuture<Collection<String>> future = dnsClient
					.addRecordToBatch(thePatch.action, thePatch.hostname, Arrays.asList(thePatch.value), 
							thePatch.type, thePatch.ttl)
					.exceptionally( t -> {
						LOG.error("Add record error", t);
						//context.fail();
						return null;
					});
		
		//Collection<String> hostnames = dnsClient.doBatchJob().join();
		//LOG.info("REGISTERD: {}", hostnames);
		//LOG.info("NS for {}: {}", thePatch.hostname, future.join());
		//results.put(Thread.currentThread().getName(), nsFuture);
				
		
		
		//nsFuture.join();
		/*
		nsFuture.thenCompose( ns -> {
			LOG.info("NS: {}", ns);
			return dnsClient.deleteZone(PowerDnsUtils.getZone(thePatch.hostname));						
		})
		.thenAccept( s -> {
			LOG.trace("{} deleted", s);
			//context.async().complete();
		}).join();
		*/
	}
	
	private static Optional<Collection<PatchRecord>> createTestData(Collection<String> domains){
		return domains.stream().map( domain -> {			
			Collection<PatchRecord> domainPatches = new ArrayList<>();
			int counter = 0;
			while(counter < HOSTS_PER_DOMAIN){
				domainPatches.add(new PatchRecord("www"+counter+"."+domain, "127.0.0.2"));
				counter++;
			}
			return domainPatches;			
		}).reduce( (alt, neu) -> {
			alt.addAll(neu);
			return alt;
		});	
	}
	
	static class PatchRecord{
		final ChangeType action;
		final String hostname;
		final String value;
		final String type;
		final int ttl;
		PatchRecord(ChangeType action, String hostname, String value, String type, int ttl){
			this.action = action;
			this.hostname = hostname;
			this.value = value;
			this.type = type;
			this.ttl = ttl;
		}
		PatchRecord(String hostname, String value){
			this.action = ChangeType.REPLACE;
			this.hostname = hostname;
			this.value = value;
			this.type = "A";
			this.ttl = 180;
		}
	}
}
