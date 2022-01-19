package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.navigation.base.route.NavigationRoute
import kotlin.math.min

internal class DirectionsRouteDiffProvider {

    fun buildRouteDiffs(
        oldRoute: NavigationRoute,
        newRoute: NavigationRoute,
        currentLegIndex: Int,
    ): List<String> {
        val oldRouteLegs = oldRoute.directionsRoute.legs() ?: return emptyList()
        val newRouteLegs = newRoute.directionsRoute.legs() ?: return emptyList()
        val routeDiffs = arrayListOf<String>()
        for (legIndex in currentLegIndex until min(oldRouteLegs.size, newRouteLegs.size)) {
            val oldLegAnnotation = oldRouteLegs[legIndex].annotation()
            val newLegAnnotation = newRouteLegs[legIndex].annotation()
            val updatedAnnotations = getUpdatedAnnotations(oldLegAnnotation, newLegAnnotation)
            if (updatedAnnotations.isNotEmpty()) {
                routeDiffs.add("Updated ${updatedAnnotations.joinToString()} at leg $legIndex")
            }
        }
        return routeDiffs
    }

    private fun getUpdatedAnnotations(
        oldLegAnnotation: LegAnnotation?,
        newLegAnnotation: LegAnnotation?,
    ): List<String> {
        val updatedAnnotations = arrayListOf<String>()
        if (oldLegAnnotation?.distance() != newLegAnnotation?.distance()) {
            updatedAnnotations.add("distance")
        }
        if (oldLegAnnotation?.duration() != newLegAnnotation?.duration()) {
            updatedAnnotations.add("duration")
        }
        if (oldLegAnnotation?.speed() != newLegAnnotation?.speed()) {
            updatedAnnotations.add("speed")
        }
        if (oldLegAnnotation?.maxspeed() != newLegAnnotation?.maxspeed()) {
            updatedAnnotations.add("maxSpeed")
        }
        if (oldLegAnnotation?.congestion() != newLegAnnotation?.congestion()) {
            updatedAnnotations.add("congestion")
        }
        if (oldLegAnnotation?.congestionNumeric() != newLegAnnotation?.congestionNumeric()) {
            updatedAnnotations.add("congestionNumeric")
        }
        return updatedAnnotations
    }
}
