xquery version "1.0-ml";
module namespace ph = "http://marklogic.com/rest-api/transform/person-harmonizer";

declare namespace s = "http://www.w3.org/2005/xpath-functions";

import module namespace functx = "http://www.functx.com"
    at "/MarkLogic/functx/functx-1.0-nodoc-2007-01.xqy";

(: The extractions import :)
import module namespace person = "http://example.org/example-person#Person-0.0.1"
    at "/ext/person-extractions-0.0.1.xqy";

(:
 Applies an extraction function to the body.  This harmonizer
 splits on the number in the source's URI -- the integer at the end of the
 filename determines which extraction function is run.
 :)
declare function ph:transform(
        $context as map:map,
        $params as map:map,
        $body as document-node()
) as document-node()?
{
    let $uri := map:get($context, "uri")
    let $filename := functx:substring-after-last($uri, "/")
    let $_ := xdmp:log(("Procesing URI " || $filename))
    let $match-source-number := fn:head( ( fn:analyze-string($filename, "source-(\d)")//s:group/text()) )
    let $function-name := "extract-instance-Person-source-" || $match-source-number
    let $function := xdmp:function( xs:QName( "person:" || $function-name))
    let $instances := xdmp:apply($function, $body)
    let $_ := for $instance at $index in $instances
        return
            xdmp:document-insert(
                    fn:concat("/person-0.0.1/", $filename, "-", $index, "-e.xml"),
                    person:instance-to-envelope($instance),
                    (xdmp:permission("race-reader", "read"), xdmp:permission("race-writer", "insert"), xdmp:permission("race-writer", "update")),
                    "person-envelopes")
    return document { " " }
};
