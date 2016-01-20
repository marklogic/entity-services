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

declare default function namespace "http://www.w3.org/2005/xpath-functions";

declare variable $ENTITY-TYPES-IRI := "http://marklogic.com/entity-services#";

declare option xdmp:mapping "false";

declare function es:entity-type-from-node(
    $node as document-node()
) as json:object
{
    let $errors := esi:entity-type-validate($node)
    return
        if ($errors)
        then fn:error( (), "ES-ENTITY-TYPE-INVALID", $errors)
        else esi:extract($node)
};

declare function es:entity-type-as-triples(
    $entity-type as document-node()
) 
{
    esi:extract($entity-type)
};
