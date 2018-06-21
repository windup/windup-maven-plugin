#!/bin/bash
# 
: ${1:?"Must specify release version. Ex: 2.0.1.Final"}
: ${2:?"Must specify next development version. Ex: 2.0.2-SNAPSHOT"}

REL=$1
DEV=$2

git clone git@github.com:windup/windup-maven-plugin.git
cd windup-maven-plugin

find . -name pom.xml -type f -exec sed -i -e "s/<version.windupcore>.*<\/version.windupcore>/<version.windupcore>$REL<\/version.windupcore>/g" {} \;
git add -u && git commit -m "Updated version.windupcore property to $REL";
mvn clean install release:prepare release:perform -DdevelopmentVersion=$DEV -DreleaseVersion=$REL -Dtag=$REL && \
find . -name pom.xml -type f -exec sed -i -e "s/<version.windupcore>.*<\/version.windupcore>/<version.windupcore>$DEV<\/version.windupcore>/g" {} \; && \
git add -u && git commit -m "Updated version.windupcore property to $DEV";

