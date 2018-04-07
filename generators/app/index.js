const chalk = require('chalk');
const packagejs = require('../../package.json');
const semver = require('semver');
const BaseGenerator = require('generator-jhipster/generators/generator-base');
const jhipsterConstants = require('generator-jhipster/generators/generator-constants');

module.exports = class extends BaseGenerator {
    get initializing() {
        return {
            init() {
                // This adds support for a `--all-entities` flag
                this.option('all-entities', {
                    desc: 'Apply grpc to all entities',
                    type: Boolean,
                    defaults: false
                });
            },
            readConfig() {
                this.jhipsterAppConfig = this.getJhipsterAppConfig();
                if (!this.jhipsterAppConfig) {
                    this.error('Can\'t read .yo-rc.json');
                }
            },
            displayLogo: function () {
                this.log('Welcome to the ' + chalk.red('JHipster grpc') + ' generator! ' + chalk.yellow('v' + packagejs.version + '\n'));
                if (this.jhipsterAppConfig.testFrameworks && this.jhipsterAppConfig.testFrameworks.includes('gatling')) {
                    this.log(chalk.red('Applications using Gatling are currently not supported (see https://github.com/cbornet/generator-jhipster-grpc/issues/4)'));
                    this.abort = true;
                }
            },
            checkJhipster() {
                const currentJhipsterVersion = this.jhipsterAppConfig.jhipsterVersion;
                const minimumJhipsterVersion = packagejs.dependencies['generator-jhipster'];
                if (!semver.satisfies(currentJhipsterVersion, minimumJhipsterVersion)) {
                    this.warning(`\nYour generated project used an old JHipster version (${currentJhipsterVersion})... you need at least (${minimumJhipsterVersion})\n`);
                }
            }
        };
    }

    prompting() {
        if (this.abort) return;
        this.entities = [];
        this.existingEntitiesNames = this.getExistingEntities()
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

        const done = this.async();

        const prompts = [{
            type: 'checkbox',
            name: 'entities',
            message: 'Select existing entities for which you want to add gRPC endpoints' ,
            choices: this.existingEntitiesNames
        }];

        this.prompt(prompts).then((props) => {
            this.entities = props.entities;
            done();
        });
    }

    get writing() {
        return {
            writeCommonFiles() {
                if (this.abort) return;
                // function to use directly template
                this.template = function (source, destination) {
                    this.fs.copyTpl(
                        this.templatePath(source),
                        this.destinationPath(destination),
                        this
                    );
                };
                this.baseName = this.jhipsterAppConfig.baseName;
                this.mainClass = this.getMainClassName();
                this.packageFolder = this.jhipsterAppConfig.packageFolder;
                this.packageName = this.jhipsterAppConfig.packageName;
                this.applicationType = this.jhipsterAppConfig.applicationType;
                this.authenticationType = this.jhipsterAppConfig.authenticationType;
                this.databaseType = this.jhipsterAppConfig.databaseType;
                this.searchEngine = this.jhipsterAppConfig.searchEngine;
                this.skipUserManagement = this.jhipsterAppConfig.skipUserManagement;
                if (this.applicationType === 'gateway' && this.authenticationType === 'uaa') {
                    this.skipUserManagement = true;
                }
                this.buildTool = this.jhipsterAppConfig.buildTool;

                const javaDir = `${jhipsterConstants.SERVER_MAIN_SRC_DIR + this.packageFolder}/`;
                const testDir = `${jhipsterConstants.SERVER_TEST_SRC_DIR + this.packageFolder}/`;
                const protoDir = jhipsterConstants.MAIN_DIR + 'proto/';
                const protoPackageDir = protoDir + this.packageFolder + '/';

                if (this.databaseType === 'sql') {
                    this.idProtoType = 'int64';
                    this.idProtoWrappedType = 'Int64Value';
                } else {
                    this.idProtoType = 'string';
                    this.idProtoWrappedType = 'StringValue';
                }

                this.template('_date.proto', protoDir + 'util/date.proto');
                this.template('_decimal.proto', protoDir + 'util/decimal.proto');
                this.template('_pagination.proto', protoDir + 'util/pagination.proto');
                this.template('_queryfilter.proto', protoDir + 'util/queryfilter.proto');

                this.template('_AuthenticationInterceptor.java', javaDir + 'grpc/AuthenticationInterceptor.java');
                this.template('_AuthenticationInterceptorTest.java', testDir + 'grpc/AuthenticationInterceptorTest.java');

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

                this.grpcVersion = '1.6.1';
                const grpcSpringVersion = '2.0.0';
                this.protocVersion = '3.1.0';
                const guavaVersion = '20.0';
                const nettyVersion = '4.1.8.Final';
                this.reactiveGrpcVersion = '0.7.2';

                if (this.buildTool === 'maven') {
                    this.addMavenDependency('org.lognet', 'grpc-spring-boot-starter', grpcSpringVersion);
                    // Resolve conflict with springfox
                    this.addMavenDependency('com.google.guava', 'guava', guavaVersion);
                    this.addMavenDependency('com.google.protobuf', 'protobuf-java', this.protocVersion);
                    this.addMavenDependency('io.grpc', 'grpc-core', this.grpcVersion);
                    this.addMavenDependency('io.grpc', 'grpc-context', this.grpcVersion);
                    this.addMavenDependency('io.grpc', 'grpc-netty', this.grpcVersion);
                    this.addMavenDependency('io.grpc', 'grpc-protobuf', this.grpcVersion);
                    this.addMavenDependency('io.grpc', 'grpc-stub', this.grpcVersion);
                    this.addMavenDependency('io.projectreactor', 'reactor-core', '3.1.1.RELEASE');
                    this.addMavenDependency('com.salesforce.servicelibs', 'reactor-grpc-stub', this.reactiveGrpcVersion);
                    if (this.databaseType === 'cassandra' || ['microservice', 'gateway', 'uaa'].includes(this.applicationType)) {
                        // grpc-java needs netty 4.1
                        this.addMavenDependency('io.netty', 'netty-handler', nettyVersion);
                    }
                    this.addMavenPlugin('org.xolstice.maven.plugins', 'protobuf-maven-plugin', '0.5.0',
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
                        '        <configuration>' + '\n                ' +
                        '            <protocPlugins>' + '\n                ' +
                        '                <protocPlugin>' + '\n                ' +
                        '                    <id>reactor-grpc</id>' + '\n                ' +
                        '                    <groupId>com.salesforce.servicelibs</groupId>' + '\n                ' +
                        '                    <artifactId>reactor-grpc</artifactId>' + '\n                ' +
                        '                    <version>' + this.reactiveGrpcVersion + '</version>' + '\n                ' +
                        '                    <mainClass>com.salesforce.reactorgrpc.ReactorGrpcGenerator</mainClass>' + '\n                ' +
                        '                </protocPlugin>' + '\n                ' +
                        '            </protocPlugins>' + '\n                ' +
                        '        </configuration>' + '\n                ' +
                        '    </execution>' + '\n                ' +
                        '</executions>'
                    );
                    this.replaceContent('pom.xml', '<build>\n        <defaultGoal>',
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
                    this.replaceContent('pom.xml', '</profiles>\n</project>',
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
                    this.copy('.mvn/mvnw', '.mvn/mvnw');
                    this.copy('.mvn/mvnw.cmd', '.mvn/mvnw.cmd');
                    this.copy('.mvn/wrapper/maven-wrapper.jar', '.mvn/wrapper/maven-wrapper.jar');
                    this.copy('.mvn/wrapper/maven-wrapper.properties', '.mvn/wrapper/maven-wrapper.properties');
                    this.template('_reactive-grpc-pom.xml', 'gradle/reactive-grpc-pom.xml');
                    this.template('_grpc.gradle', 'gradle/grpc.gradle');
                    this.addGradleDependency('compile', 'org.lognet', 'grpc-spring-boot-starter', grpcSpringVersion);
                    // Resolve conflict with springfox
                    this.addGradleDependency('compile', 'com.google.guava', 'guava', guavaVersion);
                    this.addGradleDependency('compile', 'com.google.protobuf', 'protobuf-java', this.protocVersion);
                    this.addGradleDependency('compile', 'io.grpc', 'grpc-core', this.grpcVersion);
                    this.addGradleDependency('compile', 'io.grpc', 'grpc-context', this.grpcVersion);
                    this.addGradleDependency('compile', 'io.grpc', 'grpc-netty', this.grpcVersion);
                    this.addGradleDependency('compile', 'io.grpc', 'grpc-protobuf', this.grpcVersion);
                    this.addGradleDependency('compile', 'io.grpc', 'grpc-stub', this.grpcVersion);
                    this.addGradleDependency('compile', 'io.projectreactor', 'reactor-core', '3.1.1.RELEASE');
                    this.addGradleDependency('compile', 'com.salesforce.servicelibs', 'reactor-grpc-stub', this.reactiveGrpcVersion);
                    if (this.databaseType === 'cassandra' || ['microservice', 'gateway', 'uaa'].includes(this.applicationType)) {
                        // grpc-java needs netty 4.1
                        this.addGradleDependency('compile', 'io.netty', 'netty-handler', nettyVersion);
                    }
                    this.addGradlePlugin('com.google.protobuf', 'protobuf-gradle-plugin', '0.8.1');
                    this.applyFromGradleScript('gradle/grpc');
                }

                if (!this.skipUserManagement || this.authenticationType === 'oauth2') {
                    this.template('_account.proto', protoPackageDir + 'account.proto');
                    this.template('_AccountService.java', javaDir + 'grpc/AccountService.java');
                    this.template('_AccountServiceIntTest.java', testDir + 'grpc/AccountServiceIntTest.java');
                    this.template('_user.proto', protoPackageDir + 'user.proto');
                    this.template('_UserProtoMapper.java', javaDir + 'grpc/UserProtoMapper.java');
                    if (this.applicationType === 'monolith' || this.authenticationType !== 'oauth2') {
                        this.template('_UserGrpcService.java', javaDir + 'grpc/UserGrpcService.java');
                        this.template('_UserGrpcServiceIntTest.java', testDir + 'grpc/UserGrpcServiceIntTest.java');
                        if (this.databaseType === 'sql' || this.databaseType === 'mongodb') {
                            this.template('_audit.proto', protoPackageDir + 'audit.proto');
                            this.template('_AuditGrpcService.java', javaDir + 'grpc/AuditGrpcService.java');
                            this.template('_AuditGrpcServiceIntTest.java', testDir + 'grpc/AuditGrpcServiceIntTest.java');
                        }
                        if (this.authenticationType === 'jwt') {
                            this.template('_jwt.proto', protoPackageDir + 'jwt.proto');
                            this.template('_JWTService.java', javaDir + 'grpc/JWTService.java');
                            this.template('_JWTServiceIntTest.java', testDir + 'grpc/JWTServiceIntTest.java');
                        }
                    }
                }

            },

            updateExistinfEntities() {
                if (this.abort) return;
                this.entities.forEach(entityName => {this.updateEntityConfig('.jhipster/' + entityName + '.json', 'grpcService', true);});
            },

            registering: function () {
                if (this.abort) return;
                try {
                    this.registerModule('generator-jhipster-grpc', 'entity', 'post', 'entity', 'Adds support for gRPC and generates gRPC CRUD services');
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
                        regenerate: true,
                        'skip-install': true,
                        force: this.options['force'],
                        'skip-server':true,
                        'skip-client':true,
                        arguments: [entity]
                    });
                }, this);
            }
        };
    }

    end() {
        if (this.abort) return;
        this.log('End of grpc generator');
    }
};
