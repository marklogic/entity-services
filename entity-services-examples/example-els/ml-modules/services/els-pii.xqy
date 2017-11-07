xquery version "1.0-ml";

module namespace securer = "http://marklogic.com/rest-api/resource/els-pii";

import module namespace op = "http://marklogic.com/optic" at "/MarkLogic/optic.xqy";

import module namespace sec="http://marklogic.com/xdmp/security"
at "/MarkLogic/security.xqy";

declare namespace rapi="http://marklogic.com/rest-api";


declare function securer:delete(
    $context as map:map,
    $params as map:map
) as document-node()?
{
    let $pii-columns := get-secured-properties()

    let $protect := function($type, $pname, $role) {
        function() {
            sec:unprotect-path("//envelope/instance/" || $type || "/" || $pname, ())
        }
    }
    let $remove-query-roleset := function($role) {
        function() {
            sec:remove-query-rolesets(sec:query-roleset($role))
        }
    }

    let $securing-functions :=
        ($pii-columns !  $protect(.=>map:get("type"), .=>map:get("pname"), .=>map:get("role")),
         $pii-columns !  $remove-query-roleset(.=>map:get("role")) )

    return
        document {
            for $fn in $securing-functions
            return
                xdmp:invoke-function($fn,
                    map:map()
                    =>map:with("database", xdmp:database("Security")))
            ,
            "Finished UnSecuring Socuments with functions."
        }
};


declare function securer:get-secured-properties()
{
    let $es := op:prefixer("http://marklogic.com/entity-services#")
    let $policy := op:prefixer("http://marklogic.com/entity-services/example-els/policy/")
    let $rdfs := op:prefixer("http://www.w3.org/2000/01/rdf-schema#")
    let $rdf := op:prefixer("http://www.w3.org/1999/02/22-rdf-syntax-ns#")

    let $piri := op:col("property-iri")
    let $type := op:col("type")
    let $pname := op:col("pname")
    let $role := op:col("role")
    let $pii-property := $policy("PersonallyIdentifiableInformationProperty")

    let $pii-columns :=
        op:from-triples((
            op:pattern(op:col("type-iri"), $es("property"), $piri),
            op:pattern(op:col("type-iri"), $es("title"), $type),
            op:pattern($piri, $rdf("type"), $pii-property),
            op:pattern($piri, $es("title"), $pname),
            op:pattern($pii-property, $policy("securedByRole"), $role)
        ))=>op:select(($type, $pname, $role))
        =>op:result()

    return $pii-columns

};

declare %rapi:transaction-mode("update") function securer:get(
    $context as map:map,
    $params as map:map
) as document-node()?
{
    let $pii-columns := get-secured-properties()

    let $protect := function($type, $pname, $role) {
        function() {
            sec:protect-path("//envelope/instance/" || $type || "/" || $pname,
                (),
                (xdmp:permission($role, "read")))
        }
    }
    let $add-roleset := function($role) {
        function() {
            sec:add-query-rolesets(sec:query-rolesets(sec:query-roleset($role)))
        }
    }

    let $securing-functions :=
        $pii-columns !  $protect(.=>map:get("type"), .=>map:get("pname"), .=>map:get("role"))
    let $roleset-functions :=
        $pii-columns !  $add-roleset(.=>map:get("role"))

    return
        document {
            for $fn in ($securing-functions, $roleset-functions)
            return
                xdmp:invoke-function($fn,
                    map:map()
                    =>map:with("database", xdmp:database("Security")))
            ,
            "Finished Securing Socuments with functions."
        }
};
