# currency-exchange-service

## Step 01] Setting up currency-exchange-service
### Dependencies used in pom.xml
1) spring-boot-starter-web
2) spring-boot-devtools
3) spring-boot-starter-actuator
4) spring-cloud-starter-config
	This enables us to talk to spring-cloud-config-server
5) lombok

	
### application.properties
spring.application.name=currency-exchange
server.port=8000
spring.config.import=optional:configserver:http://localhost:8888


## Step 02] Creating RestController
### CurrencyExchangeController.java
@RestController
public class CurrencyExchangeController {

	@GetMapping("/currency-exchange/from/{from}/to/{to}")
	public CurrencyExchange retrieveExchangeValue(
			@PathVariable String from,
			@PathVariable String to) {
	
		return new CurrencyExchange(100L, "USD", "INR", BigDecimal.valueOf(40), "");
	}
}


### CurrencyExchange.java
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


## Step 03] Getting port from Environment
### CurrencyExchangeController.java
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


## Step 04] Launching this microservice on port 8001

1) Below file bar, we have options to run, stop and re-run application. 
	Dropdown from run and select 'Run Configurations'.
2) Under Spring Boot Application/ Java Application -> Go to '01-currency-exchange-service - Application'.
	You will be shown name of application. Change name from '01-currency-exchange-service - Application' 
	to 'CurrencyExchangeServiceApplication8000' -> click Apply.
http://localhost:8000/currency-exchange/from/USD/to/INR

3) After that right click on LHS CurrencyExchangeServiceApplication8000 and duplicate it.
4) Change its name to 'CurrencyExchangeServiceApplication8001' -> click 'Arguments tab' under 
	'VM arguments' write: -Dserver.port=8001 ->	click Apply -> click Run.
http://localhost:8001/currency-exchange/from/USD/to/INR


## Step 05] Connecting to H2 Database
### pom.xml
add new dependency
6) spring-boot-starter-data-jpa
7) h2

### application.properties
#add below new lines
spring.jpa.show-sql=true
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true

### CurrencyExchange.java
// add @Entity, @Id annotations
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
	
/*
below error is caused because we are using 'from' as column name. 'from' is also an SQl command. To avoid below error change column name as shown above
Caused by: org.h2.jdbc.JdbcSQLSyntaxErrorException: 
Syntax error in SQL statement "create table currency_exchange 
(conversion_multiple numeric(38,2), id bigint not null, environment varchar(255), [*]from varchar(255), to varchar(255), primary key (id))"; 
expected "identifier"; SQL statement:
create table currency_exchange (conversion_multiple numeric(38,2), id bigint not null, environment varchar(255), from varchar(255), to varchar(255), primary key (id)) [42001-224]
*/
http://localhost:8000/h2-console


## Step 06] Inserting data into table
create a file named data.sql inside src/main/resources
### data.sql
insert into currency_exchange
(id, currency_from, currency_to, conversion_multiple, environment)
values(1001,'USD','INR',65,'');
insert into currency_exchange
(id, currency_from, currency_to, conversion_multiple, environment)
values(1002,'EUR','INR',75,'');
insert into currency_exchange
(id, currency_from, currency_to, conversion_multiple, environment)
values(1003,'AUD','INR',25,'');


From Spring Boot 2.4+ Loading of data.sql is done before the tables are created. To rectify this add following in application.properties
### application.properties
spring.jpa.defer-datasource-initialization=true


## Step 07] Fetching values from database
### CurrencyExchangeRepository.java
public interface CurrencyExchangeRepository extends JpaRepository<CurrencyExchange, Long> {
}

### CurrencyExchangeController.java
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




