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
          <iso:assert test="empty(es:base-uri|baseUri) or matches(es:base-uri|baseUri, '^[a-z]+:')" id="ES-BASEURI">If present, baseUri (es:base-uri) must be an absolute URI.</iso:assert>
          <iso:assert test="(title|es:title) castable as xs:NCName">Title must have no whitespace and must start with a letter.</iso:assert>
        </iso:rule>
        <iso:rule context="(definitions|es:definitions)"><iso:assert test="count(./*) ge 1" id="ES-DEFINITIONS">There must be at least one entity type in an entity services document.</iso:assert>
        </iso:rule>
        <!-- XML version of primary key rule -->
        <iso:rule context="es:definitions/node()[es:primary-key]">
          <iso:assert test="count(./es:primary-key) eq 1" id="ES-PRIMARYKEY">For each Entity Type, only one primary key allowed.</iso:assert>
        </iso:rule>
        <!-- JSON version of primary key rule -->
        <iso:rule context="object-node()/*[primaryKey]">
          <iso:assert test="count(./primaryKey) eq 1" id="ES-PRIMARYKEY">For each Entity Type, only one primary key allowed.</iso:assert>
        </iso:rule>
        <iso:rule context="properties/object-node()">
          <iso:assert test="if (./*[local-name(.) eq '$ref']) then count(./* except description) eq 1 else true()" id="ES-REF-ONLY">If a property has $ref as a child, then it cannot have a datatype.</iso:assert>
          <iso:assert test="if (not(./*[local-name(.) eq '$ref'])) then ./datatype else true()" id="ES-DATATYPE-REQUIRED">If a property is not a reference, then it must have a datatype.</iso:assert>
        </iso:rule>
        <iso:rule context="properties/*">
          <iso:assert test="./datatype|node('$ref')" id="ES-PROPERTY-IS-OBJECT">Each property must be an object, with either "datatype" or "$ref" as a key.</iso:assert>
        </iso:rule>
        <!-- xml version of properties -->
        <iso:rule context="es:properties/*">
          <iso:assert test="if (exists(./es:ref)) then count(./* except es:description) eq 1 else true()" id="ES-REF-ONLY">If a property has es:ref as a child, then it cannot have a datatype.</iso:assert>
          <iso:assert test="if (not(./*[local-name(.) eq 'ref'])) then ./es:datatype else true()" id="ES-DATATYPE-REQUIRED">If a property is not a reference, then it must have a datatype.</iso:assert>
        </iso:rule>
        <iso:rule context="es:ref|node('$ref')">
          <iso:assert test="starts-with(xs:string(.),'#/definitions/') or matches(xs:string(.), '^[a-x]+:')" id="ES-REF-VALUE">es:ref must start with "#/definitions/" or be an absolute IRI.</iso:assert>
          <iso:assert test="if (starts-with(xs:string(.), '#/definitions/')) then replace(xs:string(.), '#/definitions/', '') = (root(.)/definitions/*/node-name(.) ! xs:string(.), root(.)/es:entity-type/es:definitions/*/local-name(.)) else true()" id="ES-LOCAL-REF">Local reference <xsl:value-of select="."/> must resolve to local entity type.</iso:assert>
          <iso:assert test="if (not(contains(xs:string(.), '#/definitions/'))) then matches(xs:string(.), '^[a-z]+:') else true()" id="ES-ABSOLUTE-REF">Non-local reference <xsl:value-of select="."/> must be a valid URI.</iso:assert>
        </iso:rule>
        <iso:rule context="es:datatype">
         <iso:assert test=". = ('anyURI', 'base64Binary' , 'boolean' , 'byte', 'date', 'dateTime', 'dayTimeDuration', 'decimal', 'double', 'duration', 'float', 'gDay', 'gMonth', 'gMonthDay', 'gYear', 'gYearMonth', 'hexBinary', 'int', 'integer', 'long', 'negativeInteger', 'nonNegativeInteger', 'nonPositiveInteger', 'positiveInteger', 'short', 'string', 'time', 'unsignedByte', 'unsignedInt', 'unsignedLong', 'unsignedShort', 'yearMonthDuration', 'iri', 'array')" id="ES-UNSUPPORTED-DATATYPE">Unsupported datatype: <xsl:value-of select='.'/>.</iso:assert>
         <iso:assert test="if (. eq 'array') then exists(../es:items) else true()">Property <xsl:value-of select="local-name(..)" /> is of type "array" and must contain an "items" declaration.</iso:assert>
         <iso:assert test="if (. eq 'array') then exists(../es:items/es:ref) or ../es:items/es:datatype ne 'array' else true()">Property <xsl:value-of select="local-name(..)" /> cannot both be an "array" and have items of type "array".</iso:assert>
         <iso:assert test="not( . = ('base64Binary', 'hexBinary', 'duration', 'gMonthDay') and local-name(..) = ../../../es:range-index/text())"><xsl:value-of select="."/> in property <xsl:value-of select="local-name(..)" /> is unsupported for a range index.</iso:assert>
        </iso:rule>
        <iso:rule context="datatype">
         <iso:assert test=". = ('anyURI', 'base64Binary' , 'boolean' , 'byte', 'date', 'dateTime', 'dayTimeDuration', 'decimal', 'double', 'duration', 'float', 'gDay', 'gMonth', 'gMonthDay', 'gYear', 'gYearMonth', 'hexBinary', 'int', 'integer', 'long', 'negativeInteger', 'nonNegativeInteger', 'nonPositiveInteger', 'positiveInteger', 'short', 'string', 'time', 'unsignedByte', 'unsignedInt', 'unsignedLong', 'unsignedShort', 'yearMonthDuration', 'iri', 'array')" id="ES-UNSUPPORTED-DATATYPE">Unsupported datatype: <xsl:value-of select='.'/>.</iso:assert>
         <iso:assert test="if (. eq 'array') then exists(../items) else true()">Property <xsl:value-of select="node-name(.)" /> is of type "array" and must contain an "items" declaration.</iso:assert>
         <iso:assert test="if (. eq 'array') then exists(../items/node('$ref')) or ../items/datatype ne 'array' else true()">Property <xsl:value-of select="node-name(.)" /> cannot both be an "array" and have items of type "array".</iso:assert>
         <iso:assert test="not( . = ('base64Binary', 'hexBinary', 'duration', 'gMonthDay') and node-name(..) = ../../../rangeIndex)"><xsl:value-of select="."/> in property <xsl:value-of select="node-name(..)" /> is unsupported for a range index.</iso:assert>
        </iso:rule>
        <iso:rule context="es:collation|collation">
         <!-- this function throws an error for invalid collations, so must be caught in alidate function -->
         <iso:assert test="xdmp:collation-canonical-uri(.)">Collation <xsl:value-of select="." /> is not valid.</iso:assert>
        </iso:rule>
        <iso:rule context="required">
         <iso:assert test="xs:QName(.) = (../../properties/*/node-name())">"Required" property <xsl:value-of select="." /> doesn't exist.</iso:assert>
        </iso:rule>
        <iso:rule context="es:required">
         <iso:assert test="string(.) = (../es:properties/*/local-name())">"Required" property <xsl:value-of select="." /> doesn't exist.</iso:assert>
        </iso:rule>
        <iso:rule context="rangeIndex">
         <iso:assert test="xs:QName(.) = (../../properties/*/node-name(.))">Range index property <xsl:value-of select="." /> doesn't exist.</iso:assert>
        </iso:rule>
        <iso:rule context="es:range-index">
         <iso:assert test="string(.) = (../es:properties/*/local-name(.))">Range index property <xsl:value-of select="." /> doesn't exist.</iso:assert>
        </iso:rule>
        <iso:rule context="wordLexicon">
         <iso:assert test="xs:QName(.) = (../../properties/*/node-name(.))">Word lexicon property <xsl:value-of select="." /> doesn't exist.</iso:assert>
        </iso:rule>
        <iso:rule context="es:word-lexicon">
         <iso:assert test="string(.) = (../es:properties/*/local-name(.))">Word lexicon property <xsl:value-of select="." /> doesn't exist.</iso:assert>
        </iso:rule>
      </iso:pattern>
    </iso:schema>
;

declare function esi:entity-type-validate(
    $entity-type as document-node()
) as xs:string*
{
    try {
        validate:schematron($entity-type, $esi:entity-type-schematron)
    }
    catch ($e) {
        if ($e/error:code eq "XDMP-COLLATION")
        then "There is an invalid collation in the entity type document."
        else xdmp:rethrow()
    }
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
                esi:key-convert-to-xml($entity-type-map, "description"),
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
            let $_ := esi:put-if-exists($entity-type-map, "description", data($entity-type-node/es:description))
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
    case "anyURI" return "string"
    case "iri" return "string"
    case "byte" return "int"
    case "short" return "int"
    case "unsignedShort" return "unsignedInt"
    case "unsignedByte" return "unsignedInt"
    case "integer" return "decimal"
    case "negativeInteger" return "decimal"
    case "nonNegativeInteger" return "decimal"
    case "positiveInteger" return "decimal"
    case "nonPositiveInteger" return "decimal"
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
        let $specified-datatype := 
            if (map:contains($property, "datatype"))
            then
                if (map:get($property, "datatype") eq "array")
                then 
                    if (map:contains(map:get($property, "items"), "datatype"))
                    then map:get(map:get($property, "items"), "datatype")
                    else esi:ref-datatype($entity-type, $entity-type-name, $range-index-property)
                else map:get($property, "datatype")
            else esi:ref-datatype($entity-type, $entity-type-name, $range-index-property)
        let $datatype := esi:indexable-datatype($specified-datatype)
        let $collation := head( (map:get($property, "collation"), "http://marklogic.com/collation/en") )
        let $_ := map:put($ri-map, "collation", $collation)
        let $invalid-values := 
            if ($range-index-property = (map:get($entity-type-map, "primaryKey"), json:array-values(map:get($entity-type-map, "required"))))
            then "reject"
            else "ignore"
        let $_ := map:put($ri-map, "invalid-values", $invalid-values)
        let $_ := map:put($ri-map, "path-expression", "//es:instance/" || $entity-type-name || "/" || $range-index-property)
        let $_ := map:put($ri-map, "range-value-positions", false())
        let $_ := map:put($ri-map, "scalar-type", $datatype)
        return json:array-push($range-path-indexes, $ri-map)
        ,
        let $word-lexicon-properties := map:get(map:get($definitions, $entity-type-name), "wordLexicon")
        for $word-lexicon-property in json:array-values($word-lexicon-properties)
        let $wl-map := json:object()
        let $property := map:get(map:get($entity-type-map, "properties"), $word-lexicon-property)
        let $collation := head( (map:get($property, "collation"), "http://marklogic.com/collation/en") )
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
        let $primary-key-name := map:get($entity-type-map, "primaryKey")
        let $required-properties := ( json:array-values(map:get($entity-type-map, "required")), $primary-key-name)
        let $properties-map := map:get($entity-type-map, "properties")
        let $property-keys := map:keys($properties-map)
        return
        (
        <xs:complexType name="{ $entity-type-name }ContainerType" mixed="true">
            <xs:sequence>
                <xs:element minOccurs="0" maxOccurs="unbounded" ref="{ $entity-type-name }" />
            </xs:sequence>
        </xs:complexType>,
        <xs:complexType name="{ $entity-type-name }Type">
            <xs:sequence>
            {
                (: construct xs:element element for each property :)
                for $property-name in $property-keys
                let $property-map := map:get($properties-map, $property-name)
                let $datatype := map:get($property-map, "datatype")
                return
                    element xs:element {
                    (
                    if ($datatype eq "array")
                    then 
                       ( attribute minOccurs { "0" },
                         attribute maxOccurs { "unbounded" }
                        )
                    else if ($property-name = $required-properties )
                    then ()
                    else attribute minOccurs { "0" },
                    attribute ref { $property-name }
                    )
                }
            }
            </xs:sequence>
        </xs:complexType>,
        <xs:element name="{ $entity-type-name }" type="{ $entity-type-name }Type"/>,
        for $property-name in $property-keys
        let $property-map := map:get($properties-map, $property-name)
        return
            if (map:contains($property-map, "$ref"))
            then 
                let $ref-value := map:get($property-map, "$ref")
                return
                if (contains($ref-value, "#/definitions/"))
                then <xs:element name="{ $property-name }" type="{ replace($ref-value, '#/definitions/', '') }ContainerType"/>
                else <xs:element name="{ $property-name }" type="xs:anyURI"/>
            else if (map:contains($property-map, "datatype"))
            then
                let $datatype := map:get($property-map, "datatype")
                let $items-map := map:get($property-map, "items")
                return
                    if ($datatype eq "array")
                    then 
                        if (map:contains($items-map, "$ref"))
                        then
                            let $ref-value := map:get($items-map, "$ref")
                            return
                            if (contains($ref-value, "#/definitions/"))
                            then <xs:element name="{ $property-name }" type="{ replace($ref-value, '#/definitions/', '') }ContainerType"/>
                            else <xs:element name="{ $property-name }" type="xs:anyURI"/>
                        else
                            let $datatype := map:get($items-map, "datatype")
                            return
                                if ($datatype eq "iri")
                                then <xs:element name="{ $property-name }" type="sem:{ $datatype }"/>
                                else <xs:element name="{ $property-name }" type="xs:{ $datatype }"/>
                    else if ($datatype eq "iri")
                    then <xs:element name="{ $property-name }" type="sem:{ $datatype }"/>
                    else <xs:element name="{ $property-name }" type="xs:{ $datatype }"/>
            else ()
        )
    }
    </xs:schema>
};


declare private function esi:ref-datatype(
    $entity-type as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string
) as xs:string 
{
    let $ref-type := esi:ref-type($entity-type, $entity-type-name, $property-name)
    return 
        if (empty($ref-type))
        then "string"
        else 
            (: if the referent type has a primary key, use that type :)
            let $primary-key-property := map:get($ref-type, "primaryKey")
            return
                if (empty($primary-key-property))
                then "string"
                else map:get(
                        map:get(
                            map:get($ref-type, "properties"), 
                            $primary-key-property), 
                        "datatype")
};


declare function esi:ref-type-name(
    $entity-type as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string
) as xs:string
{
    let $definitions := map:get($entity-type, "definitions")
    let $entity-type := map:get($definitions, $entity-type-name)
    let $property := map:get(map:get($entity-type, "properties"), $property-name)
    let $ref-target := head( (map:get($property, "$ref"), 
                              map:get(map:get($property, "items"), "$ref") ) )
    return replace($ref-target, "#/definitions/", "")
};


declare private function esi:ref-type(
    $entity-type as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string
) as map:map?
{
    let $ref-type-name := esi:ref-type-name($entity-type, $entity-type-name, $property-name)
    return map:get(map:get($entity-type, "definitions"), $ref-type-name)
};

declare private function esi:ref-has-no-primary-key(
    $entity-type as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string
) as xs:boolean
{
    let $ref-type-name := esi:ref-type-name($entity-type, $entity-type-name, $property-name)
    let $ref-target := map:get(map:get($entity-type, "definitions"), $ref-type-name)
    return not("primaryKey" = map:keys($ref-target))
};

declare function esi:extraction-template-generate(
    $entity-type as map:map
) as element(tde:template)
{
    let $info := map:get($entity-type, "info")
    let $schema-name := map:get($info, "title")
    let $definitions := map:get($entity-type, "definitions")
    let $definition-keys := map:keys($definitions)
    let $scalar-rows := map:map()
    let $_ :=
        for $entity-type-name in $definition-keys
        let $entity-type-map := map:get($definitions, $entity-type-name)
        let $primary-key-name := map:get($entity-type-map, "primaryKey")
        let $required-properties := ( json:array-values(map:get($entity-type-map, "required")), $primary-key-name)
        let $properties-map := map:get($entity-type-map, "properties")
        let $primary-key-type := map:get( map:get($properties-map, $primary-key-name), "datatype" )
        let $property-keys := map:keys($properties-map)
        return
        map:put($scalar-rows, $entity-type-name,
            <tde:rows>
                <tde:row>
                    <tde:schema-name>{ $schema-name }</tde:schema-name>
                    <tde:view-name>{ $entity-type-name }</tde:view-name>
                    <tde:columns>
                    {
                    for $property-name in $property-keys
                    let $property-properties := map:get($properties-map, $property-name)
                    let $items-map := map:get($property-properties, "items")
                    let $datatype := 
                        if (map:get($property-properties, "datatype") eq "iri")
                        then "IRI"
                        else map:get($property-properties, "datatype")
                    let $is-nullable := 
                        if ($property-name = $required-properties)
                        then ()
                        else <tde:nullable>true</tde:nullable>
                    return
                        (: if the column is an array, skip it in scalar row :)
                        if (exists($items-map)) then ()
                        else
                            if ( map:contains($property-properties, "$ref") )
                            then
                            <tde:column>
                                <tde:name>{ $property-name }</tde:name>
                                <tde:scalar-type>{ esi:ref-datatype($entity-type, $entity-type-name, $property-name) } </tde:scalar-type>
                                <tde:val>{ $property-name }</tde:val>
                                {$is-nullable}
                            </tde:column>
                            else
                            <tde:column>
                                <tde:name>{ $property-name }</tde:name>
                                <tde:scalar-type>{ $datatype }</tde:scalar-type>
                                <tde:val>{ $property-name }</tde:val>
                                {$is-nullable}
                            </tde:column>
                    }
                    </tde:columns>
                </tde:row>
            </tde:rows>)
    let $array-rows := map:map()
    let $_ := 
        for $entity-type-name in $definition-keys
        let $entity-type-map := map:get($definitions, $entity-type-name)
        let $primary-key-name := map:get($entity-type-map, "primaryKey")
        let $required-properties := (json:array-values(map:get($entity-type-map, "required")), $primary-key-name)
        let $properties-map := map:get($entity-type-map, "properties")
        let $primary-key-type := map:get( map:get($properties-map, $primary-key-name), "datatype" )
        let $property-keys := map:keys($properties-map)
        let $column-map := map:map()
        let $_ := 
            for $property-name in $property-keys
            let $property-properties := map:get($properties-map, $property-name)
            let $items-map := map:get($property-properties, "items")
            let $is-ref := map:contains($items-map, "$ref")
            let $is-local-ref := map:contains($items-map, "$ref") and starts-with( map:get($items-map, "$ref"), "#/definitions/")
            let $is-nullable := 
                if ($property-name = $required-properties)
                then ()
                else <tde:nullable>true</tde:nullable>
            let $items-datatype := 
                if (map:get($items-map, "datatype") eq "iri")
                then "string"
                else map:get($items-map, "datatype")
            where exists($items-map)
            return
            map:put($column-map, $property-name,
                <tde:template>
                    <tde:context>./{ $property-name }</tde:context>
                    <tde:rows>
                      <tde:row>
                        <tde:schema-name>{ $schema-name }</tde:schema-name>
                        <tde:view-name>{ $entity-type-name }_{ $property-name }</tde:view-name>
                        <tde:columns>
                            <!-- this column joins to primary key of '{$entity-type-name}' -->
                            <tde:column>
                                <tde:name>{ $primary-key-name }</tde:name>
                                <tde:scalar-type>{ $primary-key-type }</tde:scalar-type>
                                <tde:val>../{ $primary-key-name }</tde:val>
                            </tde:column>
                            {
                            if ($is-ref and esi:ref-has-no-primary-key($entity-type, $entity-type-name, $property-name))
                            then 
                                ()
                            else if ($is-ref)
                            then
                                <tde:column>
                                    <!-- this column joins to primary key of '{$entity-type}' -->
                                    <tde:name>{ $property-name }</tde:name>
                                    <tde:scalar-type>{ esi:ref-datatype($entity-type, $entity-type-name, $property-name) }</tde:scalar-type>
                                    <tde:val>.</tde:val>
                                    {$is-nullable}
                                </tde:column>
                            else
                                <tde:column>
                                    <tde:name>{ $property-name }</tde:name>
                                    <tde:scalar-type>{ $items-datatype }</tde:scalar-type>
                                    <tde:val>.</tde:val>
                                    {$is-nullable}
                                </tde:column>,
                            if ($is-local-ref and esi:ref-has-no-primary-key($entity-type, $entity-type-name, $property-name))
                            then 
                                let $ref-type-name := esi:ref-type-name($entity-type, $entity-type-name, $property-name)
                                return (
                                map:get($scalar-rows, $ref-type-name)/tde:row[tde:view-name eq $ref-type-name ]/tde:columns/tde:column,
                                map:put($scalar-rows, $ref-type-name,
                                    comment { "No extraction template emitted for" || 
                                               $ref-type-name || 
                                               "as it was incorporated into another view. " 
                                            }
                                        )
                                )
                            else ()
                            }
                        </tde:columns>
                      </tde:row>
                    </tde:rows>
                </tde:template>
            )
        return 
        if (exists(map:keys($column-map)))
        then map:put($array-rows, $entity-type-name, $column-map)
        else ()
    let $entity-type-templates :=
        for $entity-type-name in map:keys($scalar-rows) 
        return
        if (empty ( ( json:array-values(
                        map:get(
                            map:get( $definitions, $entity-type-name ), "required")),
                        map:get(
                            map:get( $definitions, $entity-type-name ), "primaryKey"))))
        then comment { "The standalone template for " || $entity-type-name || 
                       " cannot be generated.  Each template row requires " ||
                       "a primary key or at least one requried property." }
        else 

        <tde:template>
            <tde:context>./{ $entity-type-name }</tde:context>
            { 
            map:get($scalar-rows, $entity-type-name),
            if (map:contains($array-rows, $entity-type-name))
            then 
                let $m := map:get($array-rows, $entity-type-name)
                return
                <tde:templates>{ for $k in map:keys($m) return map:get($m, $k) }</tde:templates>
            else ()
            }
        </tde:template>
    return
    <tde:template xmlns="http://marklogic.com/xdmp/tde">
        <tde:description>
Extraction Template Generated from Entity Type Document
graph uri: {esi:entity-type-graph-iri($entity-type)}
        </tde:description>
        <tde:context>//es:instance</tde:context>
        <tde:path-namespaces>
            <tde:path-namespace>
                <tde:prefix>es</tde:prefix>
                <tde:namespace-uri>http://marklogic.com/entity-services</tde:namespace-uri>
            </tde:path-namespace>
        </tde:path-namespaces>
        { 
        if ( $entity-type-templates/element() )
        then
            <tde:templates>
            { $entity-type-templates }
            </tde:templates>
        else
            comment { "An entity type must have at least one required column or a primary key to generate an extraction template." }
        }
    </tde:template>
};


declare private function esi:wrap-duplicates(
    $all-constraints as map:map,
    $property-name as xs:string,
    $constraint-template as element()
) as map:map
{
    if (map:contains($all-constraints, $property-name))
    then 
        map:with(
            $all-constraints,
            $property-name || xdmp:random(),
            comment { "This constraint is a duplicate and is commented out so as to be a valid options node.&#10;",
            xdmp:quote($constraint-template),
            "&#10;"
            })
    else map:with($all-constraints, $property-name, $constraint-template)
};


(:
 : Generates a configuration node for use with the MarkLogic Search API.
 : The resulting node can be used to configure a search application over
 : a corpus of entity types.
 :)
declare function esi:search-options-generate(
    $entity-type as map:map
) 
{
    let $info := map:get($entity-type, "info")
    let $schema-name := map:get($info, "title")
    let $definitions := map:get($entity-type, "definitions")
    let $definition-keys := map:keys($definitions)
    let $all-constraints := map:map()
    let $all-tuples-definitions := json:array()
    let $_ :=
        for $entity-type-name in $definition-keys
        let $entity-type-map := map:get($definitions, $entity-type-name)
        let $primary-key-name := map:get($entity-type-map, "primaryKey")
        let $properties-map := map:get($entity-type-map, "properties")
        let $tuples-range-definitions := json:array()
        let $_range-constraints :=
            for $property-name in json:array-values(map:get($entity-type-map, "rangeIndex"))
            let $property-map := map:get($properties-map, $property-name)
            (: TODO refactor :)
            let $specified-datatype := 
                if (map:contains($property-map, "datatype"))
                then
                    if (map:get($property-map, "datatype") eq "array")
                    then 
                        if (map:contains(map:get($property-map, "items"), "datatype"))
                        then map:get(map:get($property-map, "items"), "datatype")
                        else esi:ref-datatype($entity-type, $entity-type-name, $property-name)
                    else map:get($property-map, "datatype")
                else esi:ref-datatype($entity-type, $entity-type-name, $property-name)
            let $datatype := esi:indexable-datatype($specified-datatype)
            let $collation := if ($datatype eq "string") 
                then attribute
                    collation { 
                        head( (map:get($property-map, "collation"), "http://marklogic.com/collation/en") )
                    }
                else ()
            let $range-definition := 
                <search:range type="xs:{ $datatype }" facet="true">
                    { $collation }
                    <search:path-index
                        xmlns:es="http://marklogic.com/entity-services">//es:instance/{$entity-type-name}/{$property-name}</search:path-index>
                </search:range>
            let $constraint-template :=
                <search:constraint name="{ $property-name } ">
                    {$range-definition}
                </search:constraint>
            let $_ := json:array-push($tuples-range-definitions, $range-definition)
            return
            (
            esi:wrap-duplicates($all-constraints, $property-name, $constraint-template),
            if (json:array-size($tuples-range-definitions) gt 0)
            then
                json:array-push($all-tuples-definitions,
                    <search:tuples name="{ $entity-type-name }">
                        {json:array-values($tuples-range-definitions)}
                    </search:tuples>)
            else ()
            )
       let $_word-constraints := 
            for $property-name in json:array-values(map:get($entity-type-map, "wordLexicon"))
            return
            esi:wrap-duplicates($all-constraints, $property-name,
                <search:constraint name="{ $property-name } ">
                    <search:word>
                        <search:element ns="" name="{ $property-name }"/>
                    </search:word>
                </search:constraint>)
        let $_pk-constraint := 
            esi:wrap-duplicates($all-constraints, $primary-key-name,
                <search:constraint name="{ $primary-key-name } ">
                    <search:value>
                        <search:element ns="" name="{ $primary-key-name }"/>
                    </search:value>
                </search:constraint>)
        return ()
    let $types-expr := string-join( $definition-keys, "|" )
    let $type-constraint :=
        <search:constraint name="entity-type">
            <search:value>
                <search:element ns="http://marklogic.com/entity-services" name="title"/>
            </search:value>
        </search:constraint>
    return
    <search:options xmlns:search="http://marklogic.com/appservices/search">
        {
        $type-constraint, 
        map:keys($all-constraints) ! map:get($all-constraints, .),
        json:array-values($all-tuples-definitions),
        comment { 
            "Uncomment to return no results for a blank search, rather than the default of all results&#10;",           xdmp:quote(
        <search:term>
            <search:empty apply="no-results"/>
        </search:term>),
            "&#10;"
        },
        comment { "Change to 'filtered' to exclude false-positives in certain searches" },
        <search:search-option>unfiltered</search:search-option>,
        comment { "Modify document extraction to change results returned" },
        <search:extract-document-data selected="include">
            <search:extract-path xmlns:es="http://marklogic.com/entity-services">//es:instance/({ $types-expr })</search:extract-path>
        </search:extract-document-data>,

        comment { "Change or remove this additional-query to broaden search beyond entity instance documents" },
        <search:additional-query>
            <cts:element-query xmlns:cts="http://marklogic.com/cts">
            <cts:element xmlns:es="http://marklogic.com/entity-services">es:instance</cts:element>
            <cts:true-query/>
            </cts:element-query>
        </search:additional-query>,
        comment { "To return facets, change this option to 'true' and edit constraints" },
        <search:return-facets>false</search:return-facets>,
        comment { "To return snippets, comment out or remove this option" },
        <search:transform-results apply="empty-snippet" />
        }
    </search:options>
};

(: This function has no argument type because the XQuery engine otherwise
 : casts nodes to map:map, which would be confusing for this particular
 : function
 :)
declare function esi:ensure-entity-type(
    $entity-type
) as map:map
{
    if ($entity-type instance of map:map)
    then $entity-type
    else fn:error( (), "ES-ENTITY-TYPE-INVALID", "Entity types must be map:map (or its subtype json:object)")
};

