# entity-services-examples

This directory contains some examples.  This is a script to the creation/validation of the first one.

To set up or re-configure examples, simply run

`gradle mlDeploy`


## Scenario 1, a Race entity type model.

This entity type model is very simple, but has some test data associated
with it and demonstrate the various aspects of this feature.


### Step 1 load `data/entity-types/simple-race.json` and instance data.

EA-3 TODO

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


