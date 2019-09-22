package com.microservices.currencyconversionservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
public class CunrrencyConverterController {
    private CurrencyExchangeServiceProxy proxy;
    private Logger logger = LoggerFactory.getLogger("Currency Converter Controller");

    public CunrrencyConverterController(CurrencyExchangeServiceProxy proxy) {
        this.proxy = proxy;
    }

    @GetMapping("/convert-currency/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversionValueObject convertCurrency(@PathVariable String from,
                                                         @PathVariable String to,
                                                         @PathVariable BigDecimal quantity) {

        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("from", from);
        uriVariables.put("to", to);

        final ResponseEntity<CurrencyConversionValueObject> responseEntity = new RestTemplate().getForEntity(
                "http://localhost:8000/currency-exchange/from/{from}/to/{to}",
                CurrencyConversionValueObject.class,
                uriVariables);

        final CurrencyConversionValueObject response = responseEntity.getBody();

        return new CurrencyConversionValueObject(
                response.getFrom(),
                response.getTo(),
                response.getConversionMultiple(),
                quantity,
                quantity.multiply(response.getConversionMultiple()),
                response.getPort());
    }

    @GetMapping("/convert-currency-feign/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversionValueObject convertCurrencyFeign(@PathVariable String from,
                                                         @PathVariable String to,
                                                         @PathVariable BigDecimal quantity) {
        final CurrencyConversionValueObject response = this.proxy.retrieveExchangeValue(from, to);

        logger.info("{}", response);
        return new CurrencyConversionValueObject(
                response.getFrom(),
                response.getTo(),
                response.getConversionMultiple(),
                quantity,
                quantity.multiply(response.getConversionMultiple()),
                response.getPort());
    }
}
