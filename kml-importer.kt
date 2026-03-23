// kml-importer.kt
package com.yourapp.sabahstakeout.import

import android.content.Context
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import com.yourapp.sabahstakeout.model.Point
import com.yourapp.sabahstakeout.model.Connection

data class ImportedFeature(
    val points: List<Point>,
    val connections: List<Connection>
)

// Very basic KML parser – extracts Placemarks with Point or Polygon
// Returns list of points + connections (for polygons: sequential legs)
fun parseKmlSimple(context: Context, inputStream: InputStream): ImportedFeature {
    val points = mutableListOf<Point>()
    val connections = mutableListOf<Connection>()

    val factory = XmlPullParserFactory.newInstance()
    factory.isNamespaceAware = true
    val parser = factory.newPullParser()
    parser.setInput(inputStream, null)

    var eventType = parser.eventType
    var currentPlacemarkName: String? = null
    var currentCoordinates: MutableList<Pair<Double, Double>> = mutableListOf()

    while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
            XmlPullParser.START_TAG -> {
                when (parser.name) {
                    "Placemark" -> {
                        currentPlacemarkName = null
                        currentCoordinates.clear()
                    }
                    "name" -> {
                        if (parser.previousName == "Placemark") {
                            currentPlacemarkName = parser.nextText().trim()
                        }
                    }
                    "coordinates" -> {
                        val coordsText = parser.nextText().trim()
                        // Parse lon,lat[,alt] triples
                        coordsText.split("\\s+".toRegex()).forEach { coord ->
                            val parts = coord.split(",")
                            if (parts.size >= 2) {
                                val lon = parts[0].toDoubleOrNull() ?: 0.0
                                val lat = parts[1].toDoubleOrNull() ?: 0.0
                                currentCoordinates.add(lon to lat)
                            }
                        }
                    }
                }
            }
            XmlPullParser.END_TAG -> {
                when (parser.name) {
                    "Placemark" -> {
                        if (currentCoordinates.isNotEmpty()) {
                            // Create points
                            val placemarkPoints = currentCoordinates.mapIndexed { idx, (lon, lat) ->
                                Point(
                                    name = "${currentPlacemarkName ?: "Imported"} ${idx + 1}",
                                    // We'll set planEast/North later after relative computation
                                    fixedLat = lat,
                                    fixedLon = lon,
                                    isFixed = false  // imported as plan, not fixed
                                )
                            }
                            points.addAll(placemarkPoints)

                            // If >2 coords → assume polygon/closed → create connections
                            if (placemarkPoints.size > 2) {
                                for (i in 0 until placemarkPoints.size) {
                                    val from = placemarkPoints[i]
                                    val to = placemarkPoints[(i + 1) % placemarkPoints.size]
                                    // Bearing & distance computed later (after relative coords built)
                                    connections.add(
                                        Connection(
                                            fromPointId = from.id,
                                            toPointId = to.id,
                                            bearingDMS = "0.0000",      // placeholder – compute later
                                            distanceM = 0.0,            // placeholder
                                            isSkewBearing = true        // assume Sabah RSO for imported
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        eventType = parser.next()
    }

    return ImportedFeature(points, connections)
}
