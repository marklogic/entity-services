xquery version "1.0-ml";
module namespace hub = "http://marklogic.com/rest-api/resource/transition-B";

import module namespace es = "http://marklogic.com/entity-services" at "/MarkLogic/entity-services/entity-services.xqy";
import module namespace m = "http://marklogic.com/example/Model" at "/ext/model-original.xqy";
import module namespace m-next = "http://marklogic.com/example/Model-next" at "/ext/model-next.xqy";
import module namespace options = "http://marklogic.com/example/options.xqy" at "/ext/options.xqy";
import module namespace search = "http://marklogic.com/appservices/search" at "/MarkLogic/appservices/search/search.xqy";

(:
 : This is a hybrid hub.
 : It stores two copies of the data (not a migration model)
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
                let $search-results :=
                    if ($version = "next")
                    then search:resolve-nodes(search:parse($q), $options:hub-next)
                    else search:resolve-nodes(search:parse($q), $options:hub)
                return $search-results ! (es:instance-json-from-document(.) || "&#10;")
            else
                xdmp:sql($sql) ! (xdmp:to-json(.) || "&#10;")

    return
        document {
            $result
        }
};

(: hybrid hub puts docs into both envelopes :)
declare function hub:put(
    $context as map:map,
    $params as map:map,
    $body as document-node()?
) as document-node()
{
    let $uri := xdmp:random()
    let $extraction := m:extract-instance-Person($body)
    let $extraction-next := m-next:extract-instance-Person($body)
    let $envelope := m:instance-to-envelope($extraction)
    let $envelope-next := m-next:instance-to-envelope($extraction-next)

    return
        document {
            xdmp:document-insert("/instance/" || $uri, $envelope,
                xdmp:default-permissions(),
                "instance-envelopes"),
            xdmp:document-insert("/instance-next/" || $uri, $envelope-next,
                xdmp:default-permissions(),
                "instance-next-envelopes"),
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
