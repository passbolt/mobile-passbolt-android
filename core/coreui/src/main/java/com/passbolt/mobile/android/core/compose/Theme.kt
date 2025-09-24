import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.passbolt.mobile.android.core.compose.AppTypography

@Suppress("MagicNumber")
private val LightColors =
    lightColorScheme(
        primary = Color(0xFF2A9CEB),
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF333333),
    )

@Suppress("MagicNumber")
private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF2A9CEB),
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        onBackground = Color(0xFFDDDDDD),
    )

@Composable
fun PassboltTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}
