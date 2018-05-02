xquery version "1.0-ml";
module namespace ingester = "http://marklogic.com/rest-api/transform/ingester-devil-island";

import module namespace race = "http://grechaw.github.io/entity-types#Race-0.0.1"
    at "/ext/entity-services/Race-0.0.1.xqy";


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
    let $_ := xdmp:log(("Procesing Devil's Island URI " || $uri))
    let $_ :=
        if (contains($uri, "devil-island"))
        then xdmp:document-insert(
                concat("/runs/", $uri=>fn:substring-before(".json"), ".xml"),
                race:instance-to-envelope(
                race:extract-instance-Devil-Island(doc($uri))),
                (xdmp:permission("examples-reader", "read"), xdmp:permission("examples-writer", "insert"), xdmp:permission("examples-writer", "update")),
                ("run-envelopes", "devil-island"))
        else ()
    return document { " " }
};
