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
        },
        displayLogo: function () {
            this.log('Welcome to the ' + chalk.red('JHipster grpc') + ' generator! ' + chalk.yellow('v' + packagejs.version + '\n'));
        }
    },

    prompting: function () {
        this.entities = [];
        this.existingEntitiesNames = jhipsterFunc.getExistingEntities()
            .filter(entity => entity.definition.service === 'serviceClass' || entity.definition.service === 'serviceImpl')
            .map(entity => entity.name);

        if (this.existingEntitiesNames.length == 0) {
            this.log(chalk.yellow('No existing entities with a service layer found.'));
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
        writeTemplates: function () {
            this.mainClass = jhipsterVar.mainClassName;
            this.packageName = jhipsterVar.packageName;
            this.authenticationType = jhipsterVar.authenticationType;
            var javaDir = jhipsterVar.javaDir;
            var testDir = jhipsterVar.CONSTANTS.SERVER_TEST_SRC_DIR + jhipsterVar.packageFolder + '/';
            var protoDir = jhipsterVar.CONSTANTS.MAIN_DIR + 'proto/';
            var protoPackageDir = protoDir + jhipsterVar.packageFolder + '/';


            this.template('_date.proto', protoDir + 'util/date.proto', this, {});
            this.template('_decimal.proto', protoDir + 'util/decimal.proto', this, {});
            this.template('_pagination.proto', protoDir + 'util/pagination.proto', this, {});
            this.template('_AuthenticationInterceptor.java', javaDir + 'grpc/AuthenticationInterceptor.java');
            this.template('_ProtobufUtil.java', javaDir + 'grpc/ProtobufUtil.java');

            this.template('_health.proto', protoPackageDir + 'health.proto');
            this.template('_HealthService.java', javaDir + 'grpc/HealthService.java');
            this.template('_HealthServiceTest.java', testDir + 'grpc/HealthServiceTest.java');

            if (jhipsterVar.authenticationType === 'jwt') {
                this.template('_jwt.proto', protoPackageDir + 'jwt.proto');
                this.template('_JWTService.java', javaDir + 'grpc/JWTService.java');
                this.template('_JWTServiceTest.java', testDir + 'grpc/JWTServiceTest.java');
            }
            this.template('_logs.proto', protoPackageDir + 'logs.proto');
            this.template('_LogsService.java', javaDir + 'grpc/LogsService.java');
            this.template('_LogsServiceTest.java', testDir + 'grpc/LogsServiceTest.java');

            this.grpcVersion = '1.1.1';
            var grpcSpringVersion = '2.0.0';
            var guavaVersion = '20.0';
            if (jhipsterVar.databaseType === 'cassandra') {
                // Downgrade grpc to get a compatible guava version
                this.grpcVersion = '1.0.2';
                grpcSpringVersion = '1.0.0';
                guavaVersion = '19.0';

            }

            if (jhipsterVar.buildTool === 'maven') {
                jhipsterFunc.addMavenDependency('org.lognet', 'grpc-spring-boot-starter', grpcSpringVersion);
                // Resolve conflict with springfox
                jhipsterFunc.addMavenDependency('com.google.guava', 'guava', guavaVersion);
                jhipsterFunc.addMavenDependency('io.grpc', 'grpc-protobuf', this.grpcVersion);
                jhipsterFunc.addMavenDependency('io.grpc', 'grpc-stub', this.grpcVersion);
                if (jhipsterVar.databaseType === 'cassandra') {
                    // grpc-java needs netty 4.1
                    jhipsterFunc.addMavenDependency('io.netty', 'netty-handler', '4.1.6.Final');
                }
                jhipsterFunc.addMavenPlugin('org.xolstice.maven.plugins', 'protobuf-maven-plugin', '0.5.0',
                    '                ' +
                    '<configuration>' + '\n                ' +
                    '    <protocArtifact>com.google.protobuf:protoc:3.1.0:exe:${os.detected.classifier}</protocArtifact>' + '\n                ' +
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
                if (jhipsterVar.databaseType === 'cassandra') {
                    // grpc-java needs netty 4.1
                    jhipsterFunc.addGradleDependency('compile', 'io.netty', 'netty-handler', '4.1.6.Final');
                }
                jhipsterFunc.addGradlePlugin('com.google.protobuf', 'protobuf-gradle-plugin', '0.8.1');
                jhipsterFunc.applyFromGradleScript('gradle/grpc');
            }

            this.entities.forEach(entityName => {jhipsterFunc.updateEntityConfig('.jhipster/' + entityName + '.json', 'grpcService', true)});

        },

        registering: function () {
            try {
                jhipsterFunc.registerModule('generator-jhipster-grpc', 'entity', 'post', 'entity', 'Adds support for gRPC and generates gRPC CRUD services');
            } catch (err) {
                this.log(chalk.red.bold('WARN!') + ' Could not register as a jhipster entity post creation hook...\n');
            }
        },

        regenerateEntities: function () {
            if (this.entities.length != 0) {
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
        this.log('End of grpc generator');
    }
});
