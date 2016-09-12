xquery version "1.0-ml";

module namespace ga = "http://marklogic.com/rest-api/resource/generate-artifacts";

import module namespace es = "http://marklogic.com/entity-services"
    at "/MarkLogic/entity-services/entity-services.xqy";

declare function ga:post(
    $context as map:map,
    $params  as map:map,
    $input   as document-node()*
) as document-node()*
{
    (: /MarkLogic/git/entity-services/entity-services-e2e/gen :)
    let $model-path := map:get($params, "models-dir")
    let $path := map:get($params, "codegen-dir")
    let $_ := xdmp:log("generate-artifacts extension called... saving stubs to " || $path)

    (: the first model :)
    let $model := xdmp:filesystem-file($model-path || "/valid-ref.json")
        =>xdmp:unquote()

    (: a subsequent model :)
    let $next := xdmp:filesystem-file($model-path || "/valid-ref-2.json")
        =>xdmp:unquote()

    return ($model ,  $next,
        (: this converter is saved to temporary location for customization :)
        xdmp:save($path || "/ml-modules/ext/Northwind-0.0.1.xqy", $model=>es:instance-converter-generate()),
        xdmp:save($path || "/ml-schemas/Northwind-0.0.1.tdex", $model=>es:extraction-template-generate()),
        xdmp:save($path || "/ml-modules/options/northwind.xml", $model=>es:search-options-generate()),
        xdmp:save($path || "/ml-schemas/Northwind-0.0.1.xsd", $model=>es:schema-generate()),
        xdmp:save($path || "/ml-config/databases/content-database.json", $model=>es:database-properties-generate()),

        xdmp:save($path || "/ml-modules/ext/Northwind-0.0.2.xqy", $next=>es:instance-converter-generate()),
        xdmp:save($path || "/ml-schemas/Northwind-0.0.2.tdex", $next=>es:extraction-template-generate()),
        xdmp:save($path || "/ml-modules/ext/translator.xqy", es:version-translator-generate($model, $next))
    )

};
