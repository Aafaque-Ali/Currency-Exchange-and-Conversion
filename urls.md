Currency Exchange Service
----------------------------
```java
http://localhost:8000/currency-exchange/from/USD/to/INR
```

Currency Conversion Service
------------------------------
```java  
  http://localhost:8100/currency-conversion/from/USD/to/INR/quantity/10
  http://localhost:8100/currency-conversion-feign/from/USD/to/INR/quantity/10
```

Eureka
-----------
```java
http://localhost:8761/
```

API Gateway
----------------
```java
http://localhost:8765/CURRENCY-EXCHANGE/currency-exchange/from/USD/to/INR
http://localhost:8765/CURRENCY-CONVERSION/currency-conversion/from/USD/to/INR/quantity/10
http://localhost:8765/CURRENCY-CONVERSION/currency-conversion-feign/from/USD/to/INR/quantity/10
```

```java
http://localhost:8765/currency-exchange/currency-exchange/from/USD/to/INR
http://localhost:8765/currency-conversion/currency-conversion/from/USD/to/INR/quantity/10
http://localhost:8765/currency-conversion/currency-conversion-feign/from/USD/to/INR/quantity/10
```

Discovery Disabled and Custom Routes Configured
---------------------------------------------------
```java
http://localhost:8765/currency-exchange/from/USD/to/INR
http://localhost:8765/currency-conversion/from/USD/to/INR/quantity/10
http://localhost:8765/currency-conversion-feign/from/USD/to/INR/quantity/10
http://localhost:8765/currency-conversion-new/from/USD/to/INR/quantity/10
```



