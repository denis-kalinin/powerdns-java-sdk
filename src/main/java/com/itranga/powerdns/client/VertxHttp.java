package com.itranga.powerdns.client;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class VertxHttp extends AbstractVerticle{
	
	private static final Logger LOG = LoggerFactory.getLogger(VertxHttp.class);
	
	private WebClient client;
	
	private ExecutorService apiExecutor = Executors.newCachedThreadPool(new ThreadFactory(){
		final AtomicInteger threadNumber = new AtomicInteger(1);
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(Thread.currentThread().getThreadGroup(), r,
                    "http-api-" + threadNumber.getAndIncrement(),
                    0);
			t.setDaemon(true);
			return t;
		}
	});
	
	@Override
	public void start(){
		//HttpClientOptions options = new HttpClientOptions().setLogActivity(true);
		//
		//WebClient client = WebClient.wrap(httpClient);
		client = WebClient.create(vertx);
		LOG.debug("Created Http Client: {}", client);
	}
	
	public CompletableFuture<HttpResponse<Buffer>> request(HttpMethod httpMethod, URL url,
			Optional<Map<String, String>> headers,
			Optional<JsonObject> body){
		
		CompletableFuture<HttpResponse<Buffer>> future = new CompletableFuture<>();
		apiExecutor.execute( () -> {
			int port = url.getPort()==-1 ? url.getDefaultPort() : url.getPort();
			HttpRequest<Buffer> request = client.request(httpMethod, port, url.getHost(), url.getPath())
				.ssl(url.getProtocol().equalsIgnoreCase("HTTPS"));
			
			headers.ifPresent( m -> {
				m.entrySet().stream().forEach( e -> {
					request.putHeader(e.getKey(), e.getValue());
				});
			});
			if(body.isPresent()){
				LOG.trace("JSON sending... {}", body.get());
				request
					.followRedirects(true)
					.sendJsonObject(body.get(), ar -> handleResult(ar, future));
			}else{
				LOG.trace("GETting ... {} thread: {}", url, Thread.currentThread().getName());
				request.send( ar -> handleResult(ar, future));
			}
			if(LOG.isDebugEnabled() && httpMethod.equals(HttpMethod.PATCH)){
				LOG.trace("Patch send");
			}			
		});
		return future;
	}
	
	private void handleResult( final AsyncResult<HttpResponse<Buffer>> ar,
		CompletableFuture<HttpResponse<Buffer>> future){
		if (ar.succeeded()){
			if(LOG.isTraceEnabled()){
				LOG.trace("HttppResponse {}: {}", ar.result().statusCode(), ar.result().bodyAsString());
			}
			future.complete( ar.result() );
		} else {
			future.completeExceptionally( ar.cause() );
		}
	}
	
}
