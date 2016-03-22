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

module namespace es = "http://marklogic.com/entity-services";

import module namespace esi = "http://marklogic.com/entity-services-impl" at "entity-services-impl.xqy";

import module namespace es-codegen = "http://marklogic.com/entity-services-codegen" at "entity-services-codegen.xqy";

declare default function namespace "http://www.w3.org/2005/xpath-functions";

declare variable $ENTITY-TYPES-IRI := "http://marklogic.com/entity-services#";

declare option xdmp:mapping "false";

(:~
 : Creates an entity-type from a document node.
 : For JSON documents, this is equivalent to xdmp:json with validation.
 : For XML documents, we transform the input as well.
 : 
 : @param $node A JSON or XML document containing an entity type definition.
 :)
declare function es:entity-type-from-node(
    $node as document-node()
) as map:map
{
    let $errors := esi:entity-type-validate($node)
    let $root := $node/node()
    return
        if ($errors)
        then fn:error( (), "ES-ENTITY-TYPE-INVALID", $errors)
        else 
            if ($root/object-node()) 
            then xdmp:to-json($root)
            else esi:entity-type-from-xml($root)
};

(:~
 : Given an entity type, returns its XML representation
 :)
declare function es:entity-type-to-xml(
    $entity-type as map:map
) as element(es:entity-type)
{
    esi:entity-type-to-xml($entity-type)
};

(:~
 : Given an entity type, returns its JSON representation
 :)
declare function es:entity-type-to-json(
    $entity-type as map:map
) as object-node()
{
    xdmp:to-json($entity-type)/node()
};

(:~
 : Generate a conversion module for a given entity type
 :)
declare function es:conversion-module-generate(
    $entity-type as map:map
) as document-node()
{
    es-codegen:conversion-module-generate($entity-type)
};

(:~
 : Generate one test instance in XML for each entity type in the 
 : entity type document payload.
 :)
declare function es:entity-type-get-test-instances(
    $entity-type as map:map
) as element()*
{
    esi:entity-type-get-test-instances($entity-type)
};



(:~
 : Generate a JSON node that can be used with the Management Client API
 : to configure a database for this entity type.
 : Portions of this complete database properties file can be used 
 : as building-blocks for the completed database properties 
 : index configuration.
 :)
declare function es:database-properties-generate(
    $entity-type as map:map
) as document-node()
{
    esi:database-properties-generate($entity-type)
};



(:~
 : Generate a schema that can validate entity instance documents
 :)
declare function es:schema-generate(
    $entity-type as map:map)
as element()*
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
        <xs:complexType name="{$entity-type-name}ContainerType">
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
