#!/bin/bash
set -e

cd "$APP_FOLDER"
#-------------------------------------------------------------------------------
# Launch tests
#-------------------------------------------------------------------------------
if [ -f "gulpfile.js" ]; then
    gulp test --no-notification
fi
if [ -f "tsconfig.json" ]; then
    yarn test
fi
