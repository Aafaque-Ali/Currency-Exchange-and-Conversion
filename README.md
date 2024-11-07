# Currency-Exchange-and-Conversion
Implementing microservices using Spring Cloud. This project contains two main microservices: 
<hr>

## 01 currency-exchange-service
This is an independent microservice.

### Step 01] Setting up currency-exchange-service
#### Dependencies used in pom.xml
	1) spring-boot-starter-web
	2) spring-boot-devtools
	3) spring-boot-starter-actuator
	4) spring-cloud-starter-config
		This enables us to talk to spring-cloud-config-server
	5) lombok
	
#### application.properties
```java
spring.application.name=currency-exchange
server.port=8000
spring.config.import=optional:configserver:http://localhost:8888
```

### Step 02] Creating RestController
#### CurrencyExchangeController.java
```java
@RestController
public class CurrencyExchangeController {
	@GetMapping("/currency-exchange/from/{from}/to/{to}")
	public CurrencyExchange retrieveExchangeValue(
			@PathVariable String from,
			@PathVariable String to) {
	
		return new CurrencyExchange(100L, "USD", "INR", BigDecimal.valueOf(40), "");
	}
}
```

#### CurrencyExchange.java
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyExchange {

	private Long id;
	private String from;
	private String to;
	private BigDecimal conversionMultiple;
	private String environment;
	
}
```

### Step 03] Getting port from Environment
#### CurrencyExchangeController.java
```java
import org.springframework.core.env.Environment;

@RestController
public class CurrencyExchangeController {

	@Autowired
	private Environment environment;
	
	@GetMapping("/currency-exchange/from/{from}/to/{to}")
	public CurrencyExchange retrieveExchangeValue(
			@PathVariable String from,
			@PathVariable String to) {
	
		String port = environment.getProperty("local.server.port");
		return new CurrencyExchange(100L, "USD", "INR", BigDecimal.valueOf(40), port);
	}
}
```

### Step 04] Launching this microservice on port 8001
<ol>
<li> Below file bar, we have options to run, stop and re-run application. 
<br>	Dropdown from run and select 'Run Configurations'.</li>
<li> Under Spring Boot Application/ Java Application -> Go to '01-currency-exchange-service - Application'.
<br>	You will be shown name of application. Change name from '01-currency-exchange-service - Application' 
<br>	to 'CurrencyExchangeServiceApplication8000' -> click Apply. </li>

```java
http://localhost:8000/currency-exchange/from/USD/to/INR
```
<li> After that right click on LHS CurrencyExchangeServiceApplication8000 and duplicate it.</li>
<li> Change its name to 'CurrencyExchangeServiceApplication8001' -> click 'Arguments tab' under <b>VM arguments</b> write: 
	
	-Dserver.port=8001 
  	
->	click Apply -> click Run.</li>

```java
http://localhost:8001/currency-exchange/from/USD/to/INR
```


### Step 05] Connecting to H2 Database
#### pom.xml
add new dependency
	
 	6) spring-boot-starter-data-jpa
	7) h2
	
#### application.properties
```java
#add below new lines
spring.jpa.show-sql=true
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true
```

#### CurrencyExchange.java
// add @Entity, @Id annotations
```java
@Entity
public class CurrencyExchange {

	@Id
	private Long id;
	@Column(name = "currency_from")
	private String from;
	@Column(name = "currency_to")
	private String to;
	private BigDecimal conversionMultiple;
	private String environment;
```

/*
below error is caused because we are using 'from' as column name. 'from' is also an SQl command. To avoid below error change column name as shown above

	Caused by: org.h2.jdbc.JdbcSQLSyntaxErrorException: Syntax error in SQL statement "create table currency_exchange 
	(conversion_multiple numeric(38,2), id bigint not null, environment varchar(255), [*]from varchar(255), to varchar(255), primary key (id))"; 
	expected "identifier"; SQL statement:
	create table currency_exchange (conversion_multiple numeric(38,2), id bigint not null, environment varchar(255), from varchar(255), to varchar(255), primary key (id)) [42001-224]
*/
```java
http://localhost:8000/h2-console
```

### Step 06] Inserting data into table
create a file named data.sql inside src/main/resources
#### data.sql
```java
insert into currency_exchange
(id, currency_from, currency_to, conversion_multiple, environment)
values(1001,'USD','INR',65,'');
insert into currency_exchange
(id, currency_from, currency_to, conversion_multiple, environment)
values(1002,'EUR','INR',75,'');
insert into currency_exchange
(id, currency_from, currency_to, conversion_multiple, environment)
values(1003,'AUD','INR',25,'');
```

From Spring Boot 2.4+ Loading of data.sql is done before the tables are created. To rectify this add following in application.properties
#### application.properties
```java
spring.jpa.defer-datasource-initialization=true
```

### Step 07] Fetching values from database
#### CurrencyExchangeRepository.java
```java
public interface CurrencyExchangeRepository extends JpaRepository<CurrencyExchange, Long> {
}
```

#### CurrencyExchangeController.java
```java
@RestController
public class CurrencyExchangeController {

	@Autowired
	private CurrencyExchangeRepository repository;
	
	@Autowired
	private Environment environment;
	
	@GetMapping("/currency-exchange/from/{from}/to/{to}")
	public CurrencyExchange retrieveExchangeValue(
			@PathVariable String from,
			@PathVariable String to) {
	
		CurrencyExchange currencyExchange = repository.findByFromAndTo(from, to);
		if(currencyExchange==null) {
			throw new RuntimeException("Unable to find data for " + from + " to " + to);
		}
		
		String port = environment.getProperty("local.server.port");	
		currencyExchange.setEnvironment(port);

		return currencyExchange;
	}
}
```
<hr><hr>

## 02 currency-conversion-service
This microservice is dependent on above currency-exchange-service.

### Step 01] Setting up currency-conversion-service
#### Dependencies used in pom.xml
	1) spring-boot-starter-web
	2) spring-boot-devtools
	3) spring-boot-starter-actuator
	4) spring-cloud-starter-config
		This enables us to talk to spring-cloud-config-server
	5) lombok

#### application.properties
```java
spring.application.name=currency-conversion
server.port=8100
spring.config.import=optional:configserver:http://localhost:8888
```

### Step 02] Creating RestController
#### CurrencyConversionController.java
```java
@RestController
public class CurrencyConversionController {

	@GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
	public CurrencyConversion calculateCurrencyConversion(
			@PathVariable String from,
			@PathVariable String to,
			@PathVariable BigDecimal quantity) {
		return new CurrencyConversion(100L, from, to, quantity, BigDecimal.ONE, BigDecimal.TEN, "");
	}
	
}
```

#### CurrencyConversion.java
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyConversion {
	private Long id;
	private String from;
	private String to;
	private BigDecimal quantity;
	private BigDecimal conversionMultiple;
	private BigDecimal totalCalculatedAmount;
	private String environment;

}
```

### Step 03] Getting details from 'currency-exchange' microservice
#### CurrencyExchangeController.java
```java
@RestController
public class CurrencyConversionController {

	@GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
	public CurrencyConversion calculateCurrencyConversion(
			@PathVariable String from, 
			@PathVariable String to,
			@PathVariable BigDecimal quantity) {

		//uriVariable is used to pass values of 'from' and 'to'
		HashMap<String, String> uriVariables = new HashMap<>();
		uriVariables.put("from", from);
		uriVariables.put("to", to);
		
		// used to make RestApi calls
		// We are able to use 'CurrencyConversion.class' because structure of CurrencyConversion.java 
		// matches structure of CurrencyExchange.java
		ResponseEntity<CurrencyConversion> responseEntity = new RestTemplate().getForEntity("http://localhost:8000/currency-exchange/from/{from}/to/{to}",
				CurrencyConversion.class, uriVariables);
		CurrencyConversion currencyConversion = responseEntity.getBody();

		return new CurrencyConversion(
					currencyConversion.getId(), 
					from, 
					to, 
					quantity, 
					currencyConversion.getConversionMultiple(), 
					quantity.multiply(currencyConversion.getConversionMultiple()), 
					currencyConversion.getEnvironment() + " rest template");
	}

}
```

```java
http://localhost:8100/currency-conversion/from/USD/to/INR/quantity/10
```
/* Take note that we do not have to connect currency-conversion to database.We only connect 
   currency-conversion to currency-exchange microservice, and that in turn gets value from database.
*/


In above step there is lots of broiler plate code for connecting @pathVariable to other microservice. To reduce this we use Feign framework
### Step 4] Connecting with feign
#### pom.xml
	6) spring-cloud-starter-openfeign

#### Application.java
```java
@SpringBootApplication
@EnableFeignClients
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
```

// create a proxy to talk to currency-exchange from currency-conversion
Note: This is an interface
#### CurrencyExchangeProxy.java
```java
@FeignClient(name="currency-exchange", url="localhost:8000")
public interface CurrencyExchangeProxy {

	// We get this method from CurrencyExchangeController.
	@GetMapping("/currency-exchange/from/{from}/to/{to}")
	public CurrencyConversion retrieveExchangeValue(
			@PathVariable String from,
			@PathVariable String to);
	
}
```

#### CurrencyConversionController.java
```java
@RestController
public class CurrencyConversionController {
	
	@Autowired
	CurrencyExchangeProxy proxy;

	@GetMapping("/currency-conversion-feign/from/{from}/to/{to}/quantity/{quantity}")
	public CurrencyConversion calculateCurrencyConversionFeign(
			@PathVariable String from, 
			@PathVariable String to,
			@PathVariable BigDecimal quantity) {
		
		CurrencyConversion currencyConversion = proxy.retrieveExchangeValue(from, to);
		
		return new CurrencyConversion(
				currencyConversion.getId(), 
				from, 
				to, 
				quantity, 
				currencyConversion.getConversionMultiple(), 
				quantity.multiply(currencyConversion.getConversionMultiple()), 
				currencyConversion.getEnvironment() + " feign");
	}
}
```
<hr><hr>

we are still hardcoding instance environment url in CurrencyExchangeProxy as localhost:8000 and if we want to switch instance then we need to change value from 8000 to 8001 manually. 
To dynamically change these instances of microservice we use load balancer or service registry/naming-server.
Load Balancer: automatically discovers and load balances microservice instances. 


### 03 naming-server
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
<hr><hr>

## api-gateway
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

There are many types of implementations available for API Gateway which include — Spring Cloud Gateway, Zuul API Gateway, APIGee, EAG (Enterprise API Gateway)
We are using Spring Cloud Gateway.



### Step 01] Setting up api-gateway
#### pom.xml
add following dependencies

	1) spring-boot-starter-actuator
	2) spring-cloud-starter-gateway
	3) spring-cloud-starter-netflix-eureka-client
	4)  spring-boot-devtools
	5) spring-cloud-starter-config

#### application.properties

	spring.application.name=api-gateway
	server.port=8765

	#we need to define config server port because of <spring-cloud-starter-config> dependency
	spring.config.import=optional:configserver:http://localhost:8888
	
	eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka
	
	spring.cloud.gateway.discovery.locator.enabled=true
	spring.cloud.gateway.discovery.locator.lower-case-service-id=true
	

REST Endpoints:

	http://localhost:8765/CURRENCY-EXCHANGE/currency-exchange/from/USD/to/INR
	http://localhost:8765/CURRENCY-CONVERSION/currency-conversion/from/USD/to/INR/quantity/10
	http://localhost:8765/CURRENCY-CONVERSION/currency-conversion-feign/from/USD/to/INR/quantity/10
	
	http://localhost:8765/currency-exchange/currency-exchange/from/USD/to/INR
	http://localhost:8765/currency-conversion/currency-conversion/from/USD/to/INR/quantity/10
	http://localhost:8765/currency-conversion/currency-conversion-feign/from/USD/to/INR/quantity/10


### Step 02] Define Custom Routes
To define custom routes disable discovery locator in appplication.properties
#### application.properties

	spring.application.name=api-gateway
	server.port=8765

	#we need to define config server port because of <spring-cloud-starter-config> dependency
	spring.config.import=optional:configserver:http://localhost:8888
	
	eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka
	
	#spring.cloud.gateway.discovery.locator.enabled=true
	#spring.cloud.gateway.discovery.locator.lower-case-service-id=true


#### ApiGatewayConfiguration.java
package com.in28minutes.microservices.api_gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiGatewayConfiguration {

	@Bean
	public RouteLocator gatewayRouter(RouteLocatorBuilder builder) {
		
		return builder.routes()
				.route(p -> p.path("/get")
						.uri("http://httpbin.org:80"))
				.route(p -> p.path("/currency-exchange/**")
						.uri("lb://currency-exchange"))
				.route(p -> p.path("/currency-conversion/**")
						.uri("lb://currency-conversion"))
				.route(p -> p.path("/currency-conversion-feign/**")
						.uri("lb://currency-conversion"))
				.route(p -> p.path("/currency-conversion-new/**")
						.filters(f -> f.rewritePath(
								"/currency-conversion-new/(?<segment>.*)",
								"/currency-conversion-feign/${segment}"))
						.uri("lb://currency-conversion"))
				.build();
	}
}

### Step 03] Implementing Global Logging Filter
#### LoggingFilter.java
package com.in28minutes.microservices.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter {

	private Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
	
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// TODO Auto-generated method stub
		logger.info("Path of the request recieved -> {}", exchange.getRequest().getPath());
		return chain.filter(exchange);
	}

}

<hr>

### Step 04] @Retry Returning a fallback response if service is down
#### pom.xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-aop</artifactId>
		</dependency>

		<dependency>
			<groupId>io.github.resilience4j</groupId>
			<artifactId>resilience4j-spring-boot2</artifactId>
		</dependency>


#### CircuitBreakerController.java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.retry.annotation.Retry;
@RestController
public class CircuitBreakerController {

	private Logger logger = LoggerFactory.getLogger(CircuitBreakerController.class);
	
//	Working api
	@GetMapping("/dummy-api")
	public String dummyApi() {
		return "Dummy API";
	}

//	Non working api 
//	@Retry will attempt to connect to /sample-api
	@GetMapping("/sample-api")
	@Retry(name = "sample-api", fallbackMethod = "hardcodedResponse")
	public String sampleApi() {
		logger.info("Sample Api call received");
		ResponseEntity<String> forEntity = new RestTemplate().getForEntity("http://localhost8080/some-sample-url", 
				String.class);
		
		return forEntity.getBody();
	}
	
	public String hardcodedResponse(Exception ex) {
		return "fallback-response";
	}
	
}

#### application.properties
spring.application.name=api-gateway
server.port=8765

spring.config.import=optional:configserver:http://localhost:8888

eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka

#spring.cloud.gateway.discovery.locator.enabled=true
#spring.cloud.gateway.discovery.locator.lower-case-service-id=true

resilience4j.retry.instances.sample-api.max-attempts=5
resilience4j.retry.instances.sample-api.wait-duration=1s
resilience4j.retry.instances.sample-api.enable-exponential-backoff=true


### Step 05] Implementing Circuit Breaker
We implement circuit breaker pattern to reduce load. Instead of hitting a down/slow microservice and increasing load we implement circuit breaker which will attempt to connect few times and then will stop, and might wait and retry or directly declare fallback response without hitting microservice.

#### CircuitBreakerController.java
Replace @Retry with @CircuitBreaker in sampleApi()

	@GetMapping("/sample-api")
	@CircuitBreaker(name = "sample-api", fallbackMethod = "hardcodedResponse")
	public String sampleApi() {
		logger.info("Sample Api call received");
		ResponseEntity<String> forEntity = new RestTemplate().getForEntity("http://localhost8080/some-sample-url", 
				String.class);
		
		return forEntity.getBody();
	}
	
	public String hardcodedResponse(Exception ex) {
		return "fallback-response";
	}


There is not equivalent watch command in Windows. All we can do is to run the following command on Window's command prompt:

	for /l %g in () do @(curl http://localhost:8765/sample-api & timeout /t 5)

The output will be:

	fallback-response
	wait for 5/4/3/2/1 seconds, press a key to continue....


### Step 06] Implementing Rate Limiting
add @RateLimiter annotation to workingApi()

//	Working api
	@GetMapping("/working-api")
	@CircuitBreaker(name = "default", fallbackMethod = "hardcodedResponse")
	@RateLimiter(name = "default")
	public String workingApi() {
		logger.info("Working Api call received");
		return "working-API";
	}
	
#### application.properties
add following lines: 

	resilience4j.circuitbreaker.instances.sample-api.failure-rate-threshold=90
	resilience4j.ratelimiter.instances.default.limit-for-period=2
	resilience4j.ratelimiter.instances.default.limit-refresh-period=10s

Run in cmd

	for /l %g in () do @(curl http://localhost:8765/working-api & timeout /t 0)


### Step 06] Implementing BulkHead Features
Replace @RateLimiter annotation with @BulkHead in workingApi()

//	Working api
	@GetMapping("/working-api")
	@CircuitBreaker(name = "default", fallbackMethod = "hardcodedResponse")
//	@RateLimiter(name = "default")
	@Bulkhead(name = "working-api")
	public String workingApi() {
		logger.info("Working Api call received");
		return "working-API";
	}
	
#### application.properties
add following line:

	resilience4j.bulkhead.instances.working-api.max-concurrent-calls=10

Run in cmd 

	for /l %g in () do @(curl http://localhost:8765/working-api & timeout /t 0)


<hr><hr>
















