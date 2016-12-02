xquery version "1.0-ml";
module namespace srcRefDatatypeTgt-from-srcRefDatatypeSrc
    = "http://marklogic.com/srcRefDatatype/srcRefDatatypeTgt-0.0.2-from-srcRefDatatypeSrc-0.0.1";

import module namespace es = "http://marklogic.com/entity-services"
    at "/MarkLogic/entity-services/entity-services.xqy";

declare option xdmp:mapping "false";

(:
 This module was generated by MarkLogic Entity Services.
 Its purpose is to create instances of entity types
 defined in
 srcRefDatatypeTgt, version 0.0.2
 from documents that were persisted according to model
 srcRefDatatypeSrc, version 0.0.1

 Modification History:
 Generated at timestamp: 2016-12-02T14:09:00.627701-08:00
 Persisted by AUTHOR
 Date: DATE

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
    let $source-node := srcRefDatatypeTgt-from-srcRefDatatypeSrc:init-source($source, 'Customer')

    return
    json:object()
    (: If the source is an envelope or part of an envelope document,
     : copies attachments to the target
     :)
    =>srcRefDatatypeTgt-from-srcRefDatatypeSrc:copy-attachments($source-node)
    (: The following line identifies the type of this instance.  Do not change it. :)
    =>map:with("$type", "Customer")
    (: The following lines are generated from the "Customer" entity type. :)    =>   map:with('CustomerID',             xs:integer($source-node/CustomerID))
    =>es:optional('CompanyName',            xs:string($source-node/CompanyName))
    =>es:optional('Country',                xs:string($source-node/Country))
    =>es:optional('ContactName',            xs:string($source-node/ContactName))

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
    let $source-node := srcRefDatatypeTgt-from-srcRefDatatypeSrc:init-source($source, 'Product')

    let $extract-reference-Customer := 
        function($path) { 
         if ($path/*)
         then srcRefDatatypeTgt-from-srcRefDatatypeSrc:convert-instance-Customer($path)
         else 
           json:object()
           =>map:with('$type', 'Customer')
           =>map:with('$ref', $path/text() ) 
        }    return
    json:object()
    (: If the source is an envelope or part of an envelope document,
     : copies attachments to the target
     :)
    =>srcRefDatatypeTgt-from-srcRefDatatypeSrc:copy-attachments($source-node)
    (: The following line identifies the type of this instance.  Do not change it. :)
    =>map:with("$type", "Product")
    (: The following lines are generated from the "Product" entity type. :)    =>es:optional('CustomerID',             $extract-reference-Customer($source-node/CustomerID/*))
    =>es:optional('UnitPrice',              xs:integer($source-node/UnitPrice))
    =>   map:with('SupplierID',             xs:integer($source-node/SupplierID))
    =>es:optional('Discontinued',           xs:boolean($source-node/Discontinued))

};
    


declare private function srcRefDatatypeTgt-from-srcRefDatatypeSrc:init-source(
    $source as node()*,
    $entity-type-name as xs:string
) as node()*
{
    if ( ($source//es:instance/element()[node-name(.) eq xs:QName($entity-type-name)]))
    then $source//es:instance/element()[node-name(.) eq xs:QName($entity-type-name)]
    else $source
};


declare private function srcRefDatatypeTgt-from-srcRefDatatypeSrc:copy-attachments(
    $instance as json:object,
    $source as node()*
) as json:object
{
    $instance
    =>es:optional('$attachments',
        $source ! fn:root(.)/es:envelope/es:attachments/node())
};