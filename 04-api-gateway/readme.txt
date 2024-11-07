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










