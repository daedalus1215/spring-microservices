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
	- On Server, we create the `localconfig-repo` directory and `git init` in it, to make it a local repo
	- On Server, we register that `localconfig-repo` dir in the `application.properties`
	- Create our configs (e.g. `limits-service.properties`, `limits-service-dev.properties`, etc) in that new directory (`localconfig-repo`); and then, `git commit` them.
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
5. Limit Service, is connecting to Spring Cloud Config Server, to be able to simulationnstrate a central configuration server
	- It essentially sets a limit on some account - a minimum and maximum and it is getting those values from the Spring Cloud Config Server.
6. Currency Exchange Service: is a api accessible service that will tell you the exchange rate of a few countries
	- This api endpoint is registered, in the Currency Conversion Service, as a load balancing endpoint (via Ribbon). Nothing in the `CurrencyExchangeController` tells consumers it is a load balancing endpoint, it is actually the consumers that register this info.
	- When it initially runs it creates the data it needs in a h2 database. You can access that database at the following url http://localhost:8000/h2-console
7. Currency Conversion Service: Provides an endpoint that will convert the currencies over, then use the Currency Exchange Service endpoint, to get exchange rate details.
	- It registers the service it consumes, e.g.: `currency-exchange-service` , in `CurrencyExchangeServiceProxy`, as a load balancing capable service (`@FeignClient`)
	- It registers Zuul Api gateway server in the Proxy as a way of integrating in with the official Gatekeeper. Allows us to benefit off of the Filters defined in the Zuul API Service.

<hr />

## Using Netflix Histrix to add fault tolerance
If we want to make sure that a particular Microservice implement fault tolerance 
all we really need is 1 service running:
1. LimitService

We have an additional method in the Controller that will be deferred to when there is 
an error that is thrown in the main Controller's request handling method.

<hr />

## Using Eureka, Ribbon, & Feign we are able to distribute calls using Eureka:
If we want to make it so that a particular MicroService has it's requests distrbuted to 
many instances of another MicroService then we need 3 services running:
1. Netflix Eureka Naming Server
2. Exchange Service
3. Conversion Service

In order to run simulation we must start the apps in a certain order:
1. Netflix Eureka Naming Server
2. Exchange Service
	- Multiple instances: e.g. VM options: -dServer.port=8000 & -dServer.port=8002
3. Conversion Service

Because both the Exchange and Conversion services declares eureka in their `application.properties` 
file, they can capitalize on the integration of Eureka, Ribbon and Feign. Conversion specifies Feign and RibbonClient 
of the exchange service in it's Proxy, and Conversion wires up further in it's ApplicationService's.

Steps to get this to work:
1. Start Netflix Eureka Server
2. Check the Eureka URL and see no services connected
3. Fire up multiple instances of the Exchange Service
4. Uncomment `@FeignClient(name = "currency-exchange-service")` in the `CurrencyExchangeServiceProxy` and comment out the Zuul reference FeignClient annotation.
5. Fire up the Conversion Service.
6. Check the Netflix Eureka Server url and make sure all the services are connected: http://localhost:8761/ .
7. check each exchange services url and see they both come up and the payload specifies the port they return from.
8. Check the conversion url and see that everytime it refreshes it will get the request from a different port - indicating a different instance of exchange service.
<hr />
``

## Getting configs from Spring Cloud Config Server exercise:
If we want to make it so that a particular MicroService uses the spring cloud config server as the central location for configs,
we must do a few things and have 2 services running:
1. Spring Cloud Config Server
2. LimitsService

In order to run simulation we must start the apps in a certain order:
1. Spring Cloud Config Server
2. LimitsService

Steps to get this to work:
1. Create a local git repository in the Spring Cloud Config Server, inside of the `localconfig-repo`
2. After you create the local repo, you want to, `git add` and `commit` the config files - this allows the Config Server to feed and read them
3. Make sure Config Server's `application.properties` file is specifying where the git local repo is, in: `spring.cloud.config.server.git.uri`
3. Start the Config Server
4. Start the LimitService
5. Visit the LimitService endpoint url: http://localhost:8081/limits

You then can adjust what config we want - in the LimitService `bootstrap.properties` file, by adjusting what profile is active

<hr />


## Having multiple Service instances refresh their configs with spring cloud bus - over rabbitMQ, via amqp
If we want to be able to tell multiple instances of LimitService to refresh their config retrieval from the Spring Cloud Config Server,
we will need to run 2 services:
1. Spring Cloud Config Server
2. LimitService

The magic happens with Spring Cloud Bus dependency in both the Config Server's POM file and the LimitService's POM file. 
When the application starts up all instances of LimitService registers to the Spring Cloud Bus. When there is any change 
in configuration, and then refresh is called on any of these instances, then the microservice instance sends an event 
over to the cloud bus and the cloud bus will propagate that event to all the microservices registered to the bus.

The thing about Spring Boot, as soon as we add the right dependencies, it is all configured for us. We have RabbitMQ 
running in the background. Spring noticies that and sees there is a amqp dependency in the classpath, it will auto 
connect to rabbitMQ.

In order to run simulation we must start the apps in a certain order:
1. Spring Cloud Config Server
2. LimitsService

Steps to get this to work:
1. Create a local git repository in the Spring Cloud Config Server, inside of the `localconfig-repo`
2. After you create the local repo, you want to, `git add` and `commit` the config files - this allows the Config Server to feed and read them
3. Make sure Config Server's `application.properties` file is specifying where the git local repo is, in: `spring.cloud.config.server.git.uri`
3. Start the Config Server
4. Start the LimitService
5. Visit the LimitService endpoint url: http://localhost:8081/limits
6. Change the config, if we are pointing at the `qa` profile in LimitService, then change that corresponding config in the Cloud Config Service's min, max values
7. Make sure, in LimitService, we set the `bootstrap.properties` file's endpoint exposure to `bus-refresh` enabled: `management.endpoints.web.exposure.include=bus-refresh`
8. Open Postman, make a POST request to LimitService's actuator's instance for bus-refresh: http://localhost:8080/actuator/bus-refresh
9. Revisit all the LimitService instances and see that their output has changed - since they feed off of the Config Server, they get it's updated changes from step 6..

<hr />


## All requests are logged exercise:
If we want to simulate a live environment, where we can track and trace one request
and all of the services the request goes through, then centralize this information in a log, we 
would need to run some of these services in a certain order.

To be able to completely show the simulationnstration for running Spring Microservices that are aggregating logs into the RabbitMQ Queue. 
Then have those logs pulled with Zipkin and shown and displayed in the Zipkin url.
1. Conversion
2. Exchange
3. Eureka Naming Server
4. Netflix Zuul Api Gateway
5. Zipkin Distributed Tracing Server 

In order to run simulation we must start the apps in a certain order:
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







	
	
	
	
	
	
	
	
	