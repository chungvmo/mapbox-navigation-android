package com.mapbox.navigation.core.telemetry.events

import android.os.Build
import android.os.Parcel
import com.google.gson.Gson
import com.mapbox.android.telemetry.Event
import com.mapbox.android.telemetry.TelemetryUtils
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.core.BuildConfig

/**
 * Base Event class for navigation events, contains common properties.
 *
 * @property driverMode one of [FeedbackEvent.DriverMode]
 * @property sessionIdentifier id of [driverMode]
 * @property startTimestamp start time of [driverMode]
 * @property navigatorSessionIdentifier group id of modes under one Telemetry session
 */
internal abstract class NavigationEvent(
    phoneState: PhoneState
) : Event(), MetricEvent {

    private companion object {
        private val OPERATING_SYSTEM = "Android - ${Build.VERSION.RELEASE}"
    }

    /*
     * Don't remove any fields, cause they should match with
     * the schema downloaded from S3. Look at {@link SchemaTest}
     */
    val version = "2.2"
    val operatingSystem: String = OPERATING_SYSTEM
    val device: String? = Build.MODEL
    val sdkVersion: String = BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME
    val created: String = TelemetryUtils.obtainCurrentDate() // Schema pattern
    val volumeLevel: Int = phoneState.volumeLevel
    val batteryLevel: Int = phoneState.batteryLevel
    val screenBrightness: Int = phoneState.screenBrightness
    val batteryPluggedIn: Boolean = phoneState.isBatteryPluggedIn
    val connectivity: String = phoneState.connectivity
    val audioType: String = phoneState.audioType
    val applicationState: String = phoneState.applicationState // Schema minLength 1
    val event: String = getEventName()

    var sdkIdentifier: String? = null

    var navigatorSessionIdentifier: String? = null
    var startTimestamp: String? = null
    var driverMode: String? = null
    var sessionIdentifier: String? = null
    var geometry: String? = null
    var profile: String? = null
    var originalRequestIdentifier: String? = null
    var requestIdentifier: String? = null
    var originalGeometry: String? = null
    var locationEngine: String? = null
    var tripIdentifier: String? = null
    var lat: Double = 0.toDouble()
    var lng: Double = 0.toDouble()
    var simulation: Boolean = false
    var absoluteDistanceToDestination: Int = 0
    var percentTimeInPortrait: Int = 0
    var percentTimeInForeground: Int = 0
    var distanceCompleted: Int = 0
    var distanceRemaining: Int = 0
    var durationRemaining: Int = 0
    var eventVersion: Int = 0
    var estimatedDistance: Int = 0
    var estimatedDuration: Int = 0
    var rerouteCount: Int = 0
    var originalEstimatedDistance: Int = 0
    var originalEstimatedDuration: Int = 0
    var stepCount: Int = 0
    var originalStepCount: Int = 0
    var legIndex: Int = 0
    var legCount: Int = 0
    var stepIndex: Int = 0
    var voiceIndex: Int = 0
    var bannerIndex: Int = 0
    var totalStepCount: Int = 0
    var appMetadata: AppMetadata? = null

    internal abstract fun getEventName(): String

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
    }

    override fun toJson(gson: Gson): String = gson.toJson(this)

    override val metricName: String
        get() = getEventName()
}
