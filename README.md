# Spring Cloud Stream Binder for Solace PubSub+

An implementation of Spring's Cloud Stream Binder for integrating with Solace PubSub+ message brokers. The Spring Cloud Stream Binder project provides a higher-level abstraction towards messaging that standardizes the development of distributed message-based systems.

## Contents

* [Overview](#overview)
* [Spring Cloud Stream Binder](#spring-cloud-stream-binder)
* [Using it in your Application](#using-it-in-your-application)
* [Failed Message Error Handling](#failed-message-error-handling)
* [Building the Project Yourself](#building-the-project-yourself)
* [Contributing](#contributing)
* [Authors](#authors)
* [License](#license)
* [Resources](#resources)

---

## Overview

The Solace implementation of the Spring Cloud Stream Binder maps the following concepts from Spring to Solace:

* Destinations to topic subscriptions
* Consumer groups to durable queues
* Anonymous consumer groups to temporary queues

And internally, each consumer group queue is subscribed to at least their destination topic. So a typical message flow would then appear as follows:

1. Producer bindings publish messages to their destination topics
2. Consumer group queues receive the messages published to their destination topic
3. Consumers of a particular consumer group consume messages from their group in a round-robin fashion (by default)

Note that partitioning is not yet supported by this version of the binder.

Also, it will be assumed that you have a basic understanding of the Spring Cloud Stream project. If not, then please refer to [Spring's documentation](https://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/). For the sake of brevity, this document will solely focus on discussing components unique to Solace.

## Spring Cloud Stream Binder

This project extends the Spring Cloud Stream Binder project. If you are new to Spring Cloud Stream, [check out their documentation](https://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle).

The following is a brief excerpt from that document:

> Spring Cloud Stream is a framework for building message-driven microservice applications. Spring Cloud Stream builds upon Spring Boot to create standalone, production-grade Spring applications and uses Spring Integration to provide connectivity to message brokers. It provides opinionated configuration of middleware from several vendors, introducing the concepts of persistent publish-subscribe semantics, consumer groups, and partitions.

## Using it in your Application

### Updating your build

The releases from this project are hosted in [Maven Central](https://mvnrepository.com/artifact/com.solace.spring.cloud/spring-cloud-starter-stream-solace).

The easiest way to get started is to include the `spring-cloud-starter-stream-solace` in your application.

Here is how to include the spring cloud stream starter in your project using Gradle and Maven.

#### Using it with Gradle

```groovy
// Solace Spring Cloud Stream Binder
compile("com.solace.spring.cloud:spring-cloud-starter-stream-solace:0.+")
```

#### Using it with Maven

```xml
<!-- Solace Spring Cloud Stream Binder -->
<dependency>
  <groupId>com.solace.spring.cloud</groupId>
  <artifactId>spring-cloud-starter-stream-solace</artifactId>
  <version>0.+</version>
</dependency>
```

### Creating a Simple Solace Binding

TODO

### Configuration Options

Configuration of the Solace Spring Cloud Stream Binder is done through [Spring Boot's externalized configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html). This is where users can control the binder's configuration options as well as the Solace Java API properties.

For general binder configuration options and properties, refer to the [Spring Cloud Stream Reference Guide](https://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/#_configuration_options).

As for auto-configuration-related options required for auto-connecting to Solace message brokers, refer to the [JCSMP Spring Boot Auto-Configuration documentation](https://github.com/SolaceProducts/solace-java-spring-boot#configure-the-application-to-use-your-solace-pubsub-service-credentials).

Along with the mentioned inherited configuration options, this specific implementation of the Spring Cloud Stream Binder further augments its functionality by employing the usage of a diverse set of [Solace-specific binder configuration options](BINDER_CONFIG.md).

## Failed Message Error Handling

Spring cloud stream binders already provides a number of application-internal reprocessing strategies for failed messages during message consumption such as:

* Forwarding errors to various [Spring error message channels](https://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/#_application_error_handling)
* Internally re-processing the failed messages through the usage of a [retry template](https://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/#_retry_template)

However, after all internal error handling strategies have been exhausted, the Solace implementation of the binder would by default reject messages that consumer bindings fail to process. Though it may be desirable for these failed messages be preserved and externally re-processed, in which case this binder also provides 2 error handling strategies that consumer bindings can be configured to use:

* Re-queuing the message onto that consumer group's queue
* Re-publishing the message to another queue (a dead-message queue) for some external application/binding to process

Note that both strategies cannot be used at the same time, and that enabling both of them would result in the binder treating it as if message re-queuing was disabled. That is to say, re-publishing failed messages to a dead-message queues supersedes message re-queuing.

### Message Re-Queuing

A simple error handling strategy in which failed messages are simply re-queued onto the consumer group's queue. This is very similar to simply enabling the retry template (setting maxAttempts to a value greater than 1), but allows for the failed messages to be re-processed by the message broker.

### Dead-Message Queue Processing

First, it must be noted that the dead message queue (DMQ) that will be discussed in this section is different from the regular [Solace DMQ](https://docs.solace.com/Configuring-and-Managing/Setting-Dead-Msg-Queues.htm). In particular, the standard Solace DMQ is used for re-routing failed messages as a consequence of Solace PubSub+ messaging features such as TTL expiration or exceeding a message's max redelivery count. Whereas the purpose of a Solace binder DMQ is for re-routing messages which had been successfully consumed from the message broker, yet cannot be processed by the binder. For simplicity, in this document all mentions of the "DMQ" refers to the Solace binder DMQ.

A DMQ can be provisioned for a particular consumer group by setting the autoBindDmq consumer property to true. This DMQ is simply another durable queue which, aside from its purpose, is not much from the queue provisioned for consumer groups. These DMQs are named using a period-delimited concatenation of their consumer group name and "dmq". And like the queue used for consumer groups, their endpoint properties can be configured by means of any consumer properties whose names begin with "dmq".

Note that DMQs are not intended to be used with anonymous consumer groups. Since the names of these consumer groups, and in turn the name of their would-be DMQs, are randomly generated at runtime, it would provide little value to create bindings to these DMQs because of their unpredictable naming and temporary existence.

## Building the Project Yourself

This project depends on maven for building. To build the jar locally, check out the project and build from source by doing the following:
```
git clone https://github.com/SolaceLabs/spring-cloud-stream-binder-solace.git
cd spring-cloud-stream-binder-solace
mvn package
```

This will build the [Solace binder core](spring-cloud-stream-binder-solace-core), the [Solace binder itself](spring-cloud-stream-binder-solace), and the [Solace spring cloud stream starter](spring-cloud-starter-stream-solace) jars.

Note: As currently setup, the build requires Java 1.8. If you want to use another older version of Java adjust the build accordingly.

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Authors

See the list of [contributors](https://github.com/SolaceLabs/spring-cloud-stream-binder-solace/graphs/contributors) who participated in this project.

## License

This project is licensed under the Apache License, Version 2.0. - See the [LICENSE](LICENSE) file for details.

## Resources

For more information about Spring Cloud Streams try these resources:

- [Spring Docs - Spring Cloud Stream Reference Guide](https://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle/)
- [GitHub Samples - Spring Cloud Stream Sample Applications](https://github.com/spring-cloud/spring-cloud-stream-samples)
- [Github Source - Spring Cloud Stream Source Code](https://github.com/spring-cloud/spring-cloud-stream)

For more information about Solace technology in general please visit these resources:

- The Solace Developer Portal website at: http://dev.solace.com
- Understanding [Solace technology](http://dev.solace.com/tech/)
- Ask the [Solace community](http://dev.solace.com/community/)
