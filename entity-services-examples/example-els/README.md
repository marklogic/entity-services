Element Level Security Example (example-els)
--------------------------------------------

```text
gradlew -PexampleDir=example-els mlDeploy
gradlew -PexampleDir=example-els runExampleEls
```

Element-Level Security is a capability in MarkLogic since 9.0-1.
Under normal circumstances, permissions in MarkLogic is managed at the level of
the document.  That is to say, if a particular user has the permission to read
a particular document, then they can read the entire thing.  Since 9.0-1 one
can configure a secondary level of security based on XPath expressions, such
that, depending on permissions, one might be able to read or query only certain
parts of a document.

Element-Level Security is not directly integrated with Entity Services.
However, since a model determines document structure, the configuration for
element level security should be simple to derive given policies applied to
parts of a model.

In this example we have a "Customer" entity.  The system generates customer
documents that do not need any special ingestion handling, and they get 'harmonized'
thusly:

```json
{
    "envelope": {
        "instance": {
            "info": {
                "title": "Customer", 
                "version": "0.0.1"
            }, 
            "Customer": {
                "id": "1", 
                "name": "Julian", 
                "email": "bender@yahoo.com", 
                "ssn": "696-26-0356"
            }
        }
    }
}
```

In this customer record, I've two properties, 'email', and 'ssn', which I want
to protect.

The document at [pii-policy.ttl](pii-policy.ttl) contains a set of RDF triples that define my
company policy around personally indentifiable information.  The example loads
these triples, and adds them to the database as metadata available to queries.

The model descriptor [Customer-pii-0.0.1.json](Customer-pii-0.0.1.json) is quite straightforward.  We've
added however two triples to the document in order to capture the notion that
some properties in this model should be treated as personally-identifiable.

```
...
"triple": [
  {
    "subject": "http://marklogic.com/entity-services/example-els/Secure-0.0.1/Customer/email",
    "predicate": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
    "object": "http://marklogic.com/entity-services/example-els/PersonallyIdentifiableInformationProperty"
  },
  {
    "subject": "http://marklogic.com/entity-services/example-els/Secure-0.0.1/Customer/ssn",
    "predicate": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
    "object": "http://marklogic.com/entity-services/example-els/PersonallyIdentifiableInformationProperty"
  }
]
...
```


So, by extending the descriptor with explicit triples, we're adding more
information about particular properties. The model states:

```
The email and ssn properties in this model are both personally identifiable information.
```

The policy document states

```
* Properties marked as "Personally Identifiable" are to be protected.  Unless user has reason, she should not see such a property.
* Such a property is secured with the MarkLogic role 'privileged-to-see-pii' .
```


This example sets up element level security based on the policy and the entity
services model.  It contains a REST resource extension that queries the triples
in the model and the policy in order to make calls to configure ELS.

The example code at
[../src/main/java/com/marklogic/entityservices/examples/els/ElementLevelSecurityExample.java](ElementLevelSecurityExample.java)
steps through the following to demonstrate securing the `email` and `ssn`
properties:

* ensure the two properties are unprotected (remove protection if there)
* create a number of `Customer` objects in Java and insert them into the database as entity instances.
* query the database with an not-especially-privileged user to demonstrate retrieval, POJO hyration, and search results.
* invoke the extension that configures ELS based on the model and policy data.
* query the database with an not-especially-privileged user to demonstrate that secured data is invisible.
* query the database with a privileged user to demonstrate that secured data is visible with the right authorization.


