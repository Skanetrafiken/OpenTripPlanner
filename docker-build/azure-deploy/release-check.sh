#!/bin/bash

echo "JAVA VERSION: $JAVA_HOME"
echo "JAVA 11 VERSION: $JAVA_HOME_11_X64"
echo "MAVEN VERSION: $(mvn -version)"

vercomp () {
    if [[ $1 == $2 ]]
    then
        return 0
    fi
    local IFS=.
    local i ver1=($1) ver2=($2)
    # fill empty fields in ver1 with zeros
    for ((i=${#ver1[@]}; i<${#ver2[@]}; i++))
    do
        ver1[i]=0
    done
    for ((i=0; i<${#ver1[@]}; i++))
    do
        if [[ -z ${ver2[i]} ]]
        then
            # fill empty fields in ver2 with zeros
            ver2[i]=0
        fi
        if ((10#${ver1[i]} > 10#${ver2[i]}))
        then
            return 1
        fi
        if ((10#${ver1[i]} < 10#${ver2[i]}))
        then
            return 2
        fi
    done
    return 0
}

if ! [[ "$SOURCE_BRANCH" =~ ^refs\/heads\/release\/[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Invalid branch name: $SOURCE_BRANCH"
  echo "Release branch needs to follow naming standard: refs/heads/release/x.x.x"
  exit 1
fi

# Get version from pom.xml
NEW_VERSION=$(cd $SELF_DIRECTORY && mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

if ! [[ $NEW_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Invalid version: $NEW_VERSION"
  echo "Version needs to follow standard: x.x.x, for example 1.1.1"
  exit 1
fi

# Get old version from git tag (remove RELEASE suffix)
OLD_VERSION=$(cd $MASTER_DIRECTORY && git describe --match=[0-9]*)

if ! [[ $OLD_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-RC|\.RELEASE){1}.* ]]; then
	echo "Version $OLD_VERSION is bad. The last version is not correct formatted. Coder needs to create tag on earlier commit of correct format"
	exit 1
fi

# Remove suffix
OLD_VERSION=${OLD_VERSION%".RELEASE"}

vercomp $OLD_VERSION $NEW_VERSION
compare_result=$?

if [[ $compare_result == 1 ]] || [[ $compare_result == 0 ]]; then
    echo "New version $NEW_VERSION is lower than or equal to old version $OLD_VERSION"
    exit 1
fi
