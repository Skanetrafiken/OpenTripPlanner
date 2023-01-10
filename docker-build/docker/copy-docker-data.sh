#!/bin/bash

DATA_FOLDER="docker-build/docker/services/data"

cleanTarget() {
	if [ -d "$DATA_FOLDER" ]; then
		rm -rf $DATA_FOLDER
	fi
}

bashdir=$(dirname "$(readlink -f "$0")")
cd $bashdir/../.. || exit

cleanTarget
mkdir $DATA_FOLDER
cp target/resesok-otp*shaded.jar $DATA_FOLDER
