xquery version '1.0-ml';
module namespace srcRefDatatypeTgt-from-srcRefDatatypeSrc
    = 'http://marklogic.com/srcRefDatatype/srcRefDatatypeTgt-0.0.2-from-srcRefDatatypeSrc-0.0.1';

import module namespace es = 'http://marklogic.com/entity-services'
    at '/MarkLogic/entity-services/entity-services.xqy';

declare option xdmp:mapping 'false';

(:
 This module was generated by MarkLogic Entity Services.
 Its purpose is to create instances of entity types
 defined in
 srcRefDatatypeTgt, version 0.0.2
 from documents that were persisted according to model
 srcRefDatatypeSrc, version 0.0.1


 For usage and extension points, see the Entity Services Developer's Guide

 https://docs.marklogic.com/guide/entity-services

 Generated at timestamp: 2017-07-12T17:10:55.25613-07:00

 Target Model srcRefDatatypeTgt-0.0.2 Info:

 Type Customer: 
    primaryKey: CustomerID, ( in source: CustomerID )
    required: None, ( in source: None )
    range indexes: None, ( in source: None )
    word lexicons: None, ( in source: None )
 
 Type Product: 
    primaryKey: SupplierID, ( in source: SupplierID )
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

declare function srcRefDatatypeTgt-from-srcRefDatatypeSrc:convert-instance-Customer(
    $source as node()
) as map:map
{
    let $source-node := es:init-translation-source($source, 'Customer')

    let $CustomerID := $source-node/CustomerID ! xs:integer(.)
    let $CompanyName := $source-node/CompanyName ! xs:string(.)
    let $Country := $source-node/Country ! xs:string(.)
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

declare function srcRefDatatypeTgt-from-srcRefDatatypeSrc:convert-instance-Product(
    $source as node()
) as map:map
{
    let $source-node := es:init-translation-source($source, 'Product')

    let $extract-reference-Customer := 
        function($path) { 
         if ($path/*)
         then srcRefDatatypeTgt-from-srcRefDatatypeSrc:convert-instance-Customer($path)
         else es:init-instance($path, 'Customer')
         }

    let $CustomerID := $source-node/CustomerID/* ! $extract-reference-Customer(.)
    let $UnitPrice := $source-node/UnitPrice ! xs:integer(.)
    let $SupplierID := $source-node/SupplierID ! xs:integer(.)
    let $Discontinued := $source-node/Discontinued ! xs:boolean(.)

    return
    json:object()
    =>map:with("$type", "Product")
    (: Copy attachments from source document to the target :)
    =>es:copy-attachments($source-node)
    (: The following lines are generated from the "Product" entity type. :)
    =>es:optional('CustomerID',  $CustomerID)
    =>es:optional('UnitPrice',  $UnitPrice)
    =>   map:with('SupplierID',  $SupplierID)
    =>es:optional('Discontinued',  $Discontinued)

};