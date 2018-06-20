package com.itranga.powerdns.client.vertx;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.itranga.powerdns.client.IDnsClient;
import com.itranga.powerdns.client.model.inner.HostnameBatchResult;
import com.itranga.powerdns.client.model.inner.ZonePatchFuture;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.json.Json;

public class BatchVertx extends AbstractVerticle{
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(BatchVertx.class);
	
	public static final String ADD_HOSTNAME_TOPIC = "com.itranga.powerdns.batch.add";	
	public static final String BATCH_RESULTS_QUEUE = "com.itranga.powerdns.batch.results";
	
	private final IDnsClient dnsClient;
	//domain - deploymentId
	private final ConcurrentMap<String, String> zoneVerticles = new ConcurrentHashMap<>();
	
	private ExecutorService verticleCreateExecutor = Executors.newCachedThreadPool(new ThreadFactory(){
		final AtomicInteger threadNumber = new AtomicInteger(1);
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(Thread.currentThread().getThreadGroup(), r,
                    "patching-" + threadNumber.getAndIncrement(),
                    0);
			t.setDaemon(true);
			return t;
		}
	});

	
	public BatchVertx(IDnsClient dnsClient){
		this.dnsClient = dnsClient;
	}
	
	@Override
	public void start(){
		vertx.eventBus().consumer(ADD_HOSTNAME_TOPIC, this::addHostnameHandler );
		vertx.eventBus().consumer(BATCH_RESULTS_QUEUE, this::getResultsHandler );
	}
	
	
	protected void addHostnameHandler(Message<? extends String> message){
		LOG.trace("Hanling message for \"{}\" in thread {}", ADD_HOSTNAME_TOPIC, Thread.currentThread().getName());
		
		ZonePatchFuture patch = Json.decodeValue(message.body(), ZonePatchFuture.class);
		List<BatchRRset> brrsets = patch.getRrsets().stream().map( rrset -> {
			BatchRRset brrs = new BatchRRset();
			brrs.setFutureId(patch.getFutureResultId());
			brrs.setRrset(patch.getRrsets());
			return brrs;
		}).collect( Collectors.toList() );
		
		DeliveryOptions options = new DeliveryOptions();
		MultiMap headers = new CaseInsensitiveHeaders();
		headers.add("domain", patch.getZoneId());
		options.setHeaders(headers);
		
		if(!zoneVerticles.containsKey(patch.getZoneId())){
			LOG.trace("Creating zone vertilce for {}", patch.getZoneId());
			CompletableFuture<String> deploymentIdFuture = new CompletableFuture<>();
			verticleCreateExecutor.execute( () -> {
				vertx.deployVerticle(
						new ZoneVerticle(patch.getZoneId(), dnsClient),
						ar -> {
							if(ar.succeeded()){
								LOG.trace("Zone verticle for {} created", patch.getZoneId());
								deploymentIdFuture.complete(ar.result());
							}else{
								LOG.warn("Failed to created veticle for {}", patch.getZoneId());
								deploymentIdFuture.completeExceptionally(ar.cause());
							}
						}
				);
			});
			deploymentIdFuture.thenAccept( deploymentId -> {
				zoneVerticles.put(patch.getZoneId(), deploymentId);
				String zoneMessage = Json.encode(brrsets);
				LOG.trace("Publishing to zone {} : {}", patch.getZoneId(), zoneMessage);
				vertx.eventBus().publish(ZoneVerticle.ADD_ZONE_RECORD_TOPIC, zoneMessage, options);
			});
			
		}else{
			vertx.eventBus().publish(ZoneVerticle.ADD_ZONE_RECORD_TOPIC, Json.encode(brrsets), options);
		}		
	}
	
	protected void getResultsHandler(Message<? extends Long> message){
		//long batchId = Json.decodeValue(message.body(), Long.class);
		long batchId = message.body();
		//LinkedList<CompletableFuture<Collection<HostnameBatchResult>>> allResults = new LinkedList<>();
		CountDownLatch theLatch = new CountDownLatch(zoneVerticles.size());
		CopyOnWriteArraySet<HostnameBatchResult> batchHostnamesRegistered = new CopyOnWriteArraySet<>();
		//LOG.trace("Handling results in {} zones", zoneVerticles.size());
		//verticleCreateExecutor.execute( () -> {
			zoneVerticles.keySet().stream().forEach( domain -> {
				//CompletableFuture<Collection<HostnameBatchResult>> fut = new CompletableFuture<>();
				//allResults.add(fut);
				//verticleCreateExecutor.execute( () -> {
					vertx.eventBus().send(ZoneVerticle.FLUSH_ZONE_QUEUE+domain, batchId, response -> {
						LOG.trace("Response from zone {}", domain);
						if(response.succeeded()){
							String responseMessage = response.result().body().toString();
							LOG.trace("Zone {} replied : {}", domain, responseMessage);
							List<HostnameBatchResult> results = Arrays.asList(Json.decodeValue(responseMessage, HostnameBatchResult[].class));
							batchHostnamesRegistered.addAll(results);
						}else{
							//FIXME
							throw new RuntimeException( response.cause() );							
						}
						theLatch.countDown();
					});
				//});
			});
		//});		
		
		verticleCreateExecutor.execute( () -> {
			try {
				//LOG.trace("Waiting on Latch");
				theLatch.await();
			} catch (Exception e1) {e1.printStackTrace();}
			//LOG.trace("Latch passed");
			message.reply(Json.encode(batchHostnamesRegistered));
		});
		
		
		
		
		/*
		@SuppressWarnings("unchecked")
		CompletableFuture<Collection<HostnameBatchResult>> futures[] = new CompletableFuture[allResults.size()]; 
		CompletableFuture.allOf(futures)
			.thenAccept( v -> {
				allResults.stream().map( f -> {
					try {
						return f.get();
					} catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException(e);						
					}
				});
			});
		*/
	}

}
