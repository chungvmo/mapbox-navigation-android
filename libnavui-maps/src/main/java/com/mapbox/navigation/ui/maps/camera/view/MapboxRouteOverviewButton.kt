package com.mapbox.navigation.ui.maps.camera.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.databinding.MapboxRouteOverviewLayoutBinding
import com.mapbox.navigation.ui.utils.internal.extensions.afterMeasured
import com.mapbox.navigation.ui.utils.internal.extensions.extend
import com.mapbox.navigation.ui.utils.internal.extensions.shrink
import com.mapbox.navigation.ui.utils.internal.extensions.slideWidth

/**
 * Default view to allow user to switch to route overview mode.
 */
class MapboxRouteOverviewButton : ConstraintLayout {

    private var textWidth = 0
    private var isAnimationRunning = false
    private val binding = MapboxRouteOverviewLayoutBinding.inflate(
        LayoutInflater.from(context),
        this
    )

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
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initAttributes(attrs)
    }

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
    ) : super(context, attrs, defStyleAttr) {
        initAttributes(attrs)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding.routeOverviewText.afterMeasured {
            textWidth = width
        }
    }

    /**
     * Allows you to change the style of [MapboxRouteOverviewButton].
     * @param style Int
     */
    fun updateStyle(@StyleRes style: Int) {
        val typedArray = context.obtainStyledAttributes(
            style,
            R.styleable.MapboxRouteOverviewButton
        )
        applyAttributes(typedArray)
        typedArray.recycle()
    }

    /**
     * Invoke the function to show optional text associated with the view.
     * @param duration for the view to be in the extended mode before it starts to shrink.
     */
    fun showTextAndExtend(duration: Long) {
        if (!isAnimationRunning) {
            isAnimationRunning = true
            val extendToWidth = EXTEND_TO_WIDTH * context.resources.displayMetrics.density
            val animator = getAnimator(textWidth, extendToWidth.toInt())
            binding.routeOverviewText.extend(animator) {
                binding.routeOverviewText.text = context.getString(R.string.mapbox_route_overview)
                postDelayed(
                    {
                        val endAnimator = getAnimator(extendToWidth.toInt(), textWidth)
                        binding.routeOverviewText.shrink(endAnimator) {
                            binding.routeOverviewText.text = ""
                            isAnimationRunning = false
                        }
                    },
                    duration
                )
            }
        }
    }

    private fun initAttributes(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxRouteOverviewButton
        )
        applyAttributes(typedArray)
        typedArray.recycle()
    }

    private fun applyAttributes(typedArray: TypedArray) {
        ContextCompat.getDrawable(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxRouteOverviewButton_overviewButtonDrawable,
                R.drawable.mapbox_ic_route_overview
            )
        ).also { binding.routeOverviewIcon.setImageDrawable(it) }
    }

    private fun getAnimator(from: Int, to: Int) =
        binding.routeOverviewText.slideWidth(from, to, SLIDE_DURATION)

    private companion object {
        const val SLIDE_DURATION = 300L
        const val EXTEND_TO_WIDTH = 175
    }
}
