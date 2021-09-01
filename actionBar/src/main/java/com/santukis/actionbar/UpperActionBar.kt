package com.santukis.actionbar

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.forEach
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import kotlin.math.abs

class UpperActionBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : AppBarLayout(context, attrs),
    AppBarLayout.OnOffsetChangedListener {

    interface OnActionBarElementClick {
        fun onClick(elementRes: Int) {}
    }

    companion object {
        private const val UNDEFINED = -1
    }

    private var menu: Menu? = null

    private var title: TextView? = null

    lateinit var expandedContainer: ViewGroup
        private set

    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var toolbar: Toolbar

    private var expandedToolbarBackgroundColor: Int = R.color.colorPrimary
    private var collapsedToolbarBackgroundColor: Int = R.color.colorPrimary
    private var expandedToolbarIconColor: Int = R.color.colorPrimary
    private var collapsedToolbarIconColor: Int = R.color.colorPrimary
    private var collapsedToolbarTextColor: Int = R.color.colorPrimary
    private var expandedToolbarTextColor: Int = R.color.colorPrimary

    private var listeners = mutableListOf<OnActionBarElementClick>()

    init {
        initializeViewComponents(attrs)
    }

    private fun initializeViewComponents(attrs: AttributeSet?) {
        collapsingToolbar = (initializeComponentIfAvailable(R.layout.element_toolbar) as CollapsingToolbarLayout)
        expandedContainer = collapsingToolbar.findViewById(R.id.expanded_container)
        toolbar = collapsingToolbar.findViewById(R.id.uab_toolbar)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.UpperActionBar)

        initializeTitle(attributes)
        initializeLeftIcon(attributes)
        initializeToolbar(attributes)

        attributes.recycle()

        addView(collapsingToolbar)

        addOnOffsetChangedListener(this)
    }

    private fun initializeTitle(attributes: TypedArray?) {
        val text = attributes?.getString(R.styleable.UpperActionBar_title)
        text?.apply { showTitle(text) } ?: hideTitle()
    }

    private fun initializeLeftIcon(attributes: TypedArray?) {
        val icon = attributes?.getResourceId(R.styleable.UpperActionBar_leftIcon, UNDEFINED)
        if (icon != UNDEFINED) showLeftIcon(icon!!) else hideLeftIcon()
    }

    private fun initializeToolbar(attributes: TypedArray?) {
        expandedToolbarBackgroundColor =
            attributes?.getResourceId(R.styleable.UpperActionBar_expandedToolbarBackgroundColor, R.color.colorPrimaryDark) ?: R.color.colorPrimaryDark
        collapsedToolbarBackgroundColor =
            attributes?.getResourceId(R.styleable.UpperActionBar_collapsedToolbarBackgroundColor, R.color.colorPrimaryDark)
                ?: R.color.colorPrimaryDark
        expandedToolbarIconColor =
            attributes?.getResourceId(R.styleable.UpperActionBar_expandedToolbarIconColor, R.color.colorPrimaryDark) ?: R.color.colorPrimaryDark
        collapsedToolbarIconColor =
            attributes?.getResourceId(R.styleable.UpperActionBar_collapsedToolbarIconColor, R.color.colorPrimaryDark) ?: R.color.colorPrimaryDark
        collapsedToolbarTextColor =
            attributes?.getResourceId(R.styleable.UpperActionBar_collapsedToolbarTextColor, R.color.colorPrimaryDark) ?: R.color.colorPrimaryDark
        expandedToolbarTextColor =
            attributes?.getResourceId(R.styleable.UpperActionBar_expandedToolbarTextColor, R.color.colorPrimaryDark) ?: R.color.colorPrimaryDark

        setToolbarColor()
    }

    private fun initializeComponentIfAvailable(resourceValue: Int?): View? =
        when (resourceValue) {
            null, UNDEFINED -> null
            else -> LayoutInflater.from(context).inflate(resourceValue, this, false)
        }

    private fun setToolbarColor() {
        collapsingToolbar.setContentScrimColor(ContextCompat.getColor(context, expandedToolbarBackgroundColor))
        toolbar.setBackgroundColor(ContextCompat.getColor(context, collapsedToolbarBackgroundColor))
        changeToolbarIconColor(false)
    }

    fun showLeftIcon(iconRes: Int) {
        toolbar.navigationIcon = getDrawableFor(context, iconRes)
        toolbar.setNavigationOnClickListener { listeners.forEach { it.onClick(iconRes) } }
    }

    fun hideLeftIcon() {
        toolbar.navigationIcon = null
    }

    fun showTitle(title: String, gravity: Int = Gravity.CENTER_HORIZONTAL) {
        hideTitle()

        this.title = this.title?.apply { text = title } ?: (initializeComponentIfAvailable(R.layout.element_title) as? TextView)?.apply {
            setTextColor(ContextCompat.getColor(context, collapsedToolbarTextColor))
            text = title
        }

        toolbar.addView(this.title, ActionBar.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { this.gravity = gravity })

        this.title?.isSelected = true
    }

    @SuppressLint("RestrictedApi")
    fun showExpandedTitle(title: String, gravity: Int = Gravity.BOTTOM) {
        hideTitle()
        collapsingToolbar.isTitleEnabled = true
        collapsingToolbar.title = title
        collapsingToolbar.expandedTitleGravity = gravity
        collapsingToolbar.expandedTitleMarginBottom = resources.getDimensionPixelSize(R.dimen.large_space)
        collapsingToolbar.maxLines = 3
        collapsingToolbar.setExpandedTitleTextAppearance(android.R.style.TextAppearance_DeviceDefault_Large)
        collapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(context, expandedToolbarTextColor))
        collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(context, collapsedToolbarTextColor))
    }

    fun hideTitle() {
        collapsingToolbar.isTitleEnabled = false
        collapsingToolbar.title = " "
        title?.apply {
            text = " ";
            toolbar.removeView(this)
            isSelected = false
        }
    }

    fun showMenu(menuRes: Int, shouldClearPreviousMenu: Boolean) {
        try {
            if (shouldClearPreviousMenu) clearMenu()

            toolbar.inflateMenu(menuRes)

            menu = toolbar.menu?.let {
                it.forEach { menuItem: MenuItem ->
                    menuItem.setOnMenuItemClickListener { item ->
                        listeners.forEach { listener -> listener.onClick(item.itemId) }
                        true
                    }
                }
                it
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    fun getMenu() = menu

    fun clearMenu() {
        menu?.clear()
        menu = null
    }

    fun showExpandedView(view: View) {
        toolbar.background = null
        expandedContainer.addView(view)
        (expandedContainer.getChildAt(0) as? ViewGroup)?.children?.forEach { child ->
            child.setOnClickListener {
                listeners.forEach { listener ->
                    listener.onClick(
                        child.id
                    )
                }
            }
        }
        setExpanded(true, false)
        changeToolbarIconColor(true)
        show()
    }

    fun hideExpandedView() {
        expandedContainer.children.forEach { child -> child.setOnClickListener { } }
        expandedContainer.removeAllViews()
        toolbar.setBackgroundColor(ContextCompat.getColor(context, collapsedToolbarBackgroundColor))
        setExpanded(false, false)
        changeToolbarIconColor(false)
    }

    fun isExpandedViewPopulated() = expandedContainer.childCount > 0

    fun addOnActionBarElementClickListener(vararg listeners: OnActionBarElementClick) {
        this.listeners = mutableListOf()
        this.listeners.addAll(listeners)
    }

    fun removeOnActionBarElementClickListener(listener: OnActionBarElementClick) {
        this.listeners.remove(listener)
    }

    fun hide() {
        setExpanded(false, false)
        layoutParams = layoutParams?.apply {
            width = 0
            height = 0
        }
        toolbar.visibility = View.GONE
        postInvalidate()
    }

    fun show() {
        layoutParams = layoutParams?.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        toolbar.visibility = View.VISIBLE
        postInvalidate()
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        if (totalScrollRange > 0) {
            val alpha = 1.0f - (abs(verticalOffset).toFloat() / totalScrollRange.toFloat())
            expandedContainer.children.forEach { it.alpha = alpha }
        }

        changeToolbarIconColor(abs(verticalOffset).toFloat() < (totalScrollRange - totalScrollRange / 3.0f))
    }

    private fun changeToolbarIconColor(isExpanded: Boolean) {
        val color = when (isExpanded) {
            true -> expandedToolbarIconColor
            false -> collapsedToolbarIconColor
        }
        setToolbarIconColor(color)
    }

    private fun setToolbarIconColor(colorRes: Int) {
        toolbar.menu?.forEach { item: MenuItem -> item.icon?.setColorFilter(ContextCompat.getColor(context, colorRes), PorterDuff.Mode.SRC_ATOP) }
        toolbar.navigationIcon?.setColorFilter(ContextCompat.getColor(context, colorRes), PorterDuff.Mode.SRC_ATOP)
    }
}

fun getDrawableFor(context: Context, resource: Int?): Drawable? {
    return resource?.let {
        try {
            ContextCompat.getDrawable(context, resource)

        } catch (exception: Exception) {
            null
        }
    }
}