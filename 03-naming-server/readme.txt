## 03 naming-server
This is a service registry which uses Eureka server. 
<br>
<b><u>Eureka Server</u></b> is a service registry that plays a central role in the automatic detection of devices and services on a network. It acts as the heart of your microservices ecosystem, allowing service 
instances to register themselves and facilitating service discovery. Key aspects of Eureka Server include:

<u>Client Registration</u>: Instances of microservices automatically register themselves with Eureka Server.
<u>Service Discovery</u>: Eureka Server maintains a registry of all client applications running on different ports and IP addresses.
<br>
<b>Benefits of using Eureka Server in Spring Boot Applications</b>
Eureka Server operates on a simple “Register, Lookup, Connect” principle, making it an excellent choice for managing microservices in a Spring Boot environment. Here are some compelling reasons to use Eureka Server:

<ol type="1">
<li>Centralized Service Registry: Eureka Server knows about all client applications and their locations. This centralization simplifies service discovery.</li>
<li>Automatic Registration: Microservices automatically register themselves with Eureka Server, reducing manual configuration efforts.</li>
<li>Load Balancing: Eureka Server can help implement load balancing among service instances.</li>
<li>Health Checks: Eureka Server can perform health checks on registered services, ensuring robustness and reliability.</li>
<li>Integration with Spring Cloud: Eureka Server seamlessly integrates with the Spring Cloud ecosystem, enabling easy scaling and deployment.</li>
</ol>


### Step 01] Setting up naming-server
#### Dependencies used in pom.xml
1) spring-boot-devtools
2) spring-boot-starter-actuator
3) spring-cloud-starter-config
4) spring-cloud-starter-netflix-eureka-server
	
#### application.properties
```java
spring.application.name=naming-server
server.port=8761

#to prevent eureka server from registering with itself
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false

#to prevent below error add this line. suggestion is provided by spring boot itself.
spring.config.import=optional:configserver:http://localhost:8888

```

```java
	ERROR org.springframework.boot.diagnostics.LoggingFailureAnalysisReporter
	
	Description:
	No spring.config.import property has been defined
	
	Action:
	Add a spring.config.import=configserver: property to your configuration.
		If configuration is not required add spring.config.import=optional:configserver: instead.
		To disable this check, set spring.cloud.config.enabled=false or 
		spring.cloud.config.import-check.enabled=false.

```

#### Application.java
```java
@SpringBootApplication
@EnableEurekaServer
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
```
To avoid below warning

	Standard Commons Logging discovery in action with spring-jcl: please remove commons-logging.jar from classpath in order to avoid potential conflicts

Add following exception in pom.xml
	
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
			<exclusions>
					<exclusion>
						<groupId>commons-logging</groupId>
						<artifactId>commons-logging</artifactId>
					</exclusion>
				</exclusions>
		</dependency>

```java
http://localhost:8761/
```


### Step 02] Registering 'currency-exchange-service' and 'currency-conversion-service' with naming-server (Eureka server)
#### pom.xml  
add following dependency in pom.xml of both currency-exchange-service and currency-conversion-service microservice

	spring-cloud-starter-netflix-eureka-client 

go to below url and under instances you will observe that <b>currency-exchange-service</b> and <b>currency-conversion-service</b> are registered with eureka.
	
	http://localhost:8761

Just to be safe also configure url in application.properties of both microservices
	
#### application.properties currency-exchange-service

	eureka.client.service-url.defaultZone=http://localhost:8761/eureka

#### application.properties currency-conversion-service

	eureka.client.service-url.defaultZone=http://localhost:8761/eureka


### Step 03] Load Balancing currency-exchange-service instances
#### CurrencyExchangeProxy.java
remove url from @FeignClient and it automatically picks up instances due to Eureka serves.
Eureka server acts as service registry which means all instances are registered with eureka server. And it works on 'Register, Lookup, Connect' principle.

```java
//@FeignClient(name="currency-exchange", url="localhost:8000")
@FeignClient(name="currency-exchange")

public interface CurrencyExchangeProxy {
	// We get this method from CurrencyExchangeController.
	@GetMapping("/currency-exchange/from/{from}/to/{to}")
	public CurrencyConversion retrieveExchangeValue(
			@PathVariable String from,
			@PathVariable String to);
	
}
```

<hr><hr>
To implement a common feature across multiple microservices use API Gateway.
Instead of implementing these features in each microservice we implement it in a single microservice through which all calls are passed.

<b><u>API Gateway</u></b>
Before calling any microservice we can route the request through an API Gateway. 
In simple terms, we prefer a component between client and server to manage all API requests called API Gateway. 
It's features include:

<ol>
<li>Security — Authentication, authorization</li>
<li>Routing — routing, request/response manipulation, circuit breaker</li>
<li>Observability — metric aggregation, logging, tracing</li>
</ol>

Architectural benefits of API Gateway:
<ol>
<li>Reduced complexity</li>
<li>Centralized control of policies</li>
<li>Simplified troubleshooting</li>
</ol>


<hr> <hr>
There are many types of implementations available for API Gateway which include — Spring Cloud Gateway, Zuul API Gateway, APIGee, EAG (Enterprise API Gateway)
We are using Spring Cloud Gateway.

*****************












