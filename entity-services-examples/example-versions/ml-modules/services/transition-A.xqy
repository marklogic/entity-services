xquery version "1.0-ml";
module namespace hub = "http://marklogic.com/rest-api/resource/transition-A";

import module namespace es = "http://marklogic.com/entity-services" at "/MarkLogic/entity-services/entity-services.xqy";
import module namespace translator = "http://marklogic.com/example/translator" at "/ext/versions-translator.xqy";
import module namespace m = "http://marklogic.com/example/Model" at "/ext/model-original.xqy";
import module namespace m-next = "http://marklogic.com/example/Model-next" at "/ext/model-next.xqy";
import module namespace options = "http://marklogic.com/example/options.xqy" at "/ext/options.xqy";
import module namespace search = "http://marklogic.com/appservices/search" at "/MarkLogic/appservices/search/search.xqy";

(:
 : This is an up-converting hub.
 : It can be used to accomodate new data sources or to develop with new clients
 : before a data model change is ready to persist.
 : So this data hub supports the original model, a well as a limited runtime
 : of the new model.
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
                if ($version = "next")
                then options:results($q, $options:hub,
                    function($x) {
                        $x
                        =>translator:up-convert()
                        =>m-next:instance-to-envelope()
                        =>es:instance-json-from-document()
                    })
                else options:results($q, $options:hub, es:instance-json-from-document#1)
            else
                xdmp:sql($sql) ! (xdmp:to-json(.) || "&#10;")

    return
        document {
            $result
        }
};

declare function hub:put(
    $context as map:map,
    $params as map:map,
    $body as document-node()?
) as document-node()
{
    let $uri := xdmp:random()
    let $extraction := m-next:extract-instance-Person($body)
    let $env := m:instance-to-envelope($extraction)

    return
        document {
            xdmp:document-insert("/instance/" || $uri, $env,
                xdmp:default-permissions(),
                "instance-envelopes"),
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
