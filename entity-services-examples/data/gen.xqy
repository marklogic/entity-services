
for $x in 1 to 1000
let $names := ( "Shaunda", "Bobbi", "Donette", "Arlean", "Nickole", "Tu", "Spencer", "Suzy", "Wilhelmina", "Chu", "Monica", "Evia", "Jacqualine", "Tamera", "Nova", "Bula", "Floretta", "Willow", "Reinaldo", "Nathanial", "Vasiliki", "Kenyatta", "Lenard", "Chelsie", "Dorla", "Vanna", "Rosia", "Rene", "Fermin", "Juana", "Elvina", "Rosio", "Camille", "Marhta", "Georgiana", "Rueben", "Tomasa", "Rashida", "Palmer", "Felisha", "Clarissa", "Deneen", "Tristan", "Dayle", "Cleveland", "Long", "Cierra", "Jasper", "Columbus")
let $distance-miles := xdmp:random(150) div 10
let $minutes-per-mile := xdmp:random( 400 ) div 100 + 7
let $duration := $distance-miles * $minutes-per-mile
let $date := xs:date("2015-03-01") + xs:dayTimeDuration("P" || xdmp:random(120) || "D")
let $random := xdmp:random( count($names) - 1 ) + 1 
return xdmp:save("/tmp/" ||  $x || ".json",
object-node {
  "id": $x,
  "date":$date,
  "distance":$distance-miles,
  "distanceLabel":$distance-miles || " miles",
  "duration":$duration,
  "runByRunner":$names[$random]
  }
  )
