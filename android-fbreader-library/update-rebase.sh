#!/bin/bash

set -e

git checkout -b update $(git rev-list --max-parents=0 HEAD)
./update.sh
