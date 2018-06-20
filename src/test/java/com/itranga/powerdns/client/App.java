package com.itranga.powerdns.client;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class App {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(App.class);
	
	public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException, MalformedURLException{
		// vertx to use slf4j as logger
		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
		
		PowerDnsClient dnsClient = new PowerDnsClientBuilder()
				.setApiKey("yourApiKey")
				.setApiUrl("http://10.72.101.177:80/api")
				.setDefaultNS("nx1.example.com", "nx2.example.com")
				.setImplicitRecordTtl(180)
				.build();
		
		String deploymentId = dnsClient.init().join();
		LOG.info("Init dns client: {} - Vertx deploymentId", deploymentId);
		
		
		CompletableFuture<Collection<String>> wwwResult = dnsClient
				.addRecordToBatch("www.example.com", "127.0.0.1")
				.exceptionally( t -> {
					if(LOG.isTraceEnabled()){
						LOG.error("Failed to add record to batch", t);
					}else{
						LOG.error("Failed to add record to bacth : {}", t.getMessage());
					}					
					return null;
				});

		
		//RUN batch asyncronously
		Thread t = new Thread ( () -> {
			try {
				Thread.sleep(3000);
				dnsClient.doBatchJob();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		t.setDaemon(true);
		t.start();
		
		wwwResult.thenAccept( ns -> {
				LOG.debug("Namserver: "+ns);			
			}).get(10,  TimeUnit.SECONDS);

		dnsClient.close().join();
	}

}
