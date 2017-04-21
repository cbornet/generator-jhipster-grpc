# generator-jhipster-grpc
[![NPM version][npm-image]][npm-url] [![Build Status][travis-image]][travis-url] [![Dependency Status][daviddm-image]][daviddm-url]
> JHipster module, Adds support for gRPC and generates gRPC CRUD services

# Introduction

This is a [JHipster](http://jhipster.github.io/) module, that is meant to be used in a JHipster application.

WARN : Under developpement. See list of limitations and TODOs

# Prerequisites

As this is a [JHipster](http://jhipster.github.io/) module, we expect you have JHipster and its related tools already installed:

- [Installing JHipster](https://jhipster.github.io/installation.html)

# Installation

To install this module:

```bash
yarn add global generator-jhipster-grpc
```

Compatibility :
- JHipster 4.1.0, 4.1.1 and inferior (Note : never tested with <4.1.0) : module version 0.7.0
- JHipster 4.2.0, 4.3.0 and superior : module version 0.8.0, 0.9.0+

# Usage

At the root of your project directory:
```bash
yo jhipster-grpc
```
This will configure [grpc-java](https://github.com/grpc/grpc-java) and [grpc-spring-boot-starter](https://github.com/LogNet/grpc-spring-boot-starter) 
so that the proto files present in `src/main/proto` are compiled.
If you want to add CRUD gRPC services for an entity, just (re)generate it and confirm when the question is asked.

Notes :
* doesn't work for applications using Gatling (see https://github.com/cbornet/generator-jhipster-grpc/issues/4)
* entities must have a service layer (serviceClass or serviceImpl) or else they will be ignored by the generator (not having a common service layer between the REST resources and the gRPC endpoints would be bad design).
* just like with DTOs, entities that are referenced by another entity in a relationship (many-to-many owned side, many-to-one or one-to-ine owned side) currently must be grpc activated and thus have a service layer. The service layer constraint should be relaxed in a future release since it's only the gRPC mapper which is required.

TODOs:
- [x] ~~Generate existing entities~~
- [x] ~~Support Gradle~~
- [x] ~~Entities without DTOs~~
- [x] ~~Entities with pagination~~
- [x] ~~Support Cassandra~~
- [x] ~~Support Mongo~~
- [x] ~~Support relationships~~
- [x] ~~JWT security~~
- [x] ~~OAuth2 security~~
- [x] ~~Basic auth security~~ (used for session auth option)
- [x] ~~Entity javadoc~~
- [x] ~~Field javadoc~~
- [ ] Add ElasticSearch endpoints
- [x] ~~Management endpoints~~
  - [x] ~~Account~~
  - [x] ~~Loggers~~
  - [x] ~~Audits~~
  - [x] ~~Users~~
  - [x] ~~Profile info~~
  - [x] ~~Spring Boot Actuators~~
- [ ] Support streaming from the DB (Stream<> in repository)
- [ ] Support streaming back-pressure (reactive streams with rxJava2 or Reactor)
- [ ] Client-side configuration (micro-services)
- [ ] Client-side load-balancing with service discovery (micro-services)
- [ ] Generator tests
- [ ] Sample/demo project
- [x] ~~Generated code tests~~
- [x] ~~Travis CI~~

Mappings:

| JHipster | Protobuf      | 
|:--------:|:-------------:|
| Integer  | int32 |
| Long     | int64 |
| String   | string |
| Float   | float |
| Double   | double |
| Boolean   | bool |
| Blob (byte[]) | bytes |
| ByteBuffer | bytes |
| ZonedDateTime | google.protobuf.Timestamp |
| LocalDate | util.Date |
| BigDecimal | util.Decimal |
| enum | enum |

util.Date and util.Decimal are custom definitions. 
Non-required protobuf scalar types and enums are wrapped in OneOf types to provide nullability.

# License

Apache-2.0 © [Christophe Bornet]


[npm-image]: https://img.shields.io/npm/v/generator-jhipster-grpc.svg
[npm-url]: https://npmjs.org/package/generator-jhipster-grpc
[travis-image]: https://travis-ci.org/cbornet/generator-jhipster-grpc.svg?branch=master
[travis-url]: https://travis-ci.org/cbornet/generator-jhipster-grpc
[daviddm-image]: https://david-dm.org/cbornet/generator-jhipster-grpc.svg?theme=shields.io
[daviddm-url]: https://david-dm.org/cbornet/generator-jhipster-module
