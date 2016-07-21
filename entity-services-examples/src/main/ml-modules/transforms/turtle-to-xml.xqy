xquery version "1.0-ml";
module namespace tx = "http://marklogic.com/rest-api/transform/turtle-to-xml";

import module namespace sem = "http://marklogic.com/semantics" at "/MarkLogic/semantics.xqy";



declare function tx:transform(
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
