xquery version "1.0-ml";
module namespace ingest = "http://marklogic.com/rest-api/transform/northwind";

import module namespace northwind = "http://marklogic.com/test#Northwind-0.0.1"
    at "/ext/Northwind-0.0.1.xqy";

(:~
: User: bsrikanth
: Date: 8/28/16
: Time: 1:34 PM
:)

declare function ingest:transform(
    $context as map:map,
    $params as map:map,
    $body as document-node()
) as document-node()?
{
    let $uri := map:get($context, "uri")
    let $_ :=
        if (fn:matches($uri, "/customers/.*\.xml"))
        then (
        xdmp:log(("Procesing Northwind URI " || $uri)),
        xdmp:document-insert(
                concat("/customers/", $uri=>fn:substring-after("customers/")=>fn:substring-before(".xml"), ".xml"),
                northwind:instance-to-envelope(northwind:extract-instance-Customer(doc($uri))),
                (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")), 
                "customer-envelopes")
        )
        
        else if (fn:matches($uri, "/orders/.*\.xml"))
        then (
            	xdmp:log(("Procesing Northwind URI " || $uri)),
                xdmp:document-insert(
                		"/orders/" || $uri=>fn:substring-after("orders/")=>substring-before(".xml") || ".xml",
                		northwind:instance-to-envelope(
                		northwind:extract-instance-Order(doc($uri))),
                        (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")),
                        ("order-envelopes","upconverts"))
        )
                
        else if (fn:matches($uri, "/products/.*\.xml"))
        then (
        	xdmp:log(("Procesing Northwind URI " || $uri)),
            xdmp:document-insert(
                    concat("/products/", $uri=>fn:substring-after("products/")=>fn:substring-before(".xml"), ".xml"),
                    northwind:instance-to-envelope(northwind:extract-instance-Product(doc($uri))),
                    (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")), 
                    ("product-envelopes","upconverts"))    
        )

        else ()
    return document { " " }
};
