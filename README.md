# entity-services

This is the repository for entity services, a feature in MarkLogic 9.
Its code is shipped with MarkLogic server, but this repository is
used for testing the feature, and also contains examples code.

* See `entity-services-examples` for how to use Entity Services.


It contains several sub-projects configured to build with gradle.

* one public API server module written in XQuery, several implementation modules.
  This code is shipped with the server.  Unit tests are here under `entity-services.`

* functional tests in `entity-services-functionaltests`

* end-to-end scenario tests in `entity-services-e2e`



# Development Prerequisite 

To *develop* with this project, you'll need

* to clone this repository

Once you have cloned this repository, you'll use this code by making a
symbolic link from the Mark

* a checkout of MarkLogic Server trunk svn

or

* A built and installed current nightly MarkLogic package.  If you've used this
  method, remove or rename the entity-services directory at
  `src/Modules/MarkLogic/entity-services`


For either method of server development, you'll need to symbolically link

`./entity-services/src/main/xdmp/entity-services`

to

`{MarkLogicCheckout}/src/Modules/MarkLogic/entity-services`

Now, when you import and use entity services code, it will be the copy served
from this local checkout.


# Setup

To set up a server for running tests, 

`./gradlew mlDeploy`

This command takes the configuration at gradle.properties and
creates forests, databases and servers for running the tests.
It also sets up a separate database and server for examples.


