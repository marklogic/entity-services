xquery version "1.0-ml";

module namespace options = "http://marklogic.com/example/options.xqy";

import module namespace es = "http://marklogic.com/entity-services" at "/MarkLogic/entity-services/entity-services.xqy";
import module namespace search = "http://marklogic.com/appservices/search" at "/MarkLogic/appservices/search/search.xqy";

declare variable $options:hub :=
    <search:options xmlns:search="http://marklogic.com/appservices/search">
        <search:constraint name="entity-type">
            <search:value>
                <search:element ns="http://marklogic.com/entity-services" name="title"/>
            </search:value>
        </search:constraint>
        <search:constraint name="id">
            <search:value>
                <search:element ns="" name="id"/>
            </search:value>
        </search:constraint>
        <search:term xmlns:search="http://marklogic.com/appservices/search">
            <search:empty apply="no-results"/>
        </search:term>
        <search:values name="uris">
            <search:uri/>
        </search:values>
        <search:search-option>filtered</search:search-option>
        <search:additional-query>
            <cts:element-query xmlns:cts="http://marklogic.com/cts">
                <cts:element xmlns:es="http://marklogic.com/entity-services">es:instance</cts:element>
                <cts:element-value-query>
                    <cts:element xmlns:es="http://marklogic.com/entity-services">es:version</cts:element>
                    <cts:text>original</cts:text>
                </cts:element-value-query>
            </cts:element-query>
        </search:additional-query>
        <search:transform-results apply="empty-snippet"/>
    </search:options> ;

declare variable $options:hub-next :=
    <search:options xmlns:search="http://marklogic.com/appservices/search">
        <search:constraint name="entity-type">
            <search:value>
                <search:element ns="http://marklogic.com/entity-services" name="title"/>
            </search:value>
        </search:constraint>
        <search:constraint name="id">
            <search:value>
                <search:element ns="" name="id"/>
            </search:value>
        </search:constraint>
        <search:constraint name="fullName">
            <search:word>
                <search:element ns="" name="fullName"/>
                <search:term-option>wildcarded</search:term-option>
                <search:term-option>case-insensitive</search:term-option>
            </search:word>
        </search:constraint>
        <search:term xmlns:search="http://marklogic.com/appservices/search">
            <search:empty apply="no-results"/>
        </search:term>
        <search:values name="uris">
            <search:uri/>
        </search:values>
        <search:search-option>filtered</search:search-option>
        <search:additional-query>
            <cts:element-query xmlns:cts="http://marklogic.com/cts">
                <cts:element xmlns:es="http://marklogic.com/entity-services">es:instance</cts:element>
                <cts:element-value-query>
                    <cts:element xmlns:es="http://marklogic.com/entity-services">es:version</cts:element>
                    <cts:text>next</cts:text>
                </cts:element-value-query>
            </cts:element-query>
        </search:additional-query>
        <search:transform-results apply="empty-snippet"/>
    </search:options> ;


declare function options:results(
    $query, $options, $formatter) {
    let $parsed := search:parse($query, $options)
    return
    (
        text {
            (
            search:resolve-nodes($parsed, $options) ! ($formatter(.) || "&#10;")
            ),
            xdmp:quote($parsed)
        }
    )
};
