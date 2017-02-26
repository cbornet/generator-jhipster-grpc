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

# Usage

At the root of your project directory:
```bash
yo jhipster-grpc
```
This will configure [grpc-java](https://github.com/grpc/grpc-java) and [grpc-spring-boot-starter](https://github.com/LogNet/grpc-spring-boot-starter) 
so that the proto files present in `src/main/proto` are compiled.
If you want to add CRUD gRPC services for an entity, just (re)generate it and confirm when the question is asked.

Current limitations:
* Maven only
* only entities with DTOs and service
* no Cassandra (needs datastax driver 3.2 release because of Guava incompatibility)
* no relationships

TODOs:
- [ ] Generate existing entities
- [ ] Support Gradle
- [ ] Entities without DTOs
- [ ] Entities without service
- [x] ~~Test with Cassandra~~
- [x] ~~Test with Mongo~~
- [ ] Support relationships
- [x] ~~JWT security~~
- [x] ~~OAuth2 security~~
- [x] ~~Basic auth security~~ (used for session auth option)
- [x] ~~Entity javadoc~~
- [x] ~~Field javadoc~~
- [ ] Management endpoints (account, audits, logs, ...)
- [ ] Support streaming from the DB (Stream<> in repository)
- [ ] Support streaming back-pressure (reactive streams with rxJava2 or Reactor)
- [ ] Client-side configuration (micro-services)
- [ ] Client-side load-balancing with service discovery (micro-services)
- [ ] Generator tests
- [ ] Sample/demo project
- [ ] Generated code tests
- [ ] CI

Mappings:

| JHipster | Protobuf      | 
|:--------:|:-------------:|
| Integer  | sint32 |
| Long     | sint64 |
| String   | string |
| Float   | float |
| Double   | double |
| Boolean   | bool |
| Blob (byte[]) | bytes |
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
