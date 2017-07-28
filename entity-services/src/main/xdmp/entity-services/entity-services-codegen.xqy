(:
 Copyright 2002-2017 MarkLogic Corporation.  All Rights Reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
:)
xquery version "1.0-ml";


(: This module supports code generation for entity services.
 : This module has no public API.  See entity-services.xqy for
 : the public interface.
 :)
module namespace es-codegen = "http://marklogic.com/entity-services-codegen";

import module namespace esi = "http://marklogic.com/entity-services-impl"
    at "entity-services-impl.xqy";

import module namespace functx   = "http://www.functx.com" at "/MarkLogic/functx/functx-1.0-nodoc-2007-01.xqy";

declare namespace es = "http://marklogic.com/entity-services";
declare namespace tde = "http://marklogic.com/xdmp/tde";
declare namespace xq = "http://www.w3.org/2012/xquery";


declare option xdmp:mapping "false";
declare option xq:require-feature "xdmp:three-one";

declare private function es-codegen:casting-function-name(
    $datatype as xs:string
) as xs:string
{
    if ($datatype eq "iri")
    then "sem:iri"
    else "xs:" || $datatype
};


declare private function es-codegen:comment(
    $comment-text as xs:string*
) as xs:string
{
    concat('    (: ', string-join($comment-text, "&#10;       "), '  :)&#10;')
};


declare private function es-codegen:variable-line-for(
    $prefix as xs:string,
    $model as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string
) as xs:string
{
    let $entity-type := $model=>map:get("definitions")=>map:get($entity-type-name)
    let $namespace := $entity-type=>map:get("namespace")
    let $namespace-prefix := $entity-type=>map:get("namespacePrefix")
    let $property-qname := 
        if ($namespace)
        then $namespace-prefix || ":" || $property-name
        else $property-name
    let $properties := map:get($entity-type, "properties")
    let $required-properties := if (empty(map:get($entity-type, "required")))
        then ()
        else json:array-values( map:get($entity-type, "required") )
    let $is-required := $property-name =
            ( map:get($entity-type, "primaryKey"), $required-properties )
    let $is-array :=
            map:get(map:get($properties, $property-name), "datatype")
            eq "array"
    let $property-datatype := esi:resolve-datatype($model, $entity-type-name, $property-name)
    let $casting-function-name := es-codegen:casting-function-name($property-datatype)
    let $wrap-if-array := function($str, $fn, $arrity-ok) {
            if ($is-array and $is-required)
            then concat("json:to-array(", $str, " ! ", $fn, "(.) )")
            else
            if ($is-array)
            then concat("es:extract-array(", $str, ", ", $fn, if ($arrity-ok) then "#1" else (), ")")
            else concat($str, " ! ", $fn, "(.)")
        }
    let $ref :=
        if ($is-array)
        then $properties=>map:get($property-name)=>map:get("items")=>map:get("$ref")
        else $properties=>map:get($property-name)=>map:get("$ref")
    let $path-to-property := concat("$source-node/", $property-qname)
    let $property-comment :=
        if (empty($ref))
        then ""
        else if (contains($ref, "#/definitions"))
        then es-codegen:comment("The following property is a local reference.")
        else
        es-codegen:comment((
            'The following property assigment comes from an external reference.',
            'Its generated value probably requires developer attention.'))
    let $ref-name := functx:substring-after-last($ref, "/")
    let $extract-reference-fn := concat("es:init-instance(?, '",$ref-name,"')")
    let $value :=
        if (empty($ref))
        then
            $wrap-if-array($path-to-property, $casting-function-name, true())
        else
            if (contains($ref, "#/definitions"))
            then
                $wrap-if-array($path-to-property, concat($prefix, ":extract-instance-", $ref-name), true())
            else
                $wrap-if-array($path-to-property, $extract-reference-fn, false())

    return
        fn:concat( $property-comment,
                   "    let $", 
                   $property-name, 
               functx:pad-string-to-length(
                 "  := ",
                 " ", 
                 max( ( (string-length($property-name)+4), 16 ) )+1),
               $value)
};

declare private function es-codegen:setter-for(
    $prefix as xs:string,
    $model as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string
)
{
    (: if a property is required, use map:with to force inclusion :)
    let $entity-type := $model=>map:get("definitions")=>map:get($entity-type-name)
    let $properties := map:get($entity-type, "properties")
    let $required-properties := if (empty(map:get($entity-type, "required")))
        then ()
        else json:array-values( map:get($entity-type, "required") )
    let $is-required := $property-name =
            ( map:get($entity-type, "primaryKey"), $required-properties )
    let $function-call-string :=
        if ($is-required)
        then "        =>   map:with("
        else "        =>es:optional("
    return fn:concat($function-call-string, "'", $property-name, "', $",$property-name,")")
                     
};

declare function es-codegen:instance-converter-generate(
    $model as map:map
) as document-node()
{
    let $info := map:get($model, "info")
    let $title := map:get($info, "title")
    let $prefix := lower-case(substring($title,1,1)) || substring($title,2)
    let $version:= map:get($info, "version")
    let $base-uri := esi:resolve-base-uri($info)
    return
document {
<module>xquery version '1.0-ml';

(:
 This module was generated by MarkLogic Entity Services.
 The source model was {$title}-{$version}

 For usage and extension points, see the Entity Services Developer's Guide

 https://docs.marklogic.com/guide/entity-services

 After modifying this file, put it in your project for deployment to the modules
 database of your application, and check it into your source control system.

 Generated at timestamp: {fn:current-dateTime()}
 :)

module namespace {$prefix}
    = '{$base-uri}{$title}-{$version}';

import module namespace es = 'http://marklogic.com/entity-services'
    at '/MarkLogic/entity-services/entity-services.xqy';

{ 
  (: namespace declarations :)
    for $entity-type-name in map:keys(map:get($model, "definitions"))
    let $entity-type := $model=>map:get("definitions")=>map:get($entity-type-name)
    let $namespace := $entity-type=>map:get("namespace")
    let $namespace-prefix := $entity-type=>map:get("namespacePrefix")
    return
        if ($namespace)
        then concat("declare namespace ",
             $namespace-prefix,
             " = '",
             $namespace,
             "';&#10;")
        else ()
}
        

declare option xdmp:mapping 'false';

{
    for $entity-type-name in map:keys(map:get($model, "definitions"))
    return
    <extract-instance>
(:~
 : Extracts instance data, as a map:map, from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a {$entity-type-name}
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function {$prefix}:extract-instance-{$entity-type-name}(
    $source as item()?
) as map:map
{{
    let $source-node := es:init-source($source, '{$entity-type-name}')
    (: begin customizations here :)
{ 
    let $properties := $model
        =>map:get("definitions")
        =>map:get($entity-type-name)
        =>map:get("properties")
    let $variable-setters := 
        for $property-name in map:keys($properties)
        return es-codegen:variable-line-for($prefix, $model, $entity-type-name, $property-name)
    return fn:string-join( $variable-setters, "&#10;")
    }
    (: end customizations :)

    let $instance := es:init-instance($source-node, '{ $entity-type-name }')
    (: Comment or remove the following line to suppress attachments :)
        =>es:add-attachments($source)

    return
    if (empty($source-node/*)) 
    then $instance
    else $instance
{
    (: Begin code generation block :)

    let $entity-type := $model=>map:get("definitions")=>map:get($entity-type-name)
    let $properties := $entity-type=>map:get("properties")
    let $namespace := $entity-type=>map:get("namespace")
    let $namespace-prefix := $entity-type=>map:get("namespacePrefix")
    let $value-lines :=
        (
        for $property-name in map:keys($properties)
        return es-codegen:setter-for($prefix, $model, $entity-type-name, $property-name),
        if ($namespace) 
        then (
            "        =>map:with('$namespace', '"|| $namespace ||"')", 
            "        =>map:with('$namespacePrefix', '"|| $namespace-prefix || "')"
            )
        else ()
        )
    return fn:string-join($value-lines, "&#10;")
        (: end code generation block :)
    }
}};
</extract-instance>/text()
}




(:~
 : Turns an entity instance into a JSON structure.
 : This out-of-the box implementation traverses a map structure
 : and turns it deterministically into a JSON tree.
 : Using this function as-is should be sufficient for most use
 : cases, and will play well with other generated artifacts.
 : @param $entity-instance A map:map instance returned from one of the extract-instance
 :    functions.
 : @return An XML element that encodes the instance.
 :)
declare function {$prefix}:instance-to-canonical-json(

    $entity-instance as map:map
) as object-node()
{{
    xdmp:to-json( {$prefix}:canonicalize($entity-instance) )/node()
}};


declare function {$prefix}:canonicalize(
    $entity-instance as map:map
) as map:map
{{
    json:object()
    =>map:with( map:get($entity-instance,'$type'),
        if ( map:contains($entity-instance, '$ref') )
        then map:get($entity-instance, '$ref')
        else
        let $m := json:object()
        let $_ := 
            for $key in map:keys($entity-instance)
            let $instance-property := map:get($entity-instance, $key)
            where ($key castable as xs:NCName)
            return
                typeswitch ($instance-property)
                (: This branch handles embedded objects.  You can choose to prune
                   an entity's representation of extend it with lookups here. :)
                case json:object+
                    return
                        for $prop in $instance-property
                        return map:put($m, $key, {$prefix}:canonicalize($prop))
                (: An array can also treated as multiple elements :)
                case json:array
                    return
                        (
                        for $val at $i in json:array-values($instance-property)
                        return
                            if ($val instance of json:object)
                            then json:set-item-at($instance-property, $i, {$prefix}:canonicalize($val))
                            else (),
                        map:put($m, $key, $instance-property)
                        )
                        
                (: A sequence of values should be simply treated as multiple elements :)
                (: TODO is this lossy? :)
                case item()+
                    return
                        for $val in $instance-property
                        return map:put($m, $key, $val)
                default return map:put($m, $key, $instance-property)
        return $m)

}};





(:~
 : Turns an entity instance into an XML structure.
 : This out-of-the box implementation traverses a map structure
 : and turns it deterministically into an XML tree.
 : Using this function as-is should be sufficient for most use
 : cases, and will play well with other generated artifacts.
 : @param $entity-instance A map:map instance returned from one of the extract-instance
 :    functions.
 : @return An XML element that encodes the instance.
 :)
declare function {$prefix}:instance-to-canonical-xml(
    $entity-instance as map:map
) as element()
{{
    (: Construct an element that is named the same as the Entity Type :)
    let $namespace := map:get($entity-instance, "$namespace")
    let $namespace-prefix := map:get($entity-instance, "$namespacePrefix")
    let $nsdecl := 
        if ($namespace) then
        namespace {{ $namespace-prefix }} {{ $namespace }}
        else ()
    let $type-name := map:get($entity-instance, '$type') 
    let $type-qname :=
        if ($namespace)
        then fn:QName( $namespace, $namespace-prefix || ":" || $type-name)
        else $type-name
    return
        element {{ $type-qname }}  {{
            $nsdecl,
            if ( map:contains($entity-instance, '$ref') )
            then map:get($entity-instance, '$ref')
            else
                for $key in map:keys($entity-instance)
                let $instance-property := map:get($entity-instance, $key)
                let $ns-key :=
                    if ($namespace and $key castable as xs:NCName)
                    then fn:QName( $namespace, $namespace-prefix || ":" || $key)
                    else $key
                where ($key castable as xs:NCName)
                return
                    typeswitch ($instance-property)
                    (: This branch handles embedded objects.  You can choose to prune
                       an entity's representation of extend it with lookups here. :)
                    case json:object+
                        return
                            for $prop in $instance-property
                            return element {{ $ns-key }} {{ {$prefix}:instance-to-canonical-xml($prop) }}
                    (: An array can also treated as multiple elements :)
                    case json:array
                        return
                            for $val in json:array-values($instance-property)
                            return
                                if ($val instance of json:object)
                                then element {{ $ns-key }} {{
                                    attribute datatype {{ 'array' }},
                                    {$prefix}:instance-to-canonical-xml($val)
                                }}
                                else element {{ $ns-key }} {{
                                    attribute datatype {{ 'array' }},
                                    $val }}
                    (: A sequence of values should be simply treated as multiple elements :)
                    case item()+
                        return
                            for $val in $instance-property
                            return element {{ $ns-key }} {{ $val }}
                    default return element {{ $ns-key }} {{ $instance-property }}
        }}
}};


(:
 : Wraps a canonical instance (returned by instance-to-canonical-xml())
 : within an envelope patterned document, along with the source
 : document, which is stored in an attachments section.
 : @param $entity-instance an instance, as returned by an extract-instance
 : function
 : @return A document which wraps both the canonical instance and source docs.
 :)
declare function {$prefix}:instance-to-xml-envelope(
    $entity-instance as map:map
) as document-node()
{{
    document {{
        element es:envelope {{
            element es:instance {{
                element es:info {{
                    element es:title {{ map:get($entity-instance,'$type') }},
                    element es:version {{ '{$version}' }}
                }},
                {$prefix}:instance-to-canonical-xml($entity-instance)
            }},
            es:serialize-attachments($entity-instance, "xml")
        }}
    }}
}};


(:
 : @param $entity-instance an instance, as returned by an extract-instance
 : function
 : @return A document which wraps both the canonical instance and source docs.
 :)
declare function {$prefix}:instance-to-envelope(
    $entity-instance as map:map
) as document-node()
{{
    {$prefix}:instance-to-xml-envelope($entity-instance)
}};



(:
 : Wraps a canonical instance (returned by instance-to-canonical-json())
 : within an envelope patterned document, along with the source
 : document, which is stored in an attachments section.
 : @param $entity-instance an instance, as returned by an extract-instance
 : function
 : @return A document which wraps both the canonical instance and source docs.
 :)
declare function {$prefix}:instance-to-json-envelope(
    $entity-instance as map:map
) as document-node()
{{
    document {{
        object-node {{ 'envelope' : 
            object-node {{ 'instance' :
                object-node {{ 'info' :
                    object-node {{
                        'title' : map:get($entity-instance,'$type'),
                        'version' : '{$version}'
                    }}
                }}
                +
                {$prefix}:instance-to-canonical-json($entity-instance)
            }}
            +
            es:serialize-attachments($entity-instance, "json")
        }}
    }}
}};
</module>/text()
}


};


declare private function es-codegen:value-for-conversion(
    $source-model as map:map,
    $target-model as map:map,
    $target-entity-type-name as xs:string,
    $target-property-name as xs:string,
    $display-property-name as xs:string,
    $let-expressions as map:map
) as xs:string
{
    let $target-info := map:get($target-model, "info")
    let $target-title := map:get($target-info, "title")
    let $target-prefix := lower-case(substring($target-title,1,1)) || substring($target-title,2)
    let $source-info := map:get($source-model, "info")
    let $source-title := map:get($source-info, "title")
    let $source-prefix := lower-case(substring($source-title,1,1)) || substring($source-title,2)
    let $module-prefix := $target-prefix || "-from-" || $source-prefix

    let $target-entity-type := $target-model
        =>map:get("definitions")
        =>map:get($target-entity-type-name)
    let $target-property := $target-entity-type
        =>map:get("properties")
        =>map:get($target-property-name)
    let $source-entity-type := $source-model
        =>map:get("definitions")
        =>map:get($target-entity-type-name)    (: this function is only called with matching types/props :)
    let $source-properties :=
        if (exists($source-entity-type))
        then $source-entity-type=>map:get("properties")
        else ()
    let $is-missing-source :=
        (exists($source-properties) and not($target-property-name = map:keys($source-properties)))
    let $source-correlate :=
        if (exists($source-properties))
        then map:get($source-properties, $target-property-name)
        else ()
    let $target-is-array :=
        if (exists($target-property))
        then $target-property=>map:get("datatype") eq 'array'
        else false()
    let $source-is-array :=
        exists($source-correlate) and $source-correlate=>map:get("datatype") eq 'array'
    let $target-ref :=
        if (exists($target-property))
        then
            if ($target-is-array)
            then $target-property=>map:get("items")=>map:get("$ref")
            else $target-property=>map:get("$ref")
        else ()
    let $source-ref :=
        if ($source-is-array)
        then $source-correlate=>map:get("items")=>map:get("$ref")
        else
        if (exists($source-correlate))
        then $source-correlate=>map:get("$ref")
        else ()
    let $is-scalar-from-ref := empty($target-ref) and exists($source-ref)
    let $target-is-scalar-array := $target-is-array and empty($target-ref)
         and map:get($target-property, "items")=>map:contains("datatype")
    let $source-is-scalar-array := $source-is-array and empty($source-ref)
        and map:get($source-correlate, "items")=>map:contains("datatype")
    let $properties-correlate := not($target-is-scalar-array) and not($source-is-scalar-array)
    let $is-array-from-scalar := $target-is-scalar-array and not($source-is-scalar-array)
    let $is-array-from-array := $target-is-scalar-array and $source-is-scalar-array
    let $truncates-array := not($target-is-array) and $source-is-scalar-array
    let $is-scalar-from-array := empty($target-ref) and $truncates-array

    let $target-datatype := esi:resolve-datatype($target-model, $target-entity-type-name, $target-property-name)
    let $casting-function-name := es-codegen:casting-function-name($target-datatype)
    let $required-properties := if (empty(map:get($target-entity-type, "required")))
        then ()
        else json:array-values( map:get($target-entity-type, "required") )
    let $is-required := $target-property-name =
            ( map:get($target-entity-type, "primaryKey"), $required-properties )
    let $path-to-property := concat("$source-node/", $target-property-name)
    let $target-ref-name := functx:substring-after-last($target-ref, "/")
    let $source-ref-name := functx:substring-after-last($source-ref, "/")
    (: warning: property path override if source is ref and target is scalar :)
    let $path-to-property := if ($is-scalar-from-ref)
                            then $path-to-property || "/" || $source-ref-name
                            else $path-to-property
    let $wrap-if-array := function($str, $fn, $arrity-ok) {
            if ($target-is-array and $is-required)
            then concat("json:to-array(", $str, " ! ", $fn, "(.) )")
            else
            if ($target-is-array)
            then concat("es:extract-array(", $str, ", ", $fn, if ($arrity-ok) then "#1" else (), ")")
            else if($source-is-array)
            then concat("fn:head("||$str||")", " ! ", $fn, "(.)")
            else concat($str, " ! ", $fn, "(.)")
        }
    let $is-reference-from-scalar-or-array :=
        exists($target-ref) and empty($source-ref)

    let $extract-scalar-fn := "es:init-instance(?, '"||$target-ref-name||"')"

    let $comment :=
        if ($is-missing-source)
        then es-codegen:comment((
                "The following property was missing from the source type.",
                "The XPath will not up-convert without intervention."))
        else if ($truncates-array)
        then es-codegen:comment("Warning: potential data loss, truncated array.")
        else ""

    let $extract-reference-fn :=
        map:put($let-expressions, $target-ref-name,
            fn:string-join(
                (
                concat('    let $extract-reference-',$target-ref-name,' := '),
                if (contains($target-ref, "#/definitions"))
                then
                    ("        function($path) { ",
                     "         if ($path/*)",
                     concat("         then ", $module-prefix, ":convert-instance-", $target-ref-name, "($path)"),
                     concat("         else es:init-instance($path, '", $target-ref-name, "')"),
                    "         }")
                else
                    concat("        es:init-instance(?, '", $target-ref-name, "')"),
                    ""
                ),
            "&#10;")
        )


    let $function-call-string :=
        if ($is-required)
        then "    =>   map:with("
        else "    =>es:optional("
    let $property-padding :=
        functx:pad-string-to-length("'" || $display-property-name || "',", " ", max((  (string-length($target-property-name)+4), 10) )+1 )
    let $value :=
        if ($is-scalar-from-array)
        then concat($casting-function-name, "( fn:head(", $path-to-property, ") )")
        else if (empty($target-ref))
        then $wrap-if-array($path-to-property, $casting-function-name, true())
        else if ($is-reference-from-scalar-or-array)
        then $wrap-if-array($path-to-property, $extract-scalar-fn, true())
        else $wrap-if-array($path-to-property || "/*", "$extract-reference-" || $target-ref-name, false() )

    let $let-expr := map:put($let-expressions, $target-property-name,
         concat(
            $comment, 
            "    let $", $target-property-name, " := ", $value
            ))
    return
        fn:concat($function-call-string, 
            $property-padding, 
            "$", $target-property-name,
            ")","&#10;") 
};


declare function es-codegen:version-translator-generate(
    $source-model as map:map,
    $target-model as map:map
) as document-node()
{
    let $target-info := map:get($target-model, "info")
    let $target-title := map:get($target-info, "title")
    let $target-prefix := lower-case(substring($target-title,1,1)) || substring($target-title,2)
    let $target-version:= map:get($target-info, "version")
    let $target-definitions := map:get($target-model, "definitions")
    let $target-entity-type-names := map:keys($target-definitions)
    let $target-base-uri := esi:resolve-base-uri($target-info)

    let $source-info := map:get($source-model, "info")
    let $source-title := map:get($source-info, "title")
    let $source-prefix := lower-case(substring($source-title,1,1)) || substring($source-title,2)
    let $source-version:= map:get($source-info, "version")
    let $source-definitions := map:get($source-model, "definitions")
    let $source-base-uri := esi:resolve-base-uri($source-info)

    let $module-prefix := $target-prefix || "-from-" || $source-prefix
    let $module-namespace := concat(
        $target-base-uri,
        $target-title,
        "-" ,
        $target-version,
        "-from-",
        $source-title,
        "-",
        $source-version)
    let $target-info := json:object()

(: BEGIN convert instance block :)
    let $convert-instance :=
        for $entity-type-name in $target-entity-type-names
        let $info-map := json:object()
        let $_ := map:put($target-info, $entity-type-name, $info-map)
        return
    <convert-instance>
(:~
 : Creates a map:map instance representation of the target
 : entity type {$entity-type-name} from an envelope document
 : containing a source entity instance, that is, instance data
 : of type {$entity-type-name}, version {$source-version}.
 : @param $source  An Entity Services envelope document (&lt;es:envelope&gt;)
 :  or a canonical XML instance of type {$entity-type-name}.
 : @return A map:map instance that holds the data for {$entity-type-name},
 :  version {$target-version}.
 :)
{ if (not($entity-type-name = map:keys($source-definitions)))
    then "
(: Type " || $entity-type-name || " is not in the source model.
 : XPath expressions are created as though there were no change between source and target type.
 :)"
    else () }
declare function {$module-prefix}:convert-instance-{$entity-type-name}(
    $source as node()
) as map:map
{{
    let $source-node := es:init-translation-source($source, '{$entity-type-name}')

{
    (: Begin code generation block :)
    let $let-expressions := json:object()
    let $entity-type := map:get($target-definitions, $entity-type-name)
    let $source-entity-type :=
            if (map:contains($source-definitions, $entity-type-name))
            then map:get($source-definitions, $entity-type-name)
            else map:put($info-map, "missing from source model.", ())
    let $ts-func :=
        function($value) {
            typeswitch($value)
            case empty-sequence() return "None"
            case json:array return fn:string-join(json:array-values($value), ", ")
            default return $value
        }
    let $compare :=
        function($property) {
            (
            $ts-func(map:get($entity-type, $property)),
            if (exists($source-entity-type) and map:contains($source-entity-type, $property))
            then "( in source: " || $ts-func(map:get($source-entity-type, $property)) || " )"
            else "( in source: None )"
            )
        }
    let $_ :=
            (
                map:put($info-map, "primaryKey: ", $compare("primaryKey")),
                map:put($info-map, "required: ", $compare("required")),
                map:put($info-map, "range indexes: ", $compare("rangeIndex")),
                map:put($info-map, "word lexicons: ", $compare("wordLexicon"))
            )
    let $properties := map:get($entity-type, "properties")
    let $values :=
        for $property-name in map:keys($properties)
        return
            (: note, this call passes mutable let-expressions for modification :)
            es-codegen:value-for-conversion($source-model,
                $target-model,
                $entity-type-name,
                $property-name,
                $property-name,
                $let-expressions)
    let $missing-properties :=
        if (exists($source-entity-type))
        then
            for $property-name in map:keys(map:get($source-entity-type, "properties"))
            where not($property-name = map:keys(map:get($entity-type, "properties")))
                return
                    es-codegen:value-for-conversion($source-model, $target-model, $entity-type-name, $property-name, "NO TARGET", map:map())
        else ()
    return
        fn:concat(
            fn:string-join(
                for $k in map:keys($let-expressions)
                where $k ne ""
                return map:get($let-expressions, $k), "&#10;"),
'&#10;
    return
    json:object()
    =>map:with("$type", "', $entity-type-name, '")
    (: Copy attachments from source document to the target :)
    =>es:copy-attachments($source-node)
    (: The following lines are generated from the "',$entity-type-name,'" entity type. :)&#10;',
            fn:string-join($values),
            if (exists($missing-properties))
            then
                es-codegen:comment(fn:string-join(
                    ("The following properties are in the source, but not the target: &#10;",
                    $missing-properties)))
            else ()
            )
    (: end code generation block :)
    }
}};
    </convert-instance>
(: END convert instance block :)

    let $removed-type :=
        (: Make comments for removed ET types :)
        for $removed-entity-type-name in map:keys($source-definitions)
        let $removed-entity-type := map:get($source-definitions, $removed-entity-type-name)
        where not( $removed-entity-type-name = $target-entity-type-names)
        return
<removed-type>
(:
 Entity type {$removed-entity-type-name} is in source document
 but not in target document.
 The following XPath expressions should get values from the source
 instances but there is no specified target.
 This comment can be as a starting point for writing a custom
 version converter.

declare function {$module-prefix}:convert-instance-{$removed-entity-type-name}(
    $source-node as node()
) as map:map
{{
    json:object()
    (: If the source is an envelope or part of an envelope document,
     : copies attachments to the target
     :)
    =>es:copy-attachments($source-node)
    =>map:with('$type', '{ $removed-entity-type-name }')
{

map:put($target-info, $removed-entity-type-name, map:entry("", "Removed Type")),
let $values :=
    for $removed-property-name in $removed-entity-type=>map:get("properties")=>map:keys()
    return es-codegen:variable-line-for($module-prefix, $source-model, $removed-entity-type-name, $removed-property-name)
return fn:string-join($values, "&#10;")
}
:)
</removed-type>

    return document {
<module>xquery version '1.0-ml';
module namespace {$module-prefix}
    = '{$module-namespace}';

import module namespace es = 'http://marklogic.com/entity-services'
    at '/MarkLogic/entity-services/entity-services.xqy';

declare option xdmp:mapping 'false';

(:
 This module was generated by MarkLogic Entity Services.
 Its purpose is to create instances of entity types
 defined in
 {$target-title || ", version " || $target-version}
 from documents that were persisted according to model
 {$source-title || ", version " || $source-version}


 For usage and extension points, see the Entity Services Developer's Guide

 https://docs.marklogic.com/guide/entity-services

 Generated at timestamp: {fn:current-dateTime()}

 Target Model {$target-title || "-" || $target-version} Info:

 {
map:keys($target-info) !
    function($et) {
        let $et-report := map:get($target-info, $et)
        return
        fn:concat("Type ", $et, ": &#10;",
            fn:string-join(
               map:keys($et-report) !
                    function($report-key) {
                        "    ",
                        $report-key,
                        fn:string-join(map:get($et-report, $report-key), ", "),
                        "&#10;"
                    }(.)
            )
        ),
        "&#10;"
   }(.)
}:)

{
(: this flowr makes order deterministic, which is better for dev UX. :)
for $c in $convert-instance order by $c return $c/text()}

{for $r in $removed-type order by $r return $r/text()}




</module>/text()
    }
};

