# spring-microservices
A few microservices, a Spring Cloud Config Service, a Spring Zuul Api Service, a Spring Eureka Naming Service - are the supporting services; While Exchange, Conversion, and Limit Services are the attempt to communicate 
and simulate a microservice environment


## Pre-requisites:
<ul>
<li><a href="https://www.erlang.org/downloads">Erlang</a></li>
<li><a href="https://www.rabbitmq.com/install-windows.html">RabbitMQ</a></li>
<li><a href="https://zipkin.io/pages/quickstart.html">Zipkin</a></li>
</ul>

<hr />
# Goal
Ultimately this is an attempt to wire up all of the essential services required for a 
Spring Cloud (Netflix) Microservice Architecture.

The pieces used:
1. Spring Cloud Config Server: To have a central location for configs, for all the applications.
	- On consuming Service, Register the config server url (via `application.properties`). You can choose a profile as well (which config file to choose depending on environment). Example of this is in the `limits-service`.
	- On Server, we create the `git-localconfig-repo` directory and `git init` in it, to make it a local repo
	- On Server, we register that `git-localconfig-repo` dir in the `application.properties`
	- Create our configs (e.g. `limits-service.properties`, `limits-service-dev.properties`, etc) in that new directory (`git-localconfig-repo`); and then, `git commit` them.
	 - This is wierd, they do not store in repo well, because they belong to a local repo - which always needs to be local for the service.
2. Netflix Eureka Naming Server: This will be the way we integrate our services together by registering their name.
	- Create a Name for your Service in the application.properties (every new or existing service should do this). 
	- Register Service by having the service's `application.properties` register the eureka service url (e.g.: `limits-service`).
3. Netflix Zuul Api Gateway Server: As a central point for all requests to go through. To apply Filters (logging in our case) and load balancing (via Ribbon in our case)
	- We must register that the requests to go through the Zuul Service on the.
		Proxies or on the Controllers (e.g. `CurrencyConversionService` > `CurrencyExchangeServiceProxy`).
	- Must create `ZuulFilter`(s), by Creating Components and implementing the `ZuulFilter` abstract class' methods.
	- To utilize `RibbonClient`, we must do that on a Controller or Proxy, and in reference to the target service.
4. We install RabbitMQ, Erlang, download `zipkin-server` jar file from the site.
5. Limit Service, is connecting to Spring Cloud Config Server, to be able to demonstrate a central configuration server
	- It essentially sets a limit on some account - a minimum and maximum and it is getting those values from the Spring Cloud Config Server.
6. Currency Exchange Service: is a api accessible service that will tell you the exchange rate of a few countries
	- This api endpoint is registered, in the Currency Conversion Service, as a load balancing endpoint (via Ribbon). Nothing in the `CurrencyExchangeController` tells consumers it is a load balancing endpoint, it is actually the consumers that register this info.
	- When it initially runs it creates the data it needs in a h2 database. You can access that database at the following url http://localhost:8000/h2-console
7. Currency Conversion Service: Provides an endpoint that will convert the currencies over, then use the Currency Exchange Service endpoint, to get exchange rate details.
	- It registers the service it consumes, e.g.: `currency-exchange-service` , in `CurrencyExchangeServiceProxy`, as a load balancing capable service (`@FeignClient`)
	- It registers Zuul Api gateway server in the Proxy as a way of integrating in with the official Gatekeeper. Allows us to benefit off of the Filters defined in the Zuul API Service.

<hr />

## All requests are logged exercise:
If we want to simulate a live environment, where we can track and trace one request
and all of the services the request goes through, then centralize this information in a log, we 
would need to run some of these services in a certain order.

To be able to completely show the demonstration for running Spring Microservices that are aggregating logs into RabbitMQ, via Zipkin,
then pulled out via Zuul, then we will need 5 services running (And the Zipkin Distrbuted Tracing Server):
1. Conversion
2. Exchange
3. Eureka Naming Server
4. Netflix Zuul Api Gateway
5. Zipkin Distributed Tracing Server 

In order to run demo we must start the apps in a certain order:
1. Eureka Naming Server
2. Zipkin Tracing Server App 
 - Tell Zipkin to run, and for it to communicate with RABBITMQ, by going to the .jar file and: `RABBIT_URI=amqp://localhost java -jar zipkin-server-2.7.0-exec.jar`
 - (http://localhost:9411/zipkin/)
3. Exchange
4. Conversion
5. Netflix Zuul Api Gateway

First thing we should do is check that the 3 services are registered with Eureka, by visiting: http://localhost:8761/
Second thing we should do is check that Zipkin recognizes the 3 services, by visiting http://localhost:9411/zipkin/. keep in mind that Zipkin takes a bit before it recognizes all the services.
Make a request to the Conversion service, then go to the Zipkin URL and you can click on the request and see all of the services it went through on every level. First Currency, then Zuul, then Exchange.



	
	
	
	
	
	
	
	
	