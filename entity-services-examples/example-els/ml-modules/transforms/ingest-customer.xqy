xquery version "1.0-ml";

module namespace ingester = "http://marklogic.com/rest-api/transform/ingest-customer";

import module namespace es = "http://marklogic.com/entity-services"
    at "/MarkLogic/entity-services/entity-services.xqy";

(: in place transform to make envelope.  Note how we assume the input is canonical, and don't bother extraction :)
declare function ingester:transform(
    $context as map:map,
    $params as map:map,
    $body as document-node()
) as document-node()?
{
    let $uri := map:get($context, "uri")
    let $_ := xdmp:log(("Procesing" || $uri))
    return
        (: copied from codegen :)
        document {
            object-node {'envelope' :
            object-node {'instance' :
            object-node {'info' : object-node {
            'title' : "Customer",
            'version' : '0.0.1'
            },
            'Customer' : $body
            }
            }
            }
        }
};

