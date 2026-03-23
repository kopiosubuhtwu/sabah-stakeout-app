// bowditch.kt
package com.yourapp.sabahstakeout.adjustment

import com.yourapp.sabahstakeout.model.Connection
import com.yourapp.sabahstakeout.model.Point
import kotlin.math.*

// Simple structure to hold leg data during adjustment
data class Leg(
    val connection: Connection,
    val azimuthRad: Double,     // computed from bearing
    val length: Double,
    var deltaE: Double = 0.0,   // departure (East)
    var deltaN: Double = 0.0    // latitude (North)
)

// Adjust a closed traverse loop using Bowditch (Compass) rule
// Input: ordered list of legs forming a closed loop
// Modifies deltaE/deltaN in place and returns total misclosure
fun adjustBowditch(legs: List<Leg>): Pair<Double, Double> {  // returns (misclosure East, misclosure North)
    if (legs.isEmpty()) return 0.0 to 0.0

    // 1. Compute raw deltas from bearings + distances
    legs.forEach { leg ->
        val bearingDeg = parseBearingDMS(leg.connection.bearingDMS)
        val azimuthRad = Math.toRadians(bearingDeg)  // assume bearing is from grid north
        leg.deltaE = leg.length * sin(azimuthRad)
        leg.deltaN = leg.length * cos(azimuthRad)
    }

    // 2. Sum raw misclosure
    val sumDeltaE = legs.sumOf { it.deltaE }
    val sumDeltaN = legs.sumOf { it.deltaN }

    val perimeter = legs.sumOf { it.length }
    if (perimeter < 1e-6) return sumDeltaE to sumDeltaN

    // 3. Distribute correction proportionally to leg length (Bowditch rule)
    legs.forEach { leg ->
        val correctionE = -sumDeltaE * (leg.length / perimeter)
        val correctionN = -sumDeltaN * (leg.length / perimeter)

        leg.deltaE += correctionE
        leg.deltaN += correctionN
    }

    return sumDeltaE to sumDeltaN
}

// Helper: parse your dd.mmss format (can be moved to utils)
fun parseBearingDMS(dms: String): Double {
    val parts = dms.split(".")
    if (parts.size != 2) return 0.0
    val deg = parts[0].toDoubleOrNull() ?: 0.0
    val mmss = parts[1].padEnd(4, '0')
    val min = mmss.substring(0, 2).toDoubleOrNull() ?: 0.0
    val sec = mmss.substring(2, 4).toDoubleOrNull() ?: 0.0
    return deg + min / 60.0 + sec / 3600.0
}

// Usage example in ViewModel:
// After building graph, for each detected closed loop → call adjustBowditch → accumulate adjusted deltas → compute final planEast/North from starting point

