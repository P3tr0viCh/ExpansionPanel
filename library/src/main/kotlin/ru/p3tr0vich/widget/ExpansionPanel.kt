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

class ExpansionPanel @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private var mMainLayout: FrameLayout? = null
    private var mContentLayout: FrameLayout? = null
    private var mStateIcon: ImageView? = null

    private var mCollapsedView: View? = null
    private var mExpandedView: View? = null

    private var mCollapsedHeight: Int = 0
    private var mExpandedHeight: Int = 0

    private var mCollapsedPaddingTop: Int = 0
    private var mCollapsedPaddingBottom: Int = 0
    private var mExpandedPaddingTop: Int = 0
    private var mExpandedPaddingBottom: Int = 0

    private var mDurationToggle: Long = 0
    private var mDelayCollapsedViewHiding: Long = 0
    private var mDurationCollapsedViewChangeVisibility: Long = 0
    private var mDurationContentHeightChanged: Long = 0

    var isExpanded: Boolean = false
        private set

    private var mToggleOnCollapsedClickEnabled: Boolean = false
    private var mToggleOnExpandedClickEnabled: Boolean = false

    private var mAnimatorSet = AnimatorSet()
    private var mChangeViewAnimatorSet = AnimatorSet()

    private var mListener: ExpansionPanelListener? = null

    @State
    @get:State
    var state: Int = 0
        private set(@State state) {
            field = state
            if (mListener != null)
                when (state) {
                    COLLAPSED -> mListener!!.onCollapsed(this)
                    COLLAPSING -> mListener!!.onCollapsing(this)
                    EXPANDED -> mListener!!.onExpanded(this)
                    EXPANDING -> mListener!!.onExpanding(this)
                    UPDATING -> {
                    }
                }
        }

    private var contentLayoutHeight: Int
        get() = mContentLayout!!.layoutParams.height
        set(height) {
            mContentLayout!!.layoutParams.height = height
            mContentLayout!!.requestLayout()
        }

    var isToggleOnExpandedClickEnabled: Boolean
        get() = mToggleOnExpandedClickEnabled
        set(toggleOnExpandedClickEnabled) {
            mToggleOnExpandedClickEnabled = toggleOnExpandedClickEnabled
            if (isExpanded) mMainLayout!!.isClickable = mToggleOnExpandedClickEnabled
        }

    var isToggleOnCollapsedClickEnabled: Boolean
        get() = mToggleOnCollapsedClickEnabled
        set(toggleOnCollapsedClickEnabled) {
            mToggleOnCollapsedClickEnabled = toggleOnCollapsedClickEnabled
            if (!isExpanded) mMainLayout!!.isClickable = mToggleOnCollapsedClickEnabled
        }

    var durationToggle: Long
        get() = mDurationToggle
        set(durationToggle) {
            mDurationToggle = if (durationToggle >= 0)
                durationToggle
            else
                resources.getInteger(R.integer.expansion_panel_animation_duration_toggle).toLong()
        }

    var delayCollapsedViewHiding: Long
        get() = mDelayCollapsedViewHiding
        set(delayCollapsedViewHiding) {
            mDelayCollapsedViewHiding = if (delayCollapsedViewHiding >= 0)
                delayCollapsedViewHiding
            else
                resources.getInteger(R.integer.expansion_panel_animation_delay_collapsed_view_hiding).toLong()
        }

    var durationCollapsedViewChangeVisibility: Long
        get() = mDurationCollapsedViewChangeVisibility
        set(durationCollapsedViewChangeVisibility) {
            mDurationCollapsedViewChangeVisibility = if (durationCollapsedViewChangeVisibility >= 0)
                durationCollapsedViewChangeVisibility
            else
                resources.getInteger(R.integer.expansion_panel_animation_duration_collapsed_view_change_visibility).toLong()
        }

    var durationContentHeightChanged: Long
        get() = mDurationContentHeightChanged
        set(durationContentHeightChanged) {
            mDurationContentHeightChanged = if (durationContentHeightChanged >= 0)
                durationContentHeightChanged
            else
                resources.getInteger(R.integer.expansion_panel_animation_duration_content_height_changed).toLong()
        }

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
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

            collapsedLayoutId = a.getResourceId(R.styleable.ExpansionPanel_collapsedLayout, collapsedLayoutId)
            expandedLayoutId = a.getResourceId(R.styleable.ExpansionPanel_expandedLayout, expandedLayoutId)

            mToggleOnCollapsedClickEnabled = a.getBoolean(R.styleable.ExpansionPanel_toggleOnCollapsedClickEnabled, true)
            mToggleOnExpandedClickEnabled = a.getBoolean(R.styleable.ExpansionPanel_toggleOnExpandedClickEnabled, true)

            mDurationToggle = a.getInteger(R.styleable.ExpansionPanel_durationToggle, resources.getInteger(R.integer.expansion_panel_animation_duration_toggle)).toLong()
            mDelayCollapsedViewHiding = a.getInteger(R.styleable.ExpansionPanel_delayCollapsedViewHiding, resources.getInteger(R.integer.expansion_panel_animation_delay_collapsed_view_hiding)).toLong()
            mDurationCollapsedViewChangeVisibility = a.getInteger(R.styleable.ExpansionPanel_durationCollapsedViewChangeVisibility, resources.getInteger(R.integer.expansion_panel_animation_duration_collapsed_view_change_visibility)).toLong()
            mDurationContentHeightChanged = a.getInteger(R.styleable.ExpansionPanel_durationContentHeightChanged, resources.getInteger(R.integer.expansion_panel_animation_duration_content_height_changed)).toLong()
        } finally {
            a.recycle()
        }

        mCollapsedPaddingTop = resources.getDimensionPixelSize(R.dimen.expansion_panel_collapsed_padding_top)
        mCollapsedPaddingBottom = resources.getDimensionPixelSize(R.dimen.expansion_panel_collapsed_padding_bottom)
        mExpandedPaddingTop = resources.getDimensionPixelSize(R.dimen.expansion_panel_expanded_padding_top)
        mExpandedPaddingBottom = resources.getDimensionPixelSize(R.dimen.expansion_panel_expanded_padding_bottom)

        val inflater = getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        mMainLayout = inflater.inflate(R.layout.expansion_panel, this, false) as FrameLayout

        addView(mMainLayout)

        mContentLayout = mMainLayout!!.findViewById(R.id.expansion_panel_content_layout)
        mStateIcon = mMainLayout!!.findViewById(R.id.expansion_panel_state_icon)

        mMainLayout!!.setOnClickListener { toggle(true) }

        setCollapsedView(collapsedLayoutId)
        setExpandedView(expandedLayoutId)

        measureHeights()

        setExpanded(isExpanded, false)
    }

    fun setCollapsedView(collapsedView: View) {
        mContentLayout!!.addView(collapsedView, COLLAPSED_CHILD_INDEX)
        mCollapsedView = collapsedView

        mCollapsedView!!.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (state == COLLAPSED) {
                measureCollapsedHeight()
                this@ExpansionPanel.post { checkHeights() }
            }
        }
    }

    fun setCollapsedView(@LayoutRes collapsedViewId: Int) {
        setCollapsedView(LayoutInflater.from(context).inflate(collapsedViewId, mContentLayout, false))
    }

    fun setExpandedView(expandedView: View) {
        mContentLayout!!.addView(expandedView, EXPANDED_CHILD_INDEX)
        mExpandedView = expandedView

        mExpandedView!!.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (state == EXPANDED) {
                measureExpandedHeight()
                this@ExpansionPanel.post { checkHeights() }
            }
        }
    }

    fun setExpandedView(@LayoutRes expandedViewId: Int) {
        setExpandedView(LayoutInflater.from(context).inflate(expandedViewId, mContentLayout, false))
    }

    private fun getContentMeasuredHeight(view: View, minHeight: Int, paddingTop: Int, paddingBottom: Int): Int {
        var minHeightInt = minHeight
        view.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        var height = view.measuredHeight

        minHeightInt = minHeightInt - paddingTop - paddingBottom

        if (height < minHeightInt) height = minHeightInt

        return height
    }

    private fun measureCollapsedHeight() {
        mCollapsedHeight = getContentMeasuredHeight(mCollapsedView!!,
                resources.getDimensionPixelSize(R.dimen.expansion_panel_collapsed_min_height),
                mCollapsedPaddingTop, mCollapsedPaddingBottom)
    }

    private fun measureExpandedHeight() {
        mExpandedHeight = getContentMeasuredHeight(mExpandedView!!,
                resources.getDimensionPixelSize(R.dimen.expansion_panel_expanded_min_height),
                mExpandedPaddingTop, mExpandedPaddingBottom)
    }

    private fun measureHeights() {
        measureCollapsedHeight()
        measureExpandedHeight()
    }

    private fun setMainLayoutPadding(top: Int, bottom: Int) {
        val start = mMainLayout!!.paddingStart
        val end = mMainLayout!!.paddingEnd
        mMainLayout!!.setPaddingRelative(start, top, end, bottom)
    }

    private fun checkHeights() {
        val height = contentLayoutHeight

        if (isExpanded) {
            if (mExpandedHeight == height) return
        } else {
            if (mCollapsedHeight == height) return
        }

        if (isShown && mDurationContentHeightChanged > 0) {
            val resizePanelValueAnimator = ValueAnimator.ofInt(
                    height, if (isExpanded) mExpandedHeight else mCollapsedHeight)
            resizePanelValueAnimator.duration = mDurationContentHeightChanged
            resizePanelValueAnimator.interpolator = AccelerateDecelerateInterpolator()
            resizePanelValueAnimator.addUpdateListener { animation -> contentLayoutHeight = animation.animatedValue as Int }
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
            contentLayoutHeight = if (isExpanded) mExpandedHeight else mCollapsedHeight
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

        isToggleOnCollapsedClickEnabled = mToggleOnCollapsedClickEnabled
        isToggleOnExpandedClickEnabled = mToggleOnExpandedClickEnabled

        val hidingView: View?
        val showingView: View?

        if (isExpanded) {
            hidingView = mCollapsedView
            showingView = mExpandedView

            measureExpandedHeight()

            state = EXPANDING
        } else {
            hidingView = mExpandedView
            showingView = mCollapsedView

            measureCollapsedHeight()

            state = COLLAPSING
        }

        if (animate && isShown && mDurationToggle > 0) {
            val resizePanelAnimator = ValueAnimator.ofInt(
                    contentLayoutHeight,
                    if (isExpanded) mExpandedHeight else mCollapsedHeight)
            resizePanelAnimator.interpolator = AccelerateDecelerateInterpolator()
            resizePanelAnimator.addUpdateListener { animation -> contentLayoutHeight = animation.animatedValue as Int }

            val holderPaddingTop = ofInt(PROPERTY_NAME_PADDING_TOP,
                    mMainLayout!!.paddingTop,
                    if (isExpanded) mExpandedPaddingTop else mCollapsedPaddingTop)
            val holderPaddingBottom = ofInt(PROPERTY_NAME_PADDING_BOTTOM,
                    mMainLayout!!.paddingBottom,
                    if (isExpanded) mExpandedPaddingBottom else mCollapsedPaddingBottom)

            val resizePaddingAnimator = ValueAnimator.ofPropertyValuesHolder(
                    holderPaddingTop, holderPaddingBottom)
            resizePaddingAnimator.interpolator = LinearInterpolator()
            resizePaddingAnimator.addUpdateListener { animation ->
                setMainLayoutPadding(
                        animation.getAnimatedValue(PROPERTY_NAME_PADDING_TOP) as Int,
                        animation.getAnimatedValue(PROPERTY_NAME_PADDING_BOTTOM) as Int)
            }

            val stateIconRotation = mStateIcon!!.rotation

            val rotateIconAnimator = ValueAnimator.ofFloat(stateIconRotation, if (isExpanded) 180f else 0f)
            rotateIconAnimator.interpolator = AccelerateDecelerateInterpolator()
            rotateIconAnimator.addUpdateListener { animation -> mStateIcon!!.rotation = animation.animatedValue as Float }

            val delayCollapsedViewHiding: Long
            val durationCollapsedViewChangeVisibility: Long

            if (mDelayCollapsedViewHiding > mDurationToggle ||
                    mDurationCollapsedViewChangeVisibility > mDurationToggle ||
                    mDelayCollapsedViewHiding + mDurationCollapsedViewChangeVisibility > mDurationToggle) {
                delayCollapsedViewHiding = 0
                durationCollapsedViewChangeVisibility = 0
            } else {
                delayCollapsedViewHiding = mDelayCollapsedViewHiding
                durationCollapsedViewChangeVisibility = mDurationCollapsedViewChangeVisibility
            }

            val duration: Long
            val delay: Long

            val durationHide: Long
            val durationShow: Long

            val durationExpandedViewChangeVisibility = mDurationToggle - delayCollapsedViewHiding - durationCollapsedViewChangeVisibility

            if (isExpanded) {
                if (stateIconRotation == 0f) {
                    duration = mDurationToggle

                    delay = delayCollapsedViewHiding
                    durationHide = durationCollapsedViewChangeVisibility
                    durationShow = durationExpandedViewChangeVisibility
                } else {
                    duration = (mDurationToggle * (180f - stateIconRotation) / 180f).toLong()

                    when {
                        duration <= durationExpandedViewChangeVisibility -> {
                            delay = 0
                            durationHide = 0
                            durationShow = duration
                        }
                        duration < durationCollapsedViewChangeVisibility + durationExpandedViewChangeVisibility -> {
                            delay = 0
                            durationHide = duration - durationExpandedViewChangeVisibility
                            durationShow = durationExpandedViewChangeVisibility
                        }
                        else -> {
                            delay = duration - (durationCollapsedViewChangeVisibility + durationExpandedViewChangeVisibility)
                            durationHide = durationCollapsedViewChangeVisibility
                            durationShow = durationExpandedViewChangeVisibility
                        }
                    }
                }
            } else {
                delay = 0

                if (stateIconRotation == 180f) {
                    duration = mDurationToggle

                    durationShow = durationCollapsedViewChangeVisibility
                    durationHide = durationExpandedViewChangeVisibility
                } else {
                    duration = (mDurationToggle * stateIconRotation / 180f).toLong()

                    when {
                        duration <= delayCollapsedViewHiding -> {
                            durationShow = 0
                            durationHide = 0
                        }
                        duration <= delayCollapsedViewHiding + durationCollapsedViewChangeVisibility -> {
                            durationShow = duration - delayCollapsedViewHiding
                            durationHide = 0
                        }
                        else -> {
                            durationShow = durationCollapsedViewChangeVisibility
                            durationHide = duration - (delayCollapsedViewHiding + durationCollapsedViewChangeVisibility)
                        }
                    }
                }
            }

            val hideAnimator: ValueAnimator?

            if (durationHide > 0) {
                hideAnimator = ValueAnimator.ofFloat(hidingView!!.alpha, 0f)
                hideAnimator!!.interpolator = LinearInterpolator()
                hideAnimator.duration = durationHide
                hideAnimator.addUpdateListener { animation -> hidingView.alpha = animation.animatedValue as Float }
            } else
                hideAnimator = null

            val showAnimator: ValueAnimator?

            if (durationShow > 0) {
                showAnimator = ValueAnimator.ofFloat(showingView!!.alpha, 1f)
                showAnimator!!.interpolator = LinearInterpolator()
                showAnimator.duration = durationShow
                showAnimator.addUpdateListener { animation -> showingView.alpha = animation.animatedValue as Float }
            } else
                showAnimator = null

            mAnimatorSet.cancel()
            mChangeViewAnimatorSet.cancel()

            mAnimatorSet = AnimatorSet()
            mAnimatorSet.startDelay = resources.getInteger(R.integer.expansion_panel_animation_delay_toggle).toLong()

            mChangeViewAnimatorSet = AnimatorSet()

            if (delay > 0) mChangeViewAnimatorSet.startDelay = delay

            if (hideAnimator != null && showAnimator != null) {
                mChangeViewAnimatorSet.playSequentially(hideAnimator, showAnimator)
            } else {
                if (showAnimator != null) mChangeViewAnimatorSet.play(showAnimator)
                if (hideAnimator != null) mChangeViewAnimatorSet.play(hideAnimator)
            }
            mChangeViewAnimatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    hidingView!!.visibility = if (durationHide > 0) View.VISIBLE else View.GONE
                    showingView!!.visibility = if (durationShow > 0) View.VISIBLE else View.GONE
                }

                override fun onAnimationEnd(animation: Animator) {
                    hidingView!!.alpha = 0f
                    showingView!!.alpha = 1f

                    hidingView.visibility = View.GONE
                    showingView.visibility = View.VISIBLE
                }
            })

            mAnimatorSet.duration = duration
            mAnimatorSet.playTogether(resizePanelAnimator, resizePaddingAnimator, rotateIconAnimator)
            mAnimatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .hideSoftInputFromWindow(windowToken, 0)
                }

                override fun onAnimationEnd(animation: Animator) {
                    val height = contentLayoutHeight
                    if (isExpanded) {
                        if (height == mExpandedHeight) state = EXPANDED
                    } else {
                        if (height == mCollapsedHeight) state = COLLAPSED
                    }
                }
            })

            mAnimatorSet.start()
            mChangeViewAnimatorSet.start()
        } else {
            hidingView!!.alpha = 0f
            showingView!!.alpha = 1f

            hidingView.visibility = View.GONE
            showingView.visibility = View.VISIBLE

            if (isExpanded) {
                contentLayoutHeight = mExpandedHeight

                setMainLayoutPadding(mExpandedPaddingTop, mExpandedPaddingBottom)

                mStateIcon!!.rotation = 180f

                state = EXPANDED
            } else {
                contentLayoutHeight = mCollapsedHeight

                setMainLayoutPadding(mCollapsedPaddingTop, mCollapsedPaddingBottom)

                mStateIcon!!.rotation = 0f

                state = COLLAPSED
            }
        }
    }

    fun setListener(listener: ExpansionPanelListener?) {
        mListener = listener
    }

    fun hasListener(): Boolean {
        return mListener != null
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

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.collapsedHeight = mCollapsedHeight
        ss.expandedHeight = mExpandedHeight
        ss.expanded = isExpanded
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        mCollapsedHeight = ss.collapsedHeight
        mExpandedHeight = ss.expandedHeight
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