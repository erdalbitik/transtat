# Transactions - Statistics Challenge Project
[![Build Status](https://travis-ci.org/erdalbitik/transtat.svg?branch=master)](https://travis-ci.org/erdalbitik/transtat)

Sample Rest API about realtime statistic from the last 60 seconds

## Usage

### Maven Build

```sh
mvn clean install
```

### Spring Boot Run

```sh
mvn spring-boot:run
```
Application will start after this command. Default port is 8080

## API Usage

An API documentation (Swagger) page is hosted under http://localhost:8080/index.html

### Transaction Rest API
```sh
POST http://localhost:8080/transactions

Request : 
{
  "amount": 12.3,
  "timestamp": 1478192204000
}

Response : 
 : http 201
 : http 204 (for timestamp value which is older than 60 seconds)

```

### Statistic Rest API
```sh
GET http://localhost:8080/statistics

Response : 
{
  "sum": 1000,
  "avg": 100,
  "max": 200,
  "min": 50,
  "count": 10
}
```
