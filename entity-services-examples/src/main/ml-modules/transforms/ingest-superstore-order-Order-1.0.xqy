xquery version "1.0-ml";

module namespace nis = "http://marklogic.com/rest-api/transform/ingest-superstore-order-Order-1.0";

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
    xdmp:log(( $in )),
    let $orderId := data($in/row/OrderID)
	let $existing := 
        cts:search( collection(), 
                    cts:element-value-query(xs:QName("orderId"), $orderId) )
    let $details :=
        <OrderDetails>
            <orderId>{data($in/row/OrderID)}</orderId>
            <productName>{data($in/row/ProductName)}</productName>
            <unitPrice>{data($in/row/UnitPrice)}</unitPrice>
            <quantity>{data($in/row/OrderQuantity)}</quantity>
            <discount>{data($in/row/Discount)}</discount>
        </OrderDetails>
   	return
   		if (exists($existing))
   		then (
   			xdmp:log("Updating existing node"),
   			xdmp:node-insert-child($existing/mle:entity/Order/hasOrderDetails, $details ),
   			xdmp:node-insert-child($existing/mle:entity/mle:sources, $in/node())
   			)
   		else
		   	let $envelope := 
		    <mle:entity xmlns:mle="http://marklogic.com/entity-services">
              <mle:name>{$nis:ENTITY-TYPE-NAME}</mle:name>
              <Order>
                <orderId>{data($in/row/OrderID)}</orderId>
                <orderedBy>
                  <Customer>
                    <contactName>{data($in/row/CustomerName)}</contactName>
                  </Customer>
                </orderedBy>
                <orderDate>{data($in/row/OrderDate)}</orderDate>
                <freight>{data($in/row/ShippingCost)}</freight>
                <hasOrderDetails>{$details}</hasOrderDetails>
            </Order>
              <mle:sources>
                {$in}
              </mle:sources>
            </mle:entity>
            (: let $validate := validate { $envelope/Order } :)
            return document { $envelope }
};
