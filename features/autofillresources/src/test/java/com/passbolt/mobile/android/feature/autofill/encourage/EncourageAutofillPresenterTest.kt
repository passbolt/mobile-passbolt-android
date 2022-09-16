package com.passbolt.mobile.android.feature.autofill.encourage

import com.passbolt.mobile.android.feature.autofill.encourage.autofill.EncourageAutofillContract
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

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
        whenever(mockAutofillInformationProvider.isAutofillServiceSupported()) doReturn false

        presenter.goToSettingsClick()

        verify(view).showAutofillNotSupported()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `should navigate to autofill settings if autofill is supported`() {
        whenever(mockAutofillInformationProvider.isAutofillServiceSupported()) doReturn true

        presenter.goToSettingsClick()

        verify(view).openAutofillSettings()
        verifyNoMoreInteractions(view)
    }
}
