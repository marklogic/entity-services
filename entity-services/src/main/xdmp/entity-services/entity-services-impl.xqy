(:
 Copyright 2002-2016 MarkLogic Corporation.  All Rights Reserved. 

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

module namespace esi = "http://marklogic.com/entity-services-impl";
declare namespace es = "http://marklogic.com/entity-services";
declare namespace tde = "http://marklogic.com/xdmp/tde";

import module namespace sem = "http://marklogic.com/semantics" at "/MarkLogic/semantics.xqy"; 

import module namespace validate = "http://marklogic.com/validate" at "/MarkLogic/appservices/utils/validate.xqy";

import module namespace search = "http://marklogic.com/appservices/search" at "/MarkLogic/appservices/search/search.xqy";

declare default function namespace "http://www.w3.org/2005/xpath-functions";

declare variable $esi:MAX_TEST_INSTANCE_DEPTH := 2;
declare variable $esi:ENTITY_TYPE_COLLECTION := "http://marklogic.com/entity-services/entity-types/";

declare variable $esi:keys-to-element-names as map:map := 
    let $m := map:map()
    let $_ := map:put($m, "primaryKey", xs:QName("es:primary-key"))
    let $_ := map:put($m, "rangeIndex", xs:QName("es:range-index"))
    let $_ := map:put($m, "wordLexicon", xs:QName("es:word-lexicon"))
    let $_ := map:put($m, "baseUri", xs:QName("es:base-uri"))
    let $_ := map:put($m, "$ref", xs:QName("es:ref"))
    return $m;

declare variable $esi:element-names-to-keys as map:map :=
    let $m := map:map()
    let $_ := map:keys($esi:keys-to-element-names) !
        map:put($m, local-name-from-QName(map:get($esi:keys-to-element-names, .)), .)
    return $m;

declare variable $esi:entity-services-prefix := "http://marklogic.com/entity-services#";

declare variable $esi:entity-type-schematron :=
    <iso:schema xmlns:iso="http://purl.oclc.org/dsdl/schematron" xmlns:xsl="http://www.w3.org/1999/XSL/not-Transform">
      <iso:ns prefix="es" uri="http://marklogic.com/entity-services"/>
      <iso:pattern>
        <iso:rule context="es:entity-type|/object-node()">
          <iso:assert test="count(es:info|info) eq 1" id="ES-INFOKEY">Entity Type Document must contain exactly one info section.</iso:assert>
          <iso:assert test="count(es:definitions|definitions) eq 1" id="ES-DEFINITIONSKEY">Entity Type Document must contain exactly one definitions section.</iso:assert>
        </iso:rule>
        <iso:rule context="es:info|/info">
          <iso:assert test="count(es:title|title) eq 1" id="ES-TITLEKEY">"info" section must be an object and contain exactly one title declaration.</iso:assert>
          <iso:assert test="count(es:version|version) eq 1" id="ES-VERSIONKEY">"info" section must be an object and contain exactly one version declaration.</iso:assert>
        </iso:rule>
        <iso:rule context="es:definitions/node()[es:primary-key]">
          <iso:assert test="count(./es:primary-key) eq 1" id="ES-PRIMARYKEY">For each Entity Type, only one primary key allowed.</iso:assert>
        </iso:rule>
        <iso:rule context="object-node()/*[primaryKey]">
          <iso:assert test="count(./primaryKey) eq 1" id="ES-PRIMARYKEY">For each Entity Type, only one primary key allowed.</iso:assert>
        </iso:rule>
        <iso:rule context="properties/object-node()">
          <iso:assert test="if (./*[local-name(.) eq '$ref']) then count(./* except description) eq 1 else true()" id="ES-REF-ONLY">If a property has $ref as a child, then it cannot have a datatype.</iso:assert>
          <iso:assert test="if (not(./*[local-name(.) eq '$ref'])) then ./datatype else true()" id="ES-DATATYPE-REQUIRED">If a property is not a reference, then it must have a datatype.</iso:assert>
        </iso:rule>
        <!-- XML version of primary key rule -->
        <!-- xml version of properties -->
        <iso:rule context="es:properties/*">
          <iso:assert test="if (exists(./es:ref)) then count(./* except es:description) eq 1 else true()" id="ES-REF-ONLY">If a property has es:ref as a child, then it cannot have a datatype.</iso:assert>
          <iso:assert test="if (not(./*[local-name(.) eq 'ref'])) then ./es:datatype else true()" id="ES-DATATYPE-REQUIRED">If a property is not a reference, then it must have a datatype.</iso:assert>
        </iso:rule>
        <iso:rule context="es:ref">
          <iso:assert test="( starts-with(xs:string(.),'#/definitions/') or ( xs:string(.) castable as sem:iri) )" id="ES-REF-VALUE">es:ref must start with "#/definitions/" or be an absolute IRI.</iso:assert>
        </iso:rule>
        <iso:rule context="es:datatype">
         <iso:assert test=". = ('anyURI', 'base64Binary' , 'boolean' , 'byte', 'date', 'dateTime', 'dayTimeDuration', 'decimal', 'double', 'duration', 'float', 'gDay', 'gMonth', 'gMonthDay', 'gYear', 'gYearMonth', 'hexBinary', 'int', 'integer', 'long', 'negativeInteger', 'nonNegativeInteger', 'nonPositiveInteger', 'positiveInteger', 'short', 'string', 'time', 'unsignedByte', 'unsignedInt', 'unsignedLong', 'unsignedShort', 'yearMonthDuration', 'iri', 'array')" id="ES-UNSUPPORTED-DATATYPE">Unsupported datatype: <xsl:value-of select='.'/>.</iso:assert>
         <iso:assert test="not( . = ('base64Binary', 'hexBinary', 'duration', 'gDay', 'gMonth', 'gMonthDay', 'gYear', 'gYearMonth') and local-name(..) = ../../../es:range-index/text())"><xsl:value-of select="."/> in property <xsl:value-of select="local-name(..)" /> is unsupported for a range index.</iso:assert>
        </iso:rule>
        <iso:rule context="datatype">
         <iso:assert test=". = ('anyURI', 'base64Binary' , 'boolean' , 'byte', 'date', 'dateTime', 'dayTimeDuration', 'decimal', 'double', 'duration', 'float', 'gDay', 'gMonth', 'gMonthDay', 'gYear', 'gYearMonth', 'hexBinary', 'int', 'integer', 'long', 'negativeInteger', 'nonNegativeInteger', 'nonPositiveInteger', 'positiveInteger', 'short', 'string', 'time', 'unsignedByte', 'unsignedInt', 'unsignedLong', 'unsignedShort', 'yearMonthDuration', 'iri', 'array')" id="ES-UNSUPPORTED-DATATYPE">Unsupported datatype: <xsl:value-of select='.'/>.</iso:assert>
         <iso:assert test="not( . = ('base64Binary', 'hexBinary', 'duration', 'gDay', 'gMonth', 'gMonthDay', 'gYear', 'gYearMonth') and node-name(..) = ../../../rangeIndex)"><xsl:value-of select="."/> in property <xsl:value-of select="node-name(..)" /> is unsupported for a range index.</iso:assert>
        </iso:rule>
      </iso:pattern>
    </iso:schema>
;

declare function esi:entity-type-validate(
    $entity-type as document-node()
) as xs:string*
{
    validate:schematron($entity-type, $esi:entity-type-schematron)
};


declare function esi:entity-type-graph-iri(
    $entity-type as map:map
) as sem:iri
{
    let $info := map:get($entity-type, "info")
    let $baseUri := fn:head( (
        map:get($info, "baseUri"),
        "http://example.org/") )
    let $baseUriPrefix :=  
        if (fn:matches($baseUri, "[#/]$")) 
        then $baseUri
        else concat($baseUri, "#")
    return
    sem:iri(
        concat( $baseUriPrefix, 
               map:get($info, "title"),
               "-" ,
               map:get($info, "version")))
};



(: this funcion stores the two templates in the schemas db.
 : once Config is supported, remove this function
declare function esi:templates-bootstrap()
{
    xdmp:document-insert("json-entity-services.tde", $json-extraction-template,
                                xdmp:default-permissions(),
                                "http://marklogic.com/xdmp/tde"),
    xdmp:document-insert("xml-entity-services.tde", $xml-extraction-template,
                                xdmp:default-permissions(),
                                "http://marklogic.com/xdmp/tde")
};
 :)

(: 
 : This function is useful for debugging
 : and testing, but preferred access to triples
 : is via the triples index.
 :)
declare function esi:extract-triples(
    $entity-type-graph-iri as xs:string
) as sem:triple*
{
    sem:graph(sem:iri($entity-type-graph-iri))
};


declare private function esi:element-name-to-key(
    $key as xs:string
) as xs:string
{
    if (map:contains($esi:element-names-to-keys, $key))
    then map:get($esi:element-names-to-keys, $key)
    else $key
};

declare private function esi:key-convert-to-xml(
    $map as map:map?,
    $key as item()
) as element()*
{
    if (map:contains($map, $key))
    then 
        let $element-qname := 
            if (map:contains($esi:keys-to-element-names, $key))
            then map:get($esi:keys-to-element-names, $key)
            else xs:QName("es:" || $key) 
        return
            if (map:get($map, $key) instance of json:array)
            then 
                json:array-values(map:get($map, $key)) ! element { $element-qname } { . } 
            else
                if (map:get($map, $key) instance of map:map)
                then 
                    element { $element-qname } {
                        let $submap := map:get($map,$key)
                        return
                        map:keys($submap) ! esi:key-convert-to-xml($submap, map:get($submap,.))
                    }
                else element { $element-qname } { map:get($map, $key) }
    else ()
};

declare private function esi:put-if-exists(
    $map as map:map,
    $key-name as xs:string,
    $value as item()?
)
{
    typeswitch($value)
    case json:array return
        if (json:array-size($value) gt 0) 
        then map:put($map, $key-name, $value)
        else ()
    default return
        if (exists($value)) 
        then map:put($map, $key-name, $value)
        else ()
};

declare function esi:entity-type-to-xml(
    $entity-type as map:map
) as element(es:entity-type)
{
    let $info := map:get($entity-type, "info")
    return
    element es:entity-type { 
        namespace { "es" } { "http://marklogic.com/entity-services" },
        element es:info {
            element es:title { map:get($info, "title") },
            element es:version { map:get($info, "version") },
            esi:key-convert-to-xml($info, "baseUri"),
            esi:key-convert-to-xml($info, "description")
        },
        element es:definitions {
            let $definitions := map:get($entity-type, "definitions")
            for $entity-type-key in map:keys($definitions)
            let $entity-type-map := map:get($definitions, $entity-type-key)
            return
            element { $entity-type-key } {
                element es:properties {
                    let $properties := map:get($entity-type-map, "properties")
                    for $property-key in map:keys($properties)
                    let $property := map:get($properties, $property-key)
                    return element { $property-key } { 
                        esi:key-convert-to-xml($property, "datatype"),
                        esi:key-convert-to-xml($property, "collation"),
                        esi:key-convert-to-xml($property, "$ref"),
                        esi:key-convert-to-xml($property, "description"),
                        if (map:contains($property, "items"))
                        then
                            let $items-map := map:get($property, "items")
                            return
                            element es:items {
                                esi:key-convert-to-xml($items-map, "datatype"),
                                esi:key-convert-to-xml($items-map, "collation"),
                                esi:key-convert-to-xml($items-map, "$ref"),
                                esi:key-convert-to-xml($items-map, "description")
                            }
                        else ()
                    }
                },
                esi:key-convert-to-xml($entity-type-map, "primaryKey"),
                esi:key-convert-to-xml($entity-type-map, "required"),
                esi:key-convert-to-xml($entity-type-map, "rangeIndex"),
                esi:key-convert-to-xml($entity-type-map, "wordLexicon")
            }
        }
     }
};

declare function esi:entity-type-from-xml(
    $entity-type as element(es:entity-type)
) as map:map
{
    let $et := json:object()
    let $info := json:object()
    let $_ := map:put($info, "title", data($entity-type/es:info/es:title))
    let $_ := map:put($info, "version", data($entity-type/es:info/es:version))
    let $_ := esi:put-if-exists($info, "baseUri", data($entity-type/es:info/es:base-uri))
    let $_ := esi:put-if-exists($info, "description", data($entity-type/es:info/es:description))
    let $definitions := 
        let $d := json:object()
        let $_ := 
            for $entity-type-node in $entity-type/es:definitions/*
            let $entity-type-map := json:object()
            let $properties-map := json:object()
            let $_ := 
                for $property-node in $entity-type-node/es:properties/*
                let $property-attributes := json:object()
                let $_ := esi:put-if-exists($property-attributes, "datatype", data($property-node/es:datatype))
                let $_ := esi:put-if-exists($property-attributes, "$ref", data($property-node/es:ref))
                let $_ := esi:put-if-exists($property-attributes, "description", data($property-node/es:description))
                let $_ := esi:put-if-exists($property-attributes, "collation", data($property-node/es:collation))

                let $items-map := json:object()
                let $_ := esi:put-if-exists($items-map, "datatype", data($property-node/es:items/es:datatype))
                let $_ := esi:put-if-exists($items-map, "$ref", data($property-node/es:items/es:ref))
                let $_ := esi:put-if-exists($items-map, "description", data($property-node/es:items/es:description))
                let $_ := esi:put-if-exists($items-map, "collation", data($property-node/es:items/es:collation))
                let $_ := if (count(map:keys($items-map)) gt 0)
                        then map:put($property-attributes, "items", $items-map)
                        else ()
                return map:put($properties-map, fn:local-name($property-node), $property-attributes)
            let $_ := map:put($entity-type-map, "properties", $properties-map)
            let $_ := esi:put-if-exists($entity-type-map, "primaryKey", data($entity-type-node/es:primary-key))
            let $_ := esi:put-if-exists($entity-type-map, "required", json:to-array($entity-type-node/es:required/xs:string(.)))
            let $_ := esi:put-if-exists($entity-type-map, "rangeIndex", json:to-array($entity-type-node/es:range-index/xs:string(.)))
            let $_ := esi:put-if-exists($entity-type-map, "wordLexicon", json:to-array($entity-type-node/es:word-lexicon/xs:string(.)))
            return map:put($d, fn:local-name($entity-type-node), $entity-type-map)
        return $d

    let $_ := map:put($et, "info", $info)
    let $_ := map:put($et, "definitions", $definitions)
    
    return $et
};

(: 
 : Returns a constant value for each data type
 : -- TODO make other value generator method
 :)
declare function esi:create-test-value-from-datatype(
    $datatype as xs:string
) as item() 
{ 
    switch ($datatype)
    case "anyURI"             return xs:anyURI( "http://example.org/some-uri" ) 
    case "base64Binary"       return xs:base64Binary( "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz
IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg
dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu
dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo
ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4=" ) 
    case "boolean"            return true()  
    case "byte"               return xs:byte( 123 ) 
    case "date"               return xs:date( "2000-01-23" ) 
    case "dateTime"           return xs:dateTime( "2000-01-23T17:00:26.789186-08:00" )
    case "dayTimeDuration"    return xs:dayTimeDuration( "P1D" ) 
    case "decimal"            return xs:decimal( 113 ) 
    case "double"             return xs:double( 123 ) 
    case "duration"           return xs:duration( "P1D" ) 
    case "float"              return xs:float( 123 ) 
    case "gDay"               return xs:gDay( "---22" ) 
    case "gMonth"             return xs:gMonth("--03") 
    case "gMonthDay"          return xs:gMonthDay("--02-01")
    case "gYear"              return xs:gYear("-2001")
    case "gYearMonth"         return xs:gYearMonth("2001-01")
    case "hexBinary"          return xs:hexBinary("3f3c6d78206c657673726f693d6e3122302e20226e656f636964676e223d54552d4622383e3f")
    case "int"                return xs:int( 123 )
    case "integer"            return xs:integer( 123 ) 
    case "long"               return xs:long( 1355 ) 
    case "negativeInteger"    return xs:integer( -123 ) 
    case "nonNegativeInteger" return xs:integer( 123 ) 
    case "positiveInteger"    return xs:integer( 123 ) 
    case "nonPositiveInteger" return xs:integer( -123 ) 
    case "short"              return xs:short( 343 ) 
    case "string"             return xs:string( "some string" ) 
    case "time"               return xs:time( "09:00:15" ) 
    case "unsignedByte"       return xs:unsignedByte( 2  ) 
    case "unsignedInt"        return xs:unsignedInt( 5555  ) 
    case "unsignedLong"       return xs:unsignedLong( 999999999 ) 
    case "unsignedShort"      return xs:unsignedShort( 324 ) 
    case "yearMonthDuration"  return xs:yearMonthDuration( "P1Y" ) 
    case "iri"                return sem:iri( "http://example.org/some-iri" ) 
    default return xs:string( " ")
};

declare private function esi:resolve-test-reference(
    $entity-type as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string,
    $depth as xs:int)
{
    let $entity-definition := map:get(map:get($entity-type, "definitions"), $entity-type-name)
    let $property-definition := map:get( map:get($entity-definition, "properties"), $property-name)
    let $reference-value := 
        head( (map:get($property-definition, "$ref"),
               map:get(map:get($property-definition, "items"), "$ref") ) )
    (: is the reference value in this entity type document :)
    let $referenced-type :=
        if (contains($reference-value, "#/definitions"))
        then esi:create-test-instance($entity-type, replace($reference-value, "#/definitions/", ""), $depth + 1)
        else "externally-referenced-instance"
    return $referenced-type
};


declare function esi:create-test-value(
    $entity-type as map:map,
    $entity-name as xs:string,
    $property-name as xs:string,
    $property as map:map,
    $depth as xs:int
) as element()+
{
    let $datatype := map:get($property,"datatype")
    let $items := map:get($property, "items")
    let $ref := map:get($property,"$ref")
    return
        if (exists($datatype))
        then
            if ($datatype eq "array")
            then 
            (
            esi:create-test-value($entity-type, $entity-name, $property-name, $items, $depth) ,
            esi:create-test-value($entity-type, $entity-name, $property-name, $items, $depth) ,
            esi:create-test-value($entity-type, $entity-name, $property-name, $items, $depth) 
            )
            else 
            element { $property-name } {
                esi:create-test-value-from-datatype($datatype)
            }
        else if (exists($ref))
        then 
            element { $property-name } { 
                esi:resolve-test-reference($entity-type, $entity-name, $property-name, $depth) 
        }
        else 
            element { $property-name } { "This should not be here" }
};

declare function esi:create-test-instance(
    $entity-type as map:map,
    $entity-type-name as xs:string,
    $depth as xs:int
)
{
    let $definitions := map:get($entity-type, "definitions")
    return
    if ($depth lt $esi:MAX_TEST_INSTANCE_DEPTH)
    then
        element { $entity-type-name } {
            let $properties := map:get(map:get($definitions, $entity-type-name),"properties")
            let $property-keys := map:keys($properties)
            for $property in $property-keys
            return
                esi:create-test-value($entity-type, $entity-type-name, $property, map:get($properties, $property), $depth)
        }
    else ()
};


declare function esi:entity-type-get-test-instances(
    $entity-type as map:map
) as element()*
{
    let $definitions := map:get($entity-type, "definitions")
    let $definition-keys := map:keys($definitions)
    for $entity-type-name in $definition-keys
    return esi:create-test-instance($entity-type, $entity-type-name, 0)
};


declare function esi:indexable-datatype(
    $datatype as xs:string
) as xs:string
{
    switch ($datatype)
    case "boolean" return "string"
    case "duration" return "dayTimeDuration"
    case "byte" return "int"
    case "short" return "int"
    case "unsignedShort" return "unsignedInt"
    default return $datatype
};

declare function esi:database-properties-generate(
    $entity-type as map:map
) as document-node()
{
    let $definitions := map:get($entity-type, "definitions")
    let $definition-keys := map:keys($definitions)
    let $range-path-indexes := json:array()
    let $word-lexicons := json:array()
    let $_ := 
        for $entity-type-name in $definition-keys
        let $entity-type-map := map:get($definitions, $entity-type-name)
        return
        (
        let $range-index-properties := map:get($entity-type-map, "rangeIndex")
        for $range-index-property in json:array-values($range-index-properties)
        let $ri-map := json:object()
        let $property := map:get(map:get($entity-type-map, "properties"), $range-index-property)
        let $specified-datatype := head( (map:get($property, "datatype"), map:get(map:get($property, "items"), "datatype")) )
        let $datatype := esi:indexable-datatype($specified-datatype)
        let $collation := head( (map:get($property, "collation"), "http://marklogic.com/collation/") )
        let $_ := map:put($ri-map, "collation", $collation)
        let $_ := map:put($ri-map, "invalid-values", "reject")
        let $_ := map:put($ri-map, "path-expression", "//es:instance/" || $entity-type-name || "/" || $range-index-property)
        let $_ := map:put($ri-map, "range-value-positions", false())
        let $_ := map:put($ri-map, "scalar-type", $datatype)
        return json:array-push($range-path-indexes, $ri-map)
        ,
        let $word-lexicon-properties := map:get(map:get($definitions, $entity-type-name), "wordLexicon")
        for $word-lexicon-property in json:array-values($word-lexicon-properties)
        let $wl-map := json:object()
        let $property := map:get(map:get($entity-type-map, "properties"), $word-lexicon-property)
        let $collation := head( (map:get($property, "collation"), "http://marklogic.com/collation/") )
        let $_ := map:put($wl-map, "collation", $collation)
        let $_ := map:put($wl-map, "localname", $word-lexicon-property)
        let $_ := map:put($wl-map, "namespace-uri", "")
        return json:array-push($word-lexicons, $wl-map)
        )
    let $path-namespaces := json:array()
    let $pn := json:object()
    let $_ := map:put($pn, "prefix", "es")
    let $_ := map:put($pn, "namespace-uri", "http://marklogic.com/entity-services")
    let $_ := json:array-push($path-namespaces, $pn)
    let $database-properties := json:object()
    let $_ := map:put($database-properties, "database-name", "%%DATABASE%%")
    let $_ := map:put($database-properties, "schema-database", "%%SCHEMAS_DATABASE%%")
    let $_ := map:put($database-properties, "path-namespace", $path-namespaces)
    let $_ := map:put($database-properties, "element-word-lexicon", $word-lexicons)
    let $_ := map:put($database-properties, "range-path-index", $range-path-indexes)
    let $_ := map:put($database-properties, "triple-index", true())
    let $_ := map:put($database-properties, "collection-lexicon", true())
    return xdmp:to-json($database-properties)
};

declare function esi:schema-generate(
    $entity-type as map:map
) as element()*
{
    let $definitions := map:get($entity-type, "definitions")
    let $definition-keys := map:keys($definitions)
    return
    <xs:schema
        xmlns:xs="http://www.w3.org/2001/XMLSchema" 
        xmlns:sem="http://marklogic.com/semantics"
        elementFormDefault="qualified" 
        xmlns:es="http://marklogic.com/entity-services">
    {
        for $entity-type-name in $definition-keys
        let $entity-type-map := map:get($definitions, $entity-type-name)
        let $properties-map := map:get($entity-type-map, "properties")
        let $property-keys := map:keys($properties-map)
        return
        (
        <xs:complexType name="{$entity-type-name}ContainerType" mixed="true">
            <xs:sequence>
                <xs:element minOccurs="0" maxOccurs="unbounded" ref="{$entity-type-name}" />
            </xs:sequence>
        </xs:complexType>,
        <xs:complexType name="{$entity-type-name}Type">
            <xs:sequence>
        {
            for $property-name in $property-keys
            return
                <xs:element ref="{$property-name}"/>
        }
            </xs:sequence>
        </xs:complexType>,
        <xs:element name="{$entity-type-name}" type="{$entity-type-name}Type"/>,
        for $property-name in $property-keys
        let $property-map := map:get($properties-map, $property-name)
        return
            if (map:contains($property-map, "$ref"))
            then 
                let $ref := replace(map:get($property-map, "$ref"), "#/definitions/", "")
                return <xs:element name="{$property-name}" type="{$ref}ContainerType"/>
            else if (map:contains($property-map, "datatype"))
            then
                let $datatype := map:get($property-map, "datatype")
                return <xs:element name="{$property-name}" type="xs:{$datatype}"/>
            else ()
        )
    }
    </xs:schema>
};
