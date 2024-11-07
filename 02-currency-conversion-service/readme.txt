# currency-conversion-service

## Step 01] Setting up currency-conversion-service
### Dependencies used in pom.xml
1) spring-boot-starter-web
2) spring-boot-devtools
3) spring-boot-starter-actuator
4) spring-cloud-starter-config
	This enables us to talk to spring-cloud-config-server
5) lombok

	
### application.properties
spring.application.name=currency-conversion
server.port=8100
spring.config.import=optional:configserver:http://localhost:8888


## Step 02] Creating RestController
### CurrencyConversionController.java
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

### CurrencyConversion.java
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


## Step 03] Getting details from 'currency-exchange' microservice
### CurrencyExchangeController.java

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
http://localhost:8100/currency-conversion/from/USD/to/INR/quantity/10
/* Take note that we do not have to connect currency-conversion to database.We only connect 
   currency-conversion to currency-exchange microservice, and that in turn gets value from database.
*/


In above step there is lots of broiler plate code for connecting @pathVariable to other microservice. To reduce this we use Feign framework
## Step 4] Connecting with feign
### pom.xml
6) spring-cloud-starter-openfeign

### Application.java
@SpringBootApplication
@EnableFeignClients
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}


// create a proxy to talk to currency-exchange from currency-conversion
Note: This is an interface
### CurrencyExchangeProxy.java
@FeignClient(name="currency-exchange", url="localhost:8000")
public interface CurrencyExchangeProxy {

	// We get this method from CurrencyExchangeController.
	@GetMapping("/currency-exchange/from/{from}/to/{to}")
	public CurrencyConversion retrieveExchangeValue(
			@PathVariable String from,
			@PathVariable String to);
	
}

### CurrencyConversionController.java
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

<hr><hr>

we are still hardcoding instance environment url in CurrencyExchangeProxy as localhost:8000 and 
if we want to switch instance then we need to change value from 8000 to 8001 manually. 
To dynamically change these instances of microservice we use load balancer or service registry/naming-server.
Load Balancer: automatically discovers and load balances microservice instances. 






