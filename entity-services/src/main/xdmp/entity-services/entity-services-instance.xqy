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

module namespace i = "http://marklogic.com/entity-services-instance";
declare namespace es = "http://marklogic.com/entity-services";
declare namespace tde = "http://marklogic.com/xdmp/tde";

import module namespace sem = "http://marklogic.com/semantics" at "/MarkLogic/semantics.xqy"; 

import module namespace search = "http://marklogic.com/appservices/search" at "/MarkLogic/appservices/search/search.xqy";

declare default function namespace "http://www.w3.org/2005/xpath-functions";

declare function i:with(
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

(: instance-from-document 
 : if you have modified instance-to-envelope
 : you may need also to modify this function
 :)
declare function i:instance-from-document(
    $document as document-node()
) as map:map*
{
    let $xml-from-document := i:instance-xml-from-document($document)
    for $root-instance in $xml-from-document
        return i:child-instance($root-instance)
};

declare function i:child-instance(
    $element as element()
) as map:map*
{
    let $child := json:object()
    let $_ := 
        for $property in $element/*
        return
            if ($property/element())
                then 
                if (map:contains($child, local-name($property)))
                    then
                    let $existing-key := 
                        if (map:get($child, local-name($property)) instance of json:array)
                        then map:get($child, local-name($property))
                        else 
                            let $a := json:array()
                            let $_ := json:array-push($a, map:get($child, local-name($property)))
                            return $a
                    let $_ := for $child in $property/* return json:array-push($existing-key, i:child-instance($child))
                    return map:put($child, local-name($property), $existing-key)
                else
                    map:put($child, local-name($property), $property/* ! i:child-instance(.))
            else map:put($child, local-name($property), data($property))
    return $child
};


(:~
 : Returns all XML from within a document envelope except the es:info.
 : This function is generic enough not to require customization for
 : most entity type implementations.
 :)
declare function i:instance-xml-from-document(
    $document as document-node()
) as element()
{
    $document//es:instance/(* except es:info)
};

declare function i:instance-json-from-document(
    $document as document-node()
) as object-node()
{
    let $instance := i:instance-from-document($document)
    return xdmp:to-json($instance)/node()
};


declare function i:instance-get-attachments(
    $document as document-node()
) as element()*
{
    $document//es:attachments/*
};
