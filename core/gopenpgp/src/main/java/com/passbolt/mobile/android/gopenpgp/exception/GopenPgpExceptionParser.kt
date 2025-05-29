package com.passbolt.mobile.android.gopenpgp.exception

class GopenPgpExceptionParser {
    fun parseGopenPgpException(exception: Exception): OpenPgpError {
        // TODO decide if detecting error types based on String error from Go library needs implementation
        return OpenPgpError(exception.message.orEmpty())
    }
}
