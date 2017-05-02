xquery version "1.0-ml";
module namespace ingester = "http://marklogic.com/rest-api/transform/ingester";

import module namespace race = "http://grechaw.github.io/entity-types#Race-0.0.1"
    at "/ext/Race-0.0.1.xqy";

(:~
: User: cgreer
: Date: 7/21/16
: Time: 1:34 PM
:)

declare function ingester:transform(
    $context as map:map,
    $params as map:map,
    $body as document-node()
) as document-node()?
{
    let $uri := map:get($context, "uri")
    let $_ := xdmp:log(("Procesing URI " || $uri))
    let $_ :=
        if (fn:matches($uri, "/runners/.*\.json"))
        then () (: "runners" are denormalized into runs, so we don't put them in prod as separate doc. :)
        else if (fn:matches($uri, "/runs/.*\.json"))
        then
            xdmp:document-insert(
                    concat("/runs/", $uri=>fn:substring-after("runs/")=>fn:substring-before(".json"), ".xml"),
                    race:instance-to-envelope(race:extract-instance-Run(doc($uri))),
                    (xdmp:permission("examples-reader", "read"), xdmp:permission("examples-writer", "insert"), xdmp:permission("examples-writer", "update")),
                    "run-envelopes")
        else if (fn:matches($uri, "/races/.*\.json"))
        then
            xdmp:document-insert(
                    "/races/" || $uri=>fn:substring-after("races/")=>substring-before(".json") || ".xml",
                    race:instance-to-envelope(
                    race:extract-instance-Race(doc($uri))),
                    (xdmp:permission("examples-reader", "read"), xdmp:permission("examples-writer", "insert"), xdmp:permission("examples-writer", "update")),
                    "race-envelopes")
        else ()
    return document { " " }
};
