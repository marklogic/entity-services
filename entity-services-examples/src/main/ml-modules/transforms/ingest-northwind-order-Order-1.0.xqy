xquery version "1.0-ml";

module namespace nis = "http://marklogic.com/rest-api/transform/ingest-northwind-order-Order-1.0";

import module namespace mle = "http://marklogic.com/entity-services" 
  at "/MarkLogic/rest-api/lib/entity-services.xqy";

declare variable $nis:ENTITY-TYPE-NAME := "Order-1.0";


(: REST transform function :)
declare function nis:transform(
  $context as map:map,
  $params as map:map,
  $in as document-node()
) as document-node()?
{
    
   let $envelope := 
    <mle:entity xmlns:mle="http://marklogic.com/entity-services">
      <mle:name>{$nis:ENTITY-TYPE-NAME}</mle:name>
      <Order>
        <orderId>{data($in/Order/@OrderID)}</orderId>
        <orderedBy><Customer>{data($in/Order/CustomerID)}</Customer></orderedBy>
        <orderDate>{data($in/Order/OrderDate)}</orderDate>
        <freight>{data($in/Order/Freight)}</freight>
        <hasOrderDetails>{
            for $d in $in/Order/OrderDetails/OrderDetail
            return
            <OrderDetails>
            	<orderId>{data($in/Order/@OrderID)}</orderId>
            	<productName>{data(doc( data($d/ProductID) || ".xml")//ProductName )}</productName>
            	<unitPrice>{data($d/UnitPrice)}</unitPrice>
				<quantity>{data($d/Quantity)}</quantity>
				<discount>{data($d/Discount)}</discount>
            </OrderDetails>
        }</hasOrderDetails>
    </Order>
      <mle:sources>
        {$in}
      </mle:sources>
    </mle:entity>
  (: let $validate := validate { $envelope/Order } :)
  return document { $envelope }
};
