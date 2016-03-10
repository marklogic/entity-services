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


declare variable $esi:json-extraction-template := 
<template xmlns:es="http://marklogic.com/entity-services"
     xmlns="http://marklogic.com/xdmp/tde">
    <description>Extracts triples from JSON entity type documents which project an RDF model of entity types</description>
    <collections>
        <collection>http://marklogic.com/entity-services/entity-types</collection>
    </collections>
    <context>/definitions/*</context>
    <vars>
        <!-- constants -->
        <var><name>ESN</name><val>"http://marklogic.com/entity-services#"</val></var>
        <var><name>RDF</name><val>"http://www.w3.org/1999/02/22-rdf-syntax-ns#"</val></var>
        <var><name>RDF_TYPE</name><val>sem:iri(concat($RDF, "type"))</val></var>
        <var><name>PROP_VERSION</name><val>sem:iri(concat($ESN, 'version'))</val></var>
        <var><name>PROP_TITLE</name><val>sem:iri(concat($ESN, 'title'))</val></var>
        <var><name>PROP_DEFINITIONS</name><val>sem:iri(concat($ESN, 'definitions'))</val></var>
        <var><name>PROP_PRIMARYKEY</name><val>sem:iri(concat($ESN, 'primaryKey'))</val></var>
        <var><name>PROP_PROPERTY</name><val>sem:iri(concat($ESN, 'property'))</val></var>
        <var><name>PROP_DATATYPE</name><val>sem:iri(concat($ESN, 'datatype'))</val></var>
        <var><name>PROP_COLLATION</name><val>sem:iri(concat($ESN, 'collation'))</val></var>
        <var><name>PROP_ITEMS</name><val>sem:iri(concat($ESN, 'items'))</val></var>
        <var><name>PROP_REF</name><val>sem:iri(concat($ESN, 'ref'))</val></var>
        <var><name>XSD</name><val>"http://www.w3.org/2001/XMLSchema#"</val></var>
        <var><name>XSD_BASE64BINARY</name><val>sem:iri(concat($XSD, "base64Binary"))</val></var>
        <var><name>XSD_BOOLEAN</name><val>sem:iri(concat($XSD, "boolean"))</val></var>
        <var><name>XSD_BYTE</name><val>sem:iri(concat($XSD, "byte"))</val></var>
        <var><name>XSD_DATE</name><val>sem:iri(concat($XSD, "date"))</val></var>
        <var><name>XSD_DATETIME</name><val>sem:iri(concat($XSD, "dateTime"))</val></var>
        <var><name>XSD_DAYTIMEDURATION</name><val>sem:iri(concat($XSD, "dayTimeDuration"))</val></var>
        <var><name>XSD_DECIMAL</name><val>sem:iri(concat($XSD, "decimal"))</val></var>
        <var><name>XSD_DOUBLE</name><val>sem:iri(concat($XSD, "double"))</val></var>
        <var><name>XSD_DURATION</name><val>sem:iri(concat($XSD, "duration"))</val></var>
        <var><name>XSD_FLOAT</name><val>sem:iri(concat($XSD, "float"))</val></var>
        <var><name>XSD_INT</name><val>sem:iri(concat($XSD, "int"))</val></var>
        <var><name>XSD_INTEGER</name><val>sem:iri(concat($XSD, "integer"))</val></var>
        <var><name>XSD_LONG</name><val>sem:iri(concat($XSD, "long"))</val></var>
        <var><name>XSD_SHORT</name><val>sem:iri(concat($XSD, "short"))</val></var>
        <var><name>XSD_STRING</name><val>sem:iri(concat($XSD, "string"))</val></var>
        <var><name>XSD_TIME</name><val>sem:iri(concat($XSD, "time"))</val></var>
        <var><name>XSD_UNSIGNEDINT</name><val>sem:iri(concat($XSD, "unsignedInt"))</val></var>
        <var><name>XSD_UNSIGNEDLONG</name><val>sem:iri(concat($XSD, "unsignedLong"))</val></var>
        <var><name>XSD_UNSIGNEDSHORT</name><val>sem:iri(concat($XSD, "unsignedShort"))</val></var>
        <var><name>XSD_YEARMONTHDURATION</name><val>sem:iri(concat($XSD, "yearMonthDuration"))</val></var>
        <var><name>XSD_ANYSIMPLETYPE</name><val>sem:iri(concat($XSD, "anySimpleType"))</val></var>
        <var><name>XSD_ANYURI</name><val>sem:iri(concat($XSD, "anyURI"))</val></var>
        <var><name>SEM</name><val>"http://marklogic.com/semantics#"</val></var>
        <var><name>SEM_IRI</name><val>sem:iri(concat($SEM, "iri"))</val></var>
        <var><name>JSON</name><val>"http://marklogic.com/json#"</val></var>
        <var><name>JSON_ARRAY</name><val>sem:iri(concat($JSON, "array"))</val></var>
        <!-- metadata for entity type document iris -->
        <var>
            <name>baseUriCoalesce</name>
            <val>if (../../../info/baseUri) then ../../../info/baseUri else "http://example.org/"</val>
        </var>
        <var>
            <name>baseUriPrefix</name>
            <val>if (fn:matches($baseUriCoalesce, "[#/]$")) then $baseUriCoalesce else concat($baseUriCoalesce, "/")</val>
        </var>
        <var>
            <name>baseUri</name>
            <val>if (fn:matches($baseUriCoalesce, "[#/]$")) then $baseUriCoalesce else concat($baseUriCoalesce, "#")</val>
        </var>
        <var>
            <name>title</name>
            <val>xs:string(../../../info/title)</val>
        </var>
        <var>
            <name>version</name>
            <val>xs:string(../../../info/version)</val>
        </var>
        <var>
            <name>doc-subject-prefix</name>
            <val>sem:iri(concat($baseUriPrefix, $title, "-", $version, "/"))</val>
        </var>
        <var>
            <name>doc-subject-iri</name>
            <val>sem:iri(concat($baseUri, $title , "-", $version))</val>
        </var>
        <var><name>entityTypeName</name><val>fn:node-name(.)</val></var>
        <var>
            <name>et-subject-iri</name>
            <val>sem:iri(concat($doc-subject-prefix, $entityTypeName))</val>
        </var>
    </vars>
    <triples>
        <triple>
          <subject><val>$doc-subject-iri</val></subject>
          <predicate><val>$RDF_TYPE</val></predicate>
          <object><val>sem:iri(concat($ESN, "EntityServicesDoc"))</val></object>
        </triple>
        <triple>
          <subject><val>$doc-subject-iri</val></subject>
          <predicate><val>$PROP_TITLE</val></predicate>
          <object><val>$title</val></object>
        </triple>
        <triple>
          <subject><val>$doc-subject-iri</val></subject>
          <predicate><val>$PROP_VERSION</val></predicate>
          <object><val>$version</val></object>
        </triple>
        <triple>
          <subject><val>$et-subject-iri</val></subject>
          <predicate><val>$RDF_TYPE</val></predicate>
          <object><val>sem:iri(concat($ESN, "EntityType"))</val></object>
        </triple>
        <triple>
          <subject><val>$et-subject-iri</val></subject>
          <predicate><val>$PROP_TITLE</val></predicate>
          <object><val>xs:string($entityTypeName)</val></object>
        </triple>
        <triple>
          <subject><val>$et-subject-iri</val></subject>
          <predicate><val>$PROP_VERSION</val></predicate>
          <object><val>$version</val></object>
        </triple>
        <triple>
          <subject><val>$doc-subject-iri</val></subject>
          <predicate><val>$PROP_DEFINITIONS</val></predicate>
          <object><val>$et-subject-iri</val></object>
        </triple>
    </triples>
    <templates>
        <template>
            <context>./primaryKey</context>
            <triples>
                <triple>
                  <subject><val>$et-subject-iri</val></subject>
                  <predicate><val>$PROP_PRIMARYKEY</val></predicate>
                  <object><val>sem:iri(concat($et-subject-iri, "/", .))</val></object>
                </triple>
            </triples>
        </template>
        <template>
        <context>./properties/*</context>
        <vars>
        <var><name>propertyName</name><val>fn:node-name(.)</val></var>
        <var><name>datatype-value</name><val>
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
            <name>property-subject-iri</name>
            <val>sem:iri(concat($et-subject-iri, "/", $propertyName))</val>
        </var>
        <var><name>collation</name><val>xs:string(head((.//collation, "")))</val></var>
    </vars>
    <triples>
        <triple>
          <subject><val>$et-subject-iri</val></subject>
          <predicate><val>$PROP_PROPERTY</val></predicate>
          <object><val>$property-subject-iri</val></object>
        </triple>
        <triple>
          <subject><val>$property-subject-iri</val></subject>
          <predicate><val>$RDF_TYPE</val></predicate>
          <object><val>sem:iri(concat($ESN, "Property"))</val></object>
        </triple>
        <triple>
          <subject><val>$property-subject-iri</val></subject>
          <predicate><val>$PROP_TITLE</val></predicate>
          <object><val>xs:string($propertyName)</val></object>
        </triple>
    </triples>
    <templates>
        <template>
        <context>./node("$ref")</context>
        <vars>
            <var>
                <name>relationship-reference</name>
                <val>
                    if (fn:starts-with(., "#/definitions/")) 
                    then sem:iri(fn:replace(.,"#/definitions/",$doc-subject-prefix))
                    else sem:iri(.)
                    </val>
            </var>
        </vars>
        <triples>
            <triple>
              <subject><val>$property-subject-iri</val></subject>
              <predicate><val>$PROP_REF</val></predicate>
              <object><val>$relationship-reference</val></object>
            </triple>
        </triples>
        </template>
        <template>
        <context>./datatype</context>
        <triples>
            <triple>
              <subject><val>$property-subject-iri</val></subject>
              <predicate><val>$PROP_DATATYPE</val></predicate>
              <object><val>$datatype-value</val></object>
            </triple>
        </triples>
        </template>
        <template>
        <context>./collation</context>
        <triples>
            <triple>
              <subject><val>$property-subject-iri</val></subject>
              <predicate><val>$PROP_COLLATION</val></predicate>
              <object><val>$collation</val></object>
            </triple>
        </triples>
        </template>
        <template>
            <context>./items</context>
            <vars>
                <var>
                    <name>items-iri</name>
                    <val>sem:iri(concat($property-subject-iri, "/items"))</val>
                </var>
                <var><name>items-datatype</name><val>
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
            </vars>
            <triples>
                <triple>
                  <subject><val>$property-subject-iri</val></subject>
                  <predicate><val>$PROP_ITEMS</val></predicate>
                  <object><val>$items-iri</val></object>
                </triple>
            </triples>
            <templates>
                <template>
                <context>./node("$ref")</context>
                <vars>
                    <var>
                        <name>relationship-reference</name>
                        <val>
                            if (fn:starts-with(., "#/definitions/")) 
                            then sem:iri(fn:replace(.,"#/definitions/",$doc-subject-prefix))
                            else sem:iri(.)
                            </val>
                    </var>
                </vars>
                <triples>
                    <triple>
                      <subject><val>$items-iri</val></subject>
                      <predicate><val>$PROP_REF</val></predicate>
                      <object><val>$relationship-reference</val></object>
                    </triple>
                </triples>
                </template>
                <template>
                <context>./datatype</context>
                <triples>
                    <triple>
                      <subject><val>$items-iri</val></subject>
                      <predicate><val>$PROP_DATATYPE</val></predicate>
                      <object><val>$items-datatype </val></object>
                    </triple>
                </triples>
                </template>
                <template>
                <context>./collation</context>
                <triples>
                    <triple>
                      <subject><val>$items-iri</val></subject>
                      <predicate><val>$PROP_COLLATION</val></predicate>
                      <object><val>$collation</val></object>
                    </triple>
                </triples>
                </template>
            </templates>
        </template>
    </templates>
    </template>
    </templates>
</template>;

declare variable $esi:xml-extraction-template := 
<template xmlns:es="http://marklogic.com/entity-services"
     xmlns="http://marklogic.com/xdmp/tde">
    <description>Extracts triples from XML entity type documents which project an RDF model of entity types</description>
    <collections>
        <collection>http://marklogic.com/entity-services/entity-types</collection>
    </collections>
    <context>/es:entity-type</context>
    <path-namespaces>
        <path-namespace>
            <prefix>es</prefix>
            <namespace-uri>http://marklogic.com/entity-services</namespace-uri>
        </path-namespace>
    </path-namespaces>
    <vars>
        <!-- constants -->
        <var><name>ESN</name><val>"http://marklogic.com/entity-services#"</val></var>
        <var><name>RDF</name><val>"http://www.w3.org/1999/02/22-rdf-syntax-ns#"</val></var>
        <var><name>PROP_VERSION</name><val>sem:iri(concat($ESN, 'version'))</val></var>
        <var><name>PROP_TITLE</name><val>sem:iri(concat($ESN, 'title'))</val></var>
        <var><name>PROP_DEFINITIONS</name><val>sem:iri(concat($ESN, 'definitions'))</val></var>
        <var><name>PROP_PRIMARYKEY</name><val>sem:iri(concat($ESN, 'primaryKey'))</val></var>
        <var><name>PROP_PROPERTY</name><val>sem:iri(concat($ESN, 'property'))</val></var>
        <var><name>PROP_DATATYPE</name><val>sem:iri(concat($ESN, 'datatype'))</val></var>
        <var><name>PROP_COLLATION</name><val>sem:iri(concat($ESN, 'collation'))</val></var>
        <var><name>PROP_ITEMS</name><val>sem:iri(concat($ESN, 'items'))</val></var>
        <var><name>PROP_REF</name><val>sem:iri(concat($ESN, 'ref'))</val></var>
        <var><name>XSD</name><val>"http://www.w3.org/2001/XMLSchema#"</val></var>
        <var><name>XSD_BASE64BINARY</name><val>sem:iri(concat($XSD, "base64Binary"))</val></var>
        <var><name>XSD_BOOLEAN</name><val>sem:iri(concat($XSD, "boolean"))</val></var>
        <var><name>XSD_BYTE</name><val>sem:iri(concat($XSD, "byte"))</val></var>
        <var><name>XSD_DATE</name><val>sem:iri(concat($XSD, "date"))</val></var>
        <var><name>XSD_DATETIME</name><val>sem:iri(concat($XSD, "dateTime"))</val></var>
        <var><name>XSD_DAYTIMEDURATION</name><val>sem:iri(concat($XSD, "dayTimeDuration"))</val></var>
        <var><name>XSD_DECIMAL</name><val>sem:iri(concat($XSD, "decimal"))</val></var>
        <var><name>XSD_DOUBLE</name><val>sem:iri(concat($XSD, "double"))</val></var>
        <var><name>XSD_DURATION</name><val>sem:iri(concat($XSD, "duration"))</val></var>
        <var><name>XSD_FLOAT</name><val>sem:iri(concat($XSD, "float"))</val></var>
        <var><name>XSD_INT</name><val>sem:iri(concat($XSD, "int"))</val></var>
        <var><name>XSD_INTEGER</name><val>sem:iri(concat($XSD, "integer"))</val></var>
        <var><name>XSD_LONG</name><val>sem:iri(concat($XSD, "long"))</val></var>
        <var><name>XSD_SHORT</name><val>sem:iri(concat($XSD, "short"))</val></var>
        <var><name>XSD_STRING</name><val>sem:iri(concat($XSD, "string"))</val></var>
        <var><name>XSD_TIME</name><val>sem:iri(concat($XSD, "time"))</val></var>
        <var><name>XSD_UNSIGNEDINT</name><val>sem:iri(concat($XSD, "unsignedInt"))</val></var>
        <var><name>XSD_UNSIGNEDLONG</name><val>sem:iri(concat($XSD, "unsignedLong"))</val></var>
        <var><name>XSD_UNSIGNEDSHORT</name><val>sem:iri(concat($XSD, "unsignedShort"))</val></var>
        <var><name>XSD_YEARMONTHDURATION</name><val>sem:iri(concat($XSD, "yearMonthDuration"))</val></var>
        <var><name>XSD_ANYSIMPLETYPE</name><val>sem:iri(concat($XSD, "anySimpleType"))</val></var>
        <var><name>XSD_ANYURI</name><val>sem:iri(concat($XSD, "anyURI"))</val></var>
        <var><name>SEM</name><val>"http://marklogic.com/semantics#"</val></var>
        <var><name>SEM_IRI</name><val>sem:iri(concat($SEM, "iri"))</val></var>
        <var><name>JSON</name><val>"http://marklogic.com/json#"</val></var>
        <var><name>JSON_ARRAY</name><val>sem:iri(concat($JSON, "array"))</val></var>
        <!-- metadata for entity type document iris -->
        <var>
            <name>baseUriCoalesce</name>
            <val>if (./es:info/es:base-uri) then ./es:info/es:base-uri else "http://example.org/"</val>
        </var>
        <var>
            <name>baseUriPrefix</name>
            <val>if (fn:matches($baseUriCoalesce, "[#/]$")) then $baseUriCoalesce else concat($baseUriCoalesce, "/")</val>
        </var>
        <var>
            <name>baseUri</name>
            <val>if (fn:matches($baseUriCoalesce, "[#/]$")) then $baseUriCoalesce else concat($baseUriCoalesce, "#")</val>
        </var>
        <var>
            <name>title</name>
            <val>xs:string(./es:info/es:title)</val>
        </var>
        <var>
            <name>version</name>
            <val>xs:string(./es:info/es:version)</val>
        </var>
        <var>
            <name>doc-subject-prefix</name>
            <val>sem:iri(concat($baseUriPrefix, $title, "-", $version, "/"))</val>
        </var>
        <var>
            <name>doc-subject-iri</name>
            <val>sem:iri(concat($baseUri, $title , "-", $version))</val>
        </var>
    </vars>
    <triples>
        <triple>
          <subject><val>$doc-subject-iri</val></subject>
          <predicate><val>sem:iri(concat($RDF, "type"))</val></predicate>
          <object><val>sem:iri(concat($ESN, "EntityServicesDoc"))</val></object>
        </triple>
        <triple>
          <subject><val>$doc-subject-iri</val></subject>
          <predicate><val>$PROP_TITLE</val></predicate>
          <object><val>$title</val></object>
        </triple>
        <triple>
          <subject><val>$doc-subject-iri</val></subject>
          <predicate><val>$PROP_VERSION</val></predicate>
          <object><val>$version</val></object>
        </triple>
    </triples>

    <templates>
        <template>
            <context>es:definitions/*</context>
            <vars>
                <var><name>entityTypeName</name><val>fn:node-name(.)</val></var>
                <var>
                    <name>et-subject-iri</name>
                    <val>sem:iri(concat($doc-subject-prefix, $entityTypeName))</val>
                </var>
            </vars>
            <triples>
                <triple>
                  <subject><val>$et-subject-iri</val></subject>
                  <predicate><val>sem:iri(concat($RDF, "type"))</val></predicate>
                  <object><val>sem:iri(concat($ESN, "EntityType"))</val></object>
                </triple>
                <triple>
                  <subject><val>$et-subject-iri</val></subject>
                  <predicate><val>$PROP_TITLE</val></predicate>
                  <object><val>xs:string($entityTypeName)</val></object>
                </triple>
                <triple>
                  <subject><val>$et-subject-iri</val></subject>
                  <predicate><val>$PROP_VERSION</val></predicate>
                  <object><val>$version</val></object>
                </triple>
                <triple>
                  <subject><val>$doc-subject-iri</val></subject>
                  <predicate><val>$PROP_DEFINITIONS</val></predicate>
                  <object><val>$et-subject-iri</val></object>
                </triple>
            </triples>
            <templates>
                <template>
                    <context>./es:primary-key</context>
                    <triples>
                        <triple>
                          <subject><val>$et-subject-iri</val></subject>
                          <predicate><val>$PROP_PRIMARYKEY</val></predicate>
                          <object><val>sem:iri(concat($et-subject-iri, "/", .))</val></object>
                        </triple>
                    </triples>
                </template>
                <template>
                <context>./es:properties/*</context>
                <vars>
                <var><name>propertyName</name><val>fn:node-name(.)</val></var>
                <var><name>datatype-value</name><val>
                        switch (xs:string(./es:datatype))
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
                    <name>property-subject-iri</name>
                    <val>sem:iri(concat($et-subject-iri, "/", $propertyName))</val>
                </var>
                <var><name>collation</name><val>xs:string(head((.//es:collation, "")))</val></var>
            </vars>
            <triples>
                <triple>
                  <subject><val>$et-subject-iri</val></subject>
                  <predicate><val>sem:iri(concat($ESN, "property"))</val></predicate>
                  <object><val>$property-subject-iri</val></object>
                </triple>
                <triple>
                  <subject><val>$property-subject-iri</val></subject>
                  <predicate><val>sem:iri(concat($RDF, "type"))</val></predicate>
                  <object><val>sem:iri(concat($ESN, "Property"))</val></object>
                </triple>
                <triple>
                  <subject><val>$property-subject-iri</val></subject>
                  <predicate><val>$PROP_TITLE</val></predicate>
                  <object><val>xs:string($propertyName)</val></object>
                </triple>
            </triples>
            <templates>
                <template>
                    <context>./es:ref</context>
                    <vars>
                        <var>
                            <name>relationship-reference</name>
                            <val>
                                if (fn:starts-with(., "#/definitions/")) 
                                then sem:iri(fn:replace(.,"#/definitions/",$doc-subject-prefix))
                                else sem:iri(.)
                                </val>
                        </var>
                    </vars>
                    <triples>
                        <triple>
                          <subject><val>$property-subject-iri</val></subject>
                          <predicate><val>$PROP_REF</val></predicate>
                          <object><val>$relationship-reference</val></object>
                        </triple>
                    </triples>
                </template>
                <template>
                    <context>./es:datatype</context>
                    <triples>
                        <triple>
                          <subject><val>$property-subject-iri</val></subject>
                          <predicate><val>$PROP_DATATYPE</val></predicate>
                          <object><val>$datatype-value</val></object>
                        </triple>
                    </triples>
                </template>
                <template>
                <context>./es:collation</context>
                <triples>
                    <triple>
                      <subject><val>$property-subject-iri</val></subject>
                      <predicate><val>$PROP_COLLATION</val></predicate>
                      <object><val>$collation</val></object>
                    </triple>
                </triples>
                </template>
                <template>
                    <context>./es:items</context>
                    <vars>
                        <var>
                            <name>items-iri</name>
                            <val>sem:iri(concat($property-subject-iri, "/items"))</val>
                        </var>
                        <var><name>items-datatype</name><val>
                                switch (xs:string(./es:datatype))
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
                    </vars>
                    <triples>
                        <triple>
                          <subject><val>$property-subject-iri</val></subject>
                          <predicate><val>$PROP_ITEMS</val></predicate>
                          <object><val>$items-iri</val></object>
                        </triple>
                    </triples>
                    <templates>
                        <template>
                        <context>./es:ref</context>
                        <vars>
                            <var>
                                <name>relationship-reference</name>
                                <val>
                                    if (fn:starts-with(., "#/definitions/")) 
                                    then sem:iri(fn:replace(.,"#/definitions/",$doc-subject-prefix))
                                    else sem:iri(.)
                                    </val>
                            </var>
                        </vars>
                        <triples>
                            <triple>
                              <subject><val>$items-iri</val></subject>
                              <predicate><val>$PROP_REF</val></predicate>
                              <object><val>$relationship-reference</val></object>
                            </triple>
                        </triples>
                        </template>
                        <template>
                        <context>./es:datatype</context>
                        <triples>
                            <triple>
                              <subject><val>$items-iri</val></subject>
                              <predicate><val>$PROP_DATATYPE</val></predicate>
                              <object><val>$items-datatype </val></object>
                            </triple>
                        </triples>
                        </template>
                        <template>
                        <context>./es:collation</context>
                        <triples>
                            <triple>
                              <subject><val>$items-iri</val></subject>
                              <predicate><val>$PROP_COLLATION</val></predicate>
                              <object><val>$collation</val></object>
                            </triple>
                        </triples>
                        </template>
                    </templates>
                </template>
            </templates>
            </template>
            </templates>
        </template>
    </templates>
</template>;


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
 :)
declare function esi:templates-bootstrap()
{
    xdmp:document-insert("json-entity-services.tde", $json-extraction-template,
                                xdmp:default-permissions(),
                                "http://marklogic.com/xdmp/tde"),
    xdmp:document-insert("xml-entity-services.tde", $xml-extraction-template,
                                xdmp:default-permissions(),
                                "http://marklogic.com/xdmp/tde")
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
    case "int"                return xs:int( 123 )
    case "integer"            return xs:integer( 123 ) 
    case "long"               return xs:long( 1355 ) 
    case "short"              return xs:short( 343 ) 
    case "string"             return xs:string( "some string" ) 
    case "time"               return xs:time( "09:00:15" ) 
    case "unsignedInt"        return xs:unsignedInt( 5555  ) 
    case "unsignedLong"       return xs:unsignedLong( 999999999 ) 
    case "unsignedShort"      return xs:unsignedShort( 324 ) 
    case "yearMonthDuration"  return xs:yearMonthDuration( "P1Y" ) 
    case "anySimpleType"      return xs:string( "some string" ) 
    case "anyURI"             return xs:anyURI( "http://example.org/some-uri" ) 
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
    (: open issue -- new element for cycle :)
    else element es:cycle { }
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
