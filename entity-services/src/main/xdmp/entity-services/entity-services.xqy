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
declare namespace search = "http://marklogic.com/appservices/search";

declare option xdmp:mapping "false";


(:~
 : Validates a model.  This function will validate documents or nodes, or
 : in-memory instances of models as map:map or json:object
 :
 : @param $model Any representation of a model.
 : @return A valid representation of the model, converted to map:map as needed.
 : @throws Validation errors.
 :)
declare function es:model-validate(
    $model-descriptor
) as map:map
{
    esi:model-validate($model-descriptor)
};



(:~
 : Creates a model from an XML document or element
 : For JSON documents, this is equivalent to xdmp:json with validation.
 : For XML documents, we transform the input as well.
 :
 : @param $node A JSON or XML document containing an entity model.
 :)
declare function es:model-from-xml(
    $node
) as map:map
{
    if ($node instance of document-node())
    then
        esi:model-from-xml($node/node())
    else esi:model-from-xml($node)
};

(:~
 : Given a model, returns its XML representation
 : @param A model
 :)
declare function es:model-to-xml(
    $model as map:map
) as element(es:model)
{
    esi:model-to-xml($model)
};

(: experiment :)
declare function es:model-to-triples(
    $model as map:map
)
{
    esi:model-to-triples($model)
};
    

(:~
 : Generates an XQuery module that can be customized and used
 : to support transforms associated with a model
 : @param $model  A model.
 : @return An XQuery module (text) that can be edited and installed in a modules database.
 :)
declare function es:instance-converter-generate(
    $model as map:map
) as document-node()
{
    es-codegen:instance-converter-generate($model)
};

(:~
 : Generate one test instance in XML for each entity type in the
 : model.
 : @param A model.
 :)
declare function es:model-get-test-instances(
    $model as map:map
) as element()*
{
    esi:model-get-test-instances($model)
};



(:~
 : Generate a JSON node that can be used with the Management Client API
 : to configure a database for this model
 : Portions of this complete database properties file can be used
 : as building-blocks for the completed database properties
 : index configuration.
 : @param A model.
 :)
declare function es:database-properties-generate(
    $model as map:map
) as document-node()
{
    esi:database-properties-generate($model)
};



(:~
 : Generate a schema that can validate entity instance documents.
 : @param A model.
 : @return An XSD schema that can validate entity instances in XML form.
 :)
declare function es:schema-generate(
    $model as map:map
) as element()*
{
    esi:schema-generate($model)
};



(:~
 : Generates an extraction template that can surface the entity.
 : instance as a view in the rows index.
 : @param A model.
 :)
declare function es:extraction-template-generate(
    $model as map:map
) as document-node()
{
    document {
        esi:extraction-template-generate($model)
    }
};


(:~
 : Generates an element for configuring Search API applications
 : Intended as a starting point for developing a search grammar tailored
 : for entity and relationship searches.
 : @param An entity model
 :)
declare function es:search-options-generate(
    $model as map:map
)
{
    esi:search-options-generate($model)
};

(:~
 : Generates an XQuery module that can create an instance of one
 : type from documents saved by code from another type.
 : @param A model that describes the target type of the conversion.
 : @param A model that describes the source type of the conversion.
 :)
declare function es:version-translator-generate(
    $source-model as map:map,
    $target-model as map:map
) as document-node()
{
    es-codegen:version-translator-generate(
           $source-model,
           $target-model)
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
 : Fluent method to add key/value pairs to an entity instance, if the value exists.
 : @param $instance An instance of map:map to add a key to.
 : @param $property-key  The key to add to $instance.
 : @param $value The value to add to $instance for the given key.
:)
declare function es:optional(
    $instance as map:map,
    $property-key as xs:string,
    $value as item()*
) as map:map
{
    typeswitch($value)
    case empty-sequence() return ()
    (: this case handles empty extractions :)
    case map:map return
        if (map:contains($value, "$ref") and empty(map:get($value, "$ref")))
        then ()
        else map:put($instance, $property-key, $value)
    default return map:put($instance, $property-key, $value)
    ,
    $instance
};


(:~
 : Extract values from a sequence of nodes into an property of type array.
 : If there are no nodes on input, then this function returns the empty sequence.
 : @param $source-nodes The node(s) from which to extract values into an array.
 : @param $fn The function to be applied to each sequence item
 : @param $value - the value to add to $instance for the given key.
 :)
declare function es:extract-array(
    $source-nodes as item()*,
    $fn as function(*)
) as json:array?
{
    if (empty($source-nodes))
    then ()
    else json:to-array($source-nodes ! $fn(.))
};


