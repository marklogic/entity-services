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

module namespace es-codegen = "http://marklogic.com/entity-services-codegen";
declare namespace es = "http://marklogic.com/entity-services";
declare namespace tde = "http://marklogic.com/xdmp/tde";


declare function es-codegen:declarations(
    $entity-type as map:map
) as text()
{
    let $default-base-uri := "http://example.org/"
    let $info := map:get($entity-type, "info")
    let $doc-title := map:get($info, "title")
    let $doc-version:= map:get($info, "version")
    let $base-uri := fn:head((map:get($info, "baseUri"), $default-base-uri))
    return
    <declarations>
xquery version "1.0-ml";

module namespace {$doc-title} = "{$base-uri}/{$doc-title}-{$doc-version}";
    </declarations>/text()
};

declare function es-codegen:extract-instance(
    $entity-type as map:map
) as text()
{
    <extract-instance>
declare function es-codegen:extract-instance(
) as map:map
{{
  map:map()
}};
    </extract-instance>/text()
};
