xquery version "1.0-ml";

module namespace ga = "http://marklogic.com/rest-api/resource/generate-artifacts";

import module namespace functx = "http://www.functx.com"
    at "/MarkLogic/functx/functx-1.0-nodoc-2007-01.xqy";

import module namespace es = "http://marklogic.com/entity-services"
    at "/MarkLogic/entity-services/entity-services.xqy";

declare function ga:post(
    $context as map:map,
    $params  as map:map,
    $input   as document-node()*
) as document-node()*
{
    (: /MarkLogic/git/entity-services/entity-services-examples/gen :)
    let $path := map:get($params, "codegen-dir")
    let $_ := xdmp:log("generate-artifacts extension called... saving stubs to " || $path)

    let $models := fn:collection("http://marklogic.com/entity-services/models")
    for $model at $index in $models
    let $model-uri := functx:substring-after-last(fn:base-uri($model), "/")

    return (
        document {
            "Made model from document at ", $model-uri
        },
        (: this converter is saved to temporary location for customization :)
        xdmp:save($path || "/ml-modules/ext/" || fn:replace($model-uri, "json|xml$", "xqy"), $model=>es:instance-converter-generate() ),
        xdmp:save($path || "/ml-schemas/" || fn:replace($model-uri, "json|xml$", "tdex"), $model=>es:extraction-template-generate() ),
        xdmp:save($path || "/ml-modules/options/" || fn:replace($model-uri, "json|xml$", "xml"), $model=>es:search-options-generate()),
        xdmp:save($path || "/ml-schemas/" || fn:replace($model-uri, "json|xml$", "xsd"), $model=>es:schema-generate()),
        xdmp:save($path || "/ml-config/databases/content-database" || $index || ".json", $model=>es:database-properties-generate()),

        if ($index gt 1)
        then xdmp:save($path || "/ml-modules/ext/" || "translator" || $index || ".xqy", es:version-translator-generate($model, $models[$index - 1]))
        else ()
    )

};
