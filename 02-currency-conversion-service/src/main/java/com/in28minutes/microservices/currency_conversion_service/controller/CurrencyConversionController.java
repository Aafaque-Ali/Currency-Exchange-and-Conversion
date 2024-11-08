package com.in28minutes.microservices.currency_conversion_service.controller;

import java.math.BigDecimal;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.in28minutes.microservices.currency_conversion_service.CurrencyExchangeProxy;
import com.in28minutes.microservices.currency_conversion_service.bean.CurrencyConversion;

@RestController
public class CurrencyConversionController {
	
	@Autowired
	CurrencyExchangeProxy proxy;

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
