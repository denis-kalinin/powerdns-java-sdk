package com.itranga.powerdns.client.vertx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.itranga.powerdns.client.IDnsClient;
import com.itranga.powerdns.client.PowerDnsClient;
import com.itranga.powerdns.client.model.RRset;
import com.itranga.powerdns.client.model.Zone;
import com.itranga.powerdns.client.model.ZonePatch;
import com.itranga.powerdns.client.model.inner.HostnameBatchResult;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.Json;

public class ZoneVerticle extends AbstractVerticle{
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ZoneVerticle.class);
	static final String ADD_ZONE_RECORD_TOPIC = "com.itranga.powerdns.zone[addRecord]";
	public static final String FLUSH_ZONE_QUEUE = "com.itranga.powerdns.zone[flush].";
		
	
	private final String domain;
	private final IDnsClient dnsClient;
	//FIXME need to be cuncurrent
	private Set<BatchRRset> domainRecords = new HashSet<>();
	
	public ZoneVerticle(String domain, IDnsClient dnsClient){
		this.domain = domain;
		this.dnsClient = dnsClient;
	}
	
	@Override
	public void start(){		
		vertx.eventBus().consumer( ADD_ZONE_RECORD_TOPIC, this::hostnameHandler );
		//vertx.eventBus().consumer( FLUSH_ZONE_QUEUE+domain, this::flushZone );
		//MessageConsumer<Long> consumer = 
		vertx.eventBus().consumer(FLUSH_ZONE_QUEUE+domain, this::flushZone);
		//consumer.handler( this::flushZone );
		LOG.trace("Started verticle for {}", domain);
	}

	private void hostnameHandler(Message<? extends String> message){		
		if(!message.headers().get("domain").equalsIgnoreCase(domain)) return;
		LOG.trace("Hostname-ADD hanler for domain {}  Message headers: {}", domain, message.headers());
		List<BatchRRset> newDomainRecords = Arrays.asList(
					Json.decodeValue(message.body(), BatchRRset[].class)
				);		
		domainRecords.addAll(newDomainRecords);
	}
	
	private void flushZone(Message<? extends Long> message){
		if(domainRecords.isEmpty()){
			message.reply("[]");
			return;
		}
		LOG.trace("Handling flush in thread: {}", Thread.currentThread().getName());

		//long batchId = Json.decodeValue(message.body(), Long.class);
		long batchId = message.body();
		ZonePatch patch = new ZonePatch();
		List<RRset> rrset = domainRecords.stream().flatMap( brrs -> {
			return brrs.getRrset().stream().map( rr -> rr);
		}).collect( Collectors.toList() );
		patch.setRrsets(rrset);
		
		
		CompletableFuture<Zone> zoneGetFuture = dnsClient.getZone(domain);			
		CompletableFuture<Zone> zoneFuture = zoneGetFuture.exceptionally( ex -> {
			Zone newZone = dnsClient.getZoneTemplate()
				.orElseThrow( () -> new IllegalStateException("zone template is undefined in dns client: see " + PowerDnsClient.class.getName()));
			//Zone newZone = dnsClient.zoneTemplate.get();
			newZone.setName(domain);
			newZone.setId(domain);
			newZone.getRrsets().forEach( templateRR -> {
				templateRR.setName(domain);
			});
			return newZone;
		})
		.thenCompose( zone -> {				
			if(zoneGetFuture.isCompletedExceptionally()){
				return dnsClient.createZone(zone);
			}
			return CompletableFuture.completedFuture(zone);
		});
		CompletableFuture<Collection<String>> patchFuture = zoneFuture
			.thenCompose( zone -> dnsClient.patchZone(zone.getId(), patch));
		
		
		patchFuture
			.thenAccept( ns -> {
				LOG.trace("Preparing batch result for zone {}", domain);
				List<HostnameBatchResult> batchResults = new ArrayList<>();				
				domainRecords.stream().forEach( brrs -> {
					HostnameBatchResult br = new HostnameBatchResult();
					br.setSuccedded(true);
					br.setHostnameResultFutureid(brrs.getFutureId());
					br.setBatchId(batchId);
					br.setRrset(brrs.getRrset());
					br.setNs(ns);
					batchResults.add(br);
				});
				domainRecords.clear();
				String msg = Json.encode(batchResults);
				LOG.trace("Replying with HostnameBatchResult in thread : {}", Thread.currentThread().getName());
				message.reply(msg);
			})
			.exceptionally( t -> {
				List<HostnameBatchResult> batchResults = new ArrayList<>();
				domainRecords.stream().forEach( brrs -> {
					HostnameBatchResult br = new HostnameBatchResult();
					br.setSuccedded(false);
					br.setErrorMessage(t.getMessage());
					br.setHostnameResultFutureid(brrs.getFutureId());
					br.setBatchId(batchId);
					batchResults.add(br);
				});
				message.reply(Json.encode(batchResults));
				return null;
			});
	}
}
