xquery version "1.0-ml";

module namespace pec = "http://marklogic.com/rest-api/transform/PersonEgressCanonical";

import module namespace mle = "http://marklogic.com/entity-services" 
  at "/MarkLogic/rest-api/lib/entity-services.xqy";

(: REST transform function :)
declare function pec:transform(
  $context as map:map,
  $params as map:map,
  $source as document-node()
) as document-node()
{
  xdmp:log(("egress transform called ",$source)),
  document { $source/mle:entity/Person }
};
