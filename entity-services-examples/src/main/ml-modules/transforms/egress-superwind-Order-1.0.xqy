xquery version "1.0-ml";

module namespace pec = "http://marklogic.com/rest-api/transform/egress-superwind-Order-1.0";

declare namespace mle = "http://marklogic.com/entity-services";

(: REST transform function :)
declare function pec:transform(
  $context as map:map,
  $params as map:map,
  $source as document-node()
) as document-node()
{
  xdmp:log(("egress transform called ",$source)),
  document { $source/mle:entity/Order }
};
