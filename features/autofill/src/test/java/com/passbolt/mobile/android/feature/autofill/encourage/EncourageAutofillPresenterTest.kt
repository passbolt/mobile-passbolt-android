package com.passbolt.mobile.android.feature.autofill.encourage

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import com.passbolt.mobile.android.feature.autofill.encourage.autofill.EncourageAutofillContract
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

class EncourageAutofillPresenterTest : KoinTest {

    private val presenter: EncourageAutofillContract.Presenter by inject()
    private val view: EncourageAutofillContract.View = mock()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(encourageAutofillModule)
    }

    @Before
    fun setUp() {
        presenter.attach(view)
    }

    @Test
    fun `autofill not supported dialog should be shown when needed`() {
        whenever(mockAutofillInformationProvider.isAutofillSupported()) doReturn false

        presenter.goToSettingsClick()

        verify(view).showAutofillNotSupported()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `should navigate to autofill settings if autofill is supported`() {
        whenever(mockAutofillInformationProvider.isAutofillSupported()) doReturn true

        presenter.goToSettingsClick()

        verify(view).openAutofillSettings()
        verifyNoMoreInteractions(view)
    }
}
