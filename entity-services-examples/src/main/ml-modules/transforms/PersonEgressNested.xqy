xquery version "1.0-ml";

module namespace pec = "http://marklogic.com/rest-api/transform/PersonEgressNested";

import module namespace mle = "http://marklogic.com/entity-services" 
  at "/MarkLogic/rest-api/lib/entity-services.xqy";

(: REST transform function :)
declare function pec:transform(
  $context as map:map,
  $params as map:map,
  $source as document-node()
) as document-node()
{
  let $friends := 
  	cts:search(collection(), 
  	    cts:element-query(xs:QName("mle:entity"), 
  	    	cts:element-value-query(xs:QName("id"),
  	    							$source/mle:entity/Person/friendOf/data())))
  let $nested-structure :=
  	element Person {
  		$source/mle:entity/Person/*[not(self::friendOf)],
  		for $f in $friends
  		return element friendOf {
  		  $f/mle:entity/Person
  		}
  	}
  	let $_ := xdmp:log(("egress transform called ",$source, "nested", $nested-structure))
  
  return
  document { $nested-structure }
};
