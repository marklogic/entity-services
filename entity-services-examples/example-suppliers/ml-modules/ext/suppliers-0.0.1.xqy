xquery version "1.0-ml";


module namespace sp
    = "https://en.wikipedia.org/wiki/Suppliers_and_Parts_database#SP-0.0.1";

import module namespace es = "http://marklogic.com/entity-services"
    at "/MarkLogic/entity-services/entity-services.xqy";

import module namespace json = "http://marklogic.com/xdmp/json"
    at "/MarkLogic/json/json.xqy";

declare option xdmp:mapping "false";


(:~
 : Extracts instance data, as a map:map, from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a Supplier
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function sp:extract-instance-Supplier(
    $source as item()?
) as map:map
{
    let $source-node := es:init-source($source, 'Supplier')
    (: begin customizations here :)
    let $sid  :=             $source-node/sid ! xs:integer(.)
    let $sName  :=             $source-node/sName ! xs:string(.)
    let $status  :=             $source-node/status ! xs:integer(.)
    let $city  :=             $source-node/city ! xs:string(.)
    (: end customizations :)

    let $instance := es:init-instance($source-node, 'Supplier')

    return
    if (empty($source-node/*))
    then $instance
    else $instance
        =>   map:with('sid', $sid)
        =>   map:with('sName', $sName)
        =>   map:with('status', $status)
        =>   map:with('city', $city)
};

(:~
 : Extracts instance data, as a map:map, from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a Part
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function sp:extract-instance-Part(
    $source as item()?
) as map:map
{
    let $source-node := es:init-source($source, 'Part')
    (: begin customizations here :)
    let $pid  :=             $source-node/pid ! xs:integer(.)
    let $pName  :=             $source-node/pName ! xs:string(.)
    let $color  :=             $source-node/color ! xs:integer(.)
    let $weight  :=             $source-node/weight ! xs:decimal(.)
    let $city  :=             $source-node/city ! xs:string(.)
    (: end customizations :)

    let $instance := es:init-instance($source-node, 'Part')

    return
    if (empty($source-node/*))
    then $instance
    else $instance
        =>   map:with('pid', $pid)
        =>   map:with('pName', $pName)
        =>   map:with('color', $color)
        =>   map:with('weight', $weight)
        =>   map:with('city', $city)
};

(:~
 : Extracts instance data, as a map:map, from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a Shipment
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function sp:extract-instance-Shipment(
    $source as item()?
) as map:map
{
    let $source-node := es:init-source($source, 'Shipment')
    (: begin customizations here :)
    (: The following property is a local reference.  :)
    let $sid  :=             $source-node/sid ! sp:extract-instance-Supplier(.)
    (: The following property is a local reference.  :)
    let $pid  :=             $source-node/pid ! sp:extract-instance-Part(.)
    let $qty  :=             $source-node/qty ! xs:integer(.)
    (: end customizations :)

    let $instance := es:init-instance($source-node, 'Shipment')

    return
    if (empty($source-node/*))
    then $instance
    else $instance
        =>   map:with('sid', $sid)
        =>   map:with('pid', $pid)
        =>   map:with('qty', $qty)
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
declare function sp:instance-to-canonical(

    $entity-instance as map:map,
    $instance-format as xs:string
) as node()
{

        if ($instance-format eq "json")
        then xdmp:to-json( sp:canonicalize($entity-instance) )/node()
        else sp:instance-to-canonical-xml($entity-instance)
};


(:~
 : helper function to turn map structure of an instance, which uses specialized
 : keys to encode metadata, into a document tree, which uses the node structure
 : to encode all type and property information.
 :)
declare private function sp:canonicalize(
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
                                else map:put($m, $key, sp:canonicalize($instance-property))
                        (: An array can also treated as multiple elements :)
                        case json:array
                            return
                                (
                                for $val at $i in json:array-values($instance-property)
                                return
                                    if ($val instance of json:object)
                                    then json:set-item-at($instance-property, $i, sp:canonicalize($val))
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
declare private function sp:instance-to-canonical-xml(
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
                            return element { $ns-key } { sp:instance-to-canonical-xml($prop) }
                    (: An array can also treated as multiple elements :)
                    case json:array
                        return
                            for $val in json:array-values($instance-property)
                            return
                                if ($val instance of json:object)
                                then element { $ns-key } {
                                    attribute datatype { 'array' },
                                    sp:instance-to-canonical-xml($val)
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
declare function sp:instance-to-envelope(
    $entity-instance as map:map,
    $envelope-format as xs:string
) as document-node()
{
    let $canonical := sp:instance-to-canonical($entity-instance, $envelope-format)
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
declare function sp:instance-to-envelope(
    $entity-instance as map:map
) as document-node()
{
    sp:instance-to-envelope($entity-instance, "xml")
};



