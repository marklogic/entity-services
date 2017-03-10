xquery version "1.0-ml";
module namespace translator = "http://marklogic.com/rest-api/transform/translator";

import module namespace t = "http://grechaw.github.io/entity-types#Race-0.0.2-from-Race-0.0.1"
    at "/ext/translator.xqy";
import module namespace new = "http://grechaw.github.io/entity-types#Race-0.0.2"
    at "/ext/Race-0.0.2.xqy";

declare namespace es = "http://marklogic.com/entity-services";

(:~
: User: cgreer
: Date: 7/21/16
: Time: 1:34 PM
:)

declare function translator:transform(
    $context as map:map,
    $params as map:map,
    $body as document-node()
) as document-node()?
{
    let $uri := map:get($context, "uri")
    let $_ := xdmp:log(("Procesing URI " || $uri))
    let $new-version := t:convert-instance-Race(fn:doc($uri)//es:instance)
    let $new-envelope := new:instance-to-envelope($new-version)
    let $_ :=
        xdmp:document-insert(
                fn:concat("/upconverts/", $uri),
                $new-envelope,
                (xdmp:permission("examples-reader", "read"), xdmp:permission("examples-writer", "insert"), xdmp:permission("examples-writer", "update")),
                "race-0.0.2-envelopes")
    return document { " " }
};
