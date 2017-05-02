xquery version "1.0-ml";
module namespace m-next = "http://marklogic.com/example/Model-next";
import module namespace es = "http://marklogic.com/entity-services"
    at "/MarkLogic/entity-services/entity-services.xqy";

declare function m-next:extract-instance-Person(
    $source as node()?
) as map:map
{
    let $source-node := m-next:init-source($source, 'Person')

    let $instance := json:object()=>m-next:add-attachments($source-node, $source)
        =>map:with('$type', 'Person')
    return
        $instance
        =>   map:with('id', xs:long($source/id))
        =>   map:with('firstName', xs:string($source/firstName))
        =>   es:optional('fullName', xs:string($source/fullName))
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
declare function m-next:instance-to-canonical-xml(
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
            where ($key castable as xs:NCName)
            return
            typeswitch ($instance-property)
                case json:object+ return
                    for $prop in $instance-property
                    return element { $key } { m-next:instance-to-canonical-xml($prop) }
                (: An array can also treated as multiple elements :)
                case json:array return
                    for $val in json:array-values($instance-property)
                    return
                        if ($val instance of json:object)
                        then element { $key } {
                            attribute datatype { "array" },
                            m-next:instance-to-canonical-xml($val)
                        }
                        else element { $key } {
                            attribute datatype { "array" },
                            $val
                            }
                (: A sequence of values should be simply treated as multiple elements :)
                case item()+ return
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
declare function m-next:instance-to-envelope(
    $entity-instance as map:map
) as document-node()
{
    document {
        element es:envelope {
        element es:instance {
        element es:info {
        element es:title { map:get($entity-instance,'$type') },
        element es:version { "next" }
        },
        m-next:instance-to-canonical-xml($entity-instance)
        },
        element es:attachments {
        map:get($entity-instance, "$attachments")
        }
    }
}
};


declare private function m-next:init-source(
    $source as node()*,
    $entity-type-name as xs:string
) as node()*
{
    if ( ($source instance of document-node())
    or (exists ($source/element()[fn:node-name(.) eq xs:QName($entity-type-name)] )))
    then $source/node()
    else $source
};


declare private function m-next:add-attachments(
    $instance as json:object,
    $source-node as node()*,
    $source as node()*
) as json:object
{
    $instance
    =>map:with('$attachments',
    typeswitch($source-node)
    case object-node() return xdmp:quote($source)
    case array-node() return xdmp:quote($source)
    default return $source)
};
