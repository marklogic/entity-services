xquery version '1.0-ml';
module namespace tgtHasMorePropTgt-from-tgtHasMorePropSrc
    = 'http://marklogic.com/tgtHasMoreProp/tgtHasMorePropTgt-0.0.2-from-tgtHasMorePropSrc-0.0.1';

import module namespace es = 'http://marklogic.com/entity-services'
    at '/MarkLogic/entity-services/entity-services.xqy';

declare option xdmp:mapping 'false';

(:
 This module was generated by MarkLogic Entity Services.
 Its purpose is to create instances of entity types
 defined in
 tgtHasMorePropTgt, version 0.0.2
 from documents that were persisted according to model
 tgtHasMorePropSrc, version 0.0.1


 For usage and extension points, see the Entity Services Developer's Guide

 https://docs.marklogic.com/guide/entity-services

 Generated at timestamp: 2017-07-12T17:07:07.30681-07:00

 Target Model tgtHasMorePropTgt-0.0.2 Info:

 Type Customer: 
    primaryKey: CustomerID, ( in source: CustomerID )
    required: None, ( in source: None )
    range indexes: None, ( in source: None )
    word lexicons: None, ( in source: None )
 
 Type Product: 
    primaryKey: ProductName, ( in source: ProductName )
    required: None, ( in source: None )
    range indexes: None, ( in source: None )
    word lexicons: None, ( in source: None )
 
:)


(:~
 : Creates a map:map instance representation of the target
 : entity type Customer from an envelope document
 : containing a source entity instance, that is, instance data
 : of type Customer, version 0.0.1.
 : @param $source  An Entity Services envelope document (<es:envelope>)
 :  or a canonical XML instance of type Customer.
 : @return A map:map instance that holds the data for Customer,
 :  version 0.0.2.
 :)

declare function tgtHasMorePropTgt-from-tgtHasMorePropSrc:convert-instance-Customer(
    $source as node()
) as map:map
{
    let $source-node := es:init-translation-source($source, 'Customer')

    let $CustomerID := $source-node/CustomerID ! xs:string(.)
    let $CompanyName := $source-node/CompanyName ! xs:string(.)
    let $Country := $source-node/Country ! xs:string(.)
    (: The following property was missing from the source type.
       The XPath will not up-convert without intervention.  :)
    let $ContactName := $source-node/ContactName ! xs:string(.)

    return
    json:object()
    =>map:with("$type", "Customer")
    (: Copy attachments from source document to the target :)
    =>es:copy-attachments($source-node)
    (: The following lines are generated from the "Customer" entity type. :)
    =>   map:with('CustomerID',  $CustomerID)
    =>es:optional('CompanyName',  $CompanyName)
    =>es:optional('Country',  $Country)
    =>es:optional('ContactName',  $ContactName)

};
    
(:~
 : Creates a map:map instance representation of the target
 : entity type Product from an envelope document
 : containing a source entity instance, that is, instance data
 : of type Product, version 0.0.1.
 : @param $source  An Entity Services envelope document (<es:envelope>)
 :  or a canonical XML instance of type Product.
 : @return A map:map instance that holds the data for Product,
 :  version 0.0.2.
 :)

declare function tgtHasMorePropTgt-from-tgtHasMorePropSrc:convert-instance-Product(
    $source as node()
) as map:map
{
    let $source-node := es:init-translation-source($source, 'Product')

    let $ProductName := $source-node/ProductName ! xs:string(.)
    let $UnitPrice := $source-node/UnitPrice ! xs:integer(.)
    let $SupplierID := $source-node/SupplierID ! xs:integer(.)
    (: The following property was missing from the source type.
       The XPath will not up-convert without intervention.  :)
    let $Discontinued := $source-node/Discontinued ! xs:boolean(.)

    return
    json:object()
    =>map:with("$type", "Product")
    (: Copy attachments from source document to the target :)
    =>es:copy-attachments($source-node)
    (: The following lines are generated from the "Product" entity type. :)
    =>   map:with('ProductName',  $ProductName)
    =>es:optional('UnitPrice',  $UnitPrice)
    =>es:optional('SupplierID',  $SupplierID)
    =>es:optional('Discontinued',  $Discontinued)

};