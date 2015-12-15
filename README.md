# kml-reducer
KML Point reducer using the Douglas-Peucker Algorithm

# Build it

`mvn clean install`

# Run it

This will process every file in the location you specify and run point reductions on the polygons present in the kml files. Output will be to the input director with a "reduced.kml" appended.

`java -jar target/KmlPointReducer-<VERSION>-jar-with-dependencies.jar <PATH WHERE YOUR KML FILES ARE>`

# Notes

Douglas-Peucker Algorithm borrowed from OSMBonusPack https://github.com/MKergall/osmbonuspack
