# Currency-Exchange-and-Conversion
Implementing microservices using Spring Cloud. This project contains two main microservices: 
## 01 currency-exchange-service
currency-exchange-service is independent microservice.

## 02 currency-conversion-service
currency-conversion-service is dependent on currency-exchange-service.

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

<ol>
<li>Centralized Service Registry: Eureka Server knows about all client applications and their locations. This centralization simplifies service discovery.</li>
<li>Automatic Registration: Microservices automatically register themselves with Eureka Server, reducing manual configuration efforts.</li>
<li>Load Balancing: Eureka Server can help implement load balancing among service instances.</li>
<li>Health Checks: Eureka Server can perform health checks on registered services, ensuring robustness and reliability.</li>
<li>Integration with Spring Cloud: Eureka Server seamlessly integrates with the Spring Cloud ecosystem, enabling easy scaling and deployment.</li>
</ol>









