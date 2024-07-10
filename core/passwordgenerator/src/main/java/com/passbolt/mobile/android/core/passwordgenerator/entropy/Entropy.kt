package com.passbolt.mobile.android.core.passwordgenerator.entropy

@Suppress("MagicNumber")
enum class Entropy(val rawValue: Int) {
    ZERO(0),
    VERY_WEAK(1),
    WEAK(60),
    FAIR(80),
    STRONG(112),
    VERY_STRONG(128),
    GREATEST_FINITE(Int.MAX_VALUE);

    companion object {
        fun parse(value: Double): Entropy = when (value) {
            in Double.NEGATIVE_INFINITY..0.0 -> ZERO
            in 0.0..1.0 -> ZERO
            in 1.0..60.0 -> VERY_WEAK
            in 60.0..80.0 -> WEAK
            in 80.0..112.0 -> FAIR
            in 112.0..128.0 -> STRONG
            in 128.0..Int.MAX_VALUE.toDouble() -> VERY_STRONG
            else -> GREATEST_FINITE
        }
    }
}
