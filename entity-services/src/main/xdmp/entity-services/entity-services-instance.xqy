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

module namespace inst = "http://marklogic.com/entity-services-instance";
declare namespace es = "http://marklogic.com/entity-services";
declare namespace tde = "http://marklogic.com/xdmp/tde";

import module namespace sem = "http://marklogic.com/semantics" at "/MarkLogic/semantics.xqy"; 

declare default function namespace "http://www.w3.org/2005/xpath-functions";


declare function inst:instance-from-document(
    $document as document-node()
) as map:map*
{
    let $xml-from-document := inst:instance-xml-from-document($document)
    for $root-instance in $xml-from-document
        return inst:child-instance($root-instance)
};

declare function inst:child-instance(
    $element as element()
) as map:map*
{
    if (empty($element/*) and exists($element/text()))
    then json:object()
            =>map:with("$type", local-name($element))
            =>map:with("$ref", $element/text())
    else
        let $child := json:object()
        let $_ := 
            for $property in $element/*
            return
                if (map:contains($child, local-name($property)))
                then 
                    if (map:get($child, local-name($property)) instance of json:array)
                    then json:array-push(map:get($child, local-name($property)),
                                         inst:child-instance($property/*))
                    else map:put($child, local-name($property),
                         json:array()
                            =>json:array-with(map:get($child, local-name($property)))
                            =>json:array-with(inst:child-instance($property/*)))
                else
                    map:put($child, local-name($property),
                        if ($property/element())
                        then inst:child-instance($property/element())
                        else data($property))
        return $child
};


declare function inst:instance-xml-from-document(
    $document as document-node()
) as element()*
{
    $document//es:instance/(* except es:info)
};

declare function inst:instance-json-from-document(
    $document as document-node()
) as object-node()
{
    let $instance := inst:instance-from-document($document)
    return xdmp:to-json($instance)/node()
};


declare function inst:instance-get-attachments(
    $document as document-node()
) as element()*
{
    $document//es:attachments/*
};
