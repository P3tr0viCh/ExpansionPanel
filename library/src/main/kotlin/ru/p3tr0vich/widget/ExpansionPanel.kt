package ru.p3tr0vich.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.PropertyValuesHolder.ofInt
import android.animation.ValueAnimator
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.IntDef
import androidx.annotation.LayoutRes

class ExpansionPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var mainLayout: FrameLayout? = null
    private var contentLayout: FrameLayout? = null
    private var stateIcon: ImageView? = null

    private var collapsedView: View? = null
    private var expandedView: View? = null

    private var collapsedHeight: Int = 0
    private var expandedHeight: Int = 0

    private var collapsedPaddingTop: Int = 0
    private var collapsedPaddingBottom: Int = 0
    private var expandedPaddingTop: Int = 0
    private var expandedPaddingBottom: Int = 0

    var isExpanded: Boolean = false
        private set

    private var animatorSet = AnimatorSet()
    private var changeViewAnimatorSet = AnimatorSet()

    private var expansionPanelListener: ExpansionPanelListener? = null

    @State
    @get:State
    var state: Int = 0
        private set(@State state) {
            field = state
            if (expansionPanelListener != null)
                when (state) {
                    COLLAPSED -> expansionPanelListener!!.onCollapsed(this)
                    COLLAPSING -> expansionPanelListener!!.onCollapsing(this)
                    EXPANDED -> expansionPanelListener!!.onExpanded(this)
                    EXPANDING -> expansionPanelListener!!.onExpanding(this)
                    UPDATING -> {
                    }
                }
        }

    private var contentLayoutHeight: Int
        get() = contentLayout!!.layoutParams.height
        set(height) {
            contentLayout!!.layoutParams.height = height
            contentLayout!!.requestLayout()
        }

    var toggleOnExpandedClick: Boolean = true
        set(value) {
            field = value
            setClickable()
        }

    var toggleOnCollapsedClick: Boolean = true
        set(value) {
            field = value
            setClickable()
        }

    private fun setClickable() {
        mainLayout?.isClickable = when {
            isExpanded -> toggleOnExpandedClick
            else -> toggleOnCollapsedClick
        }
    }

    var durationToggle: Long = 0
        set(value) {
            field = if (value >= 0)
                value
            else
                resources.getInteger(R.integer.expansion_panel_animation_duration_toggle).toLong()
        }

    var delayCollapsedViewHiding: Long = 0
        set(value) {
            field = if (value >= 0)
                value
            else
                resources.getInteger(R.integer.expansion_panel_animation_delay_collapsed_view_hiding)
                    .toLong()
        }

    var durationCollapsedViewChangeVisibility: Long = 0
        set(value) {
            field = if (value >= 0)
                value
            else
                resources.getInteger(R.integer.expansion_panel_animation_duration_collapsed_view_change_visibility)
                    .toLong()
        }

    var durationContentHeightChanged: Long = 0
        set(value) {
            field = if (value >= 0)
                value
            else
                resources.getInteger(R.integer.expansion_panel_animation_duration_content_height_changed)
                    .toLong()
        }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(COLLAPSED, EXPANDED, COLLAPSING, EXPANDING, UPDATING)
    annotation class State

    init {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        @LayoutRes var collapsedLayoutId = R.layout.expansion_panel_collapsed_default
        @LayoutRes var expandedLayoutId = R.layout.expansion_panel_expanded_default

        val a = context.obtainStyledAttributes(attrs, R.styleable.ExpansionPanel, 0, 0)

        try {
            isExpanded = a.getBoolean(R.styleable.ExpansionPanel_defaultExpanded, false)

            collapsedLayoutId =
                a.getResourceId(R.styleable.ExpansionPanel_collapsedLayout, collapsedLayoutId)
            expandedLayoutId =
                a.getResourceId(R.styleable.ExpansionPanel_expandedLayout, expandedLayoutId)

            toggleOnCollapsedClick =
                a.getBoolean(R.styleable.ExpansionPanel_toggleOnCollapsedClick, true)
            toggleOnExpandedClick =
                a.getBoolean(R.styleable.ExpansionPanel_toggleOnExpandedClick, true)

            durationToggle = a.getInteger(
                R.styleable.ExpansionPanel_durationToggle,
                resources.getInteger(R.integer.expansion_panel_animation_duration_toggle)
            ).toLong()
            delayCollapsedViewHiding = a.getInteger(
                R.styleable.ExpansionPanel_delayCollapsedViewHiding,
                resources.getInteger(R.integer.expansion_panel_animation_delay_collapsed_view_hiding)
            ).toLong()
            durationCollapsedViewChangeVisibility = a.getInteger(
                R.styleable.ExpansionPanel_durationCollapsedViewChangeVisibility,
                resources.getInteger(R.integer.expansion_panel_animation_duration_collapsed_view_change_visibility)
            ).toLong()
            durationContentHeightChanged = a.getInteger(
                R.styleable.ExpansionPanel_durationContentHeightChanged,
                resources.getInteger(R.integer.expansion_panel_animation_duration_content_height_changed)
            ).toLong()
        } finally {
            a.recycle()
        }

        collapsedPaddingTop =
            resources.getDimensionPixelSize(R.dimen.expansion_panel_collapsed_padding_top)
        collapsedPaddingBottom =
            resources.getDimensionPixelSize(R.dimen.expansion_panel_collapsed_padding_bottom)
        expandedPaddingTop =
            resources.getDimensionPixelSize(R.dimen.expansion_panel_expanded_padding_top)
        expandedPaddingBottom =
            resources.getDimensionPixelSize(R.dimen.expansion_panel_expanded_padding_bottom)

        val inflater =
            getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        mainLayout = inflater.inflate(R.layout.expansion_panel, this, false) as FrameLayout

        addView(mainLayout)

        contentLayout = mainLayout!!.findViewById(R.id.expansion_panel_content_layout)
        stateIcon = mainLayout!!.findViewById(R.id.expansion_panel_state_icon)

        mainLayout!!.setOnClickListener { toggle(true) }

        setCollapsedView(collapsedLayoutId)
        setExpandedView(expandedLayoutId)

        measureHeights()

        setExpanded(isExpanded, false)
    }

    fun setCollapsedView(collapsedView: View) {
        contentLayout!!.addView(collapsedView, COLLAPSED_CHILD_INDEX)
        this.collapsedView = collapsedView

        this.collapsedView!!.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (state == COLLAPSED) {
                measureCollapsedHeight()
                this@ExpansionPanel.post { checkHeights() }
            }
        }
    }

    fun setCollapsedView(@LayoutRes collapsedViewId: Int) {
        setCollapsedView(
            LayoutInflater.from(context).inflate(collapsedViewId, contentLayout, false)
        )
    }

    fun setExpandedView(expandedView: View) {
        contentLayout!!.addView(expandedView, EXPANDED_CHILD_INDEX)
        this.expandedView = expandedView

        this.expandedView!!.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (state == EXPANDED) {
                measureExpandedHeight()
                this@ExpansionPanel.post { checkHeights() }
            }
        }
    }

    fun setExpandedView(@LayoutRes expandedViewId: Int) {
        setExpandedView(LayoutInflater.from(context).inflate(expandedViewId, contentLayout, false))
    }

    private fun getContentMeasuredHeight(
        view: View,
        minHeight: Int,
        paddingTop: Int,
        paddingBottom: Int
    ): Int {
        var minHeightInt = minHeight
        view.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        var height = view.measuredHeight

        minHeightInt = minHeightInt - paddingTop - paddingBottom

        if (height < minHeightInt) height = minHeightInt

        return height
    }

    private fun measureCollapsedHeight() {
        collapsedHeight = getContentMeasuredHeight(
            collapsedView!!,
            resources.getDimensionPixelSize(R.dimen.expansion_panel_collapsed_min_height),
            collapsedPaddingTop, collapsedPaddingBottom
        )
    }

    private fun measureExpandedHeight() {
        expandedHeight = getContentMeasuredHeight(
            expandedView!!,
            resources.getDimensionPixelSize(R.dimen.expansion_panel_expanded_min_height),
            expandedPaddingTop, expandedPaddingBottom
        )
    }

    private fun measureHeights() {
        measureCollapsedHeight()
        measureExpandedHeight()
    }

    private fun setMainLayoutPadding(top: Int, bottom: Int) {
        val start = mainLayout!!.paddingStart
        val end = mainLayout!!.paddingEnd
        mainLayout!!.setPaddingRelative(start, top, end, bottom)
    }

    private fun checkHeights() {
        val height = contentLayoutHeight

        if (isExpanded) {
            if (expandedHeight == height) return
        } else {
            if (collapsedHeight == height) return
        }

        if (isShown && durationContentHeightChanged > 0) {
            val resizePanelValueAnimator = ValueAnimator.ofInt(
                height, if (isExpanded) expandedHeight else collapsedHeight
            )
            resizePanelValueAnimator.duration = durationContentHeightChanged
            resizePanelValueAnimator.interpolator = AccelerateDecelerateInterpolator()
            resizePanelValueAnimator.addUpdateListener { animation ->
                contentLayoutHeight = animation.animatedValue as Int
            }
            resizePanelValueAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    state = UPDATING
                }

                override fun onAnimationEnd(animation: Animator) {
                    state = if (isExpanded) EXPANDED else COLLAPSED
                }
            })

            resizePanelValueAnimator.start()
        } else {
            state = UPDATING
            contentLayoutHeight = if (isExpanded) expandedHeight else collapsedHeight
            state = if (isExpanded) EXPANDED else COLLAPSED
        }
    }

    fun notifyContentHeightChanged() {
        measureHeights()
        checkHeights()
    }

    @JvmOverloads
    fun expand(animate: Boolean = true) {
        if (!isExpanded) toggle(animate)
    }

    @JvmOverloads
    fun collapse(animate: Boolean = true) {
        if (isExpanded) toggle(animate)
    }

    @JvmOverloads
    fun toggle(animate: Boolean = true) {
        setExpanded(!isExpanded, animate)
    }

    fun setExpanded(expanded: Boolean, animate: Boolean) {
        isExpanded = expanded

        setClickable()

        val hidingView: View?
        val showingView: View?

        if (isExpanded) {
            hidingView = collapsedView
            showingView = expandedView

            measureExpandedHeight()

            state = EXPANDING
        } else {
            hidingView = expandedView
            showingView = collapsedView

            measureCollapsedHeight()

            state = COLLAPSING
        }

        if (animate && isShown && this.durationToggle > 0) {
            val resizePanelAnimator = ValueAnimator.ofInt(
                contentLayoutHeight,
                if (isExpanded) expandedHeight else collapsedHeight
            )
            resizePanelAnimator.interpolator = AccelerateDecelerateInterpolator()
            resizePanelAnimator.addUpdateListener { animation ->
                contentLayoutHeight = animation.animatedValue as Int
            }

            val holderPaddingTop = ofInt(
                PROPERTY_NAME_PADDING_TOP,
                mainLayout!!.paddingTop,
                if (isExpanded) expandedPaddingTop else collapsedPaddingTop
            )
            val holderPaddingBottom = ofInt(
                PROPERTY_NAME_PADDING_BOTTOM,
                mainLayout!!.paddingBottom,
                if (isExpanded) expandedPaddingBottom else collapsedPaddingBottom
            )

            val resizePaddingAnimator = ValueAnimator.ofPropertyValuesHolder(
                holderPaddingTop, holderPaddingBottom
            )
            resizePaddingAnimator.interpolator = LinearInterpolator()
            resizePaddingAnimator.addUpdateListener { animation ->
                setMainLayoutPadding(
                    animation.getAnimatedValue(PROPERTY_NAME_PADDING_TOP) as Int,
                    animation.getAnimatedValue(PROPERTY_NAME_PADDING_BOTTOM) as Int
                )
            }

            val stateIconRotation = stateIcon!!.rotation

            val rotateIconAnimator =
                ValueAnimator.ofFloat(stateIconRotation, if (isExpanded) 180f else 0f)
            rotateIconAnimator.interpolator = AccelerateDecelerateInterpolator()
            rotateIconAnimator.addUpdateListener { animation ->
                stateIcon!!.rotation = animation.animatedValue as Float
            }

            val delayCollapsedViewHidingLocal: Long
            val durationCollapsedViewChangeVisibilityLocal: Long

            if (delayCollapsedViewHiding > durationToggle ||
                durationCollapsedViewChangeVisibility > durationToggle ||
                delayCollapsedViewHiding + durationCollapsedViewChangeVisibility > durationToggle
            ) {
                delayCollapsedViewHidingLocal = 0
                durationCollapsedViewChangeVisibilityLocal = 0
            } else {
                delayCollapsedViewHidingLocal = delayCollapsedViewHiding
                durationCollapsedViewChangeVisibilityLocal = durationCollapsedViewChangeVisibility
            }

            val delay: Long
            val duration: Long

            val durationHide: Long
            val durationShow: Long

            val durationExpandedViewChangeVisibilityLocal =
                durationToggle - delayCollapsedViewHidingLocal - durationCollapsedViewChangeVisibilityLocal

            if (isExpanded) {
                if (stateIconRotation == 0f) {
                    duration = durationToggle

                    delay = delayCollapsedViewHidingLocal
                    durationHide = durationCollapsedViewChangeVisibilityLocal
                    durationShow = durationExpandedViewChangeVisibilityLocal
                } else {
                    duration = (durationToggle * (180f - stateIconRotation) / 180f).toLong()

                    when {
                        duration <= durationExpandedViewChangeVisibilityLocal -> {
                            delay = 0
                            durationHide = 0
                            durationShow = duration
                        }

                        duration < durationCollapsedViewChangeVisibilityLocal + durationExpandedViewChangeVisibilityLocal -> {
                            delay = 0
                            durationHide = duration - durationExpandedViewChangeVisibilityLocal
                            durationShow = durationExpandedViewChangeVisibilityLocal
                        }

                        else -> {
                            delay =
                                duration - (durationCollapsedViewChangeVisibilityLocal + durationExpandedViewChangeVisibilityLocal)
                            durationHide = durationCollapsedViewChangeVisibilityLocal
                            durationShow = durationExpandedViewChangeVisibilityLocal
                        }
                    }
                }
            } else {
                delay = 0

                if (stateIconRotation == 180f) {
                    duration = durationToggle

                    durationShow = durationCollapsedViewChangeVisibilityLocal
                    durationHide = durationExpandedViewChangeVisibilityLocal
                } else {
                    duration = (durationToggle * stateIconRotation / 180f).toLong()

                    when {
                        duration <= delayCollapsedViewHidingLocal -> {
                            durationShow = 0
                            durationHide = 0
                        }

                        duration <= delayCollapsedViewHidingLocal + durationCollapsedViewChangeVisibilityLocal -> {
                            durationShow = duration - delayCollapsedViewHidingLocal
                            durationHide = 0
                        }

                        else -> {
                            durationShow = durationCollapsedViewChangeVisibilityLocal
                            durationHide =
                                duration - (delayCollapsedViewHidingLocal + durationCollapsedViewChangeVisibilityLocal)
                        }
                    }
                }
            }

            val hideAnimator: ValueAnimator?

            if (durationHide > 0 && hidingView != null) {
                hideAnimator = ValueAnimator.ofFloat(hidingView.alpha, 0f)
                hideAnimator.interpolator = LinearInterpolator()
                hideAnimator.duration = durationHide
                hideAnimator.addUpdateListener { animation ->
                    hidingView.alpha = animation.animatedValue as Float
                }
            } else {
                hideAnimator = null
            }

            val showAnimator: ValueAnimator?

            if (durationShow > 0 && showingView != null) {
                showAnimator = ValueAnimator.ofFloat(showingView.alpha, 1f)
                showAnimator.interpolator = LinearInterpolator()
                showAnimator.duration = durationShow
                showAnimator.addUpdateListener { animation ->
                    showingView.alpha = animation.animatedValue as Float
                }
            } else {
                showAnimator = null
            }

            animatorSet.cancel()
            changeViewAnimatorSet.cancel()

            animatorSet = AnimatorSet()
            animatorSet.startDelay =
                resources.getInteger(R.integer.expansion_panel_animation_delay_toggle).toLong()

            changeViewAnimatorSet = AnimatorSet()

            if (delay > 0) changeViewAnimatorSet.startDelay = delay

            if (hideAnimator != null && showAnimator != null) {
                changeViewAnimatorSet.playSequentially(hideAnimator, showAnimator)
            } else {
                if (showAnimator != null) changeViewAnimatorSet.play(showAnimator)
                if (hideAnimator != null) changeViewAnimatorSet.play(hideAnimator)
            }
            changeViewAnimatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    hidingView?.visibility = if (durationHide > 0) View.VISIBLE else View.GONE
                    showingView?.visibility = if (durationShow > 0) View.VISIBLE else View.GONE
                }

                override fun onAnimationEnd(animation: Animator) {
                    hidingView?.alpha = 0f
                    showingView?.alpha = 1f

                    hidingView?.visibility = View.GONE
                    showingView?.visibility = View.VISIBLE
                }
            })

            animatorSet.duration = duration
            animatorSet.playTogether(
                resizePanelAnimator,
                resizePaddingAnimator,
                rotateIconAnimator
            )
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(windowToken, 0)
                }

                override fun onAnimationEnd(animation: Animator) {
                    val height = contentLayoutHeight
                    if (isExpanded) {
                        if (height == expandedHeight) state = EXPANDED
                    } else {
                        if (height == collapsedHeight) state = COLLAPSED
                    }
                }
            })

            animatorSet.start()
            changeViewAnimatorSet.start()
        } else {
            hidingView?.alpha = 0f
            showingView?.alpha = 1f

            hidingView?.visibility = View.GONE
            showingView?.visibility = View.VISIBLE

            if (isExpanded) {
                contentLayoutHeight = expandedHeight

                setMainLayoutPadding(expandedPaddingTop, expandedPaddingBottom)

                stateIcon?.rotation = 180f

                state = EXPANDED
            } else {
                contentLayoutHeight = collapsedHeight

                setMainLayoutPadding(collapsedPaddingTop, collapsedPaddingBottom)

                stateIcon?.rotation = 0f

                state = COLLAPSED
            }
        }
    }

    fun setListener(listener: ExpansionPanelListener?) {
        expansionPanelListener = listener
    }

    fun hasListener(): Boolean {
        return expansionPanelListener != null
    }

    internal class SavedState : BaseSavedState {

        internal var collapsedHeight: Int = 0
        internal var expandedHeight: Int = 0

        internal var expanded: Boolean = false

        internal constructor(superState: Parcelable?) : super(superState)

        private constructor(source: Parcel) : super(source) {
            collapsedHeight = source.readInt()
            expandedHeight = source.readInt()
            expanded = source.readInt() != 0
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(if (expanded) 1 else 0)
            out.writeInt(expandedHeight)
            out.writeInt(collapsedHeight)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.collapsedHeight = collapsedHeight
        ss.expandedHeight = expandedHeight
        ss.expanded = isExpanded
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        collapsedHeight = ss.collapsedHeight
        expandedHeight = ss.expandedHeight
        setExpanded(ss.expanded, false)
    }

    companion object {

        private const val COLLAPSED_CHILD_INDEX = 0
        private const val EXPANDED_CHILD_INDEX = 1

        private const val PROPERTY_NAME_PADDING_TOP = "paddingTop"
        private const val PROPERTY_NAME_PADDING_BOTTOM = "paddingBottom"

        private const val COLLAPSED = 0
        private const val EXPANDED = 1
        private const val COLLAPSING = 2
        private const val EXPANDING = 3
        private const val UPDATING = 4
    }
}