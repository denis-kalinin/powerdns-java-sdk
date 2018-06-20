# PowerDNS client (Java)

## How to install
Later, when it will be deployd on Maven Central:

```xml
<dependency>
	<group>com.itranga</group>
	<artifact>powerdns-client</artifact>
</depenency>
```
but now - you have to clone the project and compile it with `maven`.


## How to patch multiple records in a zone

Here is an example in the **imperative style**, although the functional one would be more appropriate:

```java

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
			.setApiUrl("https://ns1api.exmple.net/api")
			.setDefaultNS("ns1.example.net", "ns2.example.net")
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


```

## Under the hood
PowerDNS client is using [Vert.x web-client][vertx-web-client] and it is actually a [Verticle][verticle]
itself&mdash;an actor, as defined in [Actor Model][actor-model-wiki].

If you are unaware how to deploy verticles to Vert.x, then just call `powerDnsClient.init().join()` method and when done&mdash;`powerDnsClient.close().join()`.

## Plans
- Add OSGi support
- Better support for errors
- integration tests
- deploy to Maven Central



[vertx-web-client]: http://vertx.io/docs/vertx-web/java/
[verticle]: http://vertx.io/docs/apidocs/io/vertx/core/Verticle.html
[actor-model-wiki]: https://en.wikipedia.org/wiki/Actor_model

