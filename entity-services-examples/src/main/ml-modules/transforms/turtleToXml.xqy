xquery version "1.0-ml";
module namespace turtleToXml = "http://marklogic.com/rest-api/transform/turtleToXml";

import module namespace sem = "http://marklogic.com/semantics" at "/MarkLogic/semantics.xqy";



declare function turtleToXml:transform(
    $context as map:map,
    $params as map:map,
    $body as document-node()
) as document-node()?
{
    let $triples := sem:rdf-parse($body, "turtle")
    return
    document {
        <triples>{$triples}</triples>
    }
};
