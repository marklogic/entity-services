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
testInstances;</query><query name="tde experiments" focus="false" listorder="3" taborder="" active="false" database="1864503829138194350" server="4141854128175404443" database-name="entity-services-examples-content" server-name="entity-services-examples" mode="xquery">let $col-prefix := "http://marklogic.com/column/id/"
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
(: this does NOT return anything 
   cts:triples((), sem:iri("http://marklogic.com/column/id/D5E179A99D6A5098"), ()),
 :)
 
sem:sparql('prefix sql: &lt;http://marklogic.com/xdmp/sql#&gt;
prefix c: &lt;http://marklogic.com/column/id/&gt; 
select ?s ?p ?o where {?s ?p ?o . filter (?p = c:D5E179A99D6A5098)} limit 100'),

sem:sparql('prefix sql: &lt;http://marklogic.com/xdmp/sql#&gt;
prefix c: &lt;http://marklogic.com/column/id/&gt; 
select ?s ?p ?o where {?s ?p ?o . filter (?p = sql:columnID("Race","Race","wonByRunner"))} limit 100'),
xdmp:sql("select wonByRunner from Race.Race")
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

:)</query><query name="Query 1" focus="false" listorder="5" taborder="" active="false" database="1864503829138194350" server="4141854128175404443" database-name="entity-services-examples-content" server-name="entity-services-examples" mode="xquery">(:
sem:sparql-update('
prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;
prefix my: &lt;http://ontology-test/&gt;
prefix sql: &lt;http://marklogic.com/xdmp/sql#&gt;
insert data { $x rdfs:subPropertyOf my:prop . } 
', map:entry("x", sql:columnID("Race","Race","wonByRunner"))),
:)
xdmp:document-insert("/sql-ontology.xml",
&lt;sem:triples xmlns:sem="http://marklogic.com/semantics"&gt;
&lt;sem:triple&gt;
&lt;sem:subject&gt;http://marklogic.com/column/id/D5E179A99D6A5098&lt;/sem:subject&gt;
&lt;sem:predicate&gt;http://www.w3.org/2000/01/rdf-schema#subPropertyOf&lt;/sem:predicate&gt;
&lt;sem:object&gt;http://ontology-test/prop&lt;/sem:object&gt;
&lt;/sem:triple&gt;
&lt;/sem:triples&gt;)

</query><query name="Query 2" focus="false" listorder="6" taborder="" active="false" database="1864503829138194350" server="4141854128175404443" database-name="entity-services-examples-content" server-name="entity-services-examples" mode="xquery">xquery version "1.0-ml";
doc("/sql-ontology.xml"),

sem:sparql('
prefix my: &lt;http://ontology-test/&gt;
prefix sql: &lt;http://marklogic.com/xdmp/sql#&gt;
select ?s ?p ?o where { ?s ?p ?o . filter( ?p = sql:columnID("Race","Race","wonByRunner"))}', (), (), sem:ruleset-store("subPropertyOf.rules", sem:store())),
sem:sparql('
prefix my: &lt;http://ontology-test/&gt;
prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;
prefix sql: &lt;http://marklogic.com/xdmp/sql#&gt;
select ?s ?p ?o where { ?s ?p ?o . filter( ?p =rdfs:subPropertyOf)}', (), (), sem:ruleset-store("subPropertyOf.rules", sem:store())),
sem:sparql('
prefix my: &lt;http://ontology-test/&gt;
prefix sql: &lt;http://marklogic.com/xdmp/sql#&gt;
select ?s ?o where { ?s ?p ?o . filter( ?p = my:prop)}', (), (), sem:ruleset-store("subPropertyOf.rules", sem:store()))</query></workspace></export>
