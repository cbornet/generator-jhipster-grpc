#!/bin/bash
set -e

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
