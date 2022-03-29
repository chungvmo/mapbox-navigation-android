package com.mapbox.navigation.dropin.component.marker

import android.location.Location
import android.location.LocationManager
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.model.Destination
import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.util.HapticFeedback
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.MockLoggerRule
import com.mapbox.navigation.utils.internal.toPoint
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RoutePreviewLongPressMapComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @get:Rule
    val mockLoggerTestRule = MockLoggerRule()

    private val mockGesturesPlugin: GesturesPlugin = mockk(relaxed = true)
    private val mockMapView: MapView = mockk {
        every { gestures } returns mockGesturesPlugin
    }
    private val mockMapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
        every { navigationOptions } returns mockk {
            every { applicationContext } returns mockk(relaxed = true)
        }
    }

    lateinit var sut: RoutePreviewLongPressMapComponent
    private lateinit var testStore: TestStore
    private lateinit var navContext: DropInNavigationViewContext

    @Before
    fun setUp() {
        mockkObject(HapticFeedback)
        every { HapticFeedback.create(any()) } returns mockk(relaxed = true)
        testStore = spyk(TestStore(coroutineRule.coroutineScope))
        navContext = mockk(relaxed = true) {
            every { viewModel } returns mockk {
                every { store } returns testStore
            }
        }

        sut = RoutePreviewLongPressMapComponent(
            navContext,
            mockMapView,
        )
    }

    @After
    fun tearDown() {
        unmockkObject(HapticFeedback)
    }

    @Test
    fun `should register OnMapLongClickListener in onAttached`() {
        sut.onAttached(mockMapboxNavigation)

        verify { mockGesturesPlugin.addOnMapLongClickListener(any()) }
    }

    @Test
    fun `should unregister OnMapLongClickListener in onDetached`() {
        sut.onAttached(mockMapboxNavigation)

        sut.onDetached(mockMapboxNavigation)

        verify { mockGesturesPlugin.removeOnMapLongClickListener(any()) }
    }

    @Test
    fun `onMapLongClick should do nothing if location is unknown`() {
        val slot = slot<OnMapLongClickListener>()
        every { mockGesturesPlugin.addOnMapLongClickListener(capture(slot)) } returns Unit
        sut.onAttached(mockMapboxNavigation)

        val point = Point.fromLngLat(11.0, 12.0)
        slot.captured.onMapLongClick(point)

        verify(exactly = 0) {
            testStore.dispatch(any())
        }
    }

    @Test
    fun `onMapLongClick should update view model state`() {
        val slot = slot<OnMapLongClickListener>()
        every { mockGesturesPlugin.addOnMapLongClickListener(capture(slot)) } returns Unit
        val currentLocation = Location(LocationManager.PASSIVE_PROVIDER).apply {
            longitude = 21.0
            latitude = 22.0
        }
        testStore.setState(State(location = currentLocation))
        sut.onAttached(mockMapboxNavigation)

        val clickPoint = Point.fromLngLat(11.0, 12.0)
        slot.captured.onMapLongClick(clickPoint)

        verifyOrder {
            testStore.dispatch(DestinationAction.SetDestination(Destination(clickPoint)))
            testStore.dispatch(
                RoutesAction.FetchPoints(listOf(currentLocation.toPoint(), clickPoint))
            )
        }
    }
}
