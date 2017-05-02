xquery version "1.0-ml";

module namespace nw = "http://marklogic.com/rest-api/resource/new-suppliers";

import module namespace sp = "https://en.wikipedia.org/wiki/Suppliers_and_Parts_database#SP-0.0.1"
    at "/ext/suppliers-0.0.1.xqy";


declare function nw:put(
    $context as map:map,
    $params as map:map,
    $body as document-node()
) as document-node()?
{
    let $create-n := map:get($params, "n")
    let $cities := ("Azkaban", "Brunello", "Cairo", "Dushanbe", "Elif", "Francisco", "Galipoli", "Harrisburg", "Inchon", "Jimtown")
    let $perms := (xdmp:permission("rest-writer", "update"), xdmp:permission("rest-reader", "read"))
    for $n at $i in fn:reverse(1 to xs:unsignedLong($create-n))
    return
        let $r := $n
        let $r2 := $i
        let $r3 := xdmp:random($n)
        let $supplier :=
                <Supplier>
                  <sid>{$r}</sid>
                  <sName>Supplier {$r}</sName>
                  <status>{xdmp:random(102)}</status>
                  <city>{$cities[($r2 mod 10+1)]}</city>
                </Supplier>
        let $part :=
                <Part>
                  <pid>{$r2}</pid>
                  <pName>Product {$r2}</pName>
                  <color>13</color>
                  <weight>113</weight>
                  <city>{$cities[($r3 mod 10)+1]}</city>
                </Part>
        let $shipment :=
                <Shipment>
                  <sid><Supplier>{$r}</Supplier></sid>
                  <pid><Part>{$r2}</Part></pid>
                    <qty>{$r3 mod 200}</qty>
                </Shipment>
        let $uri := xdmp:random()
        let $_ := xdmp:log( ("creating some stuff...", $create-n, $uri) )
        return (
            xdmp:document-insert("/suppliers/" || $uri, sp:extract-instance-Supplier($supplier)=>sp:instance-to-envelope(), $perms),
            xdmp:document-insert("/part/" || $uri, sp:extract-instance-Part($part)=>sp:instance-to-envelope(), $perms),
            xdmp:document-insert("/shipment/" || $uri, sp:extract-instance-Shipment($shipment)=>sp:instance-to-envelope(), $perms)
        )
    };
