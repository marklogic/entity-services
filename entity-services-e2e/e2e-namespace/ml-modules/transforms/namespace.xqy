xquery version "1.0-ml";
module namespace ingest = "http://marklogic.com/rest-api/transform/namespace";

import module namespace ns = "http://marklogic.com/ns1#Model_1ns-0.0.1"
    at "/ext/valid-1-namespace.xqy";


declare function ingest:transform(
    $context as map:map,
    $params as map:map,
    $body as document-node()
) as document-node()?
{
    let $uri := map:get($context, "uri")
    let $_ :=
        if (contains($uri, "namespace"))
        then (
        	xdmp:log(("Procesing Namespace URI " || $uri)),
        	xdmp:document-insert(
        		concat("/superstore/",$uri=>fn:substring-after("/data/namespace/")=>fn:substring-before(".xml"),".xml"),
        		ns:instance-to-envelope(ns:extract-instance-Superstore(doc($uri))),
                (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")), 
                ("namespace-envelopes")),
        	xdmp:document-insert(
            	concat("/customer/",$uri=>fn:substring-after("/data/namespace/")=>fn:substring-before(".xml"),".xml"),
            	ns:instance-to-envelope(ns:extract-instance-Customer(doc($uri))),
                (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")), 
                ("namespace-envelopes")),
        	xdmp:document-insert(
            	concat("/product/",$uri=>fn:substring-after("/data/namespace/")=>fn:substring-before(".xml"),".xml"),
            	ns:instance-to-envelope(ns:extract-instance-Product(doc($uri))),
                (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")), 
                ("namespace-envelopes")),
        	xdmp:document-insert(
            	concat("/order/",$uri=>fn:substring-after("/data/namespace/")=>fn:substring-before(".xml"),".xml"),
            	ns:instance-to-envelope(ns:extract-instance-Order(doc($uri))),
                (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")), 
                ("namespace-envelopes"))
        )
        else ()
        
    return document { " " }
};
