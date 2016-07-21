# entity-services-examples

This directory contains some examples.  This is a script to the creation/validation of the first one.

There are several kinds of code here in the examples directory.

* A QConsole workspace `races-qc.xml` contains XQuery scripting to work with the Entity Services API.
* Examples of code that has been generated and edited are in directories under 
 * `src/main/ml-config`
 * `src/main/ml-modules`
 * `src/main/ml-schemas`
* Java Applications, with source under `src/main/java` demonstrate use of the Data Movement SDK for an entity services toolchain.


When you run `gradle entity-services-examples:mlDeploy` *from the parent project directory*
these artifacts will be deployed to a MarkLogic server.  To change the location of the deployment
edit `entity-services-examples/gradle.properties`


## A Model for Races, Runs, and Runners

This entity type model is very simple.  It comes complete with some source data, a model, but has some test data associated
with it and demonstrate the various aspects of this feature.


### Step 1 load `data/entity-types/simple-race.json` and instance data.

To run a Java Application that loads all entity types, instance data, and reference data simply run

When you run `gradle entity-services-examples:run` *from the parent project directory*


There are examples in Java using the MarkLogic Data Movement SDK to load both entity type documents and instance data.

src/main/resources/application.properties provides the paths to the DMSDK classes for data loading.

EntityServicesLoader loads the entity-types.
JsonInstanceLoader loads the instance data



### Step 3 generate some code artifacts.

This example uses QConsole to invoke entity services functions.  So pop open QConsole, import the workspace at `qc/race-workspace.xml`.

The first tab has code to generate various things having to do with the entity types in `simple-race.json`.

Edit the filesystem output to be usful for your checkout, then execute it.


### Step 4 deploy modified artifacts


`gradle mlDeploy`


### Step 5 Use them

The QConsole workspace has just a couple of SQL and SPARQL queries at this point to peruse.


