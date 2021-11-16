package com.passbolt.mobile.android.gopenpgp.exception

class GopenPgpExceptionParser {

    fun parseGopenPgpException(exception: Exception): OpenPgpException {
        // TODO decide if detecting error types based on String error from Go library needs implementation
        return OpenPgpException(exception.message.orEmpty())
    }
}
