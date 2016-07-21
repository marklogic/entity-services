xquery version "1.0-ml";

(: 
 : Modification History:
 :   Generated at timestamp: 2016-07-27T13:39:26.95639-07:00
 :   Persisted by cgreer
 :   Date: 2016-07-27
 :)
module namespace race = "http://grechaw.github.io/entity-types#Race-0.0.2";

import module namespace functx   = "http://www.functx.com" at "/MarkLogic/functx/functx-1.0-nodoc-2007-01.xqy";

import module namespace es = "http://marklogic.com/entity-services" 
    at "/MarkLogic/entity-services/entity-services.xqy";


declare private function race:process-duration(
    $input-value as xs:string
) as xs:dayTimeDuration?
{
    if ($input-value eq 'DNS')
    then ()
    else
        let $tokens := tokenize($input-value, ":") ! xs:decimal(.)
        return functx:dayTimeDuration((), $tokens[1], $tokens[2], $tokens[3])
};



(:~
 : Creates a map:map representation of an entity instance from some source
 : document.
 : @param $source-node  A document or node that contains data for populating a Run
 : @return A map:map instance that holds the data for this entity type.
 :)
declare function race:extract-instance-Angel-Island(
    $source-node as node()
) as map:map
{
    (: if this $source-node is a reference without an embedded object, then short circuit. :)
    json:object()
    (: This line identifies the type of this instance.  Do not change it. :)
    =>map:with('$type', 'Run')
    =>map:with('$attachments', xdmp:quote($source-node))
    =>map:with('id',                     xs:string($source-node/Bib))
    =>map:with('date',                   xs:date("2016-07-23"))
    =>es:optional('distance',            if ($source-node/Time = "DNS") then () else xs:decimal("13.1"))
    =>es:optional('distanceLabel',       xs:string("Half Marathon"))
    =>es:optional('duration',            race:process-duration($source-node/Time))
    (: The following property is a local reference.                                :)
    =>map:with('runByRunner',            race:extract-instance-Runner($source-node))
};

(:~
 : Creates a map:map representation of an entity instance from some source
 : document.
 : @param $source-node  A document or node that contains data for populating a Runner
 : @return A map:map instance that holds the data for this entity type.
 :)
declare function race:extract-instance-Runner(
    $source-node as node()
) as map:map
{
    json:object()
     (: This line identifies the type of this instance.  Do not change it. :)
     =>map:with('$type', 'Runner')
     =>   map:with('name',                   xs:string#1($source-node/node("First name")))
     =>   map:with('age',                    xs:decimal#1($source-node/Age))
     =>es:optional('gender',                 xs:string#1($source-node/Gender))

};




(:~
 : This function includes an array if there are items to put in it.
 : If there are no such items, then it returns an empty sequence.
 : TODO EA-4? move to es: module
 :)
declare function race:extract-array(
    $path-to-property as item()*,
    $fn as function(*)
) as json:array?
{
    if (empty($path-to-property))
    then ()
    else json:to-array($path-to-property ! $fn(.))
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
declare function race:instance-to-canonical-xml(
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
                        return element { $key } { race:instance-to-canonical-xml($prop) }
                (: An array can also treated as multiple elements :)
                case json:array
                    return
                        for $val in json:array-values($instance-property)
                        return
                            if ($val instance of json:object)
                            then element { $key } { race:instance-to-canonical-xml($val) }
                            else element { $key } { $val }
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
declare function race:instance-to-envelope(
    $entity-instance as map:map
) as document-node()
{
    document {
        element es:envelope {
            element es:instance {
                element es:info {
                    element es:title { map:get($entity-instance,'$type') },
                    element es:version { "0.0.2" }
                },
                race:instance-to-canonical-xml($entity-instance)
            },
            element es:attachments {
                map:get($entity-instance, "$attachments") 
            }
        }
    }
};


