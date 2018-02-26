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

module namespace es = "http://marklogic.com/entity-services";

import module namespace esi = "http://marklogic.com/entity-services-impl" at "entity-services-impl.xqy";

import module namespace es-codegen = "http://marklogic.com/entity-services-codegen" at "entity-services-codegen.xqy";

import module namespace inst = "http://marklogic.com/entity-services-instance" at "entity-services-instance.xqy";

declare default function namespace "http://www.w3.org/2005/xpath-functions";
declare namespace search = "http://marklogic.com/appservices/search";
declare namespace xq = "http://www.w3.org/2012/xquery";

declare option xdmp:mapping "false";
declare option xq:require-feature "xdmp:three-one";


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
) as object-node()*
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
) as item()*
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


(:~
 : Examine an incoming data source to normalize the extraction context.
 : @param $source An incoming source node, which may be an element, JSON node or document.
 : @param $entity-type-name The expected entity type name.
 : @return Either the incoming node intact, or its contents if it's already canonicalized.
 :)
declare function es:init-source(
    $source as item()*,
    $entity-type-name as xs:string
) as item()*
{
    if ( ($source instance of document-node()) )
    then $source/node()
    else if (exists ($source/element()[fn:local-name(.) eq $entity-type-name] ))
    then $source/*
    else $source
};

(:~
 : Initializes an instance data structure, by adding a type key and, if appropriate,
 : a ref key.
 : @param $source-node The input to extractions. Used to determine whether to extract a pointer or an instance node.
 : @param $entity-type-name  The name of this instance's type.
 : @return A json object with $type key, and, if appropriate, a $ref key.
 :)
declare function es:init-instance(
    $source-node as item()*,
    $entity-type-name as xs:string
) as json:object
{
    let $source-node := es:init-source($source-node, $entity-type-name)
    let $instance := json:object()
            =>map:with('$type', $entity-type-name)
    return
        if (empty($source-node/*))
        then $instance=>map:with('$ref', $source-node/data())
        (: Otherwise, this source node contains instance data. Populate it. :)
        else $instance
};


(:~
 : Adds namespace information to an instance.
 : @param $instance the existing instance
 : @param $namespace The namespace uri to be included as metadata.
 : @param $namespace-prefix A prefix to be passed to the instance for its namespace
 : @return The $instance, with namespace info appended
 :)
declare function es:with-namespace(
    $instance as map:map,
    $namespace as xs:string?,
    $namespace-prefix as xs:string?
) as json:object
{
    let $_ :=
        if ($namespace-prefix)
        then (
            map:put($instance, "$namespace", $namespace),
            map:put($instance, "$namespacePrefix", $namespace-prefix)
        )
        else ()
    return $instance
};


(:~
 : Adds the original source document to the entity instance.
 : @param $instance The instance data, to which the source will be attached.
 : @param $source-node The extraction context for the incoming data
 : @param $source The unmodified source document.
 :)
declare function es:add-attachments(
    $instance as map:map,
    $source as item()*
) as map:map
{
    $instance=>map:with('$attachments', $source)
};

(:~
 : Initializes the context to convert instances from one version to another
 : @param $source Zero or more envelopes or canonical instances.
 : @param $entity-type-name The name of the expected Entity Type
 : @return Zero or more sources expected to contain the canonical data of the given type.
 :)
declare function es:init-translation-source(
    $source as item()*,
    $entity-type-name as xs:string
) as item()*
{
    if ( ($source//es:instance/element()[fn:local-name(.) eq $entity-type-name]))
    then $source//es:instance/element()[fn:local-name(.) eq $entity-type-name]
    else $source
};


(:~
 : Copies attachments from an envelope document to a new intance.
 : @param $instance  The target to which to attach source data.
 : @param $source The envelope or canonical instance from which to copy attachments.
 :)
declare function es:copy-attachments(
    $instance as map:map,
    $source as item()*
) as map:map
{
    let $attachments := $source ! fn:root(.)/es:envelope/es:attachments/node()
    return
    if (exists($attachments))
    then $instance=>map:with('$attachments', $attachments)
    else $instance
};


(:~
 : Serializes attachments for storage in an envelope document.  
 : @param $instance  The isntance holding attachment data.
 : @param $format The format of the envlosing envlelope.
 : @return If the format does not match that supplied in $format, then the attachment
 :     is returned as a quoted string.  Otherwise, the attachment is returned for inline 
       inclusion as a node.
 :)
declare function es:serialize-attachments(
    $instance as map:map,
    $envelope-format as xs:string
) as item()*
{
    switch ($envelope-format)
    case "json" return
        object-node { 'attachments' :
            array-node {
                for $attachment in $instance=>map:get('$attachments')
                let $attachment :=
                    if ($attachment instance of document-node())
                    then $attachment/node()
                    else $attachment
                return
                    typeswitch ($attachment)
                    case object-node() return $attachment
                    case array-node() return $attachment
                    default return xdmp:quote($attachment)
            }
        }
    case "xml" return
        element es:attachments {
            for $attachment in $instance=>map:get('$attachments')
            let $attachment :=
                if ($attachment instance of document-node())
                then $attachment/node()
                else $attachment
            return
                typeswitch ($attachment)
                case element() return $attachment
                default return xdmp:quote($attachment)
        }
    default return fn:error( (), 'Only available envelope formats are "xml" and "json"')
};
