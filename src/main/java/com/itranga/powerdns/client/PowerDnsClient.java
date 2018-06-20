package com.itranga.powerdns.client;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.itranga.powerdns.client.model.ApiVersion;
import com.itranga.powerdns.client.model.ZoneMetainfo;
import com.itranga.powerdns.client.model.ZonePatch;
import com.itranga.powerdns.client.model.inner.HostnameBatchResult;
import com.itranga.powerdns.client.model.inner.ZonePatchFuture;
import com.itranga.powerdns.client.vertx.BatchVertx;
import com.itranga.powerdns.client.model.RRset.ChangeType;
import com.itranga.powerdns.client.model.RRset.Record;
import com.itranga.powerdns.client.model.DnsServer;
import com.itranga.powerdns.client.model.RRset;
import com.itranga.powerdns.client.model.Zone;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;

/**
 * pdns_server or pdns_recursor
 * @author Denis Kalinin
 *
 */
public class PowerDnsClient extends AbstractVerticle implements IDnsClient {
	
	private static final Logger LOG = LoggerFactory.getLogger(PowerDnsClient.class);
	
	private final URL apiUrl;
	private final String apiVersion;
	//private final String server;
	private final boolean isCanonical;
	
	Optional<Zone> zoneTemplate = Optional.empty();
	String apiKeyHeader;
	private String apiKey;
	public final int implicitRecordTtl;
	private final AtomicBoolean hasApi = new AtomicBoolean();
	//map key - server id, value - dnsserver never get directly. User getServerAip() method
	private final CompletableFuture<Map<String, DnsServer>> serverApi = new CompletableFuture<>();
	
	private final VertxHttp vhttp = new VertxHttp();
	//private final VertxBatch vbatch = new VertxBatch();
	private final Map<String, String> headers = new HashMap<>();
	
	//HttpClient client = vertx.createHttpClient();
	/*
	private ExecutorService batchExecutore = Executors.newCachedThreadPool(new ThreadFactory(){
		final AtomicInteger threadNumber = new AtomicInteger(1);
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(Thread.currentThread().getThreadGroup(), r,
                    "batch-job-" + threadNumber.getAndIncrement(),
                    0);
			t.setDaemon(true);
			return t;
		}
	});
	*/
	
	
	
	PowerDnsClient(String apiUrl, String apiKey) throws MalformedURLException{
		this(apiUrl, apiKey, "1", false, 180);
	}
	
	PowerDnsClient(String apiUrl, String apiKey, String apiVersion, boolean isCanonical, 
			int implicitRecordTtl) throws MalformedURLException{
		if(apiUrl == null) throw new IllegalArgumentException("apiUrl is NULL");
		this.apiUrl = new URL(apiUrl);
		this.apiKey = apiKey;
		this.isCanonical = isCanonical;
		this.apiVersion = apiVersion;
		this.implicitRecordTtl = implicitRecordTtl > 0 ? implicitRecordTtl : DEFAULT_IMPLICIT_RECORD_TTL;
	}
	
	PowerDnsClient(URL apiUrl, String apiKey, String apiVersion, boolean isCanonical, 
			int implicitRecordTtl){
		if(apiUrl == null) throw new IllegalArgumentException("apiUrl is NULL");
		this.apiUrl = apiUrl;
		this.apiKey = apiKey;
		this.isCanonical = isCanonical;
		this.apiVersion = apiVersion;
		this.implicitRecordTtl = implicitRecordTtl > 0 ? implicitRecordTtl : DEFAULT_IMPLICIT_RECORD_TTL;
	}
	
	/**
	 * <p>Vert.x method:</p>
	 * {@inheritDoc}
	 */
	@Override
	public void start(Future<Void> startFuture){	
		LOG.info("PowerDNS API Client started");
		//vbatch.setIDnsClient(this);
		Json.mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
		CompletableFuture<Void>  httpDeployFuture = new CompletableFuture<>();
		CompletableFuture<Void>  batchDeployFuture = new CompletableFuture<>();
		vertx.deployVerticle(vhttp, asyncResult -> {
			if(asyncResult.failed()){
				LOG.warn("Http verticle failed");
				httpDeployFuture.completeExceptionally(asyncResult.cause());
			}else{
				httpDeployFuture.complete(null);
			}	
		});
		vertx.deployVerticle(new BatchVertx(this), asyncResult -> {
			if(asyncResult.failed()){
				LOG.warn("Batch verticle failed");
				batchDeployFuture.completeExceptionally(asyncResult.cause());
			}else{
				batchDeployFuture.complete(null);
				//batchDeployFuture.completeExceptionally(new RuntimeException("Artifical fail"));
			}			
		});
		CompletableFuture.allOf(httpDeployFuture, batchDeployFuture)
			.thenAccept( v -> startFuture.complete())
			.exceptionally( t -> {
				LOG.error("PowerDnsClient failed to start: {}", t.getMessage());
				startFuture.fail(t);
				return null;
			});

		//vertx.eventBus().consumer("api.batch.result", this::batchResultHandler);
	}
	
	public CompletableFuture<String> init(){
		CompletableFuture<String> verticleIdFuture = new CompletableFuture<>();
		if(getVertx() == null){
			LOG.debug("deploy itself to vertx");
			Vertx vertx = Vertx.vertx();
			vertx.deployVerticle(this, asyncResult -> {
				if(asyncResult.failed()){
					LOG.warn("Failed to deploy PowerDnsClient to Vertx");
					verticleIdFuture.completeExceptionally( asyncResult.cause() );
				}else{
					LOG.debug("Self deployed to Vertx");
					verticleIdFuture.complete( asyncResult.result() );
				}
			});			
		}else{
			LOG.warn("Vertx already defined: {}", getVertx());
			verticleIdFuture.complete( getVertx().getOrCreateContext().deploymentID() );
		}
		return verticleIdFuture;
	}
	
	/**
	 * <p>Threads maintained by PowerDnsClient instance are not daemon threads so they will
	 * prevent the JVM from exiting.</p>
	 * <p>If you have finished with it, you can call <code>close</code> to stop it. This will
	 * shut-down all internal thread pools and close other resources, and will allow the JVM to exit.</p>
	 * @return future result of asynchronous closing.
	 */
	public CompletableFuture<Void> close(){
		CompletableFuture<Void> closeFuture = new CompletableFuture<>();
		super.getVertx().close( ar -> {
			if(ar.failed()){
				closeFuture.completeExceptionally(ar.cause());
			}else{
				LOG.info("PowerDNSClient stopped");
				closeFuture.complete(null);
			}
		});
		return closeFuture;
	}
	
	public String getApiVersion(){
		return apiVersion;
	}
	public boolean isCanonical(){
		return isCanonical;
	}
	@Override
	public Optional<Zone> getZoneTemplate(){
		return zoneTemplate;
	}
	
	private CompletableFuture<Map<String, DnsServer>> getServerApi(){
		if(hasApi.compareAndSet(false, true)){
			//batchExecutor.execute( () -> {
				getApi()
					.thenApply( versions -> {
						Optional<ApiVersion> myVersion = versions.stream()
								.filter( apiVer -> apiVer.getVersion().equals(apiVersion) ? true : false)
								.findFirst();
						if(myVersion.isPresent()) return myVersion.get();
						else throw new PowerDnsApiException("API version "+apiVersion+" is not found");
					})
					.thenApply( apiVersion -> {									
						try{
							URI uri = apiUrl.toURI().resolve(apiVersion.getUrl()+"/"+ SERVERS_API_PATH);
							return uri.toURL();
						}catch (URISyntaxException | MalformedURLException e) {
							throw new PowerDnsApiException(e.getMessage());
						}
					})
					.thenCompose( url -> {
						return vhttp.request(HttpMethod.GET, url, Optional.of(headers), Optional.empty());
					})
					.thenAccept( rsp -> {
						List<DnsServer> servers = Arrays.asList(Json.decodeValue(rsp.bodyAsString(), DnsServer[].class));
						Map<String, DnsServer> serverMap =
								servers.stream().collect(Collectors.toMap(DnsServer::getId, dnsServer -> dnsServer));
						serverApi.complete(serverMap);
					});
			//});
			/*
				.thenCompose( versions -> {
					Optional<ApiVersion> myVersion = versions.stream()
						.filter( apiVer -> apiVer.getVersion().equals(apiVersion) ? true : false)
						.findFirst();
					if(myVersion.isPresent()){
						try {
							URI serversUri = apiUrl.toURI().resolve(myVersion.get().getUrl()+"/"+ SERVERS_API_PATH);
							return vhttp.request(HttpMethod.GET, serversUri.toURL(), Optional.of(headers), Optional.empty());
						} catch (URISyntaxException | MalformedURLException e) {
							throw new PowerDnsApiException(e.getMessage());
						}
					}else{
						throw new PowerDnsApiException("API version "+apiVersion+" is not found");
					}
				}).thenAccept( rsp -> {
					List<DnsServer> servers = Arrays.asList(Json.decodeValue(rsp.bodyAsString(), DnsServer[].class));
					Map<String, DnsServer> serverMap =
							servers.stream().collect(Collectors.toMap(DnsServer::getId, dnsServer -> dnsServer));
					serverApi.complete(serverMap);
				});
			*/
		}
		return serverApi;
	}
	
	@Override
	public CompletableFuture<Collection<ApiVersion>> getApi(){
		headers.put(apiKeyHeader != null ? apiKeyHeader : DEFAULT_API_KEY_HEADER, apiKey);
		headers.put("Content-Type", "application/json");
		LOG.trace("DnsClient getApi() from {}", apiUrl);
		return vhttp.request(HttpMethod.GET, apiUrl, Optional.of(headers), Optional.empty())
			.thenApply( resp -> Arrays.asList(Json.decodeValue(resp.bodyAsString(), ApiVersion[].class)));
	}
	
	public CompletableFuture<Collection<ZoneMetainfo>> getZones(){
		return getZones(DEFAULT_SERVER);
	}
	public CompletableFuture<Collection<ZoneMetainfo>> getZones(String serverId){
		return getServerApi()
			.thenApply( serverMap -> {
				if(!serverMap.containsKey(serverId)){
					throw new IllegalArgumentException("DNS server with ID ["+serverId+"] is unknown to PowerDNS API."
							+"Actual servers: "+serverMap.keySet().toString());
				}
				DnsServer dnsServer = serverMap.get(serverId);
				try{
					URL zonesApiUrl = apiUrl.toURI()
							.resolve(PowerDnsUtils.getUrlFromPattern(dnsServer.getZones_url()))
							.toURL();
					return zonesApiUrl;
				}catch(MalformedURLException | URISyntaxException e){
					throw new PowerDnsApiException(e.getMessage());
				}
			})
			.thenCompose( zonesApiUrl -> 
				vhttp.request(HttpMethod.GET, zonesApiUrl, Optional.of(headers), Optional.empty())
			)
			.thenApply( resp -> 
				Arrays.asList(Json.decodeValue(resp.bodyAsString(), ZoneMetainfo[].class))
			);
		/*
		CompletableFuture<Collection<ZoneMetainfo>> zonesFuture = new CompletableFuture<>();
		try {
			Map<String, DnsServer> serverMap = getServerApi().get(10, TimeUnit.SECONDS);
			if(!serverMap.containsKey(serverId)){
				throw new IllegalArgumentException("DNS server with ID ["+serverId+"] is unknown to PowerDNS API."
						+"Actual servers: "+serverMap.keySet().toString());
			}
			DnsServer dnsServer = serverMap.get(serverId);
			URL zonesApiUrl = apiUrl.toURI()
					.resolve(PowerDnsUtils.getUrlFromPattern(dnsServer.getZones_url()))
					.toURL();
			
			System.out.println("Querying for zones : " + zonesApiUrl);
			return vhttp.request(HttpMethod.GET, zonesApiUrl, Optional.of(headers), Optional.empty())
				.thenApply( resp -> {
					return Arrays.asList(Json.decodeValue(resp.bodyAsString(), ZoneMetainfo[].class));
				});
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			zonesFuture.completeExceptionally(e);
		} catch (MalformedURLException | URISyntaxException e) {
			zonesFuture.completeExceptionally(e);
		}
		return zonesFuture;
		*/
	}
	

	@Override
	public CompletableFuture<Zone> getZone(String serverId, String zoneId){	
		//CompletableFuture.supplyAsync( () -> {
			return getServerApi()
				.thenApply( serverMap -> {
					if(!serverMap.containsKey(serverId)){
						throw new IllegalArgumentException("DNS server with ID ["+serverId+"] is unknown to PowerDNS API."
								+"Actual servers: "+serverMap.keySet().toString());
					}
					DnsServer dnsServer = serverMap.get(serverId);
					try {
						URL zoneApiUrl = apiUrl.toURI()
								.resolve(PowerDnsUtils.getUrlFromPattern(dnsServer.getZones_url(), zoneId))
								.toURL();
						return zoneApiUrl;
					} catch (MalformedURLException | URISyntaxException e) {
						throw new RuntimeException(e);
					} 
				}).thenCompose( url -> {
					return vhttp.request(HttpMethod.GET, url, Optional.of(headers), Optional.empty());
				}).thenApply( resp -> {
					int statusCode = resp.statusCode();
					if(statusCode >=200 && statusCode < 400){
						String jsonZone = resp.bodyAsString();
						try{
							return Json.decodeValue(jsonZone, Zone.class);
						}catch(DecodeException de){
							throw new PowerDnsApiException(de.getMessage());
						}
					}else{
						throw new PowerDnsApiException(statusCode+": getZone("+serverId+","+zoneId+") : "+resp.bodyAsJsonObject().getString("error"));
					}
				});
		//});
		/*
		try{
			Map<String, DnsServer> serverMap = getServerApi().get(10, TimeUnit.SECONDS);
			if(!serverMap.containsKey(serverId)){
				throw new IllegalArgumentException("DNS server with ID ["+serverId+"] is unknown to PowerDNS API."
						+"Actual servers: "+serverMap.keySet().toString());
			}
			DnsServer dnsServer = serverMap.get(serverId);
			URL zoneApiUrl = apiUrl.toURI()
					.resolve(PowerDnsUtils.getUrlFromPattern(dnsServer.getZones_url(), zoneId))
					.toURL();
			//LOG.trace("Querying for zone : " + zoneApiUrl);
			return vhttp.request(HttpMethod.GET, zoneApiUrl, Optional.of(headers), Optional.empty())
						.thenApply( resp -> {
							int statusCode = resp.statusCode();
							if(statusCode >=200 && statusCode < 400){
								String jsonZone = resp.bodyAsString();
								try{
									return Json.decodeValue(jsonZone, Zone.class);
								}catch(DecodeException de){
									throw new PowerDnsApiException(de.getMessage());
								}
							}else{
								throw new PowerDnsApiException(statusCode+": getZone("+serverId+","+zoneId+") : "+resp.bodyAsJsonObject().getString("error"));
							}
						});			
		}catch (Exception e){
			CompletableFuture<Zone> cf = new CompletableFuture<>();
			cf.completeExceptionally(e);
			return cf;
		}
		*/
	}
	@Override
	public CompletableFuture<Zone> getZone(String zoneId){
		return getZone(DEFAULT_SERVER, zoneId);
	}
	
	@Override
	public CompletableFuture<Zone> createZone(String serverId, Zone zone){
		return getServerApi()
			.thenApply( serverMap -> {
				if(!serverMap.containsKey(serverId)){
					throw new IllegalArgumentException("DNS server with ID ["+serverId+"] is unknown to PowerDNS API."
							+"Actual servers: "+serverMap.keySet().toString());
				}
				DnsServer dnsServer = serverMap.get(serverId);
				try{
					return apiUrl.toURI()
							.resolve(PowerDnsUtils.getUrlFromPattern(dnsServer.getZones_url()))
							.toURL();
				}catch(MalformedURLException | URISyntaxException e){
					throw new PowerDnsApiException(e.getMessage());
				}
			})
			.thenCompose( zonesApiUrl -> {
				JsonObject json = JsonObject.mapFrom(zone);
				return vhttp.request(HttpMethod.POST, zonesApiUrl, Optional.of(headers), Optional.ofNullable(json));
			})
			.thenApply( resp -> {
				int statusCode = resp.statusCode();
				if(statusCode >=200 && statusCode < 400){
					String zoneJson = resp.bodyAsString();
					//System.out.println("RESULT ZONE: "+zoneJson);
					try{
						return Json.decodeValue(zoneJson, Zone.class);
					}catch(DecodeException de){
						throw new PowerDnsApiException(de.getMessage());
					}
				}else{
					throw new PowerDnsApiException(statusCode+": "+resp.bodyAsJsonObject().getString("error"));
				}
			});
		/*
		try{
			Map<String, DnsServer> serverMap = getServerApi().get(10, TimeUnit.SECONDS);
			if(!serverMap.containsKey(serverId)){
				throw new IllegalArgumentException("DNS server with ID ["+serverId+"] is unknown to PowerDNS API."
						+"Actual servers: "+serverMap.keySet().toString());
			}
			DnsServer dnsServer = serverMap.get(serverId);
			URL zonesApiUrl = apiUrl.toURI()
					.resolve(PowerDnsUtils.getUrlFromPattern(dnsServer.getZones_url()))
					.toURL();
			JsonObject json = JsonObject.mapFrom(zone);
			//System.out.println("Querying for zones : " + zonesApiUrl + " with " + json.encode());
			return vhttp.request(HttpMethod.POST, zonesApiUrl, Optional.of(headers), Optional.ofNullable(json))
						.thenApply( resp -> {
							int statusCode = resp.statusCode();
							if(statusCode >=200 && statusCode < 400){
								String zoneJson = resp.bodyAsString();
								//System.out.println("RESULT ZONE: "+zoneJson);
								try{
									return Json.decodeValue(zoneJson, Zone.class);
								}catch(DecodeException de){
									throw new PowerDnsApiException(de.getMessage());
								}
							}else{
								throw new PowerDnsApiException(statusCode+": "+resp.bodyAsJsonObject().getString("error"));
							}
						});
		}catch (Exception e){
			CompletableFuture<Zone> cf = new CompletableFuture<>();
			cf.completeExceptionally(e);
			return cf;
		}
		*/
	}
	@Override
	public CompletableFuture<Zone> createZone(Zone zone){
		return createZone(DEFAULT_SERVER, zone);
	}
	
	@Override
	public CompletableFuture<Collection<String>> patchZone(String zoneId, ZonePatch zonePatch){
		return patchZone(DEFAULT_SERVER, zoneId, zonePatch);
	}
	
	@Override
	public CompletableFuture<Collection<String>> patchZone(String serverId, String zoneId, ZonePatch zonePatch){
		return getServerApi()
			.thenApply( serverMap -> {
				if(!serverMap.containsKey(serverId)){
					throw new IllegalArgumentException("DNS server with ID ["+serverId+"] is unknown to PowerDNS API."
							+"Actual servers: "+serverMap.keySet().toString());
				}
				DnsServer dnsServer = serverMap.get(serverId);
				try{
					return apiUrl.toURI()
						.resolve(PowerDnsUtils.getUrlFromPattern(dnsServer.getZones_url(), zoneId))
						.toURL();
				}catch(MalformedURLException | URISyntaxException e){
					throw new PowerDnsApiException(e.getMessage());
				}
			})
			.thenCompose( zoneApiUrl -> {
				JsonObject json = JsonObject.mapFrom(zonePatch);				
				LOG.trace("PATCHING ZONE {}", zoneId);
				return vhttp.request(HttpMethod.PATCH, zoneApiUrl, Optional.of(headers), Optional.ofNullable(json));
			})
			.thenApply( resp -> {
				int statusCode = resp.statusCode();
				if(statusCode >=200 && statusCode < 400){
					LOG.trace("Zone {} patched", zoneId);
					return null;
				}else{
					throw new PowerDnsApiException(statusCode+": "+resp.bodyAsJsonObject().getString("error"));
				}
			})
			.thenCompose( v -> getZone(zoneId))
			.thenApply( zone -> {					
				Set<String> zoneNs = zone.getRrsets().stream()
					.filter( rrset -> rrset.getType().equalsIgnoreCase("NS"))
					.map( rrset -> rrset.getRecords())
					.flatMap( recs -> recs.stream() )
					.filter( rec -> !rec.isDisabled())
					.map( rec -> rec.getContent())
					.collect( Collectors.toSet() );
				LOG.trace("NS for zone {}: {}", zoneId, zoneNs);
				return zoneNs;
			});
		/*
		try{
			Map<String, DnsServer> serverMap = getServerApi().get(10, TimeUnit.SECONDS);
			if(!serverMap.containsKey(serverId)){
				throw new IllegalArgumentException("DNS server with ID ["+serverId+"] is unknown to PowerDNS API."
						+"Actual servers: "+serverMap.keySet().toString());
			}
			DnsServer dnsServer = serverMap.get(serverId);
			URL zoneApiUrl = apiUrl.toURI()
					.resolve(PowerDnsUtils.getUrlFromPattern(dnsServer.getZones_url(), zoneId))
					.toURL();
			JsonObject json = JsonObject.mapFrom(zonePatch);
			
			LOG.trace("PATCHING ZONE {}", zoneId);
			CompletableFuture<HttpResponse<Buffer>> respFuture = 
					vhttp.request(HttpMethod.PATCH, zoneApiUrl, Optional.of(headers), Optional.ofNullable(json));
			
			//respFuture.exceptionally( ex -> {
			//	throw new PowerDnsApiException("Vert.x Web-client exception " + ex.getMessage());
			//});
			
			//return 
			CompletableFuture<Void> patchFuture = respFuture.thenApply( resp -> {
					int statusCode = resp.statusCode();
					if(statusCode >=200 && statusCode < 400){
						LOG.trace("Zone {} patched", zoneId);
						return null;
					}else{
						//System.err.println(statusCode+": "+zoneApiUrl + " with " + json.encode());
						throw new PowerDnsApiException(statusCode+": "+resp.bodyAsJsonObject().getString("error"));
					}
				});

			return patchFuture
				.thenCompose( v -> this.getZone(zoneId))
				.thenApply( zone -> {					
					Set<String> zoneNs = zone.getRrsets().stream()
						.filter( rrset -> rrset.getType().equalsIgnoreCase("NS"))
						.map( rrset -> rrset.getRecords())
						.flatMap( recs -> recs.stream() )
						.filter( rec -> !rec.isDisabled())
						.map( rec -> rec.getContent())
						.collect( Collectors.toSet() );
					LOG.trace("NS for zone {}: {}", zoneId, zoneNs);
					return zoneNs;
				});			
			
		}catch (Exception e){
			LOG.error("PATCH failed:", e);
			CompletableFuture<Collection<String>> cf = new CompletableFuture<>();
			cf.completeExceptionally(e);
			return cf;
		}
		*/
	}
	@Override
	public CompletableFuture<String> deleteZone(String serverId, String zoneId){
		return getServerApi()
			.thenApply( serverMap -> {
				if(!serverMap.containsKey(serverId)){
					throw new IllegalArgumentException("DNS server with ID ["+serverId+"] is unknown to PowerDNS API."
							+"Actual servers: "+serverMap.keySet().toString());
				}
				DnsServer dnsServer = serverMap.get(serverId);
				try{
					return apiUrl.toURI()
						.resolve(PowerDnsUtils.getUrlFromPattern(dnsServer.getZones_url(), zoneId))
						.toURL();
				}catch(MalformedURLException | URISyntaxException e){
					throw new PowerDnsApiException(e.getMessage());
				}
			})
			.thenCompose( zoneApiUrl -> vhttp.request(HttpMethod.DELETE, zoneApiUrl, Optional.of(headers), Optional.empty()))
			.thenApply( resp -> {
				int statusCode = resp.statusCode();
				if(statusCode >=200 && statusCode < 400){
					return zoneId;
				}else{
					throw new PowerDnsApiException(statusCode+": "+resp.bodyAsJsonObject().getString("error"));
				}
			});
		/*
		try{
			Map<String, DnsServer> serverMap = getServerApi().get(10, TimeUnit.SECONDS);
			if(!serverMap.containsKey(serverId)){
				throw new IllegalArgumentException("DNS server with ID ["+serverId+"] is unknown to PowerDNS API."
						+"Actual servers: "+serverMap.keySet().toString());
			}
			DnsServer dnsServer = serverMap.get(serverId);
			URL zoneApiUrl = apiUrl.toURI()
					.resolve(PowerDnsUtils.getUrlFromPattern(dnsServer.getZones_url(), zoneId))
					.toURL();
			CompletableFuture<HttpResponse<Buffer>> respFuture = 
					vhttp.request(HttpMethod.DELETE, zoneApiUrl, Optional.of(headers), Optional.empty());
			
			return respFuture.thenApply( resp -> {
				int statusCode = resp.statusCode();
				if(statusCode >=200 && statusCode < 400){
					return zoneId;
				}else{
					//System.err.println(statusCode+": "+zoneApiUrl + " with " + json.encode());
					throw new PowerDnsApiException(statusCode+": "+resp.bodyAsJsonObject().getString("error"));
				}
			});
			
		}catch (Exception e){
			System.err.println("PATCH failed: " + e.getMessage());
			CompletableFuture<String> cf = new CompletableFuture<>();
			cf.completeExceptionally(e);
			return cf;
		}
		*/
	}
	@Override
	public CompletableFuture<String> deleteZone(String zoneId){
		return deleteZone(DEFAULT_SERVER, zoneId);
	}
	//////////////////////////BATCH methods ////////////////////
	private AtomicLong hostCounter = new AtomicLong();
	private AtomicLong batchCounter = new AtomicLong();
	private ConcurrentHashMap<Long, CompletableFuture<Collection<String>>> hostnameRegistrationMap = new ConcurrentHashMap<>();
	//private ExecutorService executorService = Executors.newFixedThreadPool(3);
	@Override
	public CompletableFuture<Collection<String>> addRecordToBatch(ChangeType action, 
			String hostname, 
			Collection<String> records,
			String recordType,
			int ttl){

		String canonicalHostname = PowerDnsUtils.toCanonical(hostname);
		LOG.trace("Adding batch record {}", canonicalHostname);
		CompletableFuture<Collection<String>> hostnameRegistrationResult = new CompletableFuture<>();
		//batchExecutor.execute( () -> {
			try{
				String zoneId = PowerDnsUtils.getZone(canonicalHostname);			
				Long hostnameFutureRegistrationId = hostCounter.incrementAndGet();
				ZonePatchFuture zp = new ZonePatchFuture();
				zp.setFutureResultId(hostnameFutureRegistrationId);
				zp.setZoneId(zoneId);
				
				RRset rrset = new RRset();
				rrset.setChangetype(action);
				rrset.setName(canonicalHostname);
				List<Record> recList = records.stream().map( rec -> new Record(rec)).collect(Collectors.toList() );
				rrset.setRecords( recList );
				rrset.setType(recordType);
				int recordTtl = ttl < 0 ? implicitRecordTtl : ttl;
				rrset.setTtl(recordTtl);
				zp.setRrsets(Arrays.asList(rrset));
				hostnameRegistrationMap.put(hostnameFutureRegistrationId, hostnameRegistrationResult);
				String json = Json.encode(zp);
				vertx.eventBus().send(BatchVertx.ADD_HOSTNAME_TOPIC, Json.encode(zp));
				LOG.trace("{}: {}", BatchVertx.ADD_HOSTNAME_TOPIC, json);
			}catch(Throwable t){
				if(LOG.isTraceEnabled()) LOG.error(t);
				hostnameRegistrationResult.completeExceptionally(t);
			}
		//});
		return hostnameRegistrationResult;
		
	}
	@Override
	public CompletableFuture<Collection<String>> addRecordToBatch(String hostname, String ipAddress){
		return addRecordToBatch(ChangeType.REPLACE, hostname, Arrays.asList(ipAddress), "A", implicitRecordTtl);		
	}
	/*
	protected void batchResultHandler(Message<? extends String> message){
		List<HostnameBatchResult> results = Arrays.asList(Json.decodeValue(message.body(), HostnameBatchResult[].class));
		results.stream().forEach( batchResult -> {
			CompletableFuture<Collection<String>> resultFuture = 
					hostnameRegistrationMap.remove(batchResult.getHostnameResultFutureid());
			if(batchResult.isSuccedded()){				
				resultFuture.complete(batchResult.getNs());
			}else{
				resultFuture.completeExceptionally(new PowerDnsApiException(batchResult.getErrorMessage()));
			}
		});
	}
	*/
	public CompletableFuture<Collection<String>> doBatchJob(){
		CompletableFuture<Collection<String>> cf = new CompletableFuture<>();
		//batchExecutor.execute(() -> {
			long batchId = batchCounter.incrementAndGet();
			//LOG.trace("Starting Batch job in thread : {}", Thread.currentThread().getName());
			vertx.eventBus().<String>send(BatchVertx.BATCH_RESULTS_QUEUE, batchId, result -> {
				//LOG.trace("RESPONSE from BATCH_RESULTS_QUEUE");
				handleBatchResult(result, cf);
			});
		//});		
		return cf;
	}
	
	private void handleBatchResult(AsyncResult<Message<String>> result, CompletableFuture<Collection<String>> batchFuture){
		Message<String> message = result.result();
		LOG.trace("Batch result: {}", result.failed() ? result.cause() : result.result().body());
		List<HostnameBatchResult> results = Arrays.asList(Json.decodeValue(message.body().toString(), HostnameBatchResult[].class));
		List<String> batchResultList = new LinkedList<>(); 
		results.stream().forEach( hostnameBatchResult -> {
			CompletableFuture<Collection<String>> resultFuture = hostnameRegistrationMap.remove(hostnameBatchResult.getHostnameResultFutureid());			
			if(hostnameBatchResult.isSuccedded()){				
				resultFuture.complete(hostnameBatchResult.getNs());
			}else{
				resultFuture.completeExceptionally(new PowerDnsApiException(hostnameBatchResult.getErrorMessage()));
			}
			List<String> rrnames = hostnameBatchResult.getRrset().stream().map( rrset -> rrset.getName()).collect( Collectors.toList() );
			batchResultList.addAll(rrnames);			
		});
		batchFuture.complete(batchResultList);
	}
	
}
