package com.passbolt.mobile.android.core.autofill.urlmatcher

import org.apache.commons.validator.routines.UrlValidator
import java.net.URL

data class UrlMetadata(
    val protocolValue: ProtocolValue,
    val host: String,
    val port: Port,
) {
    sealed class ProtocolValue {
        data object None : ProtocolValue()

        data class Protocol(
            val protocol: String,
        ) : ProtocolValue()
    }

    sealed class Port {
        data object None : Port()

        data class Number(
            val port: Int,
        ) : Port()
    }

    companion object {
        private const val NO_PORT = -1
        private val urlValidator = UrlValidator()

        @Throws
        fun parse(url: String): UrlMetadata {
            val hasProtocol = hasProtocol(url)

            // add valid scheme just to validate, parse and extract host and port
            // as urls without schemes are supported
            val url = URL(if (hasProtocol) url else "http://$url")

            require(urlValidator.isValid(url.toString())) { "Invalid URL: $url" }

            return UrlMetadata(
                protocolValue = if (hasProtocol) ProtocolValue.Protocol(url.protocol) else ProtocolValue.None,
                host = url.host,
                port = if (url.port == NO_PORT) Port.None else Port.Number(url.port),
            )
        }

        fun hasProtocol(url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            return Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(url)
        }
    }
}
