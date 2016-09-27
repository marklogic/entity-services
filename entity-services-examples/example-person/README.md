Person Example (example-person)
-------------------------------

This example loads a single model with a single entity type, and then
poopulates instance data by invoking several separate extraction functions.  In
developing this example, I started with the output from
`es:instance-converter-generate()` and then replicated the
`extract-instance-Person()` function once for each different silo that I wanted
to integrate.  To run the example:

```
./gradlew runExamplePerson
```


This example simply harmonizes the various records, then runs some SPARQL and
Optic API queries to demonstrate how the data has been integrated into a single
view.

Uses codeGen:

* extraction template (OOTB)
* instance converter (customized several ways)


