package com.passbolt.mobile.android.core.ui.initialsicon

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator

class InitialsIconGenerator(
    private val font: Typeface,
) {
    fun generate(
        name: String,
        initials: String,
    ): Drawable {
        val generator = ColorGenerator.MATERIAL
        val generatedColor = generator.getColor(name)
        val color = ColorUtils.blendARGB(generatedColor, Color.WHITE, LIGHT_RATIO)

        return TextDrawable
            .builder()
            .beginConfig()
            .textColor(ColorUtils.blendARGB(color, generatedColor, DARK_RATIO))
            .useFont(font)
            .endConfig()
            .buildRoundRect(initials, color, ICON_RADIUS)
    }

    companion object {
        private const val LIGHT_RATIO = 0.8f
        private const val DARK_RATIO = 0.95f
        private const val ICON_RADIUS = 4
    }
}
