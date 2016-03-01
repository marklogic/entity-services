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
    <iso:schema xmlns:iso="http://purl.oclc.org/dsdl/schematron">
      <iso:ns prefix="es" uri="http://marklogic.com/entity-services"/>
      <iso:pattern>
        <iso:rule context="es:entity-type|/object-node()">
          <iso:assert test="count(es:info|info) eq 1" id="ES-INFOKEY">Entity Type must contain exactly one info declaration.</iso:assert>
          <iso:assert test="count(es:definitions|definitions) eq 1" id="ES-DEFINITIONSKEY">Entity Type must contain exactly one definitions declaration.</iso:assert>
        </iso:rule>
        <iso:rule context="es:info|/info">
          <iso:assert test="count(es:title|title) eq 1" id="ES-TITLEKEY">Entity Type must contain exactly one title declaration.</iso:assert>
          <iso:assert test="count(es:version|version) eq 1" id="ES-VERSIONKEY">Entity Type must contain exactly one version declaration.</iso:assert>
        </iso:rule>
        <iso:rule context="object-node()/*[primaryKey]">
          <iso:assert test="count(./primaryKey) eq 1" id="ES-PRIMARYKEY">Only one primary key allowed.</iso:assert>
        </iso:rule>
        <iso:rule context="properties/object-node()">
          <iso:assert test="if (./*[local-name(.) eq '$ref']) then count(./* except (items, description)) eq 1 else true()" id="ES-REF-ONLY">If using $ref, it must be the only key.</iso:assert>
          <iso:assert test="if (not(./*[local-name(.) eq '$ref'])) then ./datatype else true()" id="ES-DATATYPE-REQUIRED">A non-reference property must have a datatype</iso:assert>
        </iso:rule>
        <!-- xml version of properties -->
        <iso:rule context="es:property[es:ref]">
          <iso:assert test="count(./*) eq 10" id="ES-REF-ONLY">If using es:ref, it must be the only child of es:property.</iso:assert>
        </iso:rule>
        <iso:rule context="es:datatype|datatype">
         <iso:assert test=". = ('base64Binary' , 'boolean' , 'byte', 'date', 'dateTime', 'dayTimeDuration', 'decimal', 'double', 'duration', 'float', 'int', 'integer', 'long', 'short', 'string', 'time', 'unsignedInt', 'unsignedLong', 'unsignedShort', 'yearMonthDuration', 'anySimpleType', 'anyURI', 'iri', 'array')" id="ES-UNSUPPORTED-DATATYPE">Unsupported datatype.</iso:assert>
        </iso:rule>
      </iso:pattern>
    </iso:schema>
;


declare variable $esi:extraction-template-info := 
<template xmlns:es="http://marklogic.com/entity-services"
     xmlns="http://marklogic.com/xdmp/tde">
    <context>/info</context>
    <vars>
        <var><name>esn</name><val>"http://marklogic.com/entity-services#"</val></var>
        <var><name>rdf</name><val>"http://www.w3.org/1999/02/22-rdf-syntax-ns#"</val></var>
        <!-- baseUri is not required, there's a default (for non RDF use) -->
        <var><name>baseUriCoalesce</name><val>if (./baseUri) then ./baseUri else "http://example.org/"</val></var>
        <!-- baseUri appends # if doesn't end with # or / -->
        <var><name>baseUri</name><val>if (fn:matches($baseUriCoalesce, "[#/]$")) then $baseUriCoalesce else concat($baseUriCoalesce, "#")</val></var>
 
        <var><name>doc-subject-iri</name><val>sem:iri(concat($baseUri, xs:string(./title), "-", xs:string(./version)))</val></var>
        <var><name>propVersion</name><val>sem:iri(concat($esn, 'version'))</val></var>
        <var><name>propTitle</name><val>sem:iri(concat($esn, 'title'))</val></var>
    </vars>
    <triples>
        <triple>
          <subject><val>$doc-subject-iri</val></subject>
          <predicate><val>sem:iri(concat($rdf, "type"))</val></predicate>
          <object><val>sem:iri(concat($esn, "EntityServicesDoc"))</val></object>
        </triple>
        <triple>
          <subject><val>$doc-subject-iri</val></subject>
          <predicate><val>$propTitle</val></predicate>
          <object><val>xs:string(title)</val></object>
        </triple>
        <triple>
          <subject><val>$doc-subject-iri</val></subject>
          <predicate><val>$propVersion</val></predicate>
          <object><val>xs:string(version)</val></object>
        </triple>
    </triples>
</template>;

declare variable $esi:extraction-template-definitions :=
<template xmlns:es="http://marklogic.com/entity-services"
     xmlns="http://marklogic.com/xdmp/tde">
    <context>/definitions/*</context>
    <vars>
        {$esi:shared-extraction-vars/*}
<!-- TDE doesn't accept node-name right now 
        <var><name>entityTypeName</name><val>fn:node-name(.)</val></var>
-->
        <var><name>entityTypeName</name><val>"entitytypename"</val></var>
        <var>
            <name>et-subject-iri</name>
            <val>sem:iri(concat($doc-subject-prefix, $entityTypeName))</val>
        </var>
    </vars>
    <triples>
        <triple>
          <subject><val>$et-subject-iri</val></subject>
          <predicate><val>sem:iri(concat($rdf, "type"))</val></predicate>
          <object><val>sem:iri(concat($esn, "EntityType"))</val></object>
        </triple>
        <triple>
          <subject><val>$et-subject-iri</val></subject>
          <predicate><val>sem:iri(concat($esn, "title"))</val></predicate>
          <object><val>xs:string($entityTypeName)</val></object>
        </triple>
        <triple>
          <subject><val>$et-subject-iri</val></subject>
          <predicate><val>$propVersion</val></predicate>
<!-- root is not supported right now -->
          <object><val>"$version"</val></object>
        </triple>
        <triple>
          <subject><val>$doc-subject-iri</val></subject>
          <predicate><val>sem:iri(concat($esn, "definitions"))</val></predicate>
          <object><val>$et-subject-iri</val></object>
        </triple>
<!-- todo required, rangeIndex, wordLexicon-->
    </triples>
<!-- come back when TDE progresses
    <templates>
        <template>
            <context>primaryKey</context>
            <triples>
                <triple>
                  <subject><val>$et-subject-iri</val></subject>
                  <predicate><val>sem:iri(concat($esn, "primaryKey"))</val></predicate>
                  <object><val>.</val></object>
                </triple>
            </triples>
        </template>
    </templates>
-->
</template>;

declare variable $esi:shared-extraction-vars := 
    <vars xmlns="http://marklogic.com/xdmp/tde">
        <var><name>esn</name><val>"http://marklogic.com/entity-services#"</val></var>
        <var><name>rdf</name><val>"http://www.w3.org/1999/02/22-rdf-syntax-ns#"</val></var>
        <var><name>baseUriCoalesce</name><val>if(./root()/info/baseUri) then ./root()/info/baseUri else "http://example.org/"</val></var>
        <var><name>baseUri</name><val>if (fn:matches($baseUriCoalesce, "[#/]$")) then $baseUriCoalesce else concat($baseUriCoalesce, "#")</val></var>
        <var><name>baseUriPrefix</name><val>if (fn:matches($baseUriCoalesce, "[#/]$")) then $baseUriCoalesce else concat($baseUriCoalesce, "/")</val></var>
        <var><name>version</name><val>xs:string(./root()/info/version)</val></var>
        <var><name>doc-subject-iri</name><val>sem:iri(concat($baseUri, ./root()/info/title, "-", $version))</val></var>
        <var><name>doc-subject-prefix</name><val>sem:iri(concat($baseUriPrefix, ../info/title, "-", $version, "/"))</val></var>
        <var><name>propVersion</name><val>sem:iri(concat($esn, 'version'))</val></var>
        <var><name>propTitle</name><val>sem:iri(concat($esn, 'title'))</val></var>
        <var><name>propDefines</name><val>sem:iri(concat($esn, 'definitions'))</val></var>
        <var><name>propPrimaryKey</name><val>sem:iri(concat($esn, 'primaryKey'))</val></var>
        <var><name>propDatatype</name><val>sem:iri(concat($esn, 'datatype'))</val></var>
        <var><name>propRef</name><val>sem:iri(concat($esn, 'ref'))</val></var>
        <var><name>xsd</name><val>"http://www.w3.org/2001/XMLSchema#"</val></var>
        <var><name>XSD_BASE64BINARY</name><val>sem:iri(concat($xsd, "base64Binary"))</val></var>
        <var><name>XSD_BOOLEAN</name><val>sem:iri(concat($xsd, "boolean"))</val></var>
        <var><name>XSD_BYTE</name><val>sem:iri(concat($xsd, "byte"))</val></var>
        <var><name>XSD_DATE</name><val>sem:iri(concat($xsd, "date"))</val></var>
        <var><name>XSD_DATETIME</name><val>sem:iri(concat($xsd, "dateTime"))</val></var>
        <var><name>XSD_DAYTIMEDURATION</name><val>sem:iri(concat($xsd, "dayTimeDuration"))</val></var>
        <var><name>XSD_DECIMAL</name><val>sem:iri(concat($xsd, "decimal"))</val></var>
        <var><name>XSD_DOUBLE</name><val>sem:iri(concat($xsd, "double"))</val></var>
        <var><name>XSD_DURATION</name><val>sem:iri(concat($xsd, "duration"))</val></var>
        <var><name>XSD_FLOAT</name><val>sem:iri(concat($xsd, "float"))</val></var>
        <var><name>XSD_INT</name><val>sem:iri(concat($xsd, "int"))</val></var>
        <var><name>XSD_INTEGER</name><val>sem:iri(concat($xsd, "integer"))</val></var>
        <var><name>XSD_LONG</name><val>sem:iri(concat($xsd, "long"))</val></var>
        <var><name>XSD_SHORT</name><val>sem:iri(concat($xsd, "short"))</val></var>
        <var><name>XSD_STRING</name><val>sem:iri(concat($xsd, "string"))</val></var>
        <var><name>XSD_TIME</name><val>sem:iri(concat($xsd, "time"))</val></var>
        <var><name>XSD_UNSIGNEDINT</name><val>sem:iri(concat($xsd, "unsignedInt"))</val></var>
        <var><name>XSD_UNSIGNEDLONG</name><val>sem:iri(concat($xsd, "unsignedLong"))</val></var>
        <var><name>XSD_UNSIGNEDSHORT</name><val>sem:iri(concat($xsd, "unsignedShort"))</val></var>
        <var><name>XSD_YEARMONTHDURATION</name><val>sem:iri(concat($xsd, "yearMonthDuration"))</val></var>
        <var><name>XSD_ANYSIMPLETYPE</name><val>sem:iri(concat($xsd, "anySimpleType"))</val></var>
        <var><name>XSD_ANYURI</name><val>sem:iri(concat($xsd, "anyURI"))</val></var>
        <var><name>sem</name><val>"http://marklogic.com/semantics#"</val></var>
        <var><name>SEM_IRI</name><val>sem:iri(concat($sem, "iri"))</val></var>
        <var><name>json</name><val>"http://marklogic.com/json#"</val></var>
        <var><name>JSON_ARRAY</name><val>sem:iri(concat($json, "array"))</val></var>
    </vars> ;



(: TODO items, arrays :)
declare variable $esi:extraction-template-properties :=
<template xmlns:es="http://marklogic.com/entity-services"
     xmlns="http://marklogic.com/xdmp/tde">
    <context>/definitions/*/properties/*</context>
    <vars>
        {$esi:shared-extraction-vars/*}
        <var><name>entityTypeName</name><val>fn:node-name(../..)</val></var>
        <var><name>propertyName</name><val>fn:node-name(.)</val></var>
        <var><name>datatype</name><val>
                switch (xs:string(./datatype))
                case "base64Binary"       return $XSD_BASE64BINARY
                case "boolean"            return $XSD_BOOLEAN
                case "byte"               return $XSD_BYTE
                case "date"               return $XSD_DATE
                case "dateTime"           return $XSD_DATETIME
                case "dayTimeDuration"    return $XSD_DAYTIMEDURATION
                case "decimal"            return $XSD_DECIMAL
                case "double"             return $XSD_DOUBLE
                case "duration"           return $XSD_DURATION
                case "float"              return $XSD_FLOAT
                case "int"                return $XSD_INT
                case "integer"            return $XSD_INTEGER
                case "long"               return $XSD_LONG
                case "short"              return $XSD_SHORT
                case "string"             return $XSD_STRING
                case "time"               return $XSD_TIME
                case "unsignedInt"        return $XSD_UNSIGNEDINT
                case "unsignedLong"       return $XSD_UNSIGNEDLONG
                case "unsignedShort"      return $XSD_UNSIGNEDSHORT
                case "yearMonthDuration"  return $XSD_YEARMONTHDURATION
                case "anySimpleType"      return $XSD_ANYSIMPLETYPE
                case "anyURI"             return $XSD_ANYURI
                case "iri"                return $SEM_IRI
                case "array"              return $JSON_ARRAY
                (: default case is unsupported, but for now "" to avoid using subcontext :)
                default return ""</val>
        </var>
        <var>
            <name>et-subject-iri</name>
            <val>sem:iri(concat($doc-subject-prefix, $entityTypeName))</val>
        </var>
        <var>
            <name>property-subject-iri</name>
            <val>sem:iri(concat($et-subject-iri, "/", $propertyName))</val>
        </var>
        <var>
            <name>relationship-reference</name>
            <val>
                if (./node("$ref"))
                then if (fn:starts-with(./node("$ref"), "#/definitions/")) 
                    then sem:iri(fn:replace(xs:string(./node("$ref")),"#/definitions/",$doc-subject-prefix))
                    else sem:iri(xs:string(./node("$ref")))
                else "no ref"</val>
        </var>
    </vars>
    <triples>
        <triple>
          <subject><val>$et-subject-iri</val></subject>
          <predicate><val>sem:iri(concat($esn, "property"))</val></predicate>
          <object><val>$property-subject-iri</val></object>
        </triple>
        <triple>
          <subject><val>$property-subject-iri</val></subject>
          <predicate><val>$propTitle</val></predicate>
          <object><val>xs:string($propertyName)</val></object>
        </triple>
        <triple>
          <subject><val>$property-subject-iri</val></subject>
          <predicate><val>$propDatatype</val></predicate>
          <object><val>$datatype</val></object>
        </triple>
        <triple>
          <subject><val>$property-subject-iri</val></subject>
          <predicate><val>$propRef</val></predicate>
          <object><val>$relationship-reference</val></object>
        </triple>
<!--  only if there is a datatype can we include one or the other 
        <triple>
          <subject><val>$property-subject-iri</val></subject>
          <predicate><val>$propRef</val></predicate>
          <object><val>fn:head( ( ./node("$ref"), "scalar") )</val></object>
        </triple>
      TODO, make datatype into an IRI -->
    </triples>
</template>;


declare function esi:entity-type-validate(
    $entity-type as document-node()
) as xs:string*
{
(: for debugging 
    let $props := $entity-type//properties/object-node()
    let $_ := for $p in $props
              return xdmp:log(("PROPS W REF", $p[*[local-name(.) eq '$ref']],
                               "COUNT", count($p/(* except description))))
    return
:)
    validate:schematron($entity-type, $esi:entity-type-schematron)
};


declare function esi:extract-triples(
    $entity-type as document-node()
) as sem:triple*
{
    (: TODO adjust for when TDE outputs triples :)
    let $info-array := (tde:document-data-extract($entity-type, $esi:extraction-template-info)
          =>xdmp:unquote()
          =>xdmp:from-json())[1]
    let $definitions-array := (tde:document-data-extract($entity-type, $esi:extraction-template-definitions)
          =>xdmp:unquote()
          =>xdmp:from-json())[1]
    return (
            json:array-values($info-array), 
            json:array-values($definitions-array)
            ) ! sem:triple(.) 
(:
    let $definitions-array := tde:document-data-extract($entity-type, $esi:extraction-template-definitions)
    let $definitions-json-array := xdmp:from-json(xdmp:unquote($definitions-string-output))[1]
    let $properties-string-output := tde:document-data-extract($entity-type, $esi:extraction-template-properties)
    let $properties-json-array := xdmp:from-json(xdmp:unquote($properties-string-output))[1]
    return (
            json:array-values($info-json-array), 
            json:array-values($definitions-json-array), 
            json:array-values($properties-json-array)) ! sem:triple(.) 
:)
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
    xdmp:log(("CONV", $key)),
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
