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
        <iso:rule context="es:info">
          <iso:report test="description" id="ES-DEFINITION">Your entity type should have a description.</iso:report>
        </iso:rule>
      </iso:pattern>
    </iso:schema>
;

declare variable $esi:entity-type-extraction-template :=
<template xmlns:es="http://marklogic.com/entity-services"
     xmlns="http://marklogic.com/xdmp/tde">
<!-- note vars doesn't seem to work yet, basically anywhere -->
    <context>/info</context>
    <triples>
        <triple>
          <subject>
              <val>sem:iri(concat('http://marklogic.com/entity-services#', version,"/",title))</val>
          </subject>
          <predicate>
            <val>sem:iri(concat('http://marklogic.com/entity-services#','title'))</val>
          </predicate>
          <object>
            <val>xs:string(title)</val>
          </object>
        </triple>
    </triples>
</template>
;

declare function esi:entity-type-validate(
    $entity-type as document-node()
) as xs:string*
{
    validate:schematron($entity-type, $esi:entity-type-schematron)
};


declare function esi:extract(
    $entity-type as document-node()
) 
{
    tde:document-data-extract($entity-type, $esi:entity-type-extraction-template)
};

