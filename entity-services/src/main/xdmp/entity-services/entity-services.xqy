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

import module namespace inst = "http://marklogic.com/entity-services-instance" at "entity-services-instance.xqy";

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
 : @param An entity type document.
 :)
declare function es:entity-type-to-xml(
    $entity-type as map:map
) as element(es:entity-type)
{
    esi:entity-type-to-xml($entity-type)
};

(:~
 : Given an entity type, returns its JSON representation
 : @param An entity type document.
 :)
declare function es:entity-type-to-json(
    $entity-type
) as object-node()
{
(: This function has no argument type because the XQuery engine otherwise
 : casts nodes to map:map, which would be confusing for this particular
 : function
 :)
    if ($entity-type instance of map:map)
    then xdmp:to-json($entity-type)/node()
    else fn:error( (), "ES-ENTITY-TYPE-INVALID", "Entity types must be map:map (or its subtype json:object)")
};

(:~
 : Generates an XQuery module that can be customized and used
 : to support transforms associated with an entity type
 : @param $entity-type  An entity type object.
 : @return An XQuery module (text) that can be edited and installed in a modules database.
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
 : @param An entity type document.
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
 : @param An entity type document.
 :)
declare function es:database-properties-generate(
    $entity-type as map:map
) as document-node()
{
    esi:database-properties-generate($entity-type)
};



(:~
 : Generate a schema that can validate entity instance documents.
 : @param An entity type document.
 : @return An XSD schema that can validate entity instances in XML form.
 :)
declare function es:schema-generate(
    $entity-type as map:map)
as element()*
{
    esi:schema-generate($entity-type)
};



(:~
 : Generates an extraction template that can surface the entity.
 : instance as a view in the rows index.
 : @param An entity type document.
 :)
declare function es:extraction-template-generate(
    $entity-type as map:map
) as document-node()
{
    document {
        esi:extraction-template-generate($entity-type) 
    }
};

(:~
 : Given a document, gets the instance data 
 : from it and returns instances as maps.
 : @param a document, usually es:envelope.
 : @return zero or more entity instances extracted from the document.
 :)
declare function es:instance-from-document(
    $document as document-node()
) as map:map*
{
    inst:instance-from-document($document)
};

(:~
 : Return the canonical XML representation of an instance from
 : a document.  This function does not transform; it's just a 
 : projection of elements from a document.
 : @param a document, usually es:envelope.
 : @return zero or more elements that represent instances.
 :)
declare function es:instance-xml-from-document(
    $document as document-node()
) as element()*
{
    inst:instance-xml-from-document($document)
};

(:~
 : Returns the JSON serialization of an instance from
 : a document.
 : @param a document, usually es:envelope.
 : @return zero or more JSON structures that represent instances.
 :)
declare function es:instance-json-from-document(
    $document as document-node()
) as object-node()
{
    inst:instance-json-from-document($document)
};


(:~
 : Return the attachments from within an es:envelope document.
 : @param a document, usually es:envelope.
 : @return Anything that is contained within the es:attachments
 : element.
 :)
declare function es:instance-get-attachments(
    $document as document-node()
) as element()*
{
    inst:instance-get-attachments($document)
};


(:~
 : Fluent method to add key/value pairs to an entity instance.
 : @param $instance An instance of map:map to add a key to.
 : @param $property-path - A en expression that, if non-empty, evaluates the property-key and value for addition to $instance.
 : @param $property-key - The key to add to $instance.
 : @param $value - the value to add to $instance for the given key.
:)
declare function es:with(
    $instance as map:map,
    $property-path as item()*,
    $property-key as xs:string,
    $value as item()*
) as map:map
{
    if (exists($property-path))
    then 
    map:put($instance, $property-key, $value) 
    else (),
    $instance
};
