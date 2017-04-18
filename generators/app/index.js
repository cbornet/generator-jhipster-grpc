'use strict';
var yeoman = require('yeoman-generator');
var chalk = require('chalk');
var packagejs = require(__dirname + '/../../package.json');

// Stores JHipster variables
var jhipsterVar = { moduleName: 'grpc' };

// Stores JHipster functions
var jhipsterFunc = {};

module.exports = yeoman.Base.extend({

    initializing: {
        compose: function (args) {
            this.composeWith('jhipster:modules',
                {
                    options: {
                        jhipsterVar: jhipsterVar,
                        jhipsterFunc: jhipsterFunc
                    }
                },
                this.options.testmode ? { local: require.resolve('generator-jhipster/generators/modules') } : null
            );

            // This adds support for a `--all-entities` flag
            this.option('all-entities', {
                desc: 'Apply grpc to all entities',
                type: Boolean,
                defaults: false
            });
        },
        displayLogo: function () {
            this.log('Welcome to the ' + chalk.red('JHipster grpc') + ' generator! ' + chalk.yellow('v' + packagejs.version + '\n'));
            if (jhipsterVar.testFrameworks.includes('gatling')) {
                this.log(chalk.red('Applications using Gatling are currently not supported (see https://github.com/cbornet/generator-jhipster-grpc/issues/4)'));
                this.abort = true;
            }
        }
    },

    prompting: function () {
        if (this.abort) return;
        this.entities = [];
        this.existingEntitiesNames = jhipsterFunc.getExistingEntities()
            .filter(entity => entity.definition.service === 'serviceClass' || entity.definition.service === 'serviceImpl')
            .map(entity => entity.name);

        if (this.existingEntitiesNames.length === 0) {
            this.log(chalk.yellow('No existing entities with a service layer found.'));
            return;
        }

        if (this.options.allEntities) {
            this.entities = this.existingEntitiesNames;
            return;
        }

        var done = this.async();

        var prompts = [{
            type: 'checkbox',
            name: 'entities',
            message: 'Select existing entities for which you want to add gRPC endpoints' ,
            choices: this.existingEntitiesNames
        }];

        this.prompt(prompts, props => {
            this.entities = props.entities;
            done();
        });
    },

    writing: {
        writeCommonFiles: function () {
            if (this.abort) return;
            this.mainClass = jhipsterVar.mainClassName;
            this.packageFolder = jhipsterVar.packageFolder;
            this.packageName = jhipsterVar.packageName;
            this.applicationType = jhipsterVar.applicationType;
            this.authenticationType = jhipsterVar.authenticationType;
            this.databaseType = jhipsterVar.databaseType;
            this.skipUserManagement = jhipsterVar.jhipsterConfig.skipUserManagement;
            if (this.applicationType === 'gateway' && this.authenticationType === 'uaa') {
                this.skipUserManagement = true;
            }
            var javaDir = jhipsterVar.javaDir;
            var testDir = jhipsterVar.CONSTANTS.SERVER_TEST_SRC_DIR + jhipsterVar.packageFolder + '/';
            var protoDir = jhipsterVar.CONSTANTS.MAIN_DIR + 'proto/';
            var protoPackageDir = protoDir + jhipsterVar.packageFolder + '/';

            if (this.databaseType === 'sql') {
                this.idProtoType = 'int64';
                this.idProtoWrappedType = 'Int64Value';
            } else {
                this.idProtoType = 'string';
                this.idProtoWrappedType = 'StringValue';
            }

            this.template('_date.proto', protoDir + 'util/date.proto', this, {});
            this.template('_decimal.proto', protoDir + 'util/decimal.proto', this, {});
            this.template('_pagination.proto', protoDir + 'util/pagination.proto', this, {});
            this.template('_AuthenticationInterceptor.java', javaDir + 'grpc/AuthenticationInterceptor.java');
            this.template('_ProtobufMappers.java', javaDir + 'grpc/ProtobufMappers.java');

            this.template('_configprops.proto', protoPackageDir + 'configprops.proto');
            this.template('_ConfigurationPropertiesReportService.java', javaDir + 'grpc/ConfigurationPropertiesReportService.java');
            this.template('_ConfigurationPropertiesReportServiceIntTest.java', testDir + 'grpc/ConfigurationPropertiesReportServiceIntTest.java');

            this.template('_environment.proto', protoPackageDir + 'environment.proto');
            this.template('_EnvironmentService.java', javaDir + 'grpc/EnvironmentService.java');
            this.template('_EnvironmentServiceIntTest.java', testDir + 'grpc/EnvironmentServiceIntTest.java');

            this.template('_health.proto', protoPackageDir + 'health.proto');
            this.template('_HealthService.java', javaDir + 'grpc/HealthService.java');
            this.template('_HealthServiceIntTest.java', testDir + 'grpc/HealthServiceIntTest.java');

            this.template('_loggers.proto', protoPackageDir + 'loggers.proto');
            this.template('_LoggersService.java', javaDir + 'grpc/LoggersService.java');
            this.template('_LoggersServiceIntTest.java', testDir + 'grpc/LoggersServiceIntTest.java');

            this.template('_metric.proto', protoPackageDir + 'metric.proto');
            this.template('_MetricService.java', javaDir + 'grpc/MetricService.java');
            this.template('_MetricServiceIntTest.java', testDir + 'grpc/MetricServiceIntTest.java');

            this.template('_profile_info.proto', protoPackageDir + 'profile_info.proto');
            this.template('_ProfileInfoService.java', javaDir + 'grpc/ProfileInfoService.java');
            this.template('_ProfileInfoServiceIntTest.java', testDir + 'grpc/ProfileInfoServiceIntTest.java');

            this.grpcVersion = '1.1.1';
            var grpcSpringVersion = '2.0.0';
            this.protocVersion = '3.1.0';
            var guavaVersion = '20.0';
            var nettyVersion = '4.1.8.Final';
            if (jhipsterVar.databaseType === 'cassandra') {
                // Downgrade grpc to get a compatible guava version
                this.grpcVersion = '1.0.2';
                grpcSpringVersion = '1.0.0';
                this.protocVersion = '3.0.2';
                guavaVersion = '19.0';
                nettyVersion = '4.1.6.Final';
            }

            if (jhipsterVar.buildTool === 'maven') {
                jhipsterFunc.addMavenDependency('org.lognet', 'grpc-spring-boot-starter', grpcSpringVersion);
                // Resolve conflict with springfox
                jhipsterFunc.addMavenDependency('com.google.guava', 'guava', guavaVersion);
                jhipsterFunc.addMavenDependency('io.grpc', 'grpc-protobuf', this.grpcVersion);
                jhipsterFunc.addMavenDependency('io.grpc', 'grpc-stub', this.grpcVersion);
                if (jhipsterVar.databaseType === 'cassandra' || ['microservice', 'gateway', 'uaa'].includes(jhipsterVar.applicationType)) {
                    // grpc-java needs netty 4.1
                    jhipsterFunc.addMavenDependency('io.netty', 'netty-handler', nettyVersion);
                }
                jhipsterFunc.addMavenPlugin('org.xolstice.maven.plugins', 'protobuf-maven-plugin', '0.5.0',
                    '                ' +
                    '<configuration>' + '\n                ' +
                    '    <protocArtifact>com.google.protobuf:protoc:' + this.protocVersion + ':exe:${os.detected.classifier}</protocArtifact>' + '\n                ' +
                    '    <pluginId>grpc-java</pluginId>' + '\n                ' +
                    '    <pluginArtifact>io.grpc:protoc-gen-grpc-java:'+ this.grpcVersion + ':exe:${os.detected.classifier}</pluginArtifact>' + '\n                ' +
                    '</configuration>' + '\n                ' +
                    '<executions>' + '\n                ' +
                    '    <execution>' + '\n                ' +
                    '        <goals>' + '\n                ' +
                    '            <goal>compile</goal>' + '\n                ' +
                    '            <goal>compile-custom</goal>' + '\n                ' +
                    '        </goals>' + '\n                ' +
                    '    </execution>' + '\n                ' +
                    '</executions>'
                );
                jhipsterFunc.replaceContent('pom.xml', '<build>\n        <defaultGoal>',
                    '<build>' + '\n        ' +
                    '<extensions>' + '\n        ' +
                    '    <extension>' + '\n        ' +
                    '        <groupId>kr.motd.maven</groupId>' + '\n        ' +
                    '        <artifactId>os-maven-plugin</artifactId>' + '\n        ' +
                    '        <version>1.4.1.Final</version>' + '\n        ' +
                    '    </extension>' + '\n        ' +
                    '</extensions>' + '\n        ' +
                    '<defaultGoal>'
                );

                // TODO: Remove when grpc-spring-boot-starter is available on repo.maven.apache.org
                jhipsterFunc.replaceContent('pom.xml', '</profiles>\n</project>',
                    '</profiles>' + '\n    ' +
                    '<repositories>' + '\n    ' +
                    '    <repository>' + '\n    ' +
                    '        <id>jcenter</id>' + '\n    ' +
                    '        <url>http://jcenter.bintray.com </url>' + '\n    ' +
                    '    </repository>' + '\n    ' +
                    '</repositories>' + '\n' +
                    '</project>'
                );
            } else {
                this.template('_grpc.gradle', 'gradle/grpc.gradle');
                jhipsterFunc.addGradleDependency('compile', 'org.lognet', 'grpc-spring-boot-starter', grpcSpringVersion);
                // Resolve conflict with springfox
                jhipsterFunc.addGradleDependency('compile', 'com.google.guava', 'guava', guavaVersion);
                jhipsterFunc.addGradleDependency('compile', 'io.grpc', 'grpc-protobuf', this.grpcVersion);
                jhipsterFunc.addGradleDependency('compile', 'io.grpc', 'grpc-stub', this.grpcVersion);
                if (jhipsterVar.databaseType === 'cassandra' || ['microservice', 'gateway', 'uaa'].includes(jhipsterVar.applicationType)) {
                    // grpc-java needs netty 4.1
                    jhipsterFunc.addGradleDependency('compile', 'io.netty', 'netty-handler', nettyVersion);
                }
                jhipsterFunc.addGradlePlugin('com.google.protobuf', 'protobuf-gradle-plugin', '0.8.1');
                jhipsterFunc.applyFromGradleScript('gradle/grpc');
            }

            if (this.skipUserManagement) return;

            this.template('_account.proto', protoPackageDir + 'account.proto');
            this.template('_AccountService.java', javaDir + 'grpc/AccountService.java');
            this.template('_AccountServiceIntTest.java', testDir + 'grpc/AccountServiceIntTest.java');
            //Temporary fix
            if (this.databaseType === 'cassandra') {
                this.template('_UserRepository.java', javaDir + 'repository/UserRepository.java');
            }
            //Temporary fix
            this.template('_UserServiceIntTest.java', testDir + 'service/UserServiceIntTest.java');

            if (this.databaseType === 'sql' || this.databaseType === 'mongodb') {
                this.template('_audit.proto', protoPackageDir + 'audit.proto');
                this.template('_AuditGrpcService.java', javaDir + 'grpc/AuditGrpcService.java');
                this.template('_AuditGrpcServiceIntTest.java', testDir + 'grpc/AuditGrpcServiceIntTest.java');
            }

            if (jhipsterVar.authenticationType === 'jwt') {
                this.template('_jwt.proto', protoPackageDir + 'jwt.proto');
                this.template('_JWTService.java', javaDir + 'grpc/JWTService.java');
                this.template('_JWTServiceIntTest.java', testDir + 'grpc/JWTServiceIntTest.java');
            }

            this.template('_user.proto', protoPackageDir + 'user.proto');
            this.template('_UserGrpcService.java', javaDir + 'grpc/UserGrpcService.java');
            this.template('_UserProtoMapper.java', javaDir + 'grpc/UserProtoMapper.java');
            this.template('_UserGrpcServiceIntTest.java', testDir + 'grpc/UserGrpcServiceIntTest.java');

        },

        updateExistinfEntities() {
            if (this.abort) return;
            this.entities.forEach(entityName => {jhipsterFunc.updateEntityConfig('.jhipster/' + entityName + '.json', 'grpcService', true);});
        },

        registering: function () {
            if (this.abort) return;
            try {
                jhipsterFunc.registerModule('generator-jhipster-grpc', 'entity', 'post', 'entity', 'Adds support for gRPC and generates gRPC CRUD services');
            } catch (err) {
                this.log(chalk.red.bold('WARN!') + ' Could not register as a jhipster entity post creation hook...\n');
            }
        },

        regenerateEntities: function () {
            if (this.abort) return;
            if (this.entities.length !== 0) {
                this.log(chalk.green('Regenerating entities with gRPC service'));
            }
            this.entities.forEach(function (entity) {
                this.composeWith('jhipster:entity', {
                    options: {
                        regenerate: true,
                        'skip-install': true,
                        force: this.options['force']
                    },
                    args: [entity]
                });
            }, this);
        }
    },

    end: function () {
        if (this.abort) return;
        this.log('End of grpc generator');
    }
});
