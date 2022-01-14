package com.mapbox.navigation.example.wear

import android.content.res.Resources
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.animation.PathInterpolatorCompat
import com.google.android.gms.wearable.Wearable
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.MapProjection
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.view.MapboxStepDistance
import com.mapbox.navigation.ui.maneuver.view.MapboxTurnIconManeuver
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.utils.internal.extensions.play
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapView = findViewById<MapView>(R.id.mapView)
        val navigationLocationProvider = NavigationLocationProvider()
        mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        val mapboxMap = mapView.getMapboxMap().apply {
            setCamera(
                CameraOptions.Builder()
                    .zoom(0.5)
                    .build()
            )
            setMapProjection(MapProjection.Globe)
        }

        val navigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(this)
                .accessToken(getString(R.string.mapbox_access_token))
                .build()
        )

        val viewportDataSource = MapboxNavigationViewportDataSource(
            mapView.getMapboxMap()
        )
        // TODO: update viewportDataSource.followingPadding based on boxInsets
        val pixelDensity = Resources.getSystem().displayMetrics.density
        viewportDataSource.followingPadding = EdgeInsets(
            40.0 * pixelDensity,
            30.0 * pixelDensity,
            50.0 * pixelDensity,
            30.0 * pixelDensity
        )
        val navigationCamera = NavigationCamera(
            mapView.getMapboxMap(),
            mapView.camera,
            viewportDataSource
        )

        navigation.registerLocationObserver(object : LocationObserver {
            override fun onNewRawLocation(rawLocation: Location) { }
            override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
                navigationLocationProvider.changePosition(
                    location = locationMatcherResult.enhancedLocation,
                    keyPoints = locationMatcherResult.keyPoints,
                )

                viewportDataSource.onLocationChanged(locationMatcherResult.enhancedLocation)
                viewportDataSource.evaluate()
            }
        })

        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(this)
            .withRouteLineBelowLayerId("road-label")
            .build()
        val routeLineAPI = MapboxRouteLineApi(mapboxRouteLineOptions)
        val routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)
        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        val routeArrowView = MapboxRouteArrowView(routeArrowOptions)
        val routeArrowAPI: MapboxRouteArrowApi = MapboxRouteArrowApi()

        val maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(DistanceFormatterOptions.Builder(this).build())
        )

        navigation.registerRoutesObserver { result ->
            if (result.routes.isNotEmpty()) {
                // generate route geometries asynchronously and render them
                CoroutineScope(Dispatchers.Main).launch {
                    val result = routeLineAPI.setRoutes(
                        listOf(RouteLine(result.routes.first(), null))
                    )
                    val style = mapboxMap.getStyle()
                    if (style != null) {
                        routeLineView.renderRouteDrawData(style, result)
                    }
                }

                // update the camera position to account for the new route
                viewportDataSource.onRouteChanged(result.routes.first())
                viewportDataSource.evaluate()
            } else {
                // remove the route line and route arrow from the map
                val style = mapboxMap.getStyle()
                if (style != null) {
                    routeLineAPI.clearRouteLine { value ->
                        routeLineView.renderClearRouteLineValue(
                            style,
                            value
                        )
                    }
                    routeArrowView.render(style, routeArrowAPI.clearArrows())
                }

                // remove the route reference to change camera position
                viewportDataSource.clearRouteData()
                viewportDataSource.evaluate()
            }
        }

        navigation.registerRouteProgressObserver { routeProgress ->
            // update the camera position to account for the progressed fragment of the route
            viewportDataSource.onRouteProgressChanged(routeProgress)
            viewportDataSource.evaluate()

            // show arrow on the route line with the next maneuver
            val maneuverArrowResult = routeArrowAPI.addUpcomingManeuverArrow(routeProgress)
            val style = mapboxMap.getStyle()
            if (style != null) {
                routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
            }

            updateManeuverView(maneuverApi, routeProgress)
        }

        // load map style
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
            routeLineView.initializeLayers(style)
        }

        receive(navigation, navigationCamera, mapboxMap, viewportDataSource, mapView)
    }

    private fun updateManeuverView(maneuverApi: MapboxManeuverApi, routeProgress: RouteProgress) {
        // update top maneuver instructions
        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        maneuvers.fold(
            { error ->
                Log.e(
                    "maneuvers",
                    "error getting maneuvers ${error.errorMessage}",
                    error.throwable)
            },
            {
                displayManeuvers(it)
            }
        )
    }

    private fun displayManeuvers(maneuvers: List<Maneuver>) {
        val nextManeuver = maneuvers.firstOrNull() ?: return

        findViewById<ViewGroup>(R.id.maneuverView).visibility = View.VISIBLE

        val primaryManeuverIcon = findViewById<MapboxTurnIconManeuver>(R.id.primaryManeuverIcon)
        primaryManeuverIcon.renderPrimaryTurnIcon(nextManeuver.primary)

        val primaryManeuverDistance = findViewById<MapboxStepDistance>(R.id.primaryManeuverDistance)
        primaryManeuverDistance.renderDistanceRemaining(nextManeuver.stepDistance)
    }

    private fun receive(
        navigation: MapboxNavigation,
        navigationCamera: NavigationCamera,
        mapboxMap: MapboxMap,
        viewportDataSource: MapboxNavigationViewportDataSource,
        mapView: MapView,
    ) {
        Wearable.getMessageClient(this).addListener {
            if (it.path == "/route") {
                val directionRouteJson = it.data.decodeToString()
                val directionsRoute = DirectionsRoute.fromJson(directionRouteJson)
                navigation.startTripSession(true)
                navigation.setRoutes(listOf(directionsRoute))
                navigationCamera.requestNavigationCameraToFollowing()
            }
        }
    }
}
