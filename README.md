# xdmp-entity-services

This is the repository for entity services, a feature in MarkLogic 9

# Prerequisite

To develop with this project, you'll need

* to clone this repository

Once you have cloned this repository, you'll use this code by making a
symbolic link from the Mark

* a checkout of MarkLogic Server trunk svn

or

* A built and installed current nightly MarkLogic package.  If you've used this method, remove or rename the entity-services directory at `src/Modules/MarkLogic/entity-services`


For either method of server development, you'll need to symbolically link

`./entity-services/src/main/xdmp/entity-services`

to

`{MarkLogicCheckout}/src/Modules/MarkLogic/entity-services`

# Setup

To set up a server for running tests, 

`./gradlew mlDeploy`

This command takes the configuration at gradle.properties and
creates forests, databases and servers for running the tests.


