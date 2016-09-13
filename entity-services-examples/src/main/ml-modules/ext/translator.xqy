xquery version "1.0-ml";
module namespace race-from-race 
    = "http://grechaw.github.io/entity-types#Race-0.0.2-from-Race-0.0.1";

import module namespace es = "http://marklogic.com/entity-services" 
    at "/MarkLogic/entity-services/entity-services.xqy";



declare function race-from-race:make-property-iri(
    $seed
) as sem:iri
{
    sem:iri("http://new-model/" || xdmp:random())
};

declare function race-from-race:convert-instance-Race(
    $source-node as node()
) as map:map
{
 json:object()
 =>map:with('$type', 'Race')
=>es:optional('id',                     sem:iri( race-from-race:make-property-iri( 1 ) ))
 =>   map:with('name',                   xs:string($source-node/Race/name))
=>es:optional('raceCategory',            if (contains($source-node/Race/name, "10k")) then sem:iri("http://example.org/ontologies/TenKs") else sem:iri("http://example.org/ontologies/TrailHalfMarathons"))
 =>es:optional('comprisedOfRuns',        race-from-race:extract-array($source-node/Race/comprisedOfRuns, function($path) { json:object()=>map:with('$type', 'Run')=>map:with('$ref', $path/Run/text() ) }))
 =>es:optional('wonByRunner',            function($path) { json:object()=>map:with('$type', 'Runner')=>map:with('$ref', $path/Runner/text() ) }($source-node/Race/wonByRunner))
 =>   map:with('courseLength',           xs:decimal($source-node/Race/courseLength))

};

declare function race-from-race:convert-instance-Run(
    $source-node as node()
) as map:map
{
 json:object()
 =>map:with('$type', 'Run')
 =>   map:with('id',                     sem:iri($source-node/Run/id))
 =>   map:with('date',                   xs:date($source-node/Run/date))
 =>   map:with('distance',               xs:decimal($source-node/Run/distance))
 =>es:optional('distanceLabel',          xs:string($source-node/Run/distanceLabel))
 =>es:optional('duration',               xs:dayTimeDuration($source-node/Run/duration))
 =>   map:with('runByRunner',            function($path) { json:object()=>map:with('$type', 'Runner')=>map:with('$ref', $path/Runner/text() ) }($source-node/Run/runByRunner))

};

declare function race-from-race:convert-instance-Runner(
    $source-node as node()
) as map:map
{
 json:object()
(: The following line identifies the type of this instance.  Do not change it.      :)
 =>map:with('$type', 'Runner')
(: The following lines are generated from the 'Runner' entity type.                 :)
 (: The following property was missing from the source type                          :)
=>es:optional('id',                     sem:iri( race-from-race:make-property-iri( 1) ))
 =>   map:with('name',                   xs:string($source-node/Runner/name))
 =>   map:with('age',                    xs:decimal($source-node/Runner/age))
 =>es:optional('gender',                 xs:string($source-node/Runner/gender))

};

declare function race-from-race:extract-array(
    $path-to-property as item()*,
    $fn as function(*)
) as json:array?
{
    if (empty($path-to-property))
    then ()
    else json:to-array($path-to-property ! $fn(.))
};
