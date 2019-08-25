## Solution

To start the process that charges a subscription fee every month I used coroutines and to know if it is the first of the month and know the distance to the next first of the month I implemented `TimeService`.  You can modify the return values of its function to simulate different scenarios.

I imagined this app running in multiple servers, so to prevent race conditions I add another invoice status called `IN_PROCESS` as a locking mechanism. The next iteration could have a KVS to lock the invoices. It also could be the case that the app runs multiple coroutines at the same time. The lock also could be used in this scenario. Moreover, I add a function that allows fetching invoices in batch by status.

To avoid prevent `CurrencyMismatchException` I added and mocked `CurrencyProvider`. This external provider converts an amount from one currency to another. When `InvoiceCurrency` is different than `Customer.Currency` the app updates the invoice currency and value to its customer currency.

When CustomerNotFoundException arises. The invoice is marked as `INCONSISTENT`. This is a new `InvoiceStatus` which I added. The next release could implement a feature to handle this situation.

When `PaymentService` fails or any other error arises, the Invoice is marked as a `FAIL` and it will be reprocessed after all `PENDING` invoices. The next version could add alarms, metric and a different set of fail status and implements different ways to handle the different errors. Ex. `NetworkException` shouldn't be treated in the same way that an expired card.

I added logs and tests to visualize how the app works.

## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment
- = Java 8 test environment

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.


*Running through docker*

Install docker for your platform

```
make docker-run
```

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```


### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the "rest api" models used throughout the application.
|
├── pleo-antaeus-rest
|        Entry point for REST API. This is where the routes are defined.
└──
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!
