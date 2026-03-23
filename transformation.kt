// transformation.kt
package com.yourapp.sabahstakeout.transform

import com.yourapp.sabahstakeout.model.Point
import com.yourapp.sabahstakeout.model.TransformParams
import com.yourapp.sabahstakeout.model.Residual
import kotlin.math.*

// Computes similarity transformation parameters from ≥2 fixed points
// Returns null if <2 fixed points or degenerate case
fun computeSimilarityTransform(fixedPoints: List<Point>): TransformParams? {
    val n = fixedPoints.size
    if (n < 2) return null

    // Use approximate reference lat for flat-Earth EN conversion (good enough < few km)
    val refLat = fixedPoints.map { it.fixedLat!! }.average()
    val refLon = fixedPoints.map { it.fixedLon!! }.average()
    val cosRefLat = cos(Math.toRadians(refLat))

    // Convert fixed WGS84 → local East/North meters (simple plate carrée approximation)
    val observedEN = fixedPoints.map { p ->
        val dLat = p.fixedLat!! - refLat
        val dLon = p.fixedLon!! - refLon
        val north = dLat * 111319.0           // approx m per degree lat
        val east  = dLon * 111319.0 * cosRefLat
        east to north
    }

    // Centroids (plan coords and observed EN)
    val mx = fixedPoints.map { it.planEast }.average()
    val my = fixedPoints.map { it.planNorth }.average()
    val mEast  = observedEN.map { it.first }.average()
    val mNorth = observedEN.map { it.second }.average()

    var sumA = 0.0      // Σ (dx * dX + dy * dY)
    var sumB = 0.0      // Σ (dx * dY - dy * dX)
    var sumSS = 0.0     // Σ (dx² + dy²)

    fixedPoints.forEachIndexed { i, pt ->
        val dx = pt.planEast  - mx
        val dy = pt.planNorth - my
        val dX = observedEN[i].first  - mEast
        val dY = observedEN[i].second - mNorth

        sumA += dx * dX + dy * dY
        sumB += dx * dY - dy * dX
        sumSS += dx * dx + dy * dy
    }

    if (sumSS < 1e-6) return null  // degenerate (all points coincide)

    val theta = atan2(sumB, sumA)
    val scale = sqrt(sumA * sumA + sumB * sumB) / sumSS

    return TransformParams(
        scale = scale,
        thetaRad = theta,
        tx = mEast - scale * (cos(theta) * mx - sin(theta) * my),
        ty = mNorth - scale * (sin(theta) * mx + cos(theta) * my)
    )
}

// Apply transformation to get updated target lat/lon for a point
fun applyTransform(
    point: Point,
    params: TransformParams,
    refLat: Double,
    refLon: Double
): Pair<Double, Double>? {
    val cosTheta = cos(params.thetaRad)
    val sinTheta = sin(params.thetaRad)

    val dx = point.planEast
    val dy = point.planNorth

    val rotatedE = params.scale * (cosTheta * dx - sinTheta * dy)
    val rotatedN = params.scale * (sinTheta * dx + cosTheta * dy)

    val targetE = params.tx + rotatedE
    val targetN = params.ty + rotatedN

    val newLat = refLat + targetN / 111319.0
    val newLon = refLon + targetE / (111319.0 * cos(Math.toRadians(newLat + targetN / 111319.0 / 2)))

    return newLat to newLon
}

// Compute residuals for quality report
fun computeResiduals(
    fixedPoints: List<Point>,
    params: TransformParams?,
    refLat: Double,
    refLon: Double
): List<Residual> {
    if (params == null) return emptyList()

    return fixedPoints.map { pt ->
        val (transformedLat, transformedLon) = applyTransform(pt, params, refLat, refLon) ?: return@map Residual(pt.id, pt.name, 0.0, 0.0)

        val dLat = transformedLat - pt.fixedLat!!
        val dLon = transformedLon - pt.fixedLon!!

        val resNorth = dLat * 111319.0
        val resEast  = dLon * 111319.0 * cos(Math.toRadians(refLat))

        Residual(
            pointId = pt.id,
            pointName = pt.name,
            residualEastM = resEast,
            residualNorthM = resNorth
        )
    }
}
