package com.mapbox.navigation.dropin.binder.action

import android.transition.Scene
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.navigationListOf
import com.mapbox.navigation.dropin.component.sound.SoundButtonAction
import com.mapbox.navigation.dropin.databinding.MapboxActionActiveGuidanceLayoutBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ActiveGuidanceActionBinder : UIBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_action_active_guidance_layout,
            viewGroup.context
        ).enter()

        val binding = MapboxActionActiveGuidanceLayoutBinding.bind(viewGroup)

        return navigationListOf(
            SoundButtonAction(binding.soundButton),
            // TODO add other actions here
        )
    }
}