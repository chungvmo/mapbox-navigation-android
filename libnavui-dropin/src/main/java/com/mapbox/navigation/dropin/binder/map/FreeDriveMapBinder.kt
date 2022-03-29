package com.mapbox.navigation.dropin.binder.map

import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.Binder
import com.mapbox.navigation.dropin.binder.navigationListOf
import com.mapbox.navigation.dropin.component.camera.CameraComponent
import com.mapbox.navigation.dropin.component.location.LocationComponent
import com.mapbox.navigation.dropin.component.marker.FreeDriveLongPressMapComponent
import com.mapbox.navigation.dropin.component.marker.GeocodingComponent
import com.mapbox.navigation.dropin.component.marker.MapMarkersComponent
import com.mapbox.navigation.dropin.component.routeline.RouteLineComponent
import com.mapbox.navigation.dropin.lifecycle.reloadOnChange

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class FreeDriveMapBinder(
    private val context: DropInNavigationViewContext,
) : Binder<MapView> {

    override fun bind(mapView: MapView): MapboxNavigationObserver {
        return navigationListOf(
            LocationComponent(context, mapView),
            reloadOnChange(
                context.mapStyleLoader.loadedMapStyle,
                context.options.routeLineOptions
            ) { _, lineOptions ->
                RouteLineComponent(context, mapView, lineOptions)
            },
            CameraComponent(context, mapView),
            MapMarkersComponent(context, mapView),
            FreeDriveLongPressMapComponent(context, mapView),
            GeocodingComponent(context)
        )
    }
}
