package com.passbolt.mobile.android.jsonmodel.delegates

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.passbolt.mobile.android.jsonmodel.JsonModel
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule

class RootRelativeJsonPathStringDelegateTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(jsonPathDelegatesTestModule)
        }

    @Test
    fun `read should work as expected`() {
        val jsonString =
            """
            {
                "testStringField": "test"
            }
            """
        val jsonModel =
            object : JsonModel {
                override var json: String? = jsonString

                var testStringField by RootRelativeJsonPathStringDelegate(jsonPath = "testStringField")
            }

        assertThat(jsonModel.testStringField).isEqualTo("test")
    }

    @Test
    fun `field write should work as expected`() {
        val jsonStringInputs =
            listOf(
                // empty field write
                """
            {
                "testStringField": ""
            }
            """,
                // null field write
                """
            {
                "testStringField": null
            }
            """,
            )
        val jsonModels =
            jsonStringInputs.map { jsonString ->

                object : JsonModel {
                    override var json: String? = jsonString

                    var testStringField by RootRelativeJsonPathStringDelegate(jsonPath = "testStringField")
                }
            }

        jsonModels.forEach {
            it.testStringField = "test"
        }

        jsonModels.forEach {
            val jsonObject = Gson().fromJson(it.json, JsonObject::class.java)

            assertThat(jsonObject["testStringField"].asString).isEqualTo("test")
        }
    }

    @Test
    fun `field write should create new one if not existing`() {
        val jsonString =
            """
            {}
            """

        val jsonModel =
            object : JsonModel {
                override var json: String? = jsonString

                var testStringField by RootRelativeJsonPathStringDelegate(jsonPath = "testStringField")
            }

        jsonModel.testStringField = "test"

        val jsonObject = Gson().fromJson(jsonModel.json, JsonObject::class.java)
        assertThat(jsonObject["testStringField"].asString).isEqualTo("test")
    }
}
