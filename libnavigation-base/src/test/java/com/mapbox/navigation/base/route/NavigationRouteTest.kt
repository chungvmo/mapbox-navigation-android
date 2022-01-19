package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.FileUtils
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationRouteTest {
    @Test
    fun `toNavigationRoute - waypoints back filled from route legs`() {
        val directionsRoute = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("multileg_route.json")
        )

        val navigationRoute = directionsRoute.toNavigationRoute()

        assertEquals(3, navigationRoute.directionsResponse.waypoints()!!.size)
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(-77.157347, 38.783004))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![0]
        )
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(-77.167276, 38.775717))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![1]
        )
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(-77.153468, 38.77091))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![2]
        )
    }

    @Test
    fun `toNavigationRoute - waypoints back filled from route options`() {
        val directionsRoute = mockk<DirectionsRoute> {
            every { requestUuid() } returns "asdf"
            every { routeIndex() } returns "0"
            every { routeOptions() } returns RouteOptions.builder()
                .profile("driving")
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(1.1, 1.1),
                        Point.fromLngLat(2.2, 2.2),
                    )
                )
                .build()
            every { legs() } returns null
            every { toBuilder() } returns mockk(relaxed = true)
        }

        val navigationRoute = directionsRoute.toNavigationRoute()

        assertEquals(2, navigationRoute.directionsResponse.waypoints()!!.size)
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(1.1, 1.1))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![0]
        )
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(2.2, 2.2))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![1]
        )
    }

    @Test
    fun `toNavigationRoute - waypoints back filled from route options ignoring silent`() {
        val directionsRoute = mockk<DirectionsRoute> {
            every { requestUuid() } returns "asdf"
            every { routeIndex() } returns "0"
            every { routeOptions() } returns RouteOptions.builder()
                .profile("driving")
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(1.1, 1.1),
                        Point.fromLngLat(2.2, 2.2),
                        Point.fromLngLat(3.3, 3.3),
                        Point.fromLngLat(4.4, 4.4),
                    )
                )
                .waypointIndicesList(listOf(0, 2, 3))
                .build()
            every { legs() } returns null
            every { toBuilder() } returns mockk(relaxed = true)
        }

        val navigationRoute = directionsRoute.toNavigationRoute()

        assertEquals(3, navigationRoute.directionsResponse.waypoints()!!.size)
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(1.1, 1.1))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![0]
        )
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(3.3, 3.3))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![1]
        )
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(4.4, 4.4))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![2]
        )
    }
}