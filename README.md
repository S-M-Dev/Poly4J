# Poly4J

Poly4J is a lightweight Java library that provides a clean, builder-style interface for making requests to the [Polymarket](https://polymarket.com) API. It wraps Polymarket's Gamma, Data, and CLOB APIs behind a simple, fluent client so you don't have to hand-roll HTTP requests and JSON parsing yourself.

📖 **Full documentation:** [https://docs.smdev.pro/poly4j/#/](https://docs.smdev.pro/poly4j/#/)

## Features

- Fluent, builder-style request configuration via `RequestBuilder`
- Ready-made request factories for common Polymarket endpoints (markets, profiles, authentication, orders)
- Wallet-based L1/L2 authentication (API key creation/derivation and request signing) via `AuthenticationUtils`
- Asynchronous requests backed by Java's `HttpClient`, returning `CompletableFuture`
- Pluggable response mapping — map responses to `String`, `JsonNode`, or any custom type via `ResponseMapper`
- Built on Java 21, using Jackson for JSON handling and web3j/Bouncy Castle for wallet signing

## Installation

Poly4J is published on Maven Central.

**Maven**

```xml
<dependency>
    <groupId>pro.smdev</groupId>
    <artifactId>poly4j</artifactId>
    <version>2.1.2</version>
</dependency>
```

**Gradle**

```groovy
implementation 'pro.smdev:poly4j:2.1.2'
```

## Usage

```java
import pro.smdev.poly4j.core.PolyClient;
import pro.smdev.poly4j.model.RequestBuilder;

PolyClient client = new PolyClient();

RequestBuilder requestBuilder = client.request()
        .profile()
        .getPublicProfile("0x{wallet}");

client.perform(requestBuilder)
        .thenAccept(System.out::println)
        .get();
```

By default, `perform` maps the response body to a `String`. To map it to structured JSON instead, pass a `ResponseMapper`:

```java
import pro.smdev.poly4j.mapper.ResponseMapper;

client.perform(requestBuilder, ResponseMapper.jsonMapper())
        .thenAccept(json -> System.out.println(json.get("name")))
        .get();
```

For the full list of supported requests, request factories, and mappers, see the [documentation](https://docs.smdev.pro/poly4j/#/).

## Requirements

- Java 21+

## License

Licensed under the [Apache License, Version 2.0](LICENSE). See [NOTICE](NOTICE) for third-party attributions.
