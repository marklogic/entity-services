(:
 Copyright 2002-2018 MarkLogic Corporation.  All Rights Reserved.

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
declare namespace xq = "http://www.w3.org/2012/xquery";

import module namespace sem = "http://marklogic.com/semantics" at "/MarkLogic/semantics.xqy";

import module namespace validate = "http://marklogic.com/validate" at "/MarkLogic/appservices/utils/validate.xqy";

import module namespace search = "http://marklogic.com/appservices/search" at "/MarkLogic/appservices/search/search.xqy";

import module namespace functx   = "http://www.functx.com" at "/MarkLogic/functx/functx-1.0-nodoc-2007-01.xqy";

declare default function namespace "http://www.w3.org/2005/xpath-functions";

(: declare option xdmp:mapping "false"; :)
declare option xq:require-feature "xdmp:three-one";



declare private variable $esi:DEFAULT_BASE_URI := "http://example.org/";
declare private variable $esi:MAX_TEST_INSTANCE_DEPTH := 2;
declare private variable $esi:ENTITY_TYPE_COLLECTION := "http://marklogic.com/entity-services/models";

declare private variable $esi:keys-to-element-names as map:map :=
    let $m := map:map()
    let $_ := map:put($m, "primaryKey", xs:QName("es:primary-key"))
    let $_ := map:put($m, "rangeIndex", xs:QName("es:range-index"))
    let $_ := map:put($m, "pathRangeIndex", xs:QName("es:path-range-index"))
    let $_ := map:put($m, "elementRangeIndex", xs:QName("es:element-range-index"))
    let $_ := map:put($m, "wordLexicon", xs:QName("es:word-lexicon"))
    let $_ := map:put($m, "namespacePrefix", xs:QName("es:namespace-prefix"))
    let $_ := map:put($m, "baseUri", xs:QName("es:base-uri"))
    let $_ := map:put($m, "$ref", xs:QName("es:ref"))
    return $m;

declare private variable $esi:entity-services-prefix := "http://marklogic.com/entity-services#";

declare private variable $esi:model-schematron :=
    <iso:schema xmlns:iso="http://purl.oclc.org/dsdl/schematron" xmlns:xsl="http://www.w3.org/1999/XSL/not-Transform">
      <iso:ns prefix="es" uri="http://marklogic.com/entity-services"/>
      <iso:pattern>
        <iso:rule context="es:model|/object-node()">
          <iso:assert test="count(es:info|info) eq 1" id="ES-INFOKEY">Model descriptor must contain exactly one info section.</iso:assert>
          <iso:assert test="count(es:definitions|definitions) eq 1" id="ES-DEFINITIONSKEY">Model descriptor must contain exactly one definitions section.</iso:assert>
        </iso:rule>
        <iso:rule context="es:info|/info">
          <iso:assert test="count(es:title|title) eq 1" id="ES-TITLEKEY">"info" section must be an object and contain exactly one title declaration.</iso:assert>
          <iso:assert test="count(es:version|version) eq 1" id="ES-VERSIONKEY">"info" section must be an object and contain exactly one version declaration.</iso:assert>
          <iso:assert test="empty(es:base-uri|baseUri) or matches(es:base-uri|baseUri, '^[a-z]+:')" id="ES-BASEURI">If present, baseUri (es:base-uri) must be an absolute URI.</iso:assert>
          <iso:assert test="(title|es:title) castable as xs:NCName">Title must have no whitespace and must start with a letter.</iso:assert>
        </iso:rule>
        <iso:rule context="(definitions|es:definitions)"><iso:assert test="count(./*) ge 1" id="ES-DEFINITIONS">There must be at least one entity type in a model descriptor.</iso:assert>
        </iso:rule>
        <!-- XML version of primary key rule -->
        <iso:rule context="es:definitions/node()[es:primary-key]">
          <iso:assert test="count(./es:primary-key) eq 1" id="ES-PRIMARYKEY">For each Entity Type ('<xsl:value-of select="xs:string(node-name(.))"/>'), only one primary key allowed.</iso:assert>
        </iso:rule>
        <!-- JSON version of primary key rule -->
        <iso:rule context="object-node()/*[primaryKey]">
          <iso:assert test="count(./primaryKey) eq 1" id="ES-PRIMARYKEY">For each Entity Type ('<xsl:value-of select="xs:string(node-name(.))"/>'), only one primary key allowed.</iso:assert>
        </iso:rule>
        <iso:rule context="properties/object-node()">
          <iso:assert test="if (./*[local-name(.) eq '$ref']) then count(./* except description) eq 1 else true()" id="ES-REF-ONLY">Property '<xsl:value-of select="xs:string(node-name(.))"/>' has $ref as a child, so it cannot have a datatype.</iso:assert>
          <iso:assert test="if (not(./*[local-name(.) eq '$ref'])) then ./datatype else true()" id="ES-DATATYPE-REQUIRED">Property '<xsl:value-of select="xs:string(node-name(.))"/>' is not a reference, so it must have a datatype.</iso:assert>
        </iso:rule>
        <iso:rule context="properties/*">
          <iso:assert test="if (exists(./node('$ref'))) then not(xs:string(node-name(.)) = xs:string(../../primaryKey)) else true()" id="ES-REF-NOT-PK">Property <xsl:value-of select="xs:string(node-name(.))"/>: A reference cannot be primary key.</iso:assert>
          <iso:assert test="./datatype|node('$ref')" id="ES-PROPERTY-IS-OBJECT">Property '<xsl:value-of select="xs:string(node-name(.))"/>' must be an object with either "datatype" or "$ref" as a key.</iso:assert>
          <iso:assert test="not(xs:string(node-name(.)) = root(.)/definitions/*/node-name(.) ! xs:string(.))" id="ES-PROPERTY-TYPE-CONFLICT">Type names and property names must be distinct ('<xsl:value-of select="xs:string(node-name(.))"/>').</iso:assert>
        </iso:rule>
        <!-- xml version of properties -->
        <iso:rule context="es:properties/*">
          <iso:assert test="if (empty(./es:ref)) then true() else not(local-name(.) = xs:string(../../es:primary-key))" id="ES-REF-NOT-PK">Property <xsl:value-of select="local-name(.)"/>:  A reference cannot be primary key.</iso:assert>
          <iso:assert test="if (exists(./es:ref)) then count(./* except es:description) eq 1 else true()" id="ES-REF-ONLY">Property '<xsl:value-of select="xs:string(node-name(.))"/>' has es:ref as a child, so it cannot have a datatype.</iso:assert>
          <iso:assert test="if (not(./*[local-name(.) eq 'ref'])) then ./es:datatype else true()" id="ES-DATATYPE-REQUIRED">Property '<xsl:value-of select="xs:string(node-name(.))"/>' is not a reference, so it must have a datatype.</iso:assert>
          <iso:assert test="not(local-name(.) = root(.)/es:model/es:definitions/*/local-name(.))" id="ES-PROPERTY-TYPE-CONFLICT">Type names and property names must be distinct ('<xsl:value-of select="xs:string(node-name(.))"/>')</iso:assert>
        </iso:rule>
        <iso:rule context="es:ref|node('$ref')">
          <iso:assert test="starts-with(xs:string(.),'#/definitions/') or matches(xs:string(.), '^[a-z]+:')" id="ES-REF-VALUE">es:ref (property '<xsl:value-of select="xs:string(node-name(.))"/>') must start with "#/definitions/" or be an absolute IRI.</iso:assert>
          <iso:assert test="replace(xs:string(.), '.*/', '') castable as xs:NCName" id="ES-REF-VALUE"><xsl:value-of select="."/>: ref value must end with a simple name (xs:NCName).</iso:assert>
          <iso:assert test="if (starts-with(xs:string(.), '#/definitions/')) then replace(xs:string(.), '#/definitions/', '') = (root(.)/definitions/*/node-name(.) ! xs:string(.), root(.)/es:model/es:definitions/*/local-name(.)) else true()" id="ES-LOCAL-REF">Local reference <xsl:value-of select="."/> must resolve to local entity type.</iso:assert>
          <iso:assert test="if (not(contains(xs:string(.), '#/definitions/'))) then matches(xs:string(.), '^[a-z]+:') else true()" id="ES-ABSOLUTE-REF">Non-local reference <xsl:value-of select="."/> must be a valid URI.</iso:assert>
        </iso:rule>
        <iso:rule context="es:datatype">
         <iso:assert test=". = ('anyURI', 'base64Binary' , 'boolean' , 'byte', 'date', 'dateTime', 'dayTimeDuration', 'decimal', 'double', 'duration', 'float', 'gDay', 'gMonth', 'gMonthDay', 'gYear', 'gYearMonth', 'hexBinary', 'int', 'integer', 'long', 'negativeInteger', 'nonNegativeInteger', 'nonPositiveInteger', 'positiveInteger', 'short', 'string', 'time', 'unsignedByte', 'unsignedInt', 'unsignedLong', 'unsignedShort', 'yearMonthDuration', 'iri', 'array')" id="ES-UNSUPPORTED-DATATYPE">Property '<xsl:value-of select="xs:string(node-name(..))"/>' has unsupported datatype: <xsl:value-of select='.'/>.</iso:assert>
         <iso:assert test="if (. eq 'array') then exists(../es:items/(es:datatype|es:ref)) else true()">Property <xsl:value-of select="local-name(..)" /> is of type "array" and must contain a valid "items" declaration.</iso:assert>
         <iso:assert test="if (. eq 'array') then not(../es:items/es:datatype = 'array') else true()">Property <xsl:value-of select="local-name(..)" /> cannot both be an "array" and have items of type "array".</iso:assert>
         <iso:assert test="not( . = ('base64Binary', 'hexBinary', 'duration', 'gMonthDay') and local-name(..) = ../../../(es:range-index|es:path-range-index|es:element-range-index)/text())"><xsl:value-of select="."/> in property <xsl:value-of select="local-name(..)" /> is unsupported for a range index.</iso:assert>
        </iso:rule>
        <iso:rule context="datatype">
         <iso:assert test=". = ('anyURI', 'base64Binary' , 'boolean' , 'byte', 'date', 'dateTime', 'dayTimeDuration', 'decimal', 'double', 'duration', 'float', 'gDay', 'gMonth', 'gMonthDay', 'gYear', 'gYearMonth', 'hexBinary', 'int', 'integer', 'long', 'negativeInteger', 'nonNegativeInteger', 'nonPositiveInteger', 'positiveInteger', 'short', 'string', 'time', 'unsignedByte', 'unsignedInt', 'unsignedLong', 'unsignedShort', 'yearMonthDuration', 'iri', 'array')" id="ES-UNSUPPORTED-DATATYPE">Property '<xsl:value-of select="xs:string(node-name(.))"/>' has unsupported datatype: <xsl:value-of select='.'/>.</iso:assert>
         <iso:assert test="if (. eq 'array') then exists(../items/*[string(node-name(.)) = ('$ref', 'datatype')]) else true()">Property <xsl:value-of select="node-name(.)" /> is of type "array" and must contain a valid "items" declaration.</iso:assert>
         <iso:assert test="if (. eq 'array') then not(../items/datatype = 'array') else true()">Property <xsl:value-of select="node-name(.)" /> cannot both be an "array" and have items of type "array".</iso:assert>
         <iso:assert test="not( . = ('base64Binary', 'hexBinary', 'duration', 'gMonthDay') and node-name(..) = ../../../(pathRangeIndex|elementRangeIndex|rangeIndex))"><xsl:value-of select="."/> in property <xsl:value-of select="node-name(..)" /> is unsupported for a range index.</iso:assert>
        </iso:rule>
        <iso:rule context="es:collation|collation">
         <!-- this function throws an error for invalid collations, so must be caught in alidate function -->
         <iso:assert test="xdmp:collation-canonical-uri(.)">Collation <xsl:value-of select="." /> is not valid.</iso:assert>
        </iso:rule>
        <iso:rule context="primaryKey">
         <iso:assert test="xs:string(.) = (../properties/*/node-name() ! xs:string(.))">Primary Key <xsl:value-of select="." /> doesn't exist.</iso:assert>
        </iso:rule>
        <iso:rule context="primary-key">
         <iso:assert test="xs:string(.) = (../es:properties/*/local-name())">Primary Key <xsl:value-of select="." /> doesn't exist.</iso:assert>
        </iso:rule>
        <iso:rule context="required">
         <iso:assert test="../../array-node()">value of property 'required' must be an array.</iso:assert>
         <iso:assert test="xs:QName(.) = (../../properties/*/node-name())">"Required" property <xsl:value-of select="." /> doesn't exist.</iso:assert>
        </iso:rule>
        <iso:rule context="es:required">
         <iso:assert test="string(.) = (../es:properties/*/local-name())">"Required" property <xsl:value-of select="." /> doesn't exist.</iso:assert>
        </iso:rule>
        <iso:rule context="(pathRangeIndex|elementRangeIndex|rangeIndex)">
         <iso:assert test="xs:QName(.) = (../../properties/*/node-name(.))">Range index property <xsl:value-of select="." /> doesn't exist.</iso:assert>
        </iso:rule>
        <iso:rule context="(es:range-index|es:path-range-index|es:element-range-index)">
         <iso:assert test="string(.) = (../es:properties/*/local-name(.))">Range index property <xsl:value-of select="." /> doesn't exist.</iso:assert>
        </iso:rule>
        <iso:rule context="wordLexicon">
         <iso:assert test="xs:QName(.) = (../../properties/*/node-name(.))">Word lexicon property <xsl:value-of select="." /> doesn't exist.</iso:assert>
        </iso:rule>
        <iso:rule context="es:word-lexicon">
         <iso:assert test="string(.) = (../es:properties/*/local-name(.))">Word lexicon property <xsl:value-of select="." /> doesn't exist.</iso:assert>
        </iso:rule>
        <iso:rule context="namespace">
         <iso:assert test="matches(., '^[a-z]+:')">Namespace property must be a valid absolute URI.  Value is <xsl:value-of select="." />.</iso:assert>
         <iso:assert test="../namespacePrefix">namespace <xsl:value-of select="."/> has no namespacePrefix property.</iso:assert>
        </iso:rule>

        <iso:rule context="es:namespace">
         <iso:assert test="matches(., '^[a-z]+:')">Namespace property must be a valid absolute URI.  Value is <xsl:value-of select="." />.</iso:assert>
         <iso:assert test="../es:namespace-prefix">namespace <xsl:value-of select="."/> has no namespace-prefix property.</iso:assert>
        </iso:rule>

        <iso:rule context="namespacePrefix">
         <iso:assert test="../namespace">namespacePrefix <xsl:value-of select="."/> has no namespace property.</iso:assert>
        </iso:rule>

        <iso:rule context="es:namespace-prefix">
         <iso:assert test="../es:namespace">namespace-prefix  <xsl:value-of select="."/> has no namespace property.</iso:assert>
        </iso:rule>

        <iso:rule context="es:namespace-prefix|namespacePrefix">
         <iso:assert test="not( matches( string(.), '^(es|json|xsi|xs|xsd|[xX][mM][lL])$' ) )">Namespace prefix <xsl:value-of select="." /> is not valid.  It is a reserved pattern.</iso:assert>
        </iso:rule>

        <iso:rule context="/">
         <iso:assert test="count(distinct-values( .//(namespace|es:namespace) 
                                                    ! concat(../(namespacePrefix|es:namespace-prefix), .))) eq 
                           count(distinct-values( .//(namespace|es:namespace ))) and
                           count(distinct-values( .//(namespace|es:namespace ))) eq
                           count(distinct-values( .//(namespacePrefix|es:namespace-prefix )))">Each prefix and namespace pair must be unique.</iso:assert>
        </iso:rule>
        
      </iso:pattern>
    </iso:schema>
;

declare private function esi:model-validate-document(
    $model as document-node()
) as xs:string*
{
    try {
        validate:schematron($model, $esi:model-schematron)
    }
    catch ($e) {
        if ($e/error:code eq "XDMP-COLLATION")
        then "There is an invalid collation in the model."
        else xdmp:rethrow()
    }
};

declare private function esi:model-create(
    $model-descriptor
) as map:map
{
    typeswitch ($model-descriptor)
    case document-node() return
        if ($model-descriptor/object-node())
        then xdmp:from-json($model-descriptor)
        else esi:model-from-xml($model-descriptor/node())
    case element() return
        esi:model-from-xml($model-descriptor)
    case object-node() return
        xdmp:from-json($model-descriptor)
    case map:map return $model-descriptor
    default return fn:error( (), "ES-MODEL-INVALID",
        "Valid models must be JSON, XML or map:map")
};

declare function esi:model-validate(
    $model-descriptor
) as map:map
{
    let $errors :=
        typeswitch ($model-descriptor)
        case document-node() return
            esi:model-validate-document($model-descriptor)
        case element() return
            esi:model-validate-document(document { $model-descriptor } )
        case object-node() return
            esi:model-validate-document(document { $model-descriptor } )
        case map:map return
            esi:model-validate-document(xdmp:to-json($model-descriptor))
        default return fn:error( (), "ES-MODEL-INVALID",
            "Valid models must be JSON, XML or map:map")
    return
        if ($errors)
        then fn:error( (), "ES-MODEL-INVALID", $errors)
        else esi:model-create($model-descriptor)
};


declare function esi:model-graph-iri(
    $model as map:map
) as sem:iri
{
    let $info := map:get($model, "info")
    let $base-uri-prefix := esi:resolve-base-uri($info)
    return
    sem:iri(
        concat( $base-uri-prefix,
               map:get($info, "title"),
               "-" ,
               map:get($info, "version")))
};

declare function esi:model-graph-prefix(
    $model as map:map
) as sem:iri
{
    let $info := map:get($model, "info")
    let $base-uri-prefix := esi:resolve-base-prefix($info)
    return
    sem:iri(
        concat( $base-uri-prefix,
               map:get($info, "title"),
               "-" ,
               map:get($info, "version")))
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

declare private function esi:with-if-exists(
    $map as map:map,
    $key-name as xs:string,
    $value as item()?
) as map:map
{
    typeswitch($value)
    case json:array return
        if (json:array-size($value) gt 0)
        then map:put($map, $key-name, $value)
        else ()
    default return
        if (exists($value))
        then map:put($map, $key-name, $value)
        else (),
    $map
};

declare function esi:model-to-xml(
    $model as map:map
) as element(es:model)
{
    let $info := map:get($model, "info")
    return
    element es:model {
        namespace { "es" } { "http://marklogic.com/entity-services" },
        element es:info {
            element es:title { map:get($info, "title") },
            element es:version { map:get($info, "version") },
            esi:key-convert-to-xml($info, "baseUri"),
            esi:key-convert-to-xml($info, "description")
        },
        element es:definitions {
            for $entity-type-name in $model=>map:get("definitions")=>map:keys()
            let $entity-type := $model=>map:get("definitions")=>map:get($entity-type-name)
            return
            element { $entity-type-name } {
                element es:properties {
                    let $properties := map:get($entity-type, "properties")
                    for $property-name in map:keys($properties)
                    let $property := map:get($properties, $property-name)
                    return element { $property-name } {
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
                esi:key-convert-to-xml($entity-type, "description"),
                esi:key-convert-to-xml($entity-type, "primaryKey"),
                esi:key-convert-to-xml($entity-type, "required"),
                esi:key-convert-to-xml($entity-type, "namespace"),
                esi:key-convert-to-xml($entity-type, "namespacePrefix"),
                esi:key-convert-to-xml($entity-type, "rangeIndex"),
                esi:key-convert-to-xml($entity-type, "pathRangeIndex"),
                esi:key-convert-to-xml($entity-type, "elementRangeIndex"),
                esi:key-convert-to-xml($entity-type, "wordLexicon")
            }
        }
     }
};

declare function esi:model-from-xml(
    $model as element(es:model)
) as map:map
{
    let $info := json:object()
        =>map:with("title", data($model/es:info/es:title))
        =>map:with("version", data($model/es:info/es:version))
        =>esi:with-if-exists("baseUri", data($model/es:info/es:base-uri))
        =>esi:with-if-exists("description", data($model/es:info/es:description))
    let $definitions :=
        let $d := json:object()
        let $_ :=
            for $entity-type-node in $model/es:definitions/*
            let $entity-type := json:object()
            let $properties := json:object()
            let $_ :=
                for $property-node in $entity-type-node/es:properties/*
                let $property-attributes := json:object()
                    =>esi:with-if-exists("datatype", data($property-node/es:datatype))
                    =>esi:with-if-exists("$ref", data($property-node/es:ref))
                    =>esi:with-if-exists("description", data($property-node/es:description))
                    =>esi:with-if-exists("collation", data($property-node/es:collation))

                let $items-map := json:object()
                    =>esi:with-if-exists("datatype", data($property-node/es:items/es:datatype))
                    =>esi:with-if-exists("$ref", data($property-node/es:items/es:ref))
                    =>esi:with-if-exists("description", data($property-node/es:items/es:description))
                    =>esi:with-if-exists("collation", data($property-node/es:items/es:collation))
                let $_ := if (count(map:keys($items-map)) gt 0)
                        then map:put($property-attributes, "items", $items-map)
                        else ()
                return map:put($properties, fn:local-name($property-node), $property-attributes)
            let $_ := map:put($entity-type, "properties", $properties)
            let $_ := esi:with-if-exists($entity-type, "primaryKey", data($entity-type-node/es:primary-key))
            let $_ := esi:with-if-exists($entity-type, "required", json:to-array($entity-type-node/es:required/xs:string(.)))
            let $_ := esi:with-if-exists($entity-type, "namespace", $entity-type-node/es:namespace/xs:string(.))
            let $_ := esi:with-if-exists($entity-type, "namespacePrefix", $entity-type-node/es:namespace-prefix/xs:string(.))
            let $_ := esi:with-if-exists($entity-type, "rangeIndex", json:to-array($entity-type-node/es:range-index/xs:string(.)))
            let $_ := esi:with-if-exists($entity-type, "pathRangeIndex", json:to-array($entity-type-node/es:path-range-index/xs:string(.)))
            let $_ := esi:with-if-exists($entity-type, "elementRangeIndex", json:to-array($entity-type-node/es:element-range-index/xs:string(.)))
            let $_ := esi:with-if-exists($entity-type, "wordLexicon", json:to-array($entity-type-node/es:word-lexicon/xs:string(.)))
            let $_ := esi:with-if-exists($entity-type, "description", data($entity-type-node/es:description))
            return map:put($d, fn:local-name($entity-type-node), $entity-type)
        return $d

    return json:object()
        =>map:with("info", $info)
        =>map:with("definitions", $definitions)
};


(: experiment :)
declare function esi:model-to-triples(
    $model as map:map
)
{
    tde:node-data-extract(xdmp:to-json($model))
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
    $model as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string,
    $depth as xs:int)
{
    let $property-definition := $model
            =>map:get("definitions")
            =>map:get($entity-type-name)
            =>map:get("properties")
            =>map:get($property-name)
    let $reference-value :=
        head( ($property-definition=>map:get("$ref"),
                              $property-definition=>map:get("items")=>map:get("$ref") ) )
    let $ref-name := functx:substring-after-last($reference-value, "/")
    let $namespace-prefix :=
        $model=>map:get("definitions")=>map:get($ref-name)=>map:get("namespacePrefix")
    let $prefix-value :=
        if ($namespace-prefix)
        then $namespace-prefix || ":"
        else ""
    let $namespace-uri :=
        $model=>map:get("definitions")=>map:get($ref-name)=>map:get("namespace")
    let $nsdecl :=
        if ($namespace-prefix)
        then element { "X" } { namespace { $namespace-prefix } { $namespace-uri } }
        else <x/>
    let $qname := fn:resolve-QName($prefix-value || $ref-name, $nsdecl)
    (: is the reference value in this model :)
    let $referenced-type :=
        if (contains($reference-value, "#/definitions"))
        then
            if ($depth eq $esi:MAX_TEST_INSTANCE_DEPTH - 1)
            then
                element {$qname} {
                    esi:ref-datatype($model, $entity-type-name, $property-name)
                      =>esi:create-test-value-from-datatype()
                }
            else esi:create-test-instance($model, $ref-name, $depth + 1)
        else element { $ref-name } {
            "externally-referenced-instance"
            }
    return $referenced-type
};


declare function esi:create-test-value(
    $model as map:map,
    $entity-name as xs:string,
    $property-name as xs:string,
    $property as map:map,
    $depth as xs:int,
    $parent-type as xs:string,
    $nsdecl as element()
) as element()+
{
    let $datatype := map:get($property,"datatype")
    let $items := map:get($property, "items")
    let $ref := map:get($property,"$ref")
    let $namespace-prefix :=
        $model=>map:get("definitions")=>map:get($entity-name)=>map:get("namespacePrefix")
    let $prefix-value :=
        if ($namespace-prefix)
        then $namespace-prefix || ":"
        else ""
    let $qname := fn:resolve-QName($prefix-value || $property-name, $nsdecl)
    return
        if (exists($datatype))
        then
            if ($datatype eq "array")
            then
                esi:create-test-value($model, $entity-name, $property-name, $items, $depth, "array", $nsdecl)
            else
                element { $qname } {
                    if ($parent-type eq "array")
                    then attribute datatype { "array" }
                    else (),
                    esi:create-test-value-from-datatype($datatype)
                }
        else if (exists($ref))
        then
            element { $qname } {
                if ($parent-type eq "array")
                then attribute datatype { "array" }
                else (),
                esi:resolve-test-reference($model, $entity-name, $property-name, $depth)
            }
        else
            element { $property-name } { "This should not be here" }
};

declare function esi:create-test-instance(
    $model as map:map,
    $entity-type-name as xs:string,
    $depth as xs:int
)
{
    if ($depth lt $esi:MAX_TEST_INSTANCE_DEPTH)
    then
        let $entity-type := $model
                    =>map:get("definitions")
                    =>map:get($entity-type-name)
        let $namespace-prefix := $entity-type=>map:get("namespacePrefix")
        let $prefix-value :=
            if ($namespace-prefix)
            then $namespace-prefix || ":"
            else ""
        let $nsdecl :=
            if ($namespace-prefix)
            then element { "X" } { namespace { $namespace-prefix } { $entity-type=>map:get("namespace") } }
            else element { "X" } { }
        let $qname := fn:resolve-QName($prefix-value || $entity-type-name, $nsdecl)
        return
            element { $qname } {
                let $properties := $entity-type=>map:get("properties")
                for $property in map:keys($properties)
                return
                    esi:create-test-value($model, $entity-type-name, $property, map:get($properties, $property), $depth, "none", $nsdecl)
            }
    else ()
};


declare function esi:model-get-test-instances(
    $model as map:map
) as element()*
{
    let $entity-type-names := $model=>map:get("definitions")=>map:keys()
    for $entity-type-name in $entity-type-names
    return esi:create-test-instance($model, $entity-type-name, 0)
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
    $model as map:map
) as document-node()
{
    let $entity-type-names := $model=>map:get("definitions")=>map:keys()
    let $path-range-indexes := json:array()
    let $element-range-indexes := json:array()
    let $word-lexicons := json:array()
    let $path-namespaces := json:object()
    let $_ :=
        for $entity-type-name in $entity-type-names
        let $entity-type := $model=>map:get("definitions")=>map:get($entity-type-name)
        return
        (
        let $range-index-properties := (map:get($entity-type, "rangeIndex"), map:get($entity-type, "pathRangeIndex"))
        for $range-index-property in json:array-values($range-index-properties)
        let $property := $entity-type=>map:get("properties")=>map:get($range-index-property)
        let $specified-datatype := esi:resolve-datatype($model, $entity-type-name, $range-index-property)

        let $datatype := esi:indexable-datatype($specified-datatype)
        let $collation := head( (map:get($property, "collation"), "http://marklogic.com/collation/en") )
        let $invalid-values := "reject"
        let $namespace-prefix := $entity-type=>map:get("namespacePrefix")
        let $namespace-uri := $entity-type=>map:get("namespace")
        let $namespace-prefix-value :=
            if ($namespace-uri)
            then
                (
                map:put($path-namespaces,
                        $namespace-prefix,
                        json:object()
                        =>map:with("prefix",$namespace-prefix)
                        =>map:with("namespace-uri", $namespace-uri)),
                $namespace-prefix || ":"
                )
            else ""
        let $ri-map := json:object()
            =>map:with("collation", $collation)
            =>map:with("invalid-values", $invalid-values)
            =>map:with("path-expression", "//es:instance/" || $namespace-prefix-value || $entity-type-name || "/" || $namespace-prefix-value || $range-index-property)
            =>map:with("range-value-positions", false())
            =>map:with("scalar-type", $datatype)
        return json:array-push($path-range-indexes, $ri-map)
        ,
        let $element-range-index-properties := (map:get($entity-type, "elementRangeIndex"))
        for $element-range-index-property in json:array-values($element-range-index-properties)
        let $property := $entity-type=>map:get("properties")=>map:get($element-range-index-property)
        let $specified-datatype := esi:resolve-datatype($model, $entity-type-name, $element-range-index-property)

        let $datatype := esi:indexable-datatype($specified-datatype)
        let $collation := head( (map:get($property, "collation"), "http://marklogic.com/collation/en") )
        let $invalid-values := "reject"
        let $ri-map := json:object()
            =>map:with("collation", $collation)
            =>map:with("invalid-values", $invalid-values)
            =>map:with("localname",  $element-range-index-property)
            =>map:with("namespace-uri",  $entity-type=>map:get("namespace"))
            =>map:with("range-value-positions", false())
            =>map:with("scalar-type", $datatype)
        return json:array-push($element-range-indexes, $ri-map)
        ,

        let $word-lexicon-properties := $entity-type=>map:get("wordLexicon")
        for $word-lexicon-property in json:array-values($word-lexicon-properties)
        let $property := $entity-type=>map:get("properties")=>map:get($word-lexicon-property)
        let $collation := head( (map:get($property, "collation"), "http://marklogic.com/collation/en") )
        let $namespace-uri := head( ($entity-type=>map:get("namespace"), "") )
        let $wl-map := json:object()
            =>map:with("collation", $collation)
            =>map:with("localname", $word-lexicon-property)
            =>map:with("namespace-uri", $namespace-uri)
        return json:array-push($word-lexicons, $wl-map)
        )
    let $pn := json:object()
        =>map:with("prefix", "es")
        =>map:with("namespace-uri", "http://marklogic.com/entity-services")
    let $_ := map:put($path-namespaces, "es", $pn)
    let $values := function($map) { json:to-array( for $k in $map=>map:keys() return map:get($map, $k)) }
    let $database-properties :=
        json:object()
        =>map:with("database-name", "%%DATABASE%%")
        =>map:with("schema-database", "%%SCHEMAS_DATABASE%%")
        =>map:with("path-namespace", $values($path-namespaces))
        =>esi:with-if-exists("element-word-lexicon", $word-lexicons)
        =>esi:with-if-exists("range-path-index", $path-range-indexes)
        =>esi:with-if-exists("range-element-index", $element-range-indexes)
        =>map:with("triple-index", true())
        =>map:with("collection-lexicon", true())
    return xdmp:to-json($database-properties)
};





(: used to switch on datatype to provide the right XSD element for
 : (scalar) arrays, IRIs and scalars
 :)
declare private function esi:element-for-datatype(
    $property-name as xs:string,
    $datatype as xs:string
) as element(xs:element)
{
    if ($datatype eq "iri")
    then <xs:element name="{ $property-name }" type="sem:{ $datatype }"/>
    else <xs:element name="{ $property-name }" type="xs:{ $datatype }"/>
};


declare private function esi:array-element-for-datatype(
    $reference-declarations as map:map,
    $property-name as xs:string,
    $datatype as xs:string
)
{
    (map:put($reference-declarations, $property-name || "ARRAY",
        (
        <xs:complexType name="{ $property-name }ArrayType">
            <xs:simpleContent>
                <xs:extension base="{ $property-name }SimpleType">
                    <xs:attribute name="datatype" />
                </xs:extension>
            </xs:simpleContent>
        </xs:complexType>,
        <xs:simpleType name="{ $property-name }SimpleType">
            {
            if ($datatype eq "iri")
            then <xs:restriction base="sem:iri" />
            else <xs:restriction base="xs:{ $datatype }" />
            }
        </xs:simpleType>
        )),
    <xs:element name="{ $property-name }" type="{ $property-name }ArrayType"/>
    )
};


declare private function esi:element-for-reference(
    $reference-declarations as map:map,
    $imports-accumulator as map:map,
    $model as map:map,
    $property-name as xs:string,
    $ref-value as xs:string
) as element(xs:element)
{
    let $ref-name := functx:substring-after-last($ref-value, "/")
    return
    if (contains($ref-value, "#/definitions/"))
    then
        let $namespace := $model=>map:get("definitions")=>map:get($ref-name)=>map:get("namespace")
        let $version := $model=>map:get("info")=>map:get("version")
        let $namespace-prefix := $model=>map:get("definitions")=>map:get($ref-name)=>map:get("namespacePrefix")
        let $prefix-value :=
            if ($namespace-prefix)
            then $namespace-prefix || ":"
            else ""
        let $nsdecl :=
            if ($namespace-prefix)
            then namespace { $namespace-prefix } { $namespace }
            else ()
        return
        (map:put($reference-declarations, $ref-name || "CONTAINER",
            <xs:complexType name="{ $ref-name }ContainerType">
                <xs:sequence>
                    <xs:element ref="{ $prefix-value }{ $ref-name }" >
                    {$nsdecl}
                    </xs:element>
                </xs:sequence>
                <xs:attribute name="datatype" />
            </xs:complexType>),
         map:put($imports-accumulator,
            fn:head( ($namespace, "") ),
            if ($namespace) then
            <xs:import namespace="{$namespace}" schemaLocation="{$ref-name}-{$version}.xsd"/>
            else ()),
         <xs:element name="{ $property-name }" type="{ $ref-name }ContainerType"/>)
    else
        (map:put($reference-declarations, $ref-name || "REFERENCE",
             <xs:complexType name="{ $ref-name }ReferenceType">
                <xs:sequence>
                    <xs:element name="{ $ref-name }" type="xs:anyURI" />
                </xs:sequence>
                <xs:attribute name="datatype" />
             </xs:complexType>),
         <xs:element name="{ $property-name }" type="{ $ref-name }ReferenceType"/>)
};



declare function esi:schema-generate(
    $model as map:map
) as element()*
{
    let $entity-type-names := $model=>map:get("definitions")=>map:keys()
    let $seen-keys := map:map()
    let $reference-declarations := map:map()
    let $element-declarations := map:map()
    let $entity-type-declarations := map:map()
    let $schemas := json:array()
    let $imports := map:map()
    (: construct all the element declarations :)
    let $_ :=
        for $entity-type-name in $entity-type-names
        let $properties-accumulator := json:array()
        let $reference-accumulator := map:map()
        let $types-accumulator := json:array()
        let $imports-accumulator := map:map()
        let $entity-type := $model=>map:get("definitions")=>map:get($entity-type-name)
        let $namespace := $entity-type=>map:get("namespace")
        let $properties := map:get($entity-type, "properties")
        let $primary-key-name := map:get($entity-type, "primaryKey")
        let $required-properties := ( json:array-values(map:get($entity-type, "required")), $primary-key-name)
        let $_accumulate :=
                (
                for $property-name in map:keys($properties)
                let $property := map:get($properties, $property-name)
                return
                json:array-push($properties-accumulator,
                    esi:wrap-duplicates($seen-keys, "{" || $namespace || "}" || $property-name,
                        if (map:contains($property, "$ref"))
                        then
                            esi:element-for-reference(
                                $reference-accumulator,
                                $imports-accumulator,
                                $model,
                                $property-name,
                                map:get($property, "$ref"))
                        else if (map:contains($property, "datatype"))
                        then
                            let $datatype := map:get($property, "datatype")
                            let $items-map := map:get($property, "items")
                            return
                                if ($datatype eq "array")
                                then
                                    if (map:contains($items-map, "$ref"))
                                    then esi:element-for-reference(
                                        $reference-accumulator,
                                        $imports-accumulator,
                                        $model,
                                        $property-name,
                                        map:get($items-map, "$ref"))
                                    else
                                        esi:array-element-for-datatype(
                                            $reference-accumulator,
                                            $property-name,
                                            map:get($items-map, "datatype"))
                                else esi:element-for-datatype($property-name, $datatype)
                        else (),
                        "schema")),
                json:array-push($types-accumulator,
                    <xs:complexType name="{ $entity-type-name }Type" mixed="true">
                        <xs:sequence minOccurs="0">
                        {
                            (: construct xs:element element for each property :)
                            for $property-name in map:keys($properties)
                            let $property := map:get($properties, $property-name)
                            let $datatype := map:get($property, "datatype")
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
                    </xs:complexType>),
                json:array-push($types-accumulator,
                    <xs:element name="{ $entity-type-name }" type="{ $entity-type-name }Type"/>
                    )
                )
        return (
            map:put($element-declarations, $entity-type-name, $properties-accumulator),
            map:put($reference-declarations, $entity-type-name, $reference-accumulator),
            map:put($entity-type-declarations, $entity-type-name, $types-accumulator),
            map:put($imports, head( ($namespace, "") ), (map:keys($imports-accumulator) ! map:get($imports-accumulator, .)))
        )
    let $names-by-namespace := map:map()
    let $_ :=
        for $entity-type-name in $entity-type-names
        let $entity-type := $model=>map:get("definitions")=>map:get($entity-type-name)
        let $namespace := $entity-type=>map:get("namespace")
        let $namespace-prefix := $entity-type=>map:get("namespacePrefix")
        let $extend := function($m, $ns, $et) {
            if (map:contains($m, $ns))
            then $m=>map:get($ns)=>json:array-push($et)
            else map:put($m, $ns, json:to-array( $et ))
        }
        return
            if (empty($namespace))
            then $extend($names-by-namespace, "", $entity-type-name)
            else $extend($names-by-namespace, $namespace, $entity-type-name)
    let $_ :=
        for $namespace in map:keys($names-by-namespace)
        let $target-attribute :=
            if ($namespace ne "")
            then attribute { "targetNamespace" } { $namespace }
            else ()
        return
            json:array-push($schemas,
            <xs:schema
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:sem="http://marklogic.com/semantics"
                elementFormDefault="qualified"
                xmlns:es="http://marklogic.com/entity-services">
            {$target-attribute}
            {$imports=>map:get($namespace)}
            {
                functx:distinct-deep(
                for $entity-type-name in json:array-values($names-by-namespace=>map:get($namespace))
                let $entity-type := $model=>map:get("definitions")=>map:get($entity-type-name)
                let $namespace := $entity-type=>map:get("namespace")
                let $namespace-prefix := $entity-type=>map:get("namespacePrefix")
                return
                (
                    json:array-values($element-declarations=>map:get($entity-type-name)),
                    let $m := $reference-declarations=>map:get($entity-type-name)
                    let $keys := $m=>map:keys()
                    for $k in $keys
                    return map:get($m, $k),
                    json:array-values($entity-type-declarations=>map:get($entity-type-name))
                )
                )
            }
            </xs:schema>)
    return json:array-values($schemas)
};


declare function esi:resolve-datatype(
    $model as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string
) as xs:string
{
    let $property := $model=>map:get("definitions")
        =>map:get($entity-type-name)
        =>map:get("properties")
        =>map:get($property-name)
    return
    if (map:contains($property, "datatype"))
    then
        if (map:get($property, "datatype") eq "array")
        then
            if (map:contains(map:get($property, "items"), "datatype"))
            then $property=>map:get("items")=>map:get("datatype")
            else esi:ref-datatype($model, $entity-type-name, $property-name)
        else map:get($property, "datatype")
    else esi:ref-datatype($model, $entity-type-name, $property-name)
};


(:
 : Resolves a reference and returns its datatype
 : If the reference is external, return 'string'
 :)
declare private function esi:ref-datatype(
    $model as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string
) as xs:string
{
    let $ref-type := esi:ref-type($model, $entity-type-name, $property-name)
    return
        if (esi:is-local-reference($model, $entity-type-name, $property-name))
        then
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
        else "string"
};



declare private function esi:ref-prefixed-name(
    $model as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string
) as xs:string
{
    let $ref-type := esi:ref-type( $model, $entity-type-name, $property-name )
    let $ref-name := esi:ref-type-name($model, $entity-type-name, $property-name)
    let $namespace-prefix := $ref-type=>map:get("namespacePrefix")
    let $is-local-ref := esi:is-local-reference($model, $entity-type-name, $property-name)
    return
        if ($namespace-prefix and $is-local-ref)
        then $namespace-prefix || ":" || $ref-name
        else $ref-name
};


declare private function esi:is-local-reference(
    $model as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string
)
{
    let $property := $model
        =>map:get("definitions")
        =>map:get($entity-type-name)
        =>map:get("properties")
        =>map:get($property-name)
    let $ref-target := head( ($property=>map:get("$ref"),
                              $property=>map:get("items")=>map:get("$ref") ) )
    return contains($ref-target, "#/definitions")
};

(:
 : Given a model, an entity type name and a reference property,
 : return a reference's type name
 :)
declare function esi:ref-type-name(
    $model as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string
) as xs:string
{
    let $property := $model
        =>map:get("definitions")
        =>map:get($entity-type-name)
        =>map:get("properties")
        =>map:get($property-name)
    let $ref-target := head( ($property=>map:get("$ref"),
                  $property=>map:get("items")=>map:get("$ref") ) )
    return functx:substring-after-last($ref-target, "/")
};


declare private function esi:ref-type(
    $model as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string
) as map:map?
{
    $model
        =>map:get("definitions")
        =>map:get( esi:ref-type-name($model, $entity-type-name, $property-name) )
};

(: returns empty-sequence if no primary key :)
declare private function esi:ref-primary-key-name(
    $model as map:map,
    $entity-type-name as xs:string,
    $property-name as xs:string
) as xs:string?
{
    let $ref-type-name := esi:ref-type-name($model, $entity-type-name, $property-name)
    let $ref-target := $model=>map:get("definitions")=>map:get($ref-type-name)
    return
        if (map:contains($ref-target, "primaryKey"))
        then map:get($ref-target, "primaryKey")
        else ()
};

declare function esi:extraction-template-generate(
    $model as map:map
) as element(tde:template)
{
    let $schema-name := $model=>map:get("info")=>map:get("title")
    let $entity-type-names := $model=>map:get("definitions")=>map:keys()
    let $scalar-rows := map:map()
    let $secure-tde-name := fn:replace(?, "-", "_")
    let $path-namespaces := map:map()
    let $_ :=
        for $entity-type-name in $entity-type-names
        let $entity-type := $model=>map:get("definitions")=>map:get($entity-type-name)
        let $primary-key-name := map:get($entity-type, "primaryKey")
        let $required-properties := ( json:array-values(map:get($entity-type, "required")), $primary-key-name)
        let $properties := map:get($entity-type, "properties")
        let $primary-key-type := map:get( map:get($properties, $primary-key-name), "datatype" )
        let $namespace-prefix := $entity-type=>map:get("namespacePrefix")
        let $namespace-uri := $entity-type=>map:get("namespace")
        let $prefix-value :=
            (
            map:put($path-namespaces,
                    $namespace-prefix,
                    <tde:path-namespace>
                        <tde:prefix>{$namespace-prefix}</tde:prefix>
                        <tde:namespace-uri>{$namespace-uri}</tde:namespace-uri>
                    </tde:path-namespace>),
            if ($namespace-prefix)
            then $namespace-prefix || ":"
            else ""
            )
        return
        map:put($scalar-rows, $entity-type-name,
            <tde:rows>
                <tde:row>
                    <tde:schema-name>{ $schema-name=>$secure-tde-name() }</tde:schema-name>
                    <tde:view-name>{ $entity-type-name=>$secure-tde-name() }</tde:view-name>
                    <tde:view-layout>sparse</tde:view-layout>
                    <tde:columns>
                    {
                    for $property-name in map:keys($properties)
                    let $property-definition := map:get($properties, $property-name)
                    let $items-map := map:get($property-definition, "items")
                    let $datatype :=
                        if (map:get($property-definition, "datatype") eq "iri")
                        then "IRI"
                        else map:get($property-definition, "datatype")
                    let $is-nullable :=
                        if ($property-name = $required-properties)
                        then ()
                        else <tde:nullable>true</tde:nullable>
                    return
                        (: if the column is an array, skip it in scalar row :)
                        if (exists($items-map)) then ()
                        else
                            if ( map:contains($property-definition, "$ref") )
                            then
                            <tde:column>
                                <tde:name>{ $property-name=>$secure-tde-name() }</tde:name>
                                <tde:scalar-type>{ esi:ref-datatype($model, $entity-type-name, $property-name) } </tde:scalar-type>
                                <tde:val>{ $prefix-value }{ $property-name }/{ esi:ref-prefixed-name($model, $entity-type-name, $property-name) }</tde:val>
                                {$is-nullable}
                            </tde:column>
                            else
                            <tde:column>
                                <tde:name>{ $property-name=>$secure-tde-name() }</tde:name>
                                <tde:scalar-type>{ $datatype }</tde:scalar-type>
                                <tde:val>{ $prefix-value }{$property-name }</tde:val>
                                {$is-nullable}
                            </tde:column>
                    }
                    </tde:columns>
                </tde:row>
            </tde:rows>)
    let $array-rows := map:map()
    let $triples-templates := map:map()
    let $_ :=
        (: this is a long loop.  It creates row-based templates for each
         :  entity-type name, as well as two triples that tie an entity
         :  instance document to its type and document IRI :)
        for $entity-type-name in $entity-type-names
        let $entity-type := $model=>map:get("definitions")=>map:get($entity-type-name)
        let $primary-key-name := map:get($entity-type, "primaryKey")
        let $namespace-prefix := $entity-type=>map:get("namespacePrefix")
        let $prefix-value :=
            if ($namespace-prefix)
            then $namespace-prefix || ":"
            else ""
        let $required-properties := ($primary-key-name, json:array-values(map:get($entity-type, "required")))
        let $properties := map:get($entity-type, "properties")
        let $primary-key-type := map:get( map:get($properties, $primary-key-name), "datatype" )
        let $column-map := map:map()
        let $_ :=
            for $property-name in map:keys($properties)
            let $property-definition := map:get($properties, $property-name)
            let $items-map := map:get($property-definition, "items")
            let $is-ref := map:contains($items-map, "$ref")
            let $is-local-ref := map:contains($items-map, "$ref") and starts-with( map:get($items-map, "$ref"), "#/definitions/")
            let $is-external-ref := $is-ref and not($is-local-ref)
            let $reference-value :=
                      $property-definition=>map:get("items")=>map:get("$ref")
            let $ref-name := functx:substring-after-last($reference-value, "/")
            let $is-nullable :=
                if ($property-name = $required-properties)
                then ()
                else <tde:nullable>true</tde:nullable>
            let $items-datatype :=
                if (map:get($items-map, "datatype") eq "iri")
                then "string"
                else map:get($items-map, "datatype")
            let $ref-primary-key := esi:ref-primary-key-name($model, $entity-type-name, $property-name)
            let $ref-type-name := esi:ref-type-name($model, $entity-type-name, $property-name)
            where exists($items-map)
            return
            map:put($column-map, $property-name,
                <tde:template>
                    <tde:context>./{ $prefix-value }{ $property-name }</tde:context>
                    <tde:rows>
                      <tde:row>
                        <tde:schema-name>{ $schema-name=>$secure-tde-name() }</tde:schema-name>
                        <tde:view-name>{ $entity-type-name=>$secure-tde-name() }_{ $property-name=>$secure-tde-name() }</tde:view-name>
                        <tde:view-layout>sparse</tde:view-layout>
                        <tde:columns>
                            { if (empty($primary-key-name))
                              then comment { "Warning, no primary key in enclosing type",
                                             $entity-type-name }
                              else
                                <tde:column>
                                    { comment { "This column joins to property",
                                                $primary-key-name, "of",
                                                $entity-type-name } }
                                    <tde:name>{ $primary-key-name=>$secure-tde-name() }</tde:name>
                                    <tde:scalar-type>{ $primary-key-type }</tde:scalar-type>
                                    <tde:val>../{ $prefix-value }{ $primary-key-name }</tde:val>
                                </tde:column>,
                            if ($is-local-ref and empty($ref-primary-key))
                            then
                                (
                                map:get($scalar-rows, $ref-type-name)/tde:row[tde:view-name eq $ref-type-name ]/tde:columns/tde:column,
                                map:put($scalar-rows, $ref-type-name,
                                    comment { "No extraction template emitted for" ||
                                               $ref-type-name ||
                                               "as it was incorporated into another view. "
                                            }
                                        )
                                )
                            else if ($is-local-ref)
                            then
                                <tde:column>
                                    { comment { "This column joins to primary key of",
                                                $ref-type-name } }
                                    <tde:name>{ $property-name=>$secure-tde-name() || "_" || $ref-primary-key=>$secure-tde-name() }</tde:name>
                                    <tde:scalar-type>{ esi:ref-datatype($model, $entity-type-name, $property-name) }</tde:scalar-type>
                                    <tde:val>{ $prefix-value }{ $ref-name }</tde:val>
                                </tde:column>
                            else
                            if ($is-external-ref)
                            then
                                <tde:column>
                                    { comment { "This column joins to primary key of an external reference" } }
                                    <tde:name>{ $property-name=>$secure-tde-name() }</tde:name>
                                    <tde:scalar-type>string</tde:scalar-type>
                                    <tde:val>{ $ref-name }</tde:val>
                                    {$is-nullable}
                                </tde:column>
                            else
                                <tde:column>
                                    { comment { "This column holds array values from property",
                                                $primary-key-name, "of",
                                                $entity-type-name } }
                                    <tde:name>{ $property-name=>$secure-tde-name() }</tde:name>
                                    <tde:scalar-type>{ $items-datatype }</tde:scalar-type>
                                    <tde:val>.</tde:val>
                                    {$is-nullable}
                                </tde:column>
                            }
                        </tde:columns>
                      </tde:row>
                    </tde:rows>
                </tde:template>
            )
        return
        (
        if (exists($primary-key-name))
        then
        map:put($triples-templates, $entity-type-name,
            <tde:template>
                <tde:context>./{ $prefix-value }{ $entity-type-name }</tde:context>
                <tde:vars>
                    {
                        if ($primary-key-type eq "string")
                        then
                        <tde:var><tde:name>subject-iri</tde:name><tde:val>sem:iri(concat("{ esi:model-graph-prefix($model) }/{ $entity-type-name }/", fn:encode-for-uri(./{ $prefix-value }{ $primary-key-name })))</tde:val></tde:var>
                        else
                        <tde:var><tde:name>subject-iri</tde:name><tde:val>sem:iri(concat("{ esi:model-graph-prefix($model) }/{ $entity-type-name }/", fn:encode-for-uri(xs:string(./{ $prefix-value }{ $primary-key-name }))))</tde:val></tde:var>
                    }
                </tde:vars>
                <tde:triples>
                    <tde:triple>
                        <tde:subject><tde:val>$subject-iri</tde:val></tde:subject>
                        <tde:predicate><tde:val>$RDF_TYPE</tde:val></tde:predicate>
                        <tde:object><tde:val>sem:iri("{ esi:model-graph-prefix($model) }/{ $entity-type-name }")</tde:val></tde:object>
                    </tde:triple>
                    <tde:triple>
                        <tde:subject><tde:val>$subject-iri</tde:val></tde:subject>
                        <tde:predicate><tde:val>sem:iri("http://www.w3.org/2000/01/rdf-schema#isDefinedBy")</tde:val></tde:predicate>
                        <tde:object><tde:val>fn:base-uri(.)</tde:val></tde:object>
                    </tde:triple>
                </tde:triples>
            </tde:template>)
        else (),
        if (exists(map:keys($column-map)))
        then map:put($array-rows, $entity-type-name, $column-map)
        else ()
        )


    let $entity-type-templates :=
        for $entity-type-name in map:keys($scalar-rows)
        let $entity-type := $model=>map:get("definitions")=>map:get($entity-type-name) 
        let $namespace-prefix := $entity-type=>map:get("namespacePrefix")
        let $namespace-uri := $entity-type=>map:get("namespace")
        let $prefix-value :=
            if ($namespace-uri)
            then
                $namespace-prefix || ":"
            else ""
        return
        if (empty ( ( json:array-values(
                        $entity-type=>map:get("required")),
                        $entity-type=>map:get("primaryKey"))
                    ))
        then comment { "The standalone template for " || $entity-type-name ||
                       " cannot be generated.  Each template row requires " ||
                       "a primary key or at least one required property." }
        else

        (
        map:get($triples-templates, $entity-type-name),
        <tde:template>
            <tde:context>./{ $prefix-value }{ $entity-type-name }</tde:context>
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
        )
    return
    <tde:template xmlns:tde="http://marklogic.com/xdmp/tde">
        <tde:description>
Extraction Template Generated from Entity Type Document
graph uri: {esi:model-graph-iri($model)}
        </tde:description>
        <!-- The following line matches JSON and XML instances, but may be slower to index documents. -->
        <tde:context>//*:instance[*:info/*:version = "{$model=>map:get("info")=>map:get("version")}"]</tde:context>
        <!-- Replace the above with the following line to match XML instances only.  This may speed up indexing
        <tde:context>//es:instance[es:info/es:version = "{$model=>map:get("info")=>map:get("version")}"]</tde:context>
        -->
        <!-- Replace the above with the following line to match JSON instances only.  This may speed up indexing
        <tde:context>//instance[info/version = "{$model=>map:get("info")=>map:get("version")}"]</tde:context>
        -->
        <tde:vars>
            <tde:var><tde:name>RDF</tde:name><tde:val>"http://www.w3.org/1999/02/22-rdf-syntax-ns#"</tde:val></tde:var>
            <tde:var><tde:name>RDF_TYPE</tde:name><tde:val>sem:iri(concat($RDF, "type"))</tde:val></tde:var>
        </tde:vars>
        <tde:path-namespaces>
            <tde:path-namespace>
                <tde:prefix>es</tde:prefix>
                <tde:namespace-uri>http://marklogic.com/entity-services</tde:namespace-uri>
            </tde:path-namespace>
            { ($path-namespaces=>map:keys()) ! ($path-namespaces=>map:get(.)) }
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


(: Function made public for us by MarkLogic DataHub framework,
 : to expose internal workings of envelopes  :)
declare function esi:wrap-duplicates(
    $duplicate-map as map:map,
    $property-name as xs:string,
    $item as element(),
    $section as xs:string
) as item()
{
    let $comment :=
        if ($section eq "options")
        then "The name of this constraint is a duplicate in the generated XML. It is within a comment so that the XML may be valid, but you may need to edit for your use case.&#10;"
        else if ($section eq "schema")
        then "XSD schemas prohibit duplicate element names. This element is commented out because it conflicts with another of the same name.&#10;"
        else "This item is a duplicate and is commented out so as to create a valid artifact.&#10;"
    return
    if (map:contains($duplicate-map, $property-name))
    then
        comment { $comment,
            xdmp:quote($item),
            "&#10;"
        }
    else (
        map:put($duplicate-map, $property-name, true()),
        $item)
};


(:
 : Generates a configuration node for use with the MarkLogic Search API.
 : The resulting node can be used to configure a search application over
 : a corpus of entity types.
 :)
declare function esi:search-options-generate(
    $model as map:map
)
{
    let $info := map:get($model, "info")
    let $schema-name := map:get($info, "title")
    let $entity-type-names := $model=>map:get("definitions")=>map:keys()
    let $seen-keys := map:map()
    let $all-constraints := json:array()
    let $all-tuples-definitions := json:array()
    let $prefixed-type-names := json:array()
    let $nsdecls := json:array()
    let $_ :=
        for $entity-type-name in $entity-type-names
        let $entity-type := $model=>map:get("definitions")=>map:get($entity-type-name)
        let $namespace-prefix := $entity-type=>map:get("namespacePrefix")
        let $namespace-uri := $entity-type=>map:get("namespace")
        let $prefix-value :=
            if ($namespace-uri)
            then
                $namespace-prefix || ":"
            else ""
        let $nsdecl :=
            if ($namespace-uri)
            then
                (
                json:array-push($nsdecls, namespace { $namespace-prefix } { $namespace-uri }),
                namespace { $namespace-prefix } { $namespace-uri }
                )
            else ()
        let $_ := json:array-push($prefixed-type-names, ($prefix-value || $entity-type-name))
        let $primary-key-name := map:get($entity-type, "primaryKey")
        let $properties := map:get($entity-type, "properties")
        let $tuples-range-definitions := json:array()
        let $_pk-constraint :=
            if (exists($primary-key-name))
            then
            json:array-push($all-constraints, esi:wrap-duplicates($seen-keys, $primary-key-name,
                <search:constraint name="{ $primary-key-name } ">
                    <search:value>
                        <search:element ns="" name="{ $primary-key-name }"/>
                    </search:value>
                </search:constraint>,
                "options"))
            else ()
        let $_path-range-constraints :=
            for $property-name in json:array-values( (map:get($entity-type, "rangeIndex"), map:get($entity-type, "pathRangeIndex") ) )
            let $specified-datatype := esi:resolve-datatype($model,$entity-type-name,$property-name)
            let $property := map:get($properties, $property-name)
            let $datatype := esi:indexable-datatype($specified-datatype)
            let $collation := if ($datatype eq "string")
                then attribute
                    collation {
                        head( (map:get($property, "collation"), "http://marklogic.com/collation/en") )
                    }
                else ()
            let $range-definition :=
                <search:range type="xs:{ $datatype }" facet="true">
                    { $collation }
                    <search:path-index
                        xmlns:es="http://marklogic.com/entity-services">{
                        $nsdecl
                    }//es:instance/{$prefix-value}{$entity-type-name}/{$prefix-value}{$property-name}</search:path-index>
                </search:range>
            let $constraint-template :=
                <search:constraint name="{ $property-name } ">
                    {$range-definition}
                </search:constraint>
            (: the collecting array will be added once after accumulation :)
            let $_ := json:array-push($tuples-range-definitions, $range-definition)
            return
                json:array-push($all-constraints, esi:wrap-duplicates($seen-keys, $property-name, $constraint-template, "options"))
        let $_element-range-constraints :=
            for $property-name in json:array-values( map:get($entity-type, "elementRangeIndex"))
            let $specified-datatype := esi:resolve-datatype($model,$entity-type-name,$property-name)
            let $property := map:get($properties, $property-name)
            let $datatype := esi:indexable-datatype($specified-datatype)
            let $collation := if ($datatype eq "string")
                then attribute
                    collation {
                        head( (map:get($property, "collation"), "http://marklogic.com/collation/en") )
                    }
                else ()
            let $element-range-attributes :=
                if ($namespace-uri)
                then (
                    attribute { "ns" }  { $namespace-uri },
                    attribute { "name" } { $property-name }
                    )
                else (
                    attribute { "ns" }  { "" },
                    attribute { "name" } { $property-name }
                    )
            let $range-definition :=
                <search:range type="xs:{ $datatype }" facet="true">
                    { $collation }
                    <search:element xmlns:es="http://marklogic.com/entity-services">{
                        $element-range-attributes
                    }</search:element>
                </search:range>
            let $constraint-template :=
                <search:constraint name="{ $property-name } ">
                    {$range-definition}
                </search:constraint>
            (: the collecting array will be added once after accumulation :)
            let $_ := json:array-push($tuples-range-definitions, $range-definition)
            return
                json:array-push($all-constraints, esi:wrap-duplicates($seen-keys, $property-name, $constraint-template, "options"))
        let $_ :=
            if (json:array-size($tuples-range-definitions) gt 1)
            then
                json:array-push($all-tuples-definitions,
                    <search:tuples name="{ $entity-type-name }">
                        {json:array-values($tuples-range-definitions)}
                    </search:tuples>)
            else if (json:array-size($tuples-range-definitions) eq 1)
            then
                json:array-push($all-tuples-definitions,
                    <search:values name="{ $entity-type-name }">
                        {json:array-values($tuples-range-definitions)}
                    </search:values>)
            else ()
        let $_word-constraints :=
            for $property-name in json:array-values(map:get($entity-type, "wordLexicon"))
            return
            json:array-push($all-constraints, esi:wrap-duplicates($seen-keys, $property-name,
                <search:constraint name="{ $property-name } ">
                    <search:word>
                        <search:element ns="" name="{ $property-name }"/>
                    </search:word>
                </search:constraint>, "options"))
        return ()
    let $types-expr := string-join( json:array-values($prefixed-type-names), "|" )
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
        json:array-values($all-constraints),
        json:array-values($all-tuples-definitions),
        comment {
            "Uncomment to return no results for a blank search, rather than the default of all results&#10;",           xdmp:quote(
        <search:term>
            <search:empty apply="no-results"/>
        </search:term>),
            "&#10;"
        },
        <search:values name="uris">
            <search:uri/>
        </search:values>,
        comment { "Change to 'filtered' to exclude false-positives in certain searches" },
        <search:search-option>unfiltered</search:search-option>,
        comment { "Modify document extraction to change results returned" },
        <search:extract-document-data selected="include">
            <search:extract-path xmlns:es="http://marklogic.com/entity-services">{
                for $nsdecl in json:array-values($nsdecls) return $nsdecl
            }//es:instance/({ $types-expr })</search:extract-path>
        </search:extract-document-data>,

        comment { "Change or remove this additional-query to broaden search beyond entity instance documents" },
        <search:additional-query>
            <cts:or-query xmlns:cts="http://marklogic.com/cts">
                <cts:json-property-scope-query>
                    <cts:property>instance</cts:property>
                    <cts:true-query/>
                </cts:json-property-scope-query>
                <cts:element-query>
                    <cts:element xmlns:es="http://marklogic.com/entity-services">es:instance</cts:element>
                    <cts:true-query/>
                </cts:element-query>
            </cts:or-query>
        </search:additional-query>,
        comment { "To return facets, change this option to 'true' and edit constraints" },
        <search:return-facets>false</search:return-facets>,
        comment { "To return snippets, comment out or remove this option" },
        <search:transform-results apply="empty-snippet" />
        }
    </search:options>
};



(: resolves the default URI from a model's info section :)
declare function esi:resolve-base-uri(
    $info as map:map
) as xs:string
{
    let $base-uri := fn:head((map:get($info, "baseUri"), $esi:DEFAULT_BASE_URI))
    return
        if (fn:matches($base-uri, "[#/]$"))
        then $base-uri
        else concat($base-uri, "#")
};

declare private function esi:resolve-base-prefix(
    $info as map:map
) as xs:string
{
    replace(esi:resolve-base-uri($info), "#", "/")
};

