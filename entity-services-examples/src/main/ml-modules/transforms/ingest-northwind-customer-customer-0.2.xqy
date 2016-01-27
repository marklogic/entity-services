xquery version "1.0-ml";

module namespace nis = "http://marklogic.com/rest-api/transform/ingest-northwind-customer-Customer-0.2";

import module namespace mle = "http://marklogic.com/entity-services" 
  at "/MarkLogic/rest-api/lib/entity-services.xqy";

declare variable $nis:ENTITY-TYPE-NAME := "Customer-0.2";


(: REST transform function :)
declare function nis:transform(
  $context as map:map,
  $params as map:map,
  $in as document-node()
) as document-node()
{
    
   let $envelope := 
    <mle:entity xmlns:mle="http://marklogic.com/entity-services">
      <mle:name>{$nis:ENTITY-TYPE-NAME}</mle:name>
      <Customer>
        <customerId>{data($in/Customer/@CustomerID)}</customerId>
        <name>{data($in/Customer/ContactName)}</name>
      </Customer>
      <mle:sources>
        {$in}
      </mle:sources>
    </mle:entity>
  (: let $validate := validate { $envelope/Customer } :)
  return document { $envelope }
};
