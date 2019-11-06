#!/bin/bash
set -e

#-------------------------------------------------------------------------------
# Display environment information like JDK version
#-------------------------------------------------------------------------------
cd "$APP_FOLDER"
if [ -f "mvnw" ]; then
    ./mvnw -B enforcer:display-info
elif [ -f "gradlew" ]; then
    ./gradlew -v
fi

#-------------------------------------------------------------------------------
# Check Javadoc generation
#-------------------------------------------------------------------------------
if [ -f "mvnw" ]; then
    ./mvnw -B javadoc:javadoc
elif [ -f "gradlew" ]; then
    ./gradlew javadoc
fi

#-------------------------------------------------------------------------------
# Launch UAA tests
#-------------------------------------------------------------------------------
if [[ "$JHIPSTER" == *"uaa"* ]]; then
    cd "$UAA_APP_FOLDER"
    ./mvnw -B test
fi

#-------------------------------------------------------------------------------
# Launch tests
#-------------------------------------------------------------------------------
cd "$APP_FOLDER"
if [ -f "mvnw" ]; then
    ./mvnw -B test \
        -Dlogging.level.org.zalando=OFF \
        -Dlogging.level.io.github.jhipster=OFF \
        -Dlogging.level.io.github.jhipster.sample=OFF \
        -Dlogging.level.io.github.jhipster.travis=OFF
elif [ -f "gradlew" ]; then
    ./gradlew test \
        -Dlogging.level.org.zalando=OFF \
        -Dlogging.level.io.github.jhipster=OFF \
        -Dlogging.level.io.github.jhipster.sample=OFF \
        -Dlogging.level.io.github.jhipster.travis=OFF
fi
if [ -f "tsconfig.json" ]; then
    npm test
fi
