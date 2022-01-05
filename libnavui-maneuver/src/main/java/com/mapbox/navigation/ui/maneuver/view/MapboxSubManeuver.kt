package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.mapbox.navigation.ui.maneuver.R
import com.mapbox.navigation.ui.maneuver.model.ManeuverInstructionGenerator
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.SubManeuver
import com.mapbox.navigation.ui.maneuver.model.toRouteShield
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.utils.internal.ifNonNull

/**
 * Default view to render sub banner instructions onto [MapboxManeuverView].
 * It can be directly used in any other layout.
 * @property attrs AttributeSet
 * @property defStyleAttr Int
 */
class MapboxSubManeuver : AppCompatTextView {

    /**
     *
     * @param context Context
     * @constructor
     */
    constructor(context: Context) : super(context)

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @constructor
     */
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @param defStyleAttr Int
     * @constructor
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    private var leftDrawable = ContextCompat.getDrawable(
        context, R.drawable.mapbox_ic_exit_arrow_left
    )
    private var rightDrawable = ContextCompat.getDrawable(
        context, R.drawable.mapbox_ic_exit_arrow_right
    )
    private var exitBackground = ContextCompat.getDrawable(
        context, R.drawable.mapbox_exit_board_background
    )

    /**
     * Invoke the method to render sub maneuver instructions
     * @param maneuver SubManeuver
     */
    @Deprecated(
        message = "The method may or may not render multiple shields for a given instruction",
        replaceWith = ReplaceWith("renderManeuver(maneuver, roadShields)")
    )
    @JvmOverloads
    fun render(maneuver: SubManeuver?, roadShield: RoadShield? = null) {
        val roadShields = ifNonNull(roadShield) {
            setOf(it.toRouteShield())
        }
        renderManeuver(maneuver, roadShields)
    }

    /**
     * Invoke the method to render sub maneuver instructions
     * @param maneuver SubManeuver
     */
    fun renderManeuver(maneuver: SubManeuver?, routeShields: Set<RouteShield>?) {
        val exitView = MapboxExitText(context)
        exitView.setExitStyle(exitBackground, leftDrawable, rightDrawable)
        val instruction = ManeuverInstructionGenerator.generateSub(
            context,
            lineHeight,
            exitView,
            maneuver,
            routeShields
        )
        text = instruction
    }
}
