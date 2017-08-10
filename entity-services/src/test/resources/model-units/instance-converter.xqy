xquery version '1.0-ml';

(:
 This module was generated by MarkLogic Entity Services.
 The source model was et-required-0.0.1

 For usage and extension points, see the Entity Services Developer's Guide

 https://docs.marklogic.com/guide/entity-services

 After modifying this file, put it in your project for deployment to the modules
 database of your application, and check it into your source control system.

 Generated at timestamp: 2017-08-10T17:49:34.347263Z
 :)

module namespace et-required
    = 'http://baloo#et-required-0.0.1';

import module namespace es = 'http://marklogic.com/entity-services'
    at '/MarkLogic/entity-services/entity-services.xqy';


        

declare option xdmp:mapping 'false';


(:~
 : Extracts instance data, as a map:map, from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a ETOne
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function et-required:extract-instance-ETOne(
    $source as item()?
) as map:map
{
    let $source-node := es:init-source($source, 'ETOne')
    (: begin customizations here :)
    let $a  :=             $source-node/a ! xs:integer(.)
    let $b  :=             $source-node/b ! xs:string(.)
    let $c  :=             $source-node/c ! xs:date(.)
    (: end customizations :)

    let $instance := es:init-instance($source-node, 'ETOne')
    (: Comment or remove the following line to suppress attachments :)
        =>es:add-attachments($source)

    return
    if (empty($source-node/*)) 
    then $instance
    else $instance
        =>   map:with('a', $a)
        =>   map:with('b', $b)
        =>es:optional('c', $c)
};





(:~
 : Turns an entity instance into a JSON structure.
 : This out-of-the box implementation traverses a map structure
 : and turns it deterministically into a JSON tree.
 : Using this function as-is should be sufficient for most use
 : cases, and will play well with other generated artifacts.
 : @param $entity-instance A map:map instance returned from one of the extract-instance
 :    functions.
 : @return An XML element that encodes the instance.
 :)
declare function et-required:instance-to-canonical-json(

    $entity-instance as map:map
) as object-node()
{
    xdmp:to-json( et-required:canonicalize($entity-instance) )/node()
};


declare function et-required:canonicalize(
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
                                else
                                    for $prop in $instance-property
                                    return map:put($m, $key, et-required:canonicalize($prop))
                        (: An array can also treated as multiple elements :)
                        case json:array
                            return
                                (
                                for $val at $i in json:array-values($instance-property)
                                return
                                    if ($val instance of json:object)
                                    then json:set-item-at($instance-property, $i, et-required:canonicalize($val))
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
declare function et-required:instance-to-canonical-xml(
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
                            return element { $ns-key } { et-required:instance-to-canonical-xml($prop) }
                    (: An array can also treated as multiple elements :)
                    case json:array
                        return
                            for $val in json:array-values($instance-property)
                            return
                                if ($val instance of json:object)
                                then element { $ns-key } {
                                    attribute datatype { 'array' },
                                    et-required:instance-to-canonical-xml($val)
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
 : Wraps a canonical instance (returned by instance-to-canonical-xml())
 : within an envelope patterned document, along with the source
 : document, which is stored in an attachments section.
 : @param $entity-instance an instance, as returned by an extract-instance
 : function
 : @return A document which wraps both the canonical instance and source docs.
 :)
declare function et-required:instance-to-xml-envelope(
    $entity-instance as map:map
) as document-node()
{
    document {
        element es:envelope {
            element es:instance {
                element es:info {
                    element es:title { map:get($entity-instance,'$type') },
                    element es:version { '0.0.1' }
                },
                et-required:instance-to-canonical-xml($entity-instance)
            },
            es:serialize-attachments($entity-instance, "xml")
        }
    }
};


(:
 : @param $entity-instance an instance, as returned by an extract-instance
 : function
 : @return A document which wraps both the canonical instance and source docs.
 :)
declare function et-required:instance-to-envelope(
    $entity-instance as map:map
) as document-node()
{
    et-required:instance-to-xml-envelope($entity-instance)
};



(:
 : Wraps a canonical instance (returned by instance-to-canonical-json())
 : within an envelope patterned document, along with the source
 : document, which is stored in an attachments section.
 : @param $entity-instance an instance, as returned by an extract-instance
 : function
 : @return A document which wraps both the canonical instance and source docs.
 :)
declare function et-required:instance-to-json-envelope(
    $entity-instance as map:map
) as document-node()
{
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
                et-required:instance-to-canonical-json($entity-instance)
            }
            +
            es:serialize-attachments($entity-instance, "json")
        }
    }
};
