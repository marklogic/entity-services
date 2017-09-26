xquery version "1.0-ml";
module namespace nstranslate = "http://marklogic.com/rest-api/transform/nstranslate";

import module namespace t = "http://marklogic.com/ns2#Model_2ns-0.0.2-from-Model_1ns-0.0.1"
    at "/ext/nstranslate.xqy";
import module namespace new = "http://marklogic.com/ns2#Model_2ns-0.0.2"
    at "/ext/valid-2-namespace.xqy";


(:~
: User: bsrikanth
: Date: 8/28/17
: Time: 1:34 PM
:)

declare function nstranslate:transform(
    $context as map:map,
    $params as map:map,
    $body as document-node()
) as document-node()?
{
    let $uri := map:get($context, "uri")
    let $_ :=
    		if (fn:matches($uri, "order"))
    	        then (
    	        xdmp:log(("Procesing Translator URI " || $uri)),
    	        xdmp:document-insert(
    	            fn:concat("/upconverts", $uri=>fn:substring-before(".xml"),".json"),
    	            new:instance-to-envelope(t:convert-instance-Order(fn:doc($uri)), 'json'),
    	            (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")),
    	            "ns-translator-envelopes")
    	        )
    	        	
    	    else if (fn:matches($uri, "customer"))
    	        then (
    	        xdmp:log(("Procesing Translator URI " || $uri)),
	    	    xdmp:document-insert(
    	            fn:concat("/upconverts", $uri=>fn:substring-before(".xml"),".json"),
    	            new:instance-to-envelope(t:convert-instance-Customer(fn:doc($uri)), 'json'),
    	            (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")),
    	            "ns-translator-envelopes")
	    	    )
   	        
    	    else if (fn:matches($uri, "product"))
        	    then (
        	    xdmp:log(("Procesing Translator URI " || $uri)),
	        	xdmp:document-insert(
    	            fn:concat("/upconverts", $uri=>fn:substring-before(".xml"),".json"),
    	            new:instance-to-envelope(t:convert-instance-Product(fn:doc($uri)), 'json'),
    	            (xdmp:permission("nwind-reader", "read"), xdmp:permission("nwind-writer", "insert"), xdmp:permission("nwind-writer", "update")),
    	            "ns-translator-envelopes")
	        	)
  	
        	else ()    
        
    return document { " " }
};
