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

class RootRelativeJsonPathTotpDelegateTest : KoinTest {
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
             "testTotpField" : {
                    "algorithm" : "testAlgo",
                    "secret_key" : "testKey",
                    "digits" : 6,
                    "period" : 30
                }
            }
            """
        val jsonModel =
            object : JsonModel {
                override var json: String? = jsonString

                var testTotpField by RootRelativeJsonPathTotpDelegate(jsonPath = "testTotpField")
            }

        assertThat(jsonModel.testTotpField?.algorithm).isEqualTo("testAlgo")
        assertThat(jsonModel.testTotpField?.key).isEqualTo("testKey")
        assertThat(jsonModel.testTotpField?.digits).isEqualTo(6)
        assertThat(jsonModel.testTotpField?.period).isEqualTo(30)
    }

    @Test
    fun `field write should work as expected`() {
        val jsonStringInputs =
            listOf(
                // empty field write
                """
            {
             "testTotpField" : {}
            }
            """,
                // null field write
                """
            {
                "testStringField": null
            }
            """,
                // partial field
                """
            {
             "testTotpField" : {
                    "algorithm" : "valueAlgo",
                    "secret_key" : "valueKey",
                    "digits" : 0
                }
            }
            """,
            )
        val jsonModels =
            jsonStringInputs.map { jsonString ->

                object : JsonModel {
                    override var json: String? = jsonString

                    var testTotpField by RootRelativeJsonPathTotpDelegate(jsonPath = "testTotpField")
                }
            }

        jsonModels.forEach {
            it.testTotpField =
                TotpSecret(
                    algorithm = "testAlgo",
                    key = "testKey",
                    digits = 6,
                    period = 30,
                )
        }

        jsonModels.forEach {
            val jsonObject = Gson().fromJson(it.json, JsonObject::class.java)
            val totp = Gson().fromJson(jsonObject.getAsJsonObject("testTotpField"), TotpSecret::class.java)

            assertThat(totp.algorithm).isEqualTo("testAlgo")
            assertThat(totp.key).isEqualTo("testKey")
            assertThat(totp.digits).isEqualTo(6)
            assertThat(totp.period).isEqualTo(30)
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

                var testTotpField by RootRelativeJsonPathTotpDelegate(jsonPath = "testTotpField")
            }

        jsonModel.testTotpField =
            TotpSecret(
                algorithm = "testAlgo",
                key = "testKey",
                digits = 6,
                period = 30,
            )

        val jsonObject = Gson().fromJson(jsonModel.json, JsonObject::class.java)
        val totp = Gson().fromJson(jsonObject.getAsJsonObject("testTotpField"), TotpSecret::class.java)

        assertThat(totp.algorithm).isEqualTo("testAlgo")
        assertThat(totp.key).isEqualTo("testKey")
        assertThat(totp.digits).isEqualTo(6)
        assertThat(totp.period).isEqualTo(30)
    }

    @Test
    fun `delete should work as expected`() {
        val jsonString =
            """
            {
             "testTotpField" : {
                    "algorithm" : "testAlgo",
                    "secret_key" : "testKey",
                    "digits" : 6,
                    "period" : 30
                }
            }
            """
        val jsonModel =
            object : JsonModel {
                override var json: String? = jsonString

                var testTotpField by RootRelativeJsonPathTotpDelegate(jsonPath = "testTotpField")
            }

        jsonModel.testTotpField = null

        val jsonObject = Gson().fromJson(jsonModel.json, JsonObject::class.java)
        assertThat(jsonObject.has("testTotpField")).isFalse()
    }
}
