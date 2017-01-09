xquery version "1.0-ml";
module namespace ingest = "http://marklogic.com/rest-api/transform/superstore";

(: import the Entity Services library that supports Superstore CSV :)
import module namespace northwind = "http://marklogic.com/test#Northwind-0.0.1-source-2"
    at "/ext/Northwind-0.0.1-source-2.xqy";


(:
 : This function is a REST transform for
 : use with the Data Movement SDK and Entity Servicew
 :)
declare function ingest:transform(
    $context as map:map,
    $params as map:map,
    $body as document-node()
) as document-node()?
{
    let $uri := map:get($context, "uri")
    let $_ := xdmp:log(("Procesing Superstore URI " || $uri))
    let $_ :=
        if (contains($uri, "superstore"))
        then xdmp:document-insert(
                concat("/superstore/", $uri=>fn:substring-before(".xml"), ".xml"),
                northwind:instance-to-envelope(
                northwind:extract-instance-OrderDetail(doc($uri))),
                (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")), 
                ("superstore-envelopes", "superstore-sales"))
        else ()
    return document { " " }
};
