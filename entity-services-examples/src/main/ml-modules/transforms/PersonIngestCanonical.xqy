xquery version "1.0-ml";

module namespace nis = "http://marklogic.com/rest-api/transform/PersonIngestCanonical";

import module namespace mle = "http://marklogic.com/entity-services" 
  at "/MarkLogic/rest-api/lib/entity-services.xqy";

declare function nis:render-canonical-entity(
  $source as document-node()
) {
    <Person>
      <id>{data($source/Person/id)}</id>
      <firstName>{data($source/Person/firstName)}</firstName>
      <lastName>{data($source/Person/lastName)}</lastName>
      <fullName>{data($source/Person/fullName)}</fullName>
      { for $f in $source/Person/friendOf return
      <friendOf>{data($f)}</friendOf>
      }
    </Person>
};

(: mlcp wrapper function :)
declare function nis:transform(
  $context as map:map,
  $params as map:map
) as map:map*
{
  (
    map:put($context, 
            "value", 
            nis:transform($context, $params, map:get($context, "value"))), 
    $context
  )
};

(: REST transform function :)
declare function nis:transform(
  $context as map:map,
  $params as map:map,
  $source as document-node()
) as document-node()
{
    
   let $envelope := 
    <mle:entity xmlns:mle="http://marklogic.com/entity-services">
      <mle:name>Person</mle:name>
      {nis:render-canonical-entity($source)}
      <mle:sources>
        {$source}
      </mle:sources>
    </mle:entity>
  let $validate := validate { $envelope/Person }
  return document { $envelope }
};
