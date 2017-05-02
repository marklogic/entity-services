xquery version "1.0-ml";
module namespace hub = "http://marklogic.com/rest-api/resource/hub-next";

import module namespace es = "http://marklogic.com/entity-services" at "/MarkLogic/entity-services/entity-services.xqy";
import module namespace m-next = "http://marklogic.com/example/Model-next" at "/ext/model-next.xqy";
import module namespace options = "http://marklogic.com/example/options.xqy" at "/ext/options.xqy";
import module namespace search = "http://marklogic.com/appservices/search" at "/MarkLogic/appservices/search/search.xqy";


(: search or SQL :)
declare function hub:get(
    $context as map:map,
    $params as map:map
) as document-node()
{
    let $q := $params=>map:get("q")
    let $sql := $params=>map:get("sql")
    return
        document {
            if ($q)
            then
                options:results($q, $options:hub-next, es:instance-json-from-document#1)
            else
                xdmp:sql($sql) ! (xdmp:to-json(.) || "&#10;")
        }
};

declare function hub:put(
    $context as map:map,
    $params as map:map,
    $body as document-node()?
) as document-node()
{
    let $uri := xdmp:random()
    (: no error checking! :)
    let $extraction := m-next:extract-instance-Person($body)
    let $env := m-next:instance-to-envelope($extraction)

    return
        document {
            (
                xdmp:document-insert("/instance-next/" || $uri, $env,
                    xdmp:default-permissions(),
                    "instance-next-envelopes"),
                "OK"
            )
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
