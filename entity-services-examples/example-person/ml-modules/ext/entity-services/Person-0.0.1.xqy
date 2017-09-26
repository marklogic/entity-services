xquery version '1.0-ml';
(:
 For usage and extension points, see the Entity Services Developer's Guide

 https://docs.marklogic.com/guide/entity-services

 After modifying this file, put it in your project for deployment to the modules
 database of your application, and check it into your source control system.

 Generated at timestamp: 2017-09-01T15:49:26.593-07:00
 :)

module namespace person
    = 'http://example.org/example-person/Person-0.0.1';

import module namespace es = 'http://marklogic.com/entity-services'
    at '/MarkLogic/entity-services/entity-services.xqy';

import module namespace json = "http://marklogic.com/xdmp/json"
    at "/MarkLogic/json/json.xqy";


declare namespace p = 'http://example.org/example-person';



declare option xdmp:mapping 'false';


(:~
 : Extracts instance data, as a map:map, from some source document.
 : @param $source-node  A document or node that contains
 :   data for populating a Person
 : @return A map:map instance with extracted data and
 :   metadata about the instance.
 :)
declare function person:extract-instance-Person-source-1(
    $source as item()?
) as map:map
{
    let $source-node := es:init-source($source, 'Person')
    let $id := xs:string( fn:concat( $source-node/fname, "_", $source-node/lname))
    let $firstName := xs:string($source-node/fname)
    let $lastName := xs:string($source-node/lname)
    let $fullName := concat( $firstName, " ", $lastName)

    let $instance := es:init-instance($source-node, 'Person')
    =>es:with-namespace('http://example.org/example-person', 'p')
    return
        $instance
        (: The following line identifies the type of this instance.  Do not change it.      :)
        =>map:with('$type', 'Person')
        =>   map:with('id',                     $id)
        =>   map:with('firstName',              $firstName)
        =>   map:with('lastName',               $lastName)
        =>   map:with('fullName',               $fullName)
};

declare function person:extract-instance-Person-source-2(
    $source as item()?
) as map:map
{
    let $source-node := es:init-source($source, 'Person')
    let $id := xs:string( fn:concat( $source-node/first_name, "_", $source-node/last_name))
    let $firstName := xs:string($source-node/first_name)
    let $lastName := xs:string($source-node/last_name)
    let $fullName := xs:string( concat( $firstName, " ", $lastName) )
    let $instance := es:init-instance($source-node, 'Person')
    =>es:with-namespace('http://example.org/example-person', 'p')
    return
        $instance
        (: The following line identifies the type of this instance.  Do not change it.      :)
        =>map:with('$type', 'Person')
        =>   map:with('id',                     $id)
        =>   map:with('firstName',              $firstName)
        =>   map:with('lastName',               $lastName)
        =>   map:with('fullName',               $fullName)
};


declare function person:friend-ref(
    $source-node as node()
) as map:map
{
    json:object()
    =>es:with-namespace('http://example.org/example-person', 'p')
    =>map:with('$type', 'Person')
    =>map:with('$ref', $source-node/Person/id/text())
};

(: a normalizing transform, changed signature to return a sequence :)
declare function person:extract-instance-Person-source-3(
    $source as item()?
) as map:map+
{
    let $source-node := es:init-source($source, 'Person')
    let $id :=   xs:string($source-node/id)
    let $firstName :=  xs:string($source-node/firstName)
    let $lastName :=   xs:string($source-node/lastName)
    let $fullName :=   xs:string($source-node/fullName)
    let $friends := $source-node/friendOf[id] ! person:extract-instance-Person-source-3(.)
    return ($friends ,
    let $instance := es:init-instance($source-node, 'person')
    =>es:with-namespace('http://example.org/example-person', 'p')
    return
        $instance
        (: The following line identifies the type of this instance.  Do not change it.      :)
        =>map:with('$type', 'Person')
        =>   map:with('id',                     $id)
        =>   map:with('firstName',              $firstName)
        =>   map:with('lastName',               $lastName)
        =>   map:with('fullName',               $fullName)
        =>es:optional('friends',                $source-node/friendOf ! person:friend-ref(.)))
};

(: a normalizing transform, changed signature to return a sequence :)
declare function person:extract-instance-Person-source-4(
    $source as item()?
) as map:map+
{
    let $source-node := es:init-source($source, 'Person')
    let $friends :=
        for $friend in $source-node/friendOf return
            (
                let $instance := es:init-instance($source-node, 'person')
                =>es:with-namespace('http://example.org/example-person', 'p')
                return
                    $instance
                    =>map:with("$type", "Person")
                    =>map:with("id", $friend)
                    =>map:with("firstName", fn:substring-before($friend, " "))
                    =>map:with("lastName",  fn:substring-after($friend, " "))
                    =>map:with("fullName",  $friend)
            )
    return
        json:object()
        =>es:with-namespace('http://example.org/example-person', 'p')
        (: The following line identifies the type of this instance.  Do not change it.      :)
        =>map:with('$type', 'Person')
        =>   map:with('id',                     xs:string($source-node/name))
        =>   map:with("firstName",              fn:substring-before($source-node/name, " "))
        =>   map:with("lastName",               fn:substring-after($source-node/name, " "))
        =>   map:with('fullName',               xs:string($source-node/name))
        =>es:optional('friends',                json:to-array($friends))
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
declare function person:instance-to-canonical(

    $entity-instance as map:map,
    $instance-format as xs:string
) as node()
{

    if ($instance-format eq "json")
    then xdmp:to-json( person:canonicalize($entity-instance) )/node()
    else person:instance-to-canonical-xml($entity-instance)
};


(:~
 : helper function to turn map structure of an instance, which uses specialized
 : keys to encode metadata, into a document tree, which uses the node structure
 : to encode all type and property information.
 :)
declare private function person:canonicalize(
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
                                else map:put($m, $key, person:canonicalize($instance-property))
                    (: An array can also treated as multiple elements :)
                        case json:array
                            return
                                (
                                    for $val at $i in json:array-values($instance-property)
                                    return
                                        if ($val instance of json:object)
                                        then json:set-item-at($instance-property, $i, person:canonicalize($val))
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
declare private function person:instance-to-canonical-xml(
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
                                return element { $ns-key } { person:instance-to-canonical-xml($prop) }
                    (: An array can also treated as multiple elements :)
                        case json:array
                            return
                                for $val in json:array-values($instance-property)
                                return
                                    if ($val instance of json:object)
                                    then element { $ns-key } {
                                        attribute datatype { 'array' },
                                        person:instance-to-canonical-xml($val)
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
declare function person:instance-to-envelope(
    $entity-instance as map:map,
    $envelope-format as xs:string
) as document-node()
{
    let $canonical := person:instance-to-canonical($entity-instance, $envelope-format)
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
declare function person:instance-to-envelope(
    $entity-instance as map:map
) as document-node()
{
    person:instance-to-envelope($entity-instance, "xml")
};


