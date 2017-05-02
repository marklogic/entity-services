(:
 Copyright 2002-2017 MarkLogic Corporation.  All Rights Reserved. 

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
declare namespace xq = "http://www.w3.org/2012/xquery";


import module namespace sem = "http://marklogic.com/semantics" at "/MarkLogic/semantics.xqy"; 

declare default function namespace "http://www.w3.org/2005/xpath-functions";

(: declare option xdmp:mapping "false"; :)
declare option xq:require-feature "xdmp:three-one";

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
            =>map:with("$ref", $element/data())
    else
        let $child := json:object()=>map:with("$type", local-name($element))
        let $_ := 
            for $property in $element/*
            return
                if (map:contains($child, local-name($property)))
                then 
                    if (map:get($child, local-name($property)) instance of json:array)
                    then json:array-push(map:get($child, local-name($property)),
                            if ($property/element())
                            then inst:child-instance($property/*)
                            else data($property))
                    else 
                        let $new-array := json:array()
                        return (
                            json:array-push($new-array, map:get($child, local-name($property))),
                            json:array-push($new-array, inst:child-instance($property/*)),
                            map:put($child, local-name($property), $new-array)
                        )
                else
                    if ($property[@datatype eq "array"])
                    then
                        map:put($child, local-name($property),
                            json:array()
                            =>json:array-with(
                                if ($property/element())
                                then inst:child-instance($property/element())
                                else data($property))
                            )
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

declare private function inst:wrap-instance(
    $instance as json:object
) as json:object
{
    let $type := map:get($instance, "$type")
    let $remove-it := map:delete($instance, "$type")
    return json:object()
        =>map:with($type,
            if (map:contains($instance, "$ref"))
            then map:get($instance, "$ref")
            else
                let $value-map := json:object()
                let $_ :=
                    for $k in map:keys($instance)
                    let $v := map:get($instance, $k)
                    return
                    typeswitch ($v)
                    case json:array return
                     
                        if (json:array-size($v) eq 0)
                        then json:object()=>map:with($k, $v)
                        else if ($v[1] instance of json:object)
                        then map:put($value-map, $k, (json:to-array(json:array-values($v) ! inst:wrap-instance(.))))
                        else map:put($value-map, $k, $v)
                    case json:object return
                        map:put($value-map, $k, inst:wrap-instance($v))
                    default return map:put($value-map, $k, $v)
                return $value-map)
};

declare function inst:instance-json-from-document(
    $document as document-node()
) as object-node()*
{
    (inst:instance-from-document($document) !
        (inst:wrap-instance(.)=>xdmp:to-json()))/node()
};


declare function inst:instance-get-attachments(
    $document as document-node()
) as item()*
{
    if (exists($document//es:attachments/*))
    then $document//es:attachments/*
    else $document//es:attachments/text()
};
