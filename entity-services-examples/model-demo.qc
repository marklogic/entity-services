<export><workspace name="Model Demos"><query name="models" focus="true" listorder="1" taborder="1" active="true" database="1864503829138194350" server="4141854128175404443" database-name="entity-services-examples-content" server-name="entity-services-examples" mode="javascript">var es = require("/MarkLogic/entity-services/entity-services.xqy");
var sem = require("/MarkLogic/semantics.xqy");

var model = fn.doc("/MarkLogic/git/entity-services/entity-services-examples/data/models/simple-race.json");

model;
//es.modelValidate(model);
//es.modelToXml(model)
//cts.triples(null, null, null, null, null, cts.documentQuery("/MarkLogic/git/entity-services/entity-services-examples/data/models/simple-race.json"));
</query><query name="testInstances" focus="false" listorder="2" taborder="3" active="true" database="1864503829138194350" server="4141854128175404443" database-name="entity-services-examples-content" server-name="entity-services-examples" mode="javascript">var es = require("/MarkLogic/entity-services/entity-services.xqy");
var sem = require("/MarkLogic/semantics.xqy");

var model = fn.doc("/MarkLogic/git/entity-services/entity-services-examples/data/models/simple-race.json");

var testInstances = es.modelGetTestInstances(model);
var testRace = testInstances[0];
var testRun = testInstances[1];


model;
testInstances;</query><query name="tde experiments" focus="false" listorder="3" taborder="4" active="true" database="1864503829138194350" server="4141854128175404443" database-name="entity-services-examples-content" server-name="entity-services-examples" mode="xquery">let $col-prefix := "http://marklogic.com/column/id/"
let $row-prefix := "http://marklogic.com/row/"
let $race-view := tde:get-view("Race", "Race")

let $cols-ids := 
  map:new(
    ($race-view=&gt;map:get("view")=&gt;map:get("columns")=&gt;json:array-values())
    ! 
    (.=&gt;map:get("column"))
    !
    map:entry(.=&gt;map:get("name"), concat($col-prefix, .=&gt;map:get("id")=&gt;xdmp:integer-to-hex()=&gt;fn:upper-case()))
  )
let $col-id := function($name) {
  $cols-ids=&gt;map:get($name)
}

return (
cts:triples((), sql:columnID("Race", "Race", "wonByRunner"), ()),
cts:triples((), sem:iri("http://marklogic.com/column/id/D5E179A99D6A5098"), ()),
sem:sparql('prefix c: &lt;http://marklogic.com/column/id/&gt; select ?s ?p ?o where {?s ?p ?o . filter (?p = c:D5E179A99D6A5098)} limit 10'),
xdmp:sql("select * from Race.Race limit 10")
) 
(: .columns.forEach( function(x) { var o = { x.column.name : x.column.id }; m.push( o ) }); :)

</query><query name="codegen" focus="false" listorder="4" taborder="2" active="true" database="1864503829138194350" server="4141854128175404443" database-name="entity-services-examples-content" server-name="entity-services-examples" mode="xquery">import module namespace sem = "http://marklogic.com/semantics" at "/MarkLogic/semantics.xqy"; 
import module namespace es = "http://marklogic.com/entity-services" at "/MarkLogic/entity-services/entity-services.xqy"; 

let $model := fn:doc("/MarkLogic/git/entity-services/entity-services-examples/data/models/simple-race.json")

return $model
=&gt;es:version-translator-generate( $model)
(: 
=&gt;es:model-get-test-instances()
=&gt;es:extraction-template-generate()
=&gt;es:database-properties-generate()
=&gt;es:search-options-generate()
=&gt;es:instance-converter-generate()
=&gt;es:schema-generate()

:)</query></workspace></export>
