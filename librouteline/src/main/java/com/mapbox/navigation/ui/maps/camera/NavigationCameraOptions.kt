package com.mapbox.navigation.ui.maps.camera

import com.mapbox.maps.EdgeInsets

const val MAPBOX_CAMERA_OPTION_FOLLOWING_PITCH = 40.0
const val MAPBOX_CAMERA_OPTION_MAX_ZOOM = 17.0

/**
 * This value will be used for navigation camera transition use.
 *
 * @param followingPitch the pitch for camera when following the vehicle
 * @param maxZoom the max camera zoom during the transition
 */
class NavigationCameraOptions private constructor(val followingPitch: Double,
                                                  val maxZoom: Double) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        followingPitch(followingPitch)
        maxZoom(maxZoom)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NavigationCameraOptions

        if (followingPitch != other.followingPitch) return false
        if (maxZoom != other.maxZoom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = followingPitch.hashCode()
        result = 31 * result + maxZoom.hashCode()
        return result
    }

    /**
     * Build a new [NavigationCameraOptions]
     */
    class Builder {
        private var followingPitch = MAPBOX_CAMERA_OPTION_FOLLOWING_PITCH
        private var maxZoom = MAPBOX_CAMERA_OPTION_MAX_ZOOM

        /**
         * Override [followingPitch] for camera when following the vehicle
         */
        fun followingPitch(followingPitch: Double): Builder = apply {
            this.followingPitch = followingPitch
        }

        /**
         * Override [maxZoom] during the transition
         */
        fun maxZoom(maxZoom: Double): Builder = apply {
            this.maxZoom = maxZoom
        }

        /**
         * Build a new instance of [NavigationCameraOptions]
         * @return NavigationCameraOptions
         */
        fun build(): NavigationCameraOptions = NavigationCameraOptions(
            followingPitch,
            maxZoom
        )
    }
}