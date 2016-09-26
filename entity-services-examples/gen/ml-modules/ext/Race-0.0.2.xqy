xquery version "1.0-ml";

(: This module was generated by MarkLogic Entity Services.                          :)
(: The source entity type document was Race-0.0.2                                   :)
(:                                                                                  :)
(: To use this module, examine how you wish to extract data from sources,           :)
(: and modify the various extract-instance-{X} functions to                         :)
(: create the instances you wish.                                                   :)
(:                                                                                  :)
(: You may wish to/need to alter                                                    :)
(: 1.  values.  For example, creating duration values from decimal months.          :)
(: 2.  references.  This conversion module assumes you want to denormalize          :)
(:     instances when storing them in documents.  You may choose to remove          :)
(:     code that denormalizes, and just include reference values in your instances  :)
(:     instead.                                                                     :)
(: 3.  Source XPath expressions.  The data coming into the extract-instance-{X}     :)
(:     functions will probably not be exactly what this module predicts.            :)
(:                                                                                  :)
(: After modifying this file, put it in your project for deployment to the modules  :)
(: database of your application, and check it into your source control system.      :)
(:                                                                                  :)
(: Modification History:                                                            :)
(: Generated at timestamp: 2016-08-29T17:10:26.288471-07:00                         :)
(:   Persisted by AUTHOR                                                            :)
(:   Date: DATE                                                                     :)
module namespace race 
    = "http://grechaw.github.io/entity-types#Race-0.0.2";

import module namespace es = "http://marklogic.com/entity-services" 
    at "/MarkLogic/entity-services/entity-services.xqy";



(:  extract-instance-{entity-type} functions                                        :)
(:                                                                                  :)
(:  These functions take together take a source document and create a nested        :)
(:  map structure from it.                                                          :)
(:  The resulting map is used by instance-to-canonical-xml to create documents      :)
(:  in the database.                                                                :)
(:                                                                                  :)
(:  It is expected that an implementer will edit at least XPath expressions in      :)
(:  the extraction functions.  It is less likely that you will want to edit         :)
(:  the instance-to-canonical-xml or envelope functions.                            :)

(:~
 : Creates a map:map instance from some source document.
 : @param $source-node  A document or node that contains 
 :   data for populating a Race
 : @return A map:map instance with extracted data and 
 :   metadata about the instance.
 :)
declare function race:extract-instance-Race(
    $source-node as node()
) as map:map
{
(: if this $source-node is a reference to another instance, then short circuit.     :)
    if (empty($source-node/element()/*))
    then json:object()
        =>map:with('$type', 'Race')
        =>map:with('$ref', $source-node/Race/text())
        =>map:with('$attachments', $source-node)
(: otherwise populate this instance :)
    else json:object()
(: The following line identifies the type of this instance.  Do not change it.      :)
        =>map:with('$type', 'Race')
(: The following line adds the original source document as an attachment.           :)
        =>map:with('$attachments', $source-node)
(: If the source document is JSON, remove the previous line and replace it with     :)
(: =>map:with('$attachments', xdmp:quote($source-node))                             :)
(: because this implementation uses an XML envelope.                                :)
(:                                                                                  :)
(: The following code populates the properties of the                               :)
(: 'Race' entity type                                                               :)
(: Ensure that all of the property paths are correct for your source data.          :)
(: The general pattern is                                                           :)
(: =>map:with('keyName', casting-function($source-node/path/to/data))               :)
(: but you may also wish to convert values                                          :)
(: =>map:with('dateKeyName',                                                        :)
(:       xdmp:parse-dateTime("[Y0001]-[M01]-[D01]T[h01]:[m01]:[s01].[f1][Z]",       :)
(:       $source-node/path/to/data/in/the/source))                                  :)
(: You can also implement lookup functions,                                         :)
(: =>map:with('lookupKey',                                                          :)
(:       cts:search( collection('customers'),                                       :)
(:           string($source-node/path/to/lookup/key))/id                            :)
(: or populate the instance with constants.                                         :)
(: =>map:with('constantValue', 10)                                                  :)
(: The output of this function should structurally match the output of              :)
(: es:model-get-test-instances($model)                                              :)
(:                                                                                  :)
 =>es:optional('id',                     sem:iri($source-node/Race/id))
 =>   map:with('name',                   xs:string($source-node/Race/name))
 (: The following property assigment comes from an external reference.               :)
 (: Its generated value probably requires developer attention.                       :)
 =>es:optional('raceCategory',           function($path) { json:object()=>map:with('$type', 'Running')=>map:with('$ref', $path/Running/text() ) }($source-node/Race/raceCategory))
 (: The following property is a local reference.                                     :)
 =>es:optional('comprisedOfRuns',        race:extract-array($source-node/Race/comprisedOfRuns, race:extract-instance-Run#1))
 (: The following property is a local reference.                                     :)
 =>es:optional('wonByRunner',            race:extract-instance-Runner($source-node/Race/wonByRunner))
 =>   map:with('courseLength',           xs:decimal($source-node/Race/courseLength))

};

(:~
 : Creates a map:map instance from some source document.
 : @param $source-node  A document or node that contains 
 :   data for populating a Run
 : @return A map:map instance with extracted data and 
 :   metadata about the instance.
 :)
declare function race:extract-instance-Run(
    $source-node as node()
) as map:map
{
(: if this $source-node is a reference to another instance, then short circuit.     :)
    if (empty($source-node/element()/*))
    then json:object()
        =>map:with('$type', 'Run')
        =>map:with('$ref', $source-node/Run/text())
        =>map:with('$attachments', $source-node)
(: otherwise populate this instance :)
    else json:object()
(: The following line identifies the type of this instance.  Do not change it.      :)
        =>map:with('$type', 'Run')
(: The following line adds the original source document as an attachment.           :)
        =>map:with('$attachments', $source-node)
(: If the source document is JSON, remove the previous line and replace it with     :)
(: =>map:with('$attachments', xdmp:quote($source-node))                             :)
(: because this implementation uses an XML envelope.                                :)
(:                                                                                  :)
(: The following code populates the properties of the                               :)
(: 'Run' entity type                                                                :)
(: Ensure that all of the property paths are correct for your source data.          :)
(: The general pattern is                                                           :)
(: =>map:with('keyName', casting-function($source-node/path/to/data))               :)
(: but you may also wish to convert values                                          :)
(: =>map:with('dateKeyName',                                                        :)
(:       xdmp:parse-dateTime("[Y0001]-[M01]-[D01]T[h01]:[m01]:[s01].[f1][Z]",       :)
(:       $source-node/path/to/data/in/the/source))                                  :)
(: You can also implement lookup functions,                                         :)
(: =>map:with('lookupKey',                                                          :)
(:       cts:search( collection('customers'),                                       :)
(:           string($source-node/path/to/lookup/key))/id                            :)
(: or populate the instance with constants.                                         :)
(: =>map:with('constantValue', 10)                                                  :)
(: The output of this function should structurally match the output of              :)
(: es:model-get-test-instances($model)                                              :)
(:                                                                                  :)
 =>   map:with('id',                     sem:iri($source-node/Run/id))
 =>   map:with('date',                   xs:date($source-node/Run/date))
 =>   map:with('distance',               xs:decimal($source-node/Run/distance))
 =>es:optional('distanceLabel',          xs:string($source-node/Run/distanceLabel))
 =>es:optional('duration',               xs:dayTimeDuration($source-node/Run/duration))
 (: The following property is a local reference.                                     :)
 =>   map:with('runByRunner',            race:extract-instance-Runner($source-node/Run/runByRunner))

};

(:~
 : Creates a map:map instance from some source document.
 : @param $source-node  A document or node that contains 
 :   data for populating a Runner
 : @return A map:map instance with extracted data and 
 :   metadata about the instance.
 :)
declare function race:extract-instance-Runner(
    $source-node as node()
) as map:map
{
(: if this $source-node is a reference to another instance, then short circuit.     :)
    if (empty($source-node/element()/*))
    then json:object()
        =>map:with('$type', 'Runner')
        =>map:with('$ref', $source-node/Runner/text())
        =>map:with('$attachments', $source-node)
(: otherwise populate this instance :)
    else json:object()
(: The following line identifies the type of this instance.  Do not change it.      :)
        =>map:with('$type', 'Runner')
(: The following line adds the original source document as an attachment.           :)
        =>map:with('$attachments', $source-node)
(: If the source document is JSON, remove the previous line and replace it with     :)
(: =>map:with('$attachments', xdmp:quote($source-node))                             :)
(: because this implementation uses an XML envelope.                                :)
(:                                                                                  :)
(: The following code populates the properties of the                               :)
(: 'Runner' entity type                                                             :)
(: Ensure that all of the property paths are correct for your source data.          :)
(: The general pattern is                                                           :)
(: =>map:with('keyName', casting-function($source-node/path/to/data))               :)
(: but you may also wish to convert values                                          :)
(: =>map:with('dateKeyName',                                                        :)
(:       xdmp:parse-dateTime("[Y0001]-[M01]-[D01]T[h01]:[m01]:[s01].[f1][Z]",       :)
(:       $source-node/path/to/data/in/the/source))                                  :)
(: You can also implement lookup functions,                                         :)
(: =>map:with('lookupKey',                                                          :)
(:       cts:search( collection('customers'),                                       :)
(:           string($source-node/path/to/lookup/key))/id                            :)
(: or populate the instance with constants.                                         :)
(: =>map:with('constantValue', 10)                                                  :)
(: The output of this function should structurally match the output of              :)
(: es:model-get-test-instances($model)                                              :)
(:                                                                                  :)
 =>es:optional('id',                     sem:iri($source-node/Runner/id))
 =>   map:with('name',                   xs:string($source-node/Runner/name))
 =>   map:with('age',                    xs:decimal($source-node/Runner/age))
 =>es:optional('gender',                 xs:string($source-node/Runner/gender))

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
