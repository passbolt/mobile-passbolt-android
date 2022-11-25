package com.passbolt.mobile.android

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.IBinder
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Root
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

/**
 * HasDrawable is custom matcher method, which help us in ui test, to check whether view displays the appropriate drawable.
 * Additionally, the method makes it possible to check whether the drawable with the set tint color is correct
 */

fun hasDrawable(
    @DrawableRes id: Int,
    @ColorRes tint: Int? = null,
    tintMode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN,
    drawablePosition: TextDrawablePosition = TextDrawablePosition.Left
) = object : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("View with drawable same as drawable with id $id")
        tint?.let { description.appendText(", tint color id: $tint, mode: $tintMode") }
    }

    override fun matchesSafely(view: View): Boolean {
        val context = view.context
        val tintColor = tint?.toColor(context)
        val expectedBitmap = context.getDrawable(id)?.tinted(tintColor, tintMode)?.toBitmap()

        return when (view) {
            is TextView -> view.compoundDrawables[drawablePosition.ordinal].toBitmap().sameAs(expectedBitmap)
            is ImageView -> view.drawable.toBitmap().sameAs(expectedBitmap)
            else -> false
        }
    }
}

private fun Int.toColor(context: Context) = ContextCompat.getColor(context, this)

private fun Drawable.tinted(@ColorInt tintColor: Int? = null, tintMode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN) =
    apply {
        setTintList(tintColor?.toColorStateList())
        setTintMode(tintMode)
    }

private fun Int.toColorStateList() = ColorStateList.valueOf(this)

enum class TextDrawablePosition {
    Left, Top, Right, Bottom
}


/**
 * HasBackgroundColor is custom matcher method, which help us in ui test, to check whether the background color matches
 * the color sent as a method parameter
 */

fun hasBackgroundColor(@ColorInt color: Int) = object : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description?) {
        description?.appendText("View background color to be $color")
    }

    override fun matchesSafely(item: View?): Boolean {
        val backgroundColor = item?.background as ColorDrawable
        val colorDrawable = item.context.getColor(color)
        return colorDrawable == backgroundColor.color
    }
}

fun isTextHidden() = object : BoundedMatcher<View?, EditText>(EditText::class.java) {
    override fun describeTo(description: Description) {
        description.appendText("Text is hidden")
    }

    override fun matchesSafely(editText: EditText): Boolean {
        // returns true if password is hidden
        return editText.transformationMethod is PasswordTransformationMethod
    }
}

fun hasToast() = object : TypeSafeMatcher<Root?>() {
    override fun matchesSafely(item: Root?): Boolean {
        val type: Int? = item?.windowLayoutParams?.get()?.type
        if (type == WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW) {
            val windowToken: IBinder = item.decorView.windowToken
            val appToken: IBinder = item.decorView.applicationWindowToken
            if (windowToken === appToken) {
                // means this window isn't contained by any other windows.
                return true
            }
        }
        return false
    }

    override fun describeTo(description: Description?) {
        description?.appendText("is toast")
    }
}

fun atPosition(position: Int, itemMatcher: Matcher<View?>) =
    object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description?) {
            description?.appendText("has item at position $position: ")
            itemMatcher.describeTo(description)
        }

        override fun matchesSafely(recyclerView: RecyclerView?): Boolean {
            val viewHolder: RecyclerView.ViewHolder =
                recyclerView?.findViewHolderForAdapterPosition(position) ?: return false // has no item on such position
            return itemMatcher.matches(viewHolder.itemView)
        }
    }
