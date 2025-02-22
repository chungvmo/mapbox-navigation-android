package com.mapbox.navigation.dropin

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.core.content.res.use
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.backpress.OnKeyListenerComponent
import com.mapbox.navigation.dropin.component.tripsession.LocationPermissionComponent
import com.mapbox.navigation.dropin.coordinator.ActionButtonsCoordinator
import com.mapbox.navigation.dropin.coordinator.InfoPanelCoordinator
import com.mapbox.navigation.dropin.coordinator.ManeuverCoordinator
import com.mapbox.navigation.dropin.coordinator.MapLayoutCoordinator
import com.mapbox.navigation.dropin.coordinator.RoadNameLabelCoordinator
import com.mapbox.navigation.dropin.coordinator.SpeedLimitCoordinator
import com.mapbox.navigation.dropin.databinding.DropInNavigationViewBinding
import com.mapbox.navigation.dropin.extensions.attachCreated
import com.mapbox.navigation.ui.utils.internal.lifecycle.ViewLifecycleRegistry
import com.mapbox.navigation.utils.internal.logW
import java.lang.ref.WeakReference

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    accessToken: String = attrs.navigationViewAccessToken(context),
) : FrameLayout(context, attrs), LifecycleOwner {

    private val binding: DropInNavigationViewBinding = DropInNavigationViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private val viewLifecycleRegistry: ViewLifecycleRegistry = ViewLifecycleRegistry(
        view = this,
        localLifecycleOwner = this,
        hostingLifecycleOwner = context.toLifecycleOwner(),
    )

    private val viewModelProvider by lazy {
        ViewModelProvider(context.toViewModelStoreOwner())
    }

    /**
     * The one and only view model for [DropInNavigationView]. If you need state
     * to survive orientation changes, put it in the [DropInNavigationViewModel].
     */
    private val viewModel: DropInNavigationViewModel by lazyViewModel()

    /**
     * This is a top level object to share data with all state view holders. If you need state
     * to survive orientation changes, put it in the [DropInNavigationViewModel].
     */
    private val navigationContext = DropInNavigationViewContext(
        context = context,
        lifecycleOwner = this,
        viewModel = viewModel,
    )

    /**
     * Customize the views by implementing your own [UIBinder] components.
     */
    fun customizeMapView(mapView: MapView?) {
        navigationContext.mapView.value = mapView
    }

    /**
     * Customize view by providing your own [UIBinder] components.
     */
    fun customizeViewBinders(action: ViewBinderCustomization.() -> Unit) {
        navigationContext.applyBinderCustomization(action)
    }

    /**
     * Provide custom map styles, route line and arrow options.
     */
    fun customizeViewOptions(action: ViewOptionsCustomization.() -> Unit) {
        navigationContext.applyOptionsCustomization(action)
    }

    init {
        /**
         * Default setup for MapboxNavigationApp. The developer can customize this by
         * setting up the MapboxNavigationApp before the view is constructed.
         */
        if (!MapboxNavigationApp.isSetup()) {
            MapboxNavigationApp.setup(
                NavigationOptions.Builder(context)
                    .accessToken(accessToken)
                    .build()
            )
        }

        /**
         * Attach the lifecycle to mapbox navigation. This gives MapboxNavigationApp a lifecycle
         * that allows it to determine the foreground and background state.
         */
        MapboxNavigationApp.attach(this)

        /**
         * Single point of entry for the Mapbox Navigation View.
         */
        attachCreated(
            LocationPermissionComponent(
                context.toComponentActivityRef(),
                navigationContext.viewModel.tripSessionStarterViewModel
            ),
            MapLayoutCoordinator(navigationContext, binding),
            OnKeyListenerComponent(
                navigationContext.viewModel.navigationStateViewModel,
                navigationContext.viewModel.destinationViewModel,
                navigationContext.viewModel.routesViewModel,
                this,
            ),
            ManeuverCoordinator(navigationContext, binding.guidanceLayout),
            InfoPanelCoordinator(
                navigationContext,
                binding.infoPanelLayout,
                binding.guidelineBottom
            ),
            ActionButtonsCoordinator(navigationContext, binding.actionListLayout),
            SpeedLimitCoordinator(navigationContext, binding.speedLimitLayout),
            RoadNameLabelCoordinator(navigationContext, binding.roadNameLayout)
        )
    }

    override fun getLifecycle(): Lifecycle = viewLifecycleRegistry

    private inline fun <reified T : ViewModel> lazyViewModel(): Lazy<T> = lazy {
        viewModelProvider[T::class.java]
    }
}

private tailrec fun recursiveUnwrap(context: Context): Context =
    if (context !is Activity && context is ContextWrapper) {
        recursiveUnwrap(context.baseContext)
    } else {
        context
    }

private fun AttributeSet?.navigationViewAccessToken(context: Context): String {
    val accessToken = context.obtainStyledAttributes(
        this,
        R.styleable.NavigationView,
        0,
        0
    ).use { it.getString(R.styleable.NavigationView_accessToken) }
    checkNotNull(accessToken) {
        "Provide access token directly in the constructor or via 'accessToken' layout parameter"
    }
    return accessToken
}

private fun Context.toLifecycleOwner(): LifecycleOwner {
    val lifecycleOwner = recursiveUnwrap(this) as? LifecycleOwner
    checkNotNull(lifecycleOwner) {
        "Please ensure that the hosting Context is a valid LifecycleOwner"
    }
    return lifecycleOwner
}

private fun Context.toViewModelStoreOwner(): ViewModelStoreOwner {
    val viewModelStoreOwner = recursiveUnwrap(this) as? ViewModelStoreOwner
    checkNotNull(viewModelStoreOwner) {
        "Please ensure that the hosting Context is a valid ViewModelStoreOwner"
    }
    return viewModelStoreOwner
}

private fun Context.toComponentActivityRef(): WeakReference<ComponentActivity>? {
    val componentActivity = recursiveUnwrap(this) as? ComponentActivity
    if (componentActivity == null) {
        logW("Unable to find ComponentActivity to request location permissions")
    }
    return componentActivity?.let { WeakReference(it) }
}
