Person Example (example-person)
-------------------------------

This example loads a single model with a single entity type, and then
populates instance data by invoking several separate extraction functions.  In
developing this example, I started with the output from
`es:instance-converter-generate()` and then replicated the
`extract-instance-Person()` function once for each different silo that I wanted
to integrate.  To run the example:

```
./gradlew -PexampleDir=example-person runExamplePerson
```


This example simply harmonizes the various records, then runs some SPARQL and
Optic API queries to demonstrate how the data has been integrated into a single
view.

There is also a qconsole workspace which replicates the SPARQL and Optic queries
in an interactive UI.

Uses codeGen:

* extraction template (OOTB)
* instance converter (customized several ways)
* namespace configuration for instance data


