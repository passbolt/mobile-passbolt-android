package com.passbolt.mobile.android.common.validation

class RequiredIntInInclusiveRangeValidation : (Int, InclusiveMin, InclusiveMax) -> Boolean {
    override fun invoke(
        value: Int,
        minLength: MinLength,
        maxLength: MaxLength,
    ) = value in minLength..maxLength
}
