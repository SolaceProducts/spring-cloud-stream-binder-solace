# Spring Cloud Stream Binder for Solace PubSub+

An implementation of Spring's Cloud Stream Binder to be used for binding with Solace PubSub+ message brokers.

## Contents

* [Overview](#overview)
* [Spring Cloud Stream Binder](#spring-cloud-stream-binder)
* [Using it in your Application](#using-it-in-your-application)
* [Building the Project Yourself](#building-the-project-yourself)
* [Contributing](#contributing)
* [Authors](#authors)
* [License](#license)
* [Resources](#resources)

---

## Overview



## Spring Cloud Stream Binder

This project extends Spring Cloud Stream Binder. If you are new to Spring Cloud Stream Binder, [check out their documentation](https://docs.spring.io/spring-cloud-stream/docs/current/reference/htmlsingle).

The following is a brief introduction copied from their documentation:

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

For general binder configuration options and properties, refer to the [Spring Cloud Stream core documentation](https://github.com/spring-cloud/spring-cloud-stream/blob/master/spring-cloud-stream-core-docs/src/main/asciidoc/spring-cloud-stream-overview.adoc#configuration-options).

As for auto-configuration-related options required for auto-connecting to Solace message brokers, refer to the [JCSMP Spring Boot Auto-Configuration documentation](https://github.com/SolaceProducts/solace-java-spring-boot#configure-the-application-to-use-your-solace-pubsub-service-credentials).

Along with the mentioned inherited configuration options, this specific implementation of the Spring Cloud Stream Binder further augments its functionality through using a set of [Solace-specific binder configuration options](BINDER_CONFIG.md).

## Building the Project Yourself

This project depends on maven for building. To build the jar locally, check out the project and build from source by doing the following:
```
git clone https://github.com/SolaceLabs/spring-cloud-stream-binder-solace.git
cd spring-cloud-stream-binder-solace
mvn package
```

This will build the [Solace binder core](https://github.com/SolaceLabs/spring-cloud-stream-binder-solace/tree/dev/spring-cloud-stream-binder-solace-core), the [Solace binder itself](https://github.com/SolaceLabs/spring-cloud-stream-binder-solace/tree/dev/spring-cloud-stream-binder-solace), and the [Solace spring cloud stream starter](https://github.com/SolaceLabs/spring-cloud-stream-binder-solace/tree/dev/spring-cloud-starter-stream-solace) jars.

Note: As currently setup, the build requires Java 1.8. If you want to use another older version of Java adjust the build accordingly.

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Authors

See the list of [contributors](https://github.com/SolaceLabs/spring-cloud-stream-binder-solace/graphs/contributors) who participated in this project.

## License

This project is licensed under the Apache License, Version 2.0. - See the [LICENSE](LICENSE) file for details.

## Resources

For more information about Spring Cloud Stream Binders try these resources:

- [Spring Docs - Spring Boot Auto-Configuration](http://docs.spring.io/autorepo/docs/spring-boot/current/reference/htmlsingle/#using-boot-auto-configuration)
- [Spring Docs - Developing Auto-Configuration](http://docs.spring.io/autorepo/docs/spring-boot/current/reference/htmlsingle/#boot-features-developing-auto-configuration)
- [GitHub Tutorial - Master Spring Boot Auto-Configuration](https://github.com/snicoll-demos/spring-boot-master-auto-configuration)

For more information about Solace technology in general please visit these resources:

- The Solace Developer Portal website at: http://dev.solace.com
- Understanding [Solace technology.](http://dev.solace.com/tech/)
- Ask the [Solace community](http://dev.solace.com/community/).
