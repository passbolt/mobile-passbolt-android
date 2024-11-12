package com.passbolt.mobile.android.jsonmodel.delegates

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.jsonmodel.JsonModel
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule

class StringDelegateTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(jsonPathDelegatesTestModule)
    }

    @Test
    fun `read should work as expected`() {
        val jsonString = "test"
        val jsonModel = object : JsonModel {
            override var json = jsonString

            var testStringField by StringDelegate()
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
                override var json = jsonString

                var testStringField by StringDelegate()
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
