# CASHDAQ

![Status](https://img.shields.io/badge/status-in%20progress-yellow?style=for-the-badge)
![Java](https://img.shields.io/badge/java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)

CASHDAQ is an experimental stock exchange built from scratch in Java with a
market of fictional companies. Orders are placed by bots and users and
matched by a price-time priority engine.

The project is currently in its early stages of development.

## Why

I have been investing for a while, and I wanted to understand what actually
happens when I place an order in my broker's app.

Who is on the other side? How is the price decided? How does the money move
without getting lost on the way?

CASHDAQ is my attempt to explore these questions by building a simplified
exchange from the ground up.

## How

The system is made up of several modules:

- **Matching engine:** Keeps an order book for each company and matches buy
  and sell orders by price, then by time. Trades happen at the price of the
  order that was already waiting.
- **Ledger (to-do):** Records every movement of money and shares with
  double-entry bookkeeping, so the totals always add up.
- **API (to-do):** Receives orders from bots and users, holds the money they
  need and passes them to the engine in a fixed order.
- **Market data (to-do):** Streams the order book and every trade in real time
  over WebSocket.
- **Bots and news (to-do):** Fake traders that keep the market moving, and
  fake news about the companies that they react to.

## Stack

- Java 25
- Gradle (Kotlin DSL)
- JUnit 6 and AssertJ

## Setup

Requires JDK 25. The Gradle wrapper downloads everything else.

```bash
./gradlew test
```

## Roadmap

- [x] `engine/`: matching engine with limit and market orders, partial
  fills and cancellation.
- [ ] `ledger/`: double-entry ledger with PostgreSQL, holds and settlement.
- [ ] `app/`: REST API, order sequencing and deterministic replay.
- [ ] Live market data over WebSocket and trading bots.
- [ ] News generator, web dashboard, Docker setup and benchmarks.

## Disclaimer

The system does not connect to real financial markets or handle real money.
All companies, orders and market activity are simulated.