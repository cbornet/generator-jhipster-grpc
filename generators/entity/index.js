'use strict';

const chalk = require('chalk');
const packagejs = require('../../package.json');
const BaseGenerator = require('generator-jhipster/generators/generator-base');
const jhipsterConstants = require('generator-jhipster/generators/generator-constants');
const _ = require('lodash');
const randexp = require('randexp');
const fs = require('fs');
const pluralize = require('pluralize');

module.exports = class extends BaseGenerator {
    get initializing() {
        return {
            readConfig() {
                this.entityConfig = this.options.entityConfig;
                this.jhipsterAppConfig = this.getJhipsterAppConfig();
                if (!this.jhipsterAppConfig) {
                    this.error('Can\'t read .yo-rc.json');
                }
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
        };
    }

    prompting() {
        if (this.noEntityService) {
            return;
        }
        // don't prompt if data are imported from a file
        if (this.entityConfig.useConfigurationFile === true && this.entityConfig.data && typeof this.entityConfig.data.grpcService !== 'undefined') {
            this.grpcService = this.entityConfig.data.grpcService;
            return;
        }
        const done = this.async();
        const prompts = [
            {
                type: 'confirm',
                name: 'grpcService',
                message: 'Do you want to generate a gRPC service for this entity ?',
                default: true
            }
        ];

        this.prompt(prompts).then((props) => {
            this.props = props;
            this.grpcService = this.props.grpcService;
            done();
        });
    }

    get writing() {
        return {
            updateFiles: function () {
                if (!this.grpcService) {
                    return;
                }
                this.baseName = this.jhipsterAppConfig.baseName;
                this.mainClass = this.getMainClassName();
                this.packageFolder = this.jhipsterAppConfig.packageFolder;
                this.packageName = this.jhipsterAppConfig.packageName;
                this.databaseType = this.jhipsterAppConfig.databaseType;
                this.applicationType = this.jhipsterAppConfig.applicationType;
                this.authenticationType = this.jhipsterAppConfig.authenticationType;
                this.searchEngine = this.jhipsterAppConfig.searchEngine;
                this.entityClass = this.entityConfig.entityClass;
                this.entityClassPlural = pluralize(this.entityClass);
                this.entityInstance = this.entityConfig.entityInstance;
                this.entityInstancePlural = pluralize(this.entityInstance);
                this.entityUnderscoredName = _.snakeCase(this.entityClass).toLowerCase();
                this.dto = this.entityConfig.data.dto || 'no';
                this.instanceType = (this.dto === 'mapstruct') ? this.entityClass + 'DTO' : this.entityClass;
                this.instanceName = (this.dto === 'mapstruct') ? this.entityInstance + 'DTO' : this.entityInstance;
                this.fieldsContainInstant = this.entityConfig.fieldsContainInstant;
                this.fieldsContainZonedDateTime = this.entityConfig.fieldsContainZonedDateTime;
                this.fieldsContainLocalDate = this.entityConfig.fieldsContainLocalDate;
                this.fieldsContainBigDecimal = this.entityConfig.fieldsContainBigDecimal;
                this.fieldsContainBlob = this.entityConfig.fieldsContainBlob;
                this.pagination = this.entityConfig.data.pagination || 'no';
                this.jpaMetamodelFiltering = (this.databaseType === 'sql') && (this.entityConfig.data.jpaMetamodelFiltering || false);
                this.entitySearchType = (this.pagination === 'no') ? 'StringValue' : this.entityClass + 'SearchPageRequest';
                if(this.entityConfig.data.javadoc === undefined) {
                    this.entityJavadoc = '// Protobuf message for entity ' + this.entityClass;
                } else {
                    this.entityJavadoc = '// ' + this.entityConfig.data.javadoc.replace('\n', '\n// ');
                }
                this.fluentMethods = this.entityConfig.data.fluentMethods;
                this.fields = this.entityConfig.data.fields || [];
                this.fields.forEach(f => {
                    if (f.fieldTypeBlobContent === 'text') {
                        f.fieldDomainType = 'String';
                    } else {
                        f.fieldDomainType = f.fieldType;
                    }
                    f.fieldTypeUpperUnderscored = _.snakeCase(f.fieldType).toUpperCase();
                    f.fieldProtobufType = getProtobufType(f.fieldDomainType);
                    f.isProtobufCustomType = isProtobufCustomType(f.fieldProtobufType);
                });
                this.relationships = this.entityConfig.data.relationships || [];
                this.relationships.forEach(r => {
                    r.relationshipNameUnderscored = _.snakeCase(r.relationshipName).toLowerCase();
                    r.otherEntityNameUnderscored = _.snakeCase(r.otherEntityName).toLowerCase();
                    r.otherEntityFieldUnderscored = _.snakeCase(r.otherEntityField).toLowerCase();
                    if(r.otherEntityNameCapitalized === 'User') {
                        r.otherEntityProtobufType = this.packageName + '.UserProto';
                        r.otherEntityProtoMapper = this.packageName + '.grpc.UserProtoMapper';
                        r.otherEntityTest = this.packageName + '.grpc.UserGrpcServiceIntTest';
                        r.otherEntityProtobufFile = this.packageFolder + '/user.proto';
                    } else {
                        r.otherEntityProtobufType = this.packageName + '.entity.' + r.otherEntityNameCapitalized + 'Proto';
                        r.otherEntityProtoMapper = this.packageName + '.grpc.entity.' + r.otherEntityNameUnderscored +
                            '.' + r.otherEntityNameCapitalized + 'ProtoMapper';
                        r.otherEntityTest = this.packageName + '.grpc.entity.' + r.otherEntityNameUnderscored +
                            '.' + r.otherEntityNameCapitalized + 'GrpcServiceIntTest';
                        r.otherEntityProtobufFile = this.packageFolder + '/entity/' + r.otherEntityNameUnderscored + '.proto';
                    }
                });

                if (this.databaseType === 'sql') {
                    this.idProtoType = 'int64';
                    this.idProtoWrappedType = 'Int64Value';
                } else {
                    this.idProtoType = 'string';
                    this.idProtoWrappedType = 'StringValue';
                }

                this.isFilterableType = function(fieldType) {
                    return !(['byte[]', 'ByteBuffer'].includes(fieldType));
                }
            },

            writeFiles: function () {
                if (!this.grpcService) {
                    return;
                }
                // function to use directly template
                this.template = function (source, destination, context) {
                    this.fs.copyTpl(
                        this.templatePath(source),
                        this.destinationPath(destination),
                        this,
                        context
                    );
                };
                const grpcEntityDir = jhipsterConstants.SERVER_MAIN_SRC_DIR + this.packageFolder + '/grpc/entity/' + this.entityUnderscoredName ;
                const grpcEntityTestDir = jhipsterConstants.SERVER_TEST_SRC_DIR + this.packageFolder + '/grpc/entity/' + this.entityUnderscoredName ;
                this.log(grpcEntityDir);
                this.template('_entity.proto', 'src/main/proto/' + this.packageFolder + '/entity/' + this.entityUnderscoredName + '.proto');
                this.template('_entityProtoMapper.java', grpcEntityDir + '/'+ this.entityClass + 'ProtoMapper.java');
                this.template('_entityGrpcService.java', grpcEntityDir + '/'+ this.entityClass + 'GrpcService.java');
                this.template('_entityGrpcServiceIntTest.java', grpcEntityTestDir + '/'+ this.entityClass + 'GrpcServiceIntTest.java', { context: { randexp, _, chalkRed: chalk.red, fs, SERVER_TEST_SRC_DIR: grpcEntityTestDir } });
            },

            updateConfig: function () {
                this.updateEntityConfig(this.entityConfig.filename, 'grpcService', this.grpcService);
            }
        };
    }

    end() {
        if (this.grpcService) {
            this.log('\n' + chalk.bold.green('gRPC enabled for entity ' + this.entityClass));
        }
    }
};

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
