xquery version "1.0-ml";


module namespace sp
    = "https://en.wikipedia.org/wiki/Suppliers_and_Parts_database#SP-0.0.1";

import module namespace es = "http://marklogic.com/entity-services"
    at "/MarkLogic/entity-services/entity-services.xqy";

declare option xdmp:mapping "false";


(:
  extract-instance-{entity-type} functions

  These functions together take a source document and create a nested
  map structure from it.
  The resulting map is used by instance-to-canonical-xml to create documents
  in the database.

  It is expected that an implementer will edit at least XPath expressions in
  the extraction functions.  It is less likely that you will want to edit
  the instance-to-canonical-xml or envelope functions.
 :)


(:~
 : Creates a map:map instance from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a Supplier
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function sp:extract-instance-Supplier(
    $source as node()?
) as map:map
{
    let $source-node :=
        if ( ($source instance of document-node())
            or (exists($source/Supplier)))
        then $source/node()
        else $source
    let $instance := json:object()
    (: Add type information to the entity instance (required, do not change) :)
        =>map:with('$type', 'Supplier')
    return
    if (empty($source-node/*))
    then $instance=>map:with('$ref', $source-node/text())
    else
    $instance
    =>   map:with('sid',                    xs:integer($source-node/sid))
    =>   map:with('sName',                  xs:string($source-node/sName))
    =>   map:with('status',                 xs:integer($source-node/status))
    =>   map:with('city',                   xs:string($source-node/city))
};

(:~
 : Creates a map:map instance from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a Part
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function sp:extract-instance-Part(
    $source as node()?
) as map:map
{
    let $source-node :=
        if ( ($source instance of document-node())
        or (exists($source/Part)))
        then $source/node()
        else $source
    let $instance := json:object()
        =>map:with('$type', 'Part')
    return
    if (empty($source-node/*))
    then $instance=>map:with('$ref', $source-node/text())
    else
    $instance
    =>   map:with('pid',                    xs:integer($source-node/pid))
    =>   map:with('pName',                  xs:string($source-node/pName))
    =>   map:with('color',                  xs:integer($source-node/color))
    =>   map:with('weight',                 xs:decimal($source-node/weight))
    =>   map:with('city',                   xs:string($source-node/city))
};

(:~
 : Creates a map:map instance from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a Shipment
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function sp:extract-instance-Shipment(
    $source-node as node()?
) as map:map
{
    let $instance := json:object()
        =>map:with('$type', 'Shipment')
    return
    if (empty($source-node/*))
    then $instance=>map:with('$ref', $source-node/text())
    else
    let $instance := json:object()
        =>map:with('$type', 'Shipment')
    return

    $instance
    (: The following property is a local reference.  :)
    =>es:optional('sid',                    sp:extract-instance-Supplier($source-node/sid))
    (: The following property is a local reference.  :)
    =>es:optional('pid',                    sp:extract-instance-Part($source-node/pid))
    =>es:optional('qty',                    xs:integer($source-node/qty))
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
declare function sp:instance-to-canonical-xml(
    $entity-instance as map:map
) as element()
{
    (: Construct an element that is named the same as the Entity Type :)
    element { map:get($entity-instance, "$type") }  {
        if ( map:contains($entity-instance, "$ref") )
        then map:get($entity-instance, "$ref")
        else
            for $key in map:keys($entity-instance)
            let $instance-property := map:get($entity-instance, $key)
            where ($key castable as xs:NCName and $key ne "$type")
            return
                typeswitch ($instance-property)
                (: This branch handles embedded objects.  You can choose to prune
                   an entity's representation of extend it with lookups here. :)
                case json:object+
                    return
                        for $prop in $instance-property
                        return element { $key } { sp:instance-to-canonical-xml($prop) }
                (: An array can also treated as multiple elements :)
                case json:array
                    return
                        for $val in json:array-values($instance-property)
                        return
                            if ($val instance of json:object)
                            then element { $key } {
                                attribute datatype { "array" },
                                sp:instance-to-canonical-xml($val) }
                            else element { $key } {
                                attribute datatype { "array" },
                                $val }
                (: A sequence of values should be simply treated as multiple elements :)
                case item()+
                    return
                        for $val in $instance-property
                        return element { $key } { $val }
                default return element { $key } { $instance-property }
    }
};


(:
 : Wraps a canonical instance (returned by instance-to-canonical-xml())
 : within an envelope patterned document, along with the source
 : document, which is stored in an attachments section.
 : @param $entity-instance an instance, as returned by an extract-instance
 : function
 : @return A document which wraps both the canonical instance and source docs.
 :)
declare function sp:instance-to-envelope(
    $entity-instance as map:map
) as document-node()
{
    document {
        element es:envelope {
            element es:instance {
                element es:info {
                    element es:title { map:get($entity-instance,'$type') },
                    element es:version { "0.0.1" }
                },
                sp:instance-to-canonical-xml($entity-instance)
            },
            element es:attachments {
                map:get($entity-instance, "$attachments")
            }
        }
    }
};

