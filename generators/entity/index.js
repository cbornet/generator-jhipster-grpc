'use strict';

var yeoman = require('yeoman-generator');
var chalk = require('chalk');
var _ = require('lodash');
var pluralize = require('pluralize');
var packagejs = require(__dirname + '/../../package.json');

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
            this.log(chalk.white('Running ' + chalk.bold('JHipster grpc') + ' Generator! ' + chalk.yellow('v' + packagejs.version + '\n')));
        },

        validate: function () {
            // this shouldnt be run directly
            if (!this.entityConfig) {
                this.env.error(chalk.red.bold('ERROR!') + ' This sub generator should be used only from JHipster and cannot be run directly...\n');
            }
        }
    },

    prompting: function () {
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
                message: 'Do you want to generate a grpc service for this entity ?',
                default: true
            }
        ];

        this.prompt(prompts, function (props) {
            this.props = props;
            // To access props later use this.props.someOption;

            done();
        }.bind(this));
    },
    writing: {
        updateFiles: function () {
            this.baseName = jhipsterVar.baseName;
            this.packageName = jhipsterVar.packageName;
            this.entityClass = this.entityConfig.entityClass;
            this.entityClassPlural = pluralize(this.entityClass);
            this.entityInstance = this.entityConfig.entityInstance;
            this.entityUnderscoredName = _.snakeCase(this.entityClass).toLowerCase();
            this.javadoc = this.entityConfig.javadoc;
            this.fields = this.entityConfig.data.fields;
            this.fields.forEach(f => f.fieldProtobufType = getProtobufType(f.fieldType));
            this.fields.forEach(f => f.isProtobufCustomType = isProtobufCustomType(f.fieldProtobufType));
        },

        writeFiles: function () {
            let javaDir = jhipsterVar.javaDir;
            this.template('_entity.proto', 'src/main/proto/' + jhipsterVar.packageFolder + '/entity/' + this.entityUnderscoredName + '.proto', this, {});
            this.template('_entityProtoMapper.java', javaDir + '/grpc/entity/' + this.entityUnderscoredName + '/'+ this.entityClass + 'ProtoMapper.java', this, {});
        },

        updateConfig: function () {
            jhipsterFunc.updateEntityConfig(this.entityConfig.filename, 'grpcService', this.grpcService);
        }
    },

    end: function () {
        if (this.yourOptionKey) {
            this.log('\n' + chalk.bold.green('grpc enabled'));
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
        return 'sint32';
    case 'Long':
        return 'sint64';
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
        return 'bytes';
    default:
        // It's an enum
        return type + 'Proto';
    }

}

function isProtobufCustomType(type) {
    return type.startsWith('google') || type.startsWith('util');
    //return ['string', 'sint32', 'sint64', 'float', 'double', 'bool', 'bytes'].includes(type);
}
