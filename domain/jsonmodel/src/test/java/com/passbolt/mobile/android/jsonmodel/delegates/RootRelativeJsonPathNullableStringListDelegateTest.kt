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

class RootRelativeJsonPathNullableStringListDelegateTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(jsonPathDelegatesTestModule)
    }

    @Test
    fun `read should work as expected`() {
        val jsonString =
            """
            {
                "testListField": [
                    "test1",
                    "test2"
                ]
            }
            """
        val jsonModel = object : JsonModel {
            override var json = jsonString

            var testListField by RootRelativeJsonPathNullableStringListDelegate(jsonPath = "testListField")
        }

        assertThat(jsonModel.testListField).isNotNull()
        assertThat(jsonModel.testListField!!).containsExactly("test1", "test2")
    }

    @Test
    fun `read for null should work as expected`() {
        val jsonString =
            """
            {
                "testListField": null
            }
            """
        val jsonModel = object : JsonModel {
            override var json = jsonString

            var testListField by RootRelativeJsonPathNullableStringListDelegate(jsonPath = "testListField")
        }

        assertThat(jsonModel.testListField).isEqualTo(null)
    }

    @Test
    fun `field write should work as expected`() {
        val jsonStringInputs = listOf(
            // empty field write
            """
            {
                "testListField": ""
            }
            """,
            // null field write
            """
            {
                "testListField": null
            }
            """
        )
        val jsonModels = jsonStringInputs.map { jsonString ->

            object : JsonModel {
                override var json = jsonString

                var testListField by RootRelativeJsonPathNullableStringListDelegate(jsonPath = "testListField")
            }
        }

        jsonModels.forEach {
            it.testListField = listOf("test1, test2")
        }

        jsonModels.forEach {
            val jsonObject = Gson().fromJson(it.json, JsonObject::class.java)
            val list = Gson().fromJson(jsonObject.getAsJsonArray("testListField"), ArrayList::class.java)

            assertThat(list).containsExactly("test1, test2")
        }
    }

    @Test
    fun `field null write should work as expected`() {
        val jsonStringInput =
            """
            {
                "testListField": ""
            }
            """

        val jsonModel = object : JsonModel {
            override var json = jsonStringInput

            var testListField by RootRelativeJsonPathNullableStringDelegate(jsonPath = "testListField")
        }

        jsonModel.testListField = null

        val jsonObject = Gson().fromJson(jsonModel.json, JsonObject::class.java)
        val list = Gson().fromJson(jsonObject.getAsJsonArray("testListField"), ArrayList::class.java)
        assertThat(list).isNull()
    }

    @Test
    fun `field write should create new one if not existing`() {
        val jsonString =
            """
        {}
        """

        val jsonModel = object : JsonModel {
            override var json = jsonString

            var testStringField by RootRelativeJsonPathNullableStringDelegate(jsonPath = "testStringField")
        }

        jsonModel.testStringField = "test"

        val jsonObject = Gson().fromJson(jsonModel.json, JsonObject::class.java)
        assertThat(jsonObject["testStringField"].asString).isEqualTo("test")
    }
}
