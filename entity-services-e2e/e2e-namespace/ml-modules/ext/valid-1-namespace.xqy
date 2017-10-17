xquery version '1.0-ml';

(:
 This module was generated by MarkLogic Entity Services.
 The source model was Model_1ns-0.0.1

 For usage and extension points, see the Entity Services Developer's Guide

 https://docs.marklogic.com/guide/entity-services

 After modifying this file, put it in your project for deployment to the modules
 database of your application, and check it into your source control system.

 Generated at timestamp: 2017-09-17T16:59:33.098664-07:00
 :)

module namespace model_1ns
    = 'http://marklogic.com/ns1#Model_1ns-0.0.1';

import module namespace es = 'http://marklogic.com/entity-services'
    at '/MarkLogic/entity-services/entity-services.xqy';

import module namespace json = "http://marklogic.com/xdmp/json"
    at "/MarkLogic/json/json.xqy";


declare namespace cust = 'http://marklogic.com/customer';
 declare namespace sup = 'http://marklogic.com/super';



declare option xdmp:mapping 'false';


(:~
 : Extracts instance data, as a map:map, from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a Customer
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function model_1ns:extract-instance-Customer(
    $source as item()?
) as map:map
{
    let $source-node := es:init-source($source/root/cust:Customer, 'Customer')
    let $CustomerID  :=             $source-node/@CustomerID ! xs:string(.)
    let $CompanyName  :=             $source-node/cust:CompanyName ! xs:string(.)
    let $Country  :=             $source-node/cust:Country ! xs:string(.)
    let $Address  :=             $source-node/cust:Address ! xs:string(.) 
    let $instance := es:init-instance($source-node, 'Customer')
    =>es:with-namespace('http://marklogic.com/customer','cust')
    (: Comment or remove the following line to suppress attachments 
        =>es:add-attachments($source) :)

    return
    if (empty($source-node/*))
    then $instance
    else $instance
        =>   map:with('CustomerID', $CustomerID)
        =>es:optional('CompanyName', $CompanyName)
        =>es:optional('Country', $Country)
        =>es:optional('Address', $Address)
};

(:~
 : Extracts instance data, as a map:map, from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a Product
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function model_1ns:extract-instance-Product(
    $source as item()?
) as map:map
{
    let $source-node := es:init-source($source/root/Product, 'Product')
    let $ProductName  :=             $source-node/ProductName ! xs:string(.)
    let $ProductID  :=             $source-node/@ProductID ! xs:integer(.)
    let $UnitPrice  :=             $source-node/UnitPrice ! xs:double(.)
    let $SupplierID  :=             $source-node/SupplierID ! xs:integer(.)
    let $Discontinued  :=             $source-node/Discontinued ! xs:boolean(.) 
    let $instance := es:init-instance($source-node, 'Product')
    (: Comment or remove the following line to suppress attachments 
        =>es:add-attachments($source) :)

    return
    if (empty($source-node/*))
    then $instance
    else $instance
        =>es:optional('ProductName', $ProductName)
        =>   map:with('ProductID', $ProductID)
        =>es:optional('UnitPrice', $UnitPrice)
        =>es:optional('SupplierID', $SupplierID)
        =>es:optional('Discontinued', $Discontinued)
};

(:~
 : Extracts instance data, as a map:map, from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a Order
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function model_1ns:extract-instance-Order(
    $source as item()?
) as map:map
{
    let $source-node := es:init-source($source/root/Order, 'Order')
    let $OrderID  :=             $source-node/@OrderID ! xs:integer(.)
    let $hasCustomerID  :=              $source-node/CustomerID ! xs:string(.)
    let $OrderDate  :=             $source-node/OrderDate ! xs:dateTime(.)
    let $ShipAddress  :=             $source-node/ShipAddress ! xs:string(.)
    (: The following property is a local reference.  :)
    let $OrderDetails  :=             es:extract-array($source-node/OrderDetails/*, model_1ns:extract-instance-OrderDetail#1) 
    let $instance := es:init-instance($source-node, 'Order')
    (: Comment or remove the following line to suppress attachments 
        =>es:add-attachments($source) :)

    return
    if (empty($source-node/*))
    then $instance
    else $instance
        =>   map:with('OrderID', $OrderID)
        =>es:optional('hasCustomerID', $hasCustomerID)
        =>es:optional('OrderDate', $OrderDate)
        =>es:optional('ShipAddress', $ShipAddress)
        =>es:optional('OrderDetails', $OrderDetails)
};

(:~
 : Extracts instance data, as a map:map, from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a OrderDetail
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function model_1ns:extract-instance-OrderDetail(
    $source as item()?
) as map:map
{
    let $source-node := es:init-source($source, 'OrderDetail')
    let $hasProductID  :=             $source-node/hasProductID ! xs:integer(.)
    let $hasUnitPrice  :=             $source-node/hasUnitPrice ! xs:double(.)
    let $Quantity  :=             $source-node/Quantity ! xs:integer(.) 
    let $instance := es:init-instance($source-node, 'OrderDetail')
    (: Comment or remove the following line to suppress attachments 
        =>es:add-attachments($source) :)

    return
    if (empty($source-node/*))
    then $instance
    else $instance
        =>es:optional('hasProductID', $hasProductID)
        =>es:optional('hasUnitPrice', $hasUnitPrice)
        =>es:optional('Quantity', $Quantity)
};

(:~
 : Extracts instance data, as a map:map, from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a Superstore
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function model_1ns:extract-instance-Superstore(
    $source as item()?
) as map:map
{
    let $source-node := es:init-source($source/root/sup:Superstore, 'Superstore')
    let $OrdID  :=             $source-node/sup:OrderID ! xs:integer(.)
    let $CustID  :=             $source-node/sup:CustomerID ! xs:string(.)
    let $OrdDate  :=             $source-node/sup:OrderDate ! xs:dateTime(.)
    let $ShippedDate  :=             $source-node/sup:ShippedDate ! xs:dateTime(.)
    let $Product-Name  :=             $source-node/sup:ProductName ! xs:string(.)
    let $Unit-Price  :=             $source-node/sup:UnitPrice ! xs:double(.)
    let $Quant  :=             $source-node/sup:OrderQuantity ! xs:integer(.)
    let $Discount  :=             $source-node/sup:Discount ! xs:string(.)
    (: The following property is a local reference.  :)
    let $Ship-Address  :=             es:extract-array($source-node, model_1ns:extract-instance-ShipDetails#1) 
    let $instance := es:init-instance($source-node, 'Superstore')
    =>es:with-namespace('http://marklogic.com/super','sup')
    (: Comment or remove the following line to suppress attachments 
        =>es:add-attachments($source) :)

    return
    if (empty($source-node/*))
    then $instance
    else $instance
        =>   map:with('OrdID', $OrdID)
        =>es:optional('CustID', $CustID)
        =>es:optional('OrdDate', $OrdDate)
        =>es:optional('ShippedDate', $ShippedDate)
        =>es:optional('Product-Name', $Product-Name)
        =>es:optional('Unit-Price', $Unit-Price)
        =>es:optional('Quant', $Quant)
        =>es:optional('Discount', $Discount)
        =>es:optional('Ship-Address', $Ship-Address)
};

(:~
 : Extracts instance data, as a map:map, from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a ShipDetails
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function model_1ns:extract-instance-ShipDetails(
    $source as item()?
) as map:map
{
    let $source-node := es:init-source($source, 'ShipDetails')
    let $Province  :=             $source-node/Province ! xs:string(.)
    let $Region  :=             $source-node/Region ! xs:string(.)
    let $ShipMode  :=             $source-node/ShipMode ! xs:string(.)
    let $ShippingCost  :=             $source-node/ShippingCost ! xs:double(.) 
    let $instance := es:init-instance($source-node, 'ShipDetails')
    (: Comment or remove the following line to suppress attachments 
        =>es:add-attachments($source)  :)

    return
    if (empty($source-node/*))
    then $instance
    else $instance
        =>es:optional('Province', $Province)
        =>es:optional('Region', $Region)
        =>es:optional('ShipMode', $ShipMode)
        =>es:optional('ShippingCost', $ShippingCost)
};





(:~
 : Turns an entity instance into a canonical document structure.
 : Results in either a JSON document, or an XML document that conforms
 : to the entity-services schema.
 : Using this function as-is should be sufficient for most use
 : cases, and will play well with other generated artifacts.
 : @param $entity-instance A map:map instance returned from one of the extract-instance
 :    functions.
 : @param $format Either "json" or "xml". Determines output format of function
 : @return An XML element that encodes the instance.
 :)
declare function model_1ns:instance-to-canonical(

    $entity-instance as map:map,
    $instance-format as xs:string
) as node()
{

        if ($instance-format eq "json")
        then xdmp:to-json( model_1ns:canonicalize($entity-instance) )/node()
        else model_1ns:instance-to-canonical-xml($entity-instance)
};


(:~
 : helper function to turn map structure of an instance, which uses specialized
 : keys to encode metadata, into a document tree, which uses the node structure
 : to encode all type and property information.
 :)
declare private function model_1ns:canonicalize(
    $entity-instance as map:map
) as map:map
{
    json:object()
    =>map:with( map:get($entity-instance,'$type'),
                if ( map:contains($entity-instance, '$ref') )
                then fn:head( (map:get($entity-instance, '$ref'), json:object()) )
                else
                let $m := json:object()
                let $_ :=
                    for $key in map:keys($entity-instance)
                    let $instance-property := map:get($entity-instance, $key)
                    where ($key castable as xs:NCName)
                    return
                        typeswitch ($instance-property)
                        (: This branch handles embedded objects.  You can choose to prune
                           an entity's representation of extend it with lookups here. :)
                        case json:object
                            return
                                if (empty(map:keys($instance-property)))
                                then map:put($m, $key, json:object())
                                else map:put($m, $key, model_1ns:canonicalize($instance-property))
                        (: An array can also treated as multiple elements :)
                        case json:array
                            return
                                (
                                for $val at $i in json:array-values($instance-property)
                                return
                                    if ($val instance of json:object)
                                    then json:set-item-at($instance-property, $i, model_1ns:canonicalize($val))
                                    else (),
                                map:put($m, $key, $instance-property)
                                )

                        (: A sequence of values should be simply treated as multiple elements :)
                        (: TODO is this lossy? :)
                        case item()+
                            return
                                for $val in $instance-property
                                return map:put($m, $key, $val)
                        default return map:put($m, $key, $instance-property)
                return $m)
};





(:~
 : Turns an entity instance into an XML structure.
 : This out-of-the box implementation traverses a map structure
 : and turns it deterministically into an XML tree.
 : Using this function as-is should be sufficient for most use
 : cases, and will play well with other generated artifacts.
 : @param $entity-instance A map:map instance returned from one of the extract-instance
 :    functions.
 : @return An XML element that encodes the instance.
 :)
declare private function model_1ns:instance-to-canonical-xml(
    $entity-instance as map:map
) as element()
{
    (: Construct an element that is named the same as the Entity Type :)
    let $namespace := map:get($entity-instance, "$namespace")
    let $namespace-prefix := map:get($entity-instance, "$namespacePrefix")
    let $nsdecl :=
        if ($namespace) then
        namespace { $namespace-prefix } { $namespace }
        else ()
    let $type-name := map:get($entity-instance, '$type')
    let $type-qname :=
        if ($namespace)
        then fn:QName( $namespace, $namespace-prefix || ":" || $type-name)
        else $type-name
    return
        element { $type-qname }  {
            $nsdecl,
            if ( map:contains($entity-instance, '$ref') )
            then map:get($entity-instance, '$ref')
            else
                for $key in map:keys($entity-instance)
                let $instance-property := map:get($entity-instance, $key)
                let $ns-key :=
                    if ($namespace and $key castable as xs:NCName)
                    then fn:QName( $namespace, $namespace-prefix || ":" || $key)
                    else $key
                where ($key castable as xs:NCName)
                return
                    typeswitch ($instance-property)
                    (: This branch handles embedded objects.  You can choose to prune
                       an entity's representation of extend it with lookups here. :)
                    case json:object+
                        return
                            for $prop in $instance-property
                            return element { $ns-key } { model_1ns:instance-to-canonical-xml($prop) }
                    (: An array can also treated as multiple elements :)
                    case json:array
                        return
                            for $val in json:array-values($instance-property)
                            return
                                if ($val instance of json:object)
                                then element { $ns-key } {
                                    attribute datatype { 'array' },
                                    model_1ns:instance-to-canonical-xml($val)
                                }
                                else element { $ns-key } {
                                    attribute datatype { 'array' },
                                    $val }
                    (: A sequence of values should be simply treated as multiple elements :)
                    case item()+
                        return
                            for $val in $instance-property
                            return element { $ns-key } { $val }
                    default return element { $ns-key } { $instance-property }
        }
};


(:
 : Wraps a canonical instance (returned by instance-to-canonical())
 : within an envelope patterned document, along with the source
 : document, which is stored in an attachments section.
 : @param $entity-instance an instance, as returned by an extract-instance
 : function
 : @param $entity-format Either "json" or "xml", selects the output format
 : for the envelope
 : @return A document which wraps both the canonical instance and source docs.
 :)
declare function model_1ns:instance-to-envelope(
    $entity-instance as map:map,
    $envelope-format as xs:string
) as document-node()
{
    let $canonical := model_1ns:instance-to-canonical($entity-instance, $envelope-format)
    let $attachments := es:serialize-attachments($entity-instance, $envelope-format)
    return
    if ($envelope-format eq "xml")
    then
        document {
            element es:envelope {
                element es:instance {
                    element es:info {
                        element es:title { map:get($entity-instance,'$type') },
                        element es:version { '0.0.1' }
                    },
                    $canonical
                },
                $attachments
            }
        }
    else
    document {
        object-node { 'envelope' :
            object-node { 'instance' :
                object-node { 'info' :
                    object-node {
                        'title' : map:get($entity-instance,'$type'),
                        'version' : '0.0.1'
                    }
                }
                +
                $canonical
            }
            +
            $attachments
        }
    }
};


(:
 : @param $entity-instance an instance, as returned by an extract-instance
 : function
 : @return A document which wraps both the canonical instance and source docs.
 :)
declare function model_1ns:instance-to-envelope(
    $entity-instance as map:map
) as document-node()
{
    model_1ns:instance-to-envelope($entity-instance, "xml")
};

