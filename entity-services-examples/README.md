# entity-services-examples

This directory contains some examples.  This is a script to the creation/validation of the first one.

To set up or re-configure examples, simply run

`gradle mlDeploy`


## Scenario 1, a Race entity type model.

This entity type model is very simple, but has some test data associated
with it and demonstrate the various aspects of this feature.


### Step 1 load `src/main/ml-modules/entity-types/simple-race.json`


This is an entity type document.  Entity type documents are stored in the content database, so that they can provide a real-time schema for entity instances.

For now, use curl to load this entity type as a first step.  Entity type documents must be loaded into a specific collection to be queried with SPARQL.

```
curl --user admin:admin --digest -X PUT --data-binary @src/main/ml-modules/entity-types/simple-race.json -Hcontent-type:application/json "http://localhost:8000/v1/documents?uri=simple-race.json&database=entity-services-examples-content&collection=http://marklogic.com/entity-services/entity-types"
```

Future: ml-gradle will load entity types from here or another directory.



### Step 2 load staging data

The 'data' directory contains "raw" data for this ingestion scenario.  mlcp will load all of the documents under this directory into a "raw" collection for entity services code to process.  Here's an mlcp command that loads this data.

```
mlcp.sh IMPORT -input_file_path data -input_file_pattern ".*.json" -username admin -password admin -host localhost -port 8000 -database entity-services-examples-content -output_collections raw
```


### Step 3 generate some code artifacts.

This example uses QConsole to invoke entity services functions.  So pop open QConsole, import the workspace at `qc/race-workspace.xml`.

The first tab has code to generate various things having to do with the entity types in `simple-race.json`.

Edit the filesystem output to be usful for your checkout, then execute it.


### Step 4 deploy modified artifacts


`gradle mlDeploy`


### Step 5 Use them

The QConsole workspace has just a couple of SQL and SPARQL queries at this point to peruse.


