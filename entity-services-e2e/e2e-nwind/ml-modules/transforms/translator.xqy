xquery version "1.0-ml";
module namespace translator = "http://marklogic.com/rest-api/transform/translator";

import module namespace t = "http://marklogic.com/test#Northwind-0.0.2-from-Northwind-0.0.1"
    at "/ext/translator.xqy";
import module namespace new = "http://marklogic.com/test#Northwind-0.0.2"
    at "/ext/Northwind-0.0.2.xqy";

declare namespace es = "http://marklogic.com/entity-services";

(:~
: User: bsrikanth
: Date: 8/28/16
: Time: 1:34 PM
:)

declare function translator:transform(
    $context as map:map,
    $params as map:map,
    $body as document-node()
) as document-node()?
{
    let $uri := map:get($context, "uri")
    let $_ := xdmp:log(("Procesing Translator URI " || $uri))
    let $new-version := t:convert-instance-Order(fn:doc($uri)//es:attachments/Order)
    let $new-envelope := new:instance-to-envelope($new-version)
    let $_ :=
        xdmp:document-insert(
                fn:concat("/upconverts", $uri),
                $new-envelope,
                (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")),
                "Order-0.0.2-envelopes")
    return document { " " }
};
