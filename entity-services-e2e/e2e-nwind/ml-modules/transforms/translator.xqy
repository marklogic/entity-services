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
    let $_ :=
    		if (fn:matches($uri, "/orders/.*\.xml"))
    	        then (
    	        	xdmp:log(("Procesing Translator URI " || $uri)),
    	        	xdmp:document-insert(
    	                fn:concat("/upconverts", $uri),
    	                new:instance-to-envelope(t:convert-instance-Order(fn:doc($uri))),
    	                (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")),
    	                "Order-0.0.2-envelopes")
    	        )
    	    (:    	
    	    else if (fn:matches($uri, "/customers/.*\.xml"))
    	        then (
    	        	xdmp:log(("Procesing Translator URI " || $uri)),
	    	        xdmp:document-insert(
    	                fn:concat("/upconverts", $uri),
    	                new:instance-to-envelope(t:convert-instance-Customer(fn:doc($uri))),
    	                (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")),
    	                "Customer-0.0.2-envelopes")
	    	    )
   	        :)
    	    else if (fn:matches($uri, "/products/.*\.xml"))
        	    then (
        	    	xdmp:log(("Procesing Translator URI " || $uri)),
	        	    xdmp:document-insert(
    	                fn:concat("/upconverts", $uri),
    	                new:instance-to-envelope(t:convert-instance-Product(fn:doc($uri))),
    	                (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")),
    	                "Product-0.0.2-envelopes")
	        	)
  	
        	else ()    
        
    return document { " " }
};
