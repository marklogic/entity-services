xquery version "1.0-ml";
module namespace person = "http://example.org/example-person#Person-0.0.1";

import module namespace es = "http://marklogic.com/entity-services"
at "/MarkLogic/entity-services/entity-services.xqy";

declare function person:extract-instance-Person-source-1(
        $source-node as node()
) as map:map
{
    let $id := xs:string( fn:concat( $source-node/fname, "_", $source-node/lname))
    return
        json:object()
    (: The following line identifies the type of this instance.  Do not change it.      :)
    =>map:with('$type', 'Person')
    =>   map:with('id',                     $id)
    =>   map:with('firstName',              xs:string($source-node/fname))
    =>   map:with('lastName',               xs:string($source-node/lname))
    =>   map:with('fullName',               xs:string( concat( $source-node/fname, " ", $source-node/lname)) )
};

declare function person:extract-instance-Person-source-2(
$source-node as node()
) as map:map
{
let $id := xs:string( fn:concat( $source-node/first_name, "_", $source-node/last_name))
return
json:object()
(: The following line identifies the type of this instance.  Do not change it.      :)
=>map:with('$type', 'Person')
=>   map:with('id',                     $id)
=>   map:with('firstName',              xs:string($source-node/first_name))
=>   map:with('lastName',               xs:string($source-node/last_name))
=>   map:with('fullName',               xs:string( concat( $source-node/first_name, " ", $source-node/last_name)) )
};


declare function person:friend-ref(
    $source-node as node()
) as map:map
{
    json:object()
    =>map:with('$type', 'Person')
    =>map:with('$ref', $source-node/Person/id/text())
};

(: a normalizing transform, changed signature to return a sequence :)
declare function person:extract-instance-Person-source-3(
$source-node as node()
) as map:map+
{
let $friends := $source-node/Person/friendOf[Person/id] ! person:extract-instance-Person-source-3(.)
return ($friends ,
json:object()
(: The following line identifies the type of this instance.  Do not change it.      :)
=>map:with('$type', 'Person')
=>   map:with('id',                     xs:string($source-node/Person/id))
=>   map:with('firstName',              xs:string($source-node/Person/firstName))
=>   map:with('lastName',               xs:string($source-node/Person/lastName))
=>   map:with('fullName',               xs:string($source-node/Person/fullName))
=>es:optional('friends',                $source-node/Person/friendOf ! person:friend-ref(.)))
};

(: a normalizing transform, changed signature to return a sequence :)
declare function person:extract-instance-Person-source-4(
$source-node as node()
) as map:map+
{
let $friends :=
    for $friend in $source-node/friendOf return
    (json:object()
        =>map:with("$type", "Person")
        =>map:with("id", $friend)
        =>map:with("firstName", fn:substring-before($friend, " "))
        =>map:with("lastName",  fn:substring-after($friend, " "))
        =>map:with("fullName",  $friend)
    )
return
json:object()
(: The following line identifies the type of this instance.  Do not change it.      :)
=>map:with('$type', 'Person')
=>   map:with('id',                     xs:string($source-node/name))
=>   map:with("firstName",              fn:substring-before($source-node/name, " "))
=>   map:with("lastName",               fn:substring-after($source-node/name, " "))
=>   map:with('fullName',               xs:string($source-node/name))
=>es:optional('friends',                json:to-array($friends))
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
declare function person:instance-to-canonical-xml(
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
return element { $key } { person:instance-to-canonical-xml($prop) }
(: An array can also treated as multiple elements :)
case json:array
return
for $val in json:array-values($instance-property)
return
if ($val instance of json:object)
then element { $key } { person:instance-to-canonical-xml($val) }
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
declare function person:instance-to-envelope(
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
person:instance-to-canonical-xml($entity-instance)
},
element es:attachments {
map:get($entity-instance, "$attachments")
}
}
}
};
