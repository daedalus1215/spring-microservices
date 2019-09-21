package com.microservices.currencyexchangeservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrencyExchangeController {
    private Environment environment;
    private ExchangeValueRepository exchangeValueRepository;
    private Logger logger = LoggerFactory.getLogger("Currency Exchange Controller");

    public CurrencyExchangeController(Environment environment, ExchangeValueRepository exchangeValueRepository) {
        this.environment = environment;
        this.exchangeValueRepository = exchangeValueRepository;
    }

    @GetMapping("/currency-exchange/from/{from}/to/{to}")
    public ExchangeValue retrieveExchangeValue(@PathVariable String from, @PathVariable String to) {
        final ExchangeValue exchangeValue = this.exchangeValueRepository.findByFromAndTo(from, to);
        final String property = this.environment.getProperty("local.server.port");
        exchangeValue.setPort(Integer.parseInt(property));
        logger.info("{}", exchangeValue);
        return exchangeValue;
    }
}
