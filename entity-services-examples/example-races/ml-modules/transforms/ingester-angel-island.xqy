xquery version "1.0-ml";
module namespace ingester = "http://marklogic.com/rest-api/transform/ingester-angel-island";

import module namespace race = "http://grechaw.github.io/entity-types#Race-0.0.1"
    at "/ext/Race-0.0.1.xqy";


(:
 : This function is a REST transform for
 : use with the Data Movement SDK and Entity Servicew
 :)
declare function ingester:transform(
    $context as map:map,
    $params as map:map,
    $body as document-node()
) as document-node()?
{
    let $uri := map:get($context, "uri")
    let $_ := xdmp:log(("Procesing Angel Island URI " || $uri))
    let $_ :=
        if (contains($uri, "angel-island"))
        then xdmp:document-insert(
                concat("/runs/", $uri=>fn:substring-before(".json"), ".xml"),
                race:instance-to-envelope(
                race:extract-instance-Angel-Island(doc($uri))),
                (xdmp:permission("race-reader", "read"), xdmp:permission("race-writer", "insert"), xdmp:permission("race-writer", "update")), 
                ("run-envelopes", "angel-island"))
        else ()
    return document { " " }
};
