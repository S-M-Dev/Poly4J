# Poly4J

Poly4J is a lightweight Java library that provides a clean, builder-style interface for making requests to the [Polymarket](https://polymarket.com) API. It wraps Polymarket's Gamma, Data, CLOB and relayer APIs behind a simple, fluent client so you don't have to hand-roll HTTP requests, JSON parsing, or EIP-712 order signing yourself.

📖 **Full documentation:** [https://docs.smdev.pro/poly4j/#/](https://docs.smdev.pro/poly4j/#/)

## Features

- Fluent, builder-style request configuration via `RequestBuilder`
- Ready-made request factories for common Polymarket endpoints (markets, profiles, orderbook, positions, authentication, orders, relayer/builder)
- Wallet-based L1/L2/builder authentication (API key creation/derivation and request signing) via `AuthenticationUtils` and `PolyClient`
- EIP-712 order building and signing via `OrderSigningUtils`, including ERC-7739-wrapped signatures for deposit-wallet-signed orders
- Offline deposit-wallet address derivation via `DepositWalletUtils` (no network round-trip required)
- `UpDownClient`, a ready-to-use trading client for Polymarket's "Up/Down" markets: resolves markets, sizes and places depth-aware FOK market orders with automatic retries, and polls for fill settlement
- Asynchronous requests backed by Java's `HttpClient`, returning `CompletableFuture`, with synchronous equivalents throughout
- Pluggable response mapping — map responses to `String`, `JsonNode`, or any custom type via `ResponseMapper`
- Unchecked exceptions for common failure modes — `ClientRequestPerformException` for transport failures and `NotAuthenticatedException` for missing credentials — instead of checked exceptions or silent failures
- [SLF4J](https://www.slf4j.org/)-based logging of outgoing requests and `UpDownClient` trading activity
- Built on Java 21, using Jackson for JSON handling and web3j/Bouncy Castle for wallet signing

## Installation

Poly4J is published on Maven Central.

**Maven**

```xml
<dependency>
    <groupId>pro.smdev</groupId>
    <artifactId>poly4j</artifactId>
    <version>2.2.0</version>
</dependency>
```

**Gradle**

```groovy
implementation 'pro.smdev:poly4j:2.2.0'
```

## Usage

### Unauthenticated requests

Public endpoints (profiles, markets, orderbook, positions) don't require any credentials:

```java
import pro.smdev.poly4j.core.PolyClient;
import pro.smdev.poly4j.model.RequestBuilder;

PolyClient client = new PolyClient();

RequestBuilder requestBuilder = client.request()
        .profile()
        .getPublicProfile("0x{wallet}");

String response = client.perform(requestBuilder);
System.out.println(response);
```

By default, `perform` maps the response body to a `String`. To map it to structured JSON instead, pass a `ResponseMapper`:

```java
import com.fasterxml.jackson.databind.JsonNode;
import pro.smdev.poly4j.mapper.ResponseMapper;

JsonNode json = client.perform(requestBuilder, ResponseMapper.jsonMapper());
System.out.println(json.get("name"));
```

Both methods have `performAsync` equivalents returning a `CompletableFuture` instead of blocking:

```java
client.performAsync(requestBuilder, ResponseMapper.jsonMapper())
        .thenAccept(node -> System.out.println(node.get("name")))
        .get();
```

If the underlying HTTP call fails or is interrupted, `perform`/`performAsync` throw (or complete exceptionally with) a `ClientRequestPerformException`:

```java
import pro.smdev.poly4j.exception.ClientRequestPerformException;

try {
    client.perform(requestBuilder);
} catch (ClientRequestPerformException e) {
    System.err.println("Request failed: " + e.getCause());
}
```

### Authenticated requests

Supplying an `Authentication` (derived from your wallet's private key) unlocks L1-signed requests (e.g. deriving/creating a CLOB API key) and, once an API key has been derived, L2-signed requests (e.g. placing/cancelling orders):

```java
import pro.smdev.poly4j.model.Authentication;

PolyClient client = new PolyClient()
        .authenticated(new Authentication("0x{signerPrivateKey}"));

client.deriveL2CredentialsAsync("0").get();

JsonNode openOrders = client.perform(client.request().orders().getOpenOrders(), ResponseMapper.jsonMapper());
System.out.println(openOrders);
```

Builder API credentials (used to deploy a deposit wallet via the relayer) can be supplied up front instead:

```java
Authentication authentication = new Authentication(
        "0x{signerPrivateKey}", "{builderApiKey}", "{builderSecret}", "{builderPassphrase}");

PolyClient client = new PolyClient().authenticated(authentication);
client.perform(client.request().builder().createWallet());
```

### Trading Up/Down markets

`UpDownClient` wraps the above into a ready-to-use client for Polymarket's "Up/Down" markets (e.g. `btc-updown-15m-*`), signing orders against the deposit wallet ([`SignatureType.DEPOSIT_WALLET`](https://docs.polymarket.com/trading/place-orders)):

```java
import pro.smdev.poly4j.core.UpDownClient;
import pro.smdev.poly4j.model.Side;
import pro.smdev.poly4j.model.dto.OrderResult;
import pro.smdev.poly4j.model.dto.UpDownMarket;

UpDownClient upDown = client.getUpDownClient("0");

UpDownMarket market = upDown.getMarket("btc-updown-15m-1786708800");

// Spend $1 buying the "up" outcome with a depth-aware FOK market order
OrderResult buy = upDown.placeOrder(Side.BUY, market.upId(), 1);

if (buy.amount() > 0) {
    double confirmedShares = upDown.awaitOpenPosition(market, "up", buy.amount(), 5_000, 1_000);
    OrderResult sell = upDown.placeOrder(Side.SELL, market.upId(), confirmedShares);
}
```

For the full list of supported requests, request factories, and mappers, see the [documentation](https://docs.smdev.pro/poly4j/#/).

## Logging

Poly4J logs via [SLF4J](https://www.slf4j.org/) — add a binding (Logback, Log4j2, etc.) on your classpath to see output; without one, calls are silently no-ops.

- `PolyClient` logs each outgoing request at `INFO` (`[METHOD] url`) and the response status/truncated body at `TRACE`
- `UpDownClient` logs market resolution, order placement, and position-polling progress at `DEBUG`, and raw order/response payloads at `TRACE`

## Requirements

- Java 21+

## License

Licensed under the [Apache License, Version 2.0](LICENSE). See [NOTICE](NOTICE) for third-party attributions.
