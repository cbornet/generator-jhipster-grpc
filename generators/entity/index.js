'use strict';

const yeoman = require('yeoman-generator');
const chalk = require('chalk');
const _ = require('lodash');
const randexp = require('randexp');
const fs = require('fs');
const pluralize = require('pluralize');
const packagejs = require(__dirname + '/../../package.json');

// Stores JHipster variables
var jhipsterVar = { moduleName: 'grpc' };

// Stores JHipster functions
var jhipsterFunc = {};

module.exports = yeoman.Base.extend({

    initializing: {

        compose: function (args) {
            this.entityConfig = this.options.entityConfig;
            this.composeWith('jhipster:modules', {
                options: {
                    jhipsterVar: jhipsterVar,
                    jhipsterFunc: jhipsterFunc
                }
            });
        },

        displayLogo: function () {
            this.log(chalk.white('Running ' + chalk.bold('JHipster gRPC') + ' Generator! ' + chalk.yellow('v' + packagejs.version + '\n')));
        },

        validate: function () {
            // this should not be run directly
            if (!this.entityConfig) {
                this.env.error(chalk.red.bold('ERROR!') + ' This sub generator should be used only from JHipster and cannot be run directly...\n');
            }
            this.noEntityService = this.entityConfig.data.service !== 'serviceClass' && this.entityConfig.data.service !== 'serviceImpl';
            if (this.noEntityService) {
                this.log(chalk.yellow('Entity ' + this.entityConfig.entityClass + ' doesn\'t have a service layer so skipping gRPC generator !'));
            }
        }
    },

    prompting: function () {
        if (this.noEntityService) {
            return;
        }
        // don't prompt if data are imported from a file
        if (this.entityConfig.useConfigurationFile === true && this.entityConfig.data && typeof this.entityConfig.data.grpcService !== 'undefined') {
            this.grpcService = this.entityConfig.data.grpcService;
            return;
        }
        var done = this.async();
        var prompts = [
            {
                type: 'confirm',
                name: 'grpcService',
                message: 'Do you want to generate a gRPC service for this entity ?',
                default: true
            }
        ];

        this.prompt(prompts, function (props) {
            this.props = props;
            this.grpcService = this.props.grpcService;
            done();
        }.bind(this));
    },
    writing: {
        updateFiles: function () {
            if (!this.grpcService) {
                return;
            }
            this.mainClass = jhipsterVar.mainClassName;
            this.packageName = jhipsterVar.packageName;
            this.databaseType = jhipsterVar.databaseType;
            this.applicationType = jhipsterVar.applicationType;
            this.authenticationType = jhipsterVar.authenticationType;
            this.searchEngine = jhipsterVar.searchEngine;
            this.entityClass = this.entityConfig.entityClass;
            this.entityClassPlural = pluralize(this.entityClass);
            this.entityInstance = this.entityConfig.entityInstance;
            this.entityInstancePlural = pluralize(this.entityInstance);
            this.entityUnderscoredName = _.snakeCase(this.entityClass).toLowerCase();
            this.dto = this.entityConfig.data.dto || 'no';
            this.instanceType = (this.dto === 'mapstruct') ? this.entityClass + 'DTO' : this.entityClass;
            this.instanceName = (this.dto === 'mapstruct') ? this.entityInstance + 'DTO' : this.entityInstance;
            this.fieldsContainZonedDateTime = this.entityConfig.fieldsContainZonedDateTime;
            this.fieldsContainLocalDate = this.entityConfig.fieldsContainLocalDate;
            this.fieldsContainBigDecimal = this.entityConfig.fieldsContainBigDecimal;
            this.fieldsContainBlob = this.entityConfig.fieldsContainBlob;
            this.pagination = this.entityConfig.data.pagination || 'no';
            if(this.entityConfig.data.javadoc === undefined) {
                this.entityJavadoc = '// Protobuf message for entity ' + this.entityClass;
            } else {
                this.entityJavadoc = '// ' + this.entityConfig.data.javadoc.replace('\n', '\n// ');
            }
            this.fluentMethods = this.entityConfig.data.fluentMethods;
            this.fields = this.entityConfig.data.fields;
            this.fields.forEach(f => {
                if (f.fieldTypeBlobContent === 'text') {
                    f.fieldDomainType = 'String';
                } else {
                    f.fieldDomainType = f.fieldType;
                }
                f.fieldProtobufType = getProtobufType(f.fieldDomainType);
                f.isProtobufCustomType = isProtobufCustomType(f.fieldProtobufType);
            });
            this.relationships = this.entityConfig.data.relationships;
            this.relationships.forEach(r => {
                r.relationshipNameUnderscored = _.snakeCase(r.relationshipName).toLowerCase();
                r.otherEntityNameUnderscored = _.snakeCase(r.otherEntityName).toLowerCase();
                r.otherEntityFieldUnderscored = _.snakeCase(r.otherEntityField).toLowerCase();
                if(r.otherEntityNameCapitalized === 'User') {
                    r.otherEntityProtobufType = this.packageName + '.UserProto';
                    r.otherEntityProtoMapper = this.packageName + '.grpc.UserProtoMapper';
                    r.otherEntityTest = this.packageName + '.grpc.UserGrpcServiceIntTest';
                    r.otherEntityProtobufFile = jhipsterVar.packageFolder + '/user.proto';
                } else {
                    r.otherEntityProtobufType = this.packageName + '.entity.' + r.otherEntityNameCapitalized + 'Proto';
                    r.otherEntityProtoMapper = this.packageName + '.grpc.entity.' + r.otherEntityNameUnderscored +
                        '.' + r.otherEntityNameCapitalized + 'ProtoMapper';
                    r.otherEntityTest = this.packageName + '.grpc.entity.' + r.otherEntityNameUnderscored +
                        '.' + r.otherEntityNameCapitalized + 'GrpcServiceIntTest';
                    r.otherEntityProtobufFile = jhipsterVar.packageFolder + '/entity/' + r.otherEntityNameUnderscored + '.proto';
                }
            });

            if (this.databaseType === 'sql') {
                this.idProtoType = 'int64';
                this.idProtoWrappedType = 'Int64Value';
            } else {
                this.idProtoType = 'string';
                this.idProtoWrappedType = 'StringValue';
            }
        },

        writeFiles: function () {
            if (!this.grpcService) {
                return;
            }
            let grpcEntityDir = jhipsterVar.javaDir + '/grpc/entity/' + this.entityUnderscoredName ;
            let grpcEntityTestDir = jhipsterVar.CONSTANTS.SERVER_TEST_SRC_DIR + jhipsterVar.packageFolder + '/' + '/grpc/entity/' + this.entityUnderscoredName ;
            this.template('_entity.proto', 'src/main/proto/' + jhipsterVar.packageFolder + '/entity/' + this.entityUnderscoredName + '.proto', this, {});
            this.template('_entityProtoMapper.java', grpcEntityDir + '/'+ this.entityClass + 'ProtoMapper.java', this, {});
            this.template('_entityGrpcService.java', grpcEntityDir + '/'+ this.entityClass + 'GrpcService.java', this, {});
            this.template('_entityGrpcServiceIntTest.java', grpcEntityTestDir + '/'+ this.entityClass + 'GrpcServiceIntTest.java', this, { context: { randexp, _, chalkRed: chalk.red, fs, SERVER_TEST_SRC_DIR: grpcEntityTestDir } });
        },

        updateConfig: function () {
            jhipsterFunc.updateEntityConfig(this.entityConfig.filename, 'grpcService', this.grpcService);
        }
    },

    end: function () {
        if (this.grpcService) {
            this.log('\n' + chalk.bold.green('gRPC enabled for entity ' + this.entityClass));
        }
    }
});

function getProtobufType(type) {
    switch (type) {
    case 'String':
    case 'Float':
    case 'Double':
        return type.toLowerCase();
    case 'Integer':
        return 'int32';
    case 'Long':
        return 'int64';
    case 'Boolean':
        return 'bool';
    case 'Instant':
    case 'OffsetDateTime':
    case 'ZonedDateTime':
        return 'google.protobuf.Timestamp';
    case 'LocalDate':
        return 'util.Date';
    case 'BigDecimal':
        return 'util.Decimal';
    case 'byte[]':
    case 'ByteBuffer':
        return 'bytes';
    case 'UUID':
        return 'string';
    default:
        // It's an enum
        return type + 'Proto';
    }

}

function isProtobufCustomType(type) {
    return type.startsWith('google') || type.startsWith('util');
}
