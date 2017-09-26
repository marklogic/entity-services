xquery version "1.0-ml";
module namespace test = "http://marklogic.com/rest-api/resource/test-example";

import module namespace translator = "http://marklogic.com/example/translator" at "/ext/versions-translator.xqy";
import module namespace m = "http://marklogic.com/example/Model" at "/ext/model-original.xqy";
import module namespace m-next = "http://marklogic.com/example/Model-next" at "/ext/model-next.xqy";

declare variable $test:payload-one := <x><p>1</p></x>;
declare variable $test:payload-two := <x><p>2</p><p2>data2</p2></x>;

declare function test:get(
        $context as map:map,
        $params as map:map
) as document-node()
{
    let $extraction := m:extract-instance-Person($test:payload-one)
    let $extraction-next := m-next:extract-instance-Person($test:payload-one)
    let $env := m:instance-to-envelope($extraction)
    let $env-next := m-next:instance-to-envelope($extraction-next)
    let $up-convert := translator:up-convert($env)
    let $down-convert := translator:down-convert($env-next)
    return
        document {
        <doc>{
            xdmp:quote($extraction),
            xdmp:quote($extraction-next),
            $env,
            $env-next,
            xdmp:quote($up-convert),
            xdmp:quote($down-convert)
        }</doc>
        }
};
