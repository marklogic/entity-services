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
        <iso:rule context="datatype">
         <iso:assert test=". = ('base64Binary' , 'boolean' , 'byte', 'date', 'dateTime', 'dayTimeDuration', 'decimal', 'double', 'duration', 'float', 'int', 'integer', 'long', 'short', 'string', 'time', 'unsignedInt', 'unsignedLong', 'unsignedShort', 'yearMonthDuration', 'anySimpleType', 'anyURI', 'sem:iri', 'array')" id="ES-UNSUPPORTED-DATATYPE">Unsupported datatype.</iso:assert>
        </iso:rule>
      </iso:pattern>
    </iso:schema>
;


declare variable $esi:extraction-template := 
<template xmlns:es="http://marklogic.com/entity-services"
     xmlns="http://marklogic.com/xdmp/tde">
<!-- note vars doesn't seem to work yet, basically anywhere -->
    <context>/info</context>
    <vars>
        <var><name>esn</name><val>"http://marklogic.com/entity-services#"</val></var>
        <var><name>subject-iri</name><val>sem:iri(concat(baseURI, title, "-", version))</val></var>
        <var><name>propVersion</name><val>sem:iri('http://marklogic.com/entity-services#version')</val></var>
        <var><name>propTitle</name><val>sem:iri('http://marklogic.com/entity-services#title')</val></var>
    </vars>
    <triples>
        <triple>
          <subject>
            <val>$subject-iri</val>
          </subject>
          <predicate>
            <val>$propTitle</val>
          </predicate>
          <object>
            <val>xs:string(title)</val>
          </object>
        </triple>
        <triple>
          <subject><val>$subject-iri</val></subject>
          <predicate>
            <val>$propVersion</val>
          </predicate>
          <object>
            <val>xs:string(version)</val>
          </object>
        </triple>
    </triples>
</template>;


declare function esi:entity-type-validate(
    $entity-type as document-node()
) as xs:string*
{
    let $props := $entity-type//properties/object-node()
    let $_ := for $p in $props
                return xdmp:log(("PROPS W REF", $p[*[local-name(.) eq '$ref']],
              "COUNT", count($p/(* except description))))
    return
    validate:schematron($entity-type, $esi:entity-type-schematron)
};


declare function esi:extract-triples(
    $entity-type as document-node()
) as sem:triple*
{
    (: TODO adjust for when TDE outputs triples :)
    let $string-output := tde:document-data-extract($entity-type, $esi:extraction-template)
    let $json-array := xdmp:from-json(xdmp:unquote($string-output))[1]
    return json:array-values($json-array) ! sem:triple(.)
};

