xquery version "1.0-ml";
module namespace hub = "http://marklogic.com/rest-api/resource/transition-C";

import module namespace es = "http://marklogic.com/entity-services" at "/MarkLogic/entity-services/entity-services.xqy";
import module namespace translator = "http://marklogic.com/example/translator" at "/ext/versions-translator.xqy";
import module namespace m = "http://marklogic.com/example/Model" at "/ext/model-original.xqy";
import module namespace m-next = "http://marklogic.com/example/Model-next" at "/ext/model-next.xqy";
import module namespace options = "http://marklogic.com/example/options.xqy" at "/ext/options.xqy";
import module namespace search = "http://marklogic.com/appservices/search" at "/MarkLogic/appservices/search/search.xqy";

(:
 : This is a hybrid hub which uses the version translation
 : to create new envelopes from the old envelopes.
 : This pattern might be used to migrate data when re-extraction
 : is not desired.
 :)

(: search or SQL :)
declare function hub:get(
    $context as map:map,
    $params as map:map
) as document-node()
{
    let $q := $params=>map:get("q")
    let $sql := $params=>map:get("sql")
    let $version := $params=>map:get("version")
    let $result :=
            if ($q)
            then
                if ($version = "original")
                then options:results($q, $options:hub-next,
                    function($x) { es:instance-json-from-document($x)[1] })
                else options:results($q, $options:hub-next,
                    function($x) { es:instance-json-from-document($x)[2] })
            else
                xdmp:sql($sql) ! (xdmp:to-json(.) || "&#10;")

    return
        document {
            $result
        }
};

(: this hybrid hub doesn't accept new data.  It uses
 : data that was loaded into the original hub.
 : put for this hub triggers the migration (in one transaction, not realistic)
 :)
declare function hub:put(
    $context as map:map,
    $params as map:map,
    $body as document-node()?
) as document-node()
{
    for $envelope in cts:search(fn:collection("instance-envelopes"), cts:true-query())
    let $next-envelope :=
        $envelope
            =>translator:migrate()
        =>m-next:instance-to-envelope()
    return
        (
            xdmp:node-insert-after($envelope/es:envelope/es:instance, $next-envelope/es:envelope/es:instance),
            xdmp:document-add-collections(fn:base-uri($envelope), "instance-next-envelopes")
        ),
    document {
        "OK"
    }
};


(: clears the content database :)
declare function hub:delete(
    $context as map:map,
    $params as map:map
) as document-node()?
{
    (cts:collections() ! xdmp:collection-delete(.))[false()]
};
