package com.passbolt.mobile.android.jsonmodel.delegates

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.jsonmodel.JsonModel
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule

class NullableStringDelegateTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(jsonPathDelegatesTestModule)
    }

    @Test
    fun `read should work as expected`() {
        val jsonString = "test"
        val jsonModel = object : JsonModel {

            override var json: String? = jsonString

            var testStringField by NullableStringDelegate()
        }

        assertThat(jsonModel.testStringField).isEqualTo("test")
    }

    @Test
    fun `field write should work as expected`() {
        val jsonStringInputs = listOf(
            // empty field write
            "value",
            // empty field write
            ""
        )
        val jsonModels = jsonStringInputs.map { jsonString ->

            object : JsonModel {
                override var json: String? = jsonString

                var testStringField by NullableStringDelegate()
            }
        }

        jsonModels.forEach {
            it.testStringField = "test"
        }

        jsonModels.forEach {
            assertThat(it.testStringField).isEqualTo("test")
        }
    }
}
