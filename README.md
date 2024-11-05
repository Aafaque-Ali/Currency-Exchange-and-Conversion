# Currency-Exchange-and-Conversion
Implementing microservices using Spring Cloud. This project contains two main microservices: 
<hr>

## 01 currency-exchange-service
This is an independent microservice.

### Step 01] Setting up currency-exchange-service
#### Dependencies used in pom.xml
<ol>
<li>spring-boot-starter-web</li>
<li>spring-boot-devtools</li>
<li>spring-boot-starter-actuator</li>
<li>spring-cloud-starter-config</li>
<br>	This enables us to talk to spring-cloud-config-server
<li> lombok</li>
</ol>
	
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
<ol start="6" type="1">
<li>spring-boot-starter-data-jpa</li>
<li>h2</li>
</ol>
	
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
<ol type="1">
<li>spring-boot-starter-web</li>
<li>spring-boot-devtools</li>
<li>spring-boot-starter-actuator</li>
<li>spring-cloud-starter-config</li>
	This enables us to talk to spring-cloud-config-server
<li>lombok</li>
</ol>
	
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









