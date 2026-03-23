package com.passbolt.mobile.android.feature.autofill.resources

import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Bundle
import android.view.autofill.AutofillManager.EXTRA_ASSIST_STRUCTURE
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.remember
import androidx.core.content.IntentCompat
import com.passbolt.mobile.android.common.lifecycleawarelazy.lifecycleAwareLazy
import com.passbolt.mobile.android.core.navigation.ActivityIntents
import com.passbolt.mobile.android.core.navigation.AutofillMode
import com.passbolt.mobile.android.core.navigation.compose.APP_NAVIGATOR_SCOPE
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.NewResourceCreated
import com.passbolt.mobile.android.feature.autofill.resources.AutofillResourcesIntent.SelectAutofillItem
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.AutofillCallback
import com.passbolt.mobile.android.feature.autofill.resources.datasetstrategy.ReturnAutofillDatasetStrategy
import com.passbolt.mobile.android.feature.home.screen.ResourceHandlingStrategyProvider
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.scope.activityScope
import org.koin.compose.scope.KoinScope
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

private const val AUTOFILL_NAVIGATOR_SCOPE_ID = "autofill_navigator"

// NOTE: When changing name or package read core/navigation/README.md
class AutofillResourcesActivity :
    AppCompatActivity(),
    AndroidScopeComponent,
    AutofillCallback,
    ResourceHandlingStrategyProvider {
    override val scope: Scope by activityScope()

    private lateinit var viewModel: AutofillResourcesViewModel

    override lateinit var resourceHandlingStrategy: AutofillResourceHandlingStrategy

    private val bundledAutofillUri by lifecycleAwareLazy {
        intent.getStringExtra(ActivityIntents.EXTRA_AUTOFILL_URI)
    }
    private val bundledAutofillMode by lifecycleAwareLazy {
        intent.getStringExtra(ActivityIntents.EXTRA_AUTOFILL_MODE_NAME).let {
            AutofillMode.valueOf(requireNotNull(it))
        }
    }

    private lateinit var returnAutofillDatasetStrategy: ReturnAutofillDatasetStrategy

    @OptIn(KoinExperimentalAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        returnAutofillDatasetStrategy =
            scope.get(named(bundledAutofillMode)) { parametersOf(this as AutofillCallback) }
        returnAutofillDatasetStrategy =
            scope.get(named(bundledAutofillMode)) { parametersOf(this as AutofillCallback) }

        setContent {
            viewModel =
                koinViewModel(
                    parameters = { parametersOf(bundledAutofillUri) },
                )
            resourceHandlingStrategy =
                remember {
                    AutofillResourceHandlingStrategy(
                        autofillUri = bundledAutofillUri,
                        onItemClick = { viewModel.onIntent(SelectAutofillItem(it)) },
                        onResourceCreated = { viewModel.onIntent(NewResourceCreated(it)) },
                    )
                }

            setContent {
                viewModel =
                    koinViewModel(
                        parameters = { parametersOf(bundledAutofillUri) },
                    )
                resourceHandlingStrategy =
                    AutofillResourceHandlingStrategy(
                        autofillUri = bundledAutofillUri,
                        onItemClick = { viewModel.onIntent(SelectAutofillItem(it)) },
                        onResourceCreated = { viewModel.onIntent(NewResourceCreated(it)) },
                    )

                KoinScope(
                    scopeID = AUTOFILL_NAVIGATOR_SCOPE_ID,
                    scopeQualifier = APP_NAVIGATOR_SCOPE,
                ) {
                    AutofillResourcesScreen(
                        autofillUri = bundledAutofillUri,
                        returnAutofillDatasetStrategy = returnAutofillDatasetStrategy,
                        viewModel = viewModel,
                    )
                }
            }
            KoinScope(
                scopeID = AUTOFILL_NAVIGATOR_SCOPE_ID,
                scopeQualifier = APP_NAVIGATOR_SCOPE,
            ) {
                AutofillResourcesScreen(
                    autofillUri = bundledAutofillUri,
                    returnAutofillDatasetStrategy = returnAutofillDatasetStrategy,
                    viewModel = viewModel,
                )
            }
        }
    }

    override fun getAutofillStructure(): AssistStructure =
        requireNotNull(
            IntentCompat.getParcelableExtra(intent, EXTRA_ASSIST_STRUCTURE, AssistStructure::class.java),
        )

    override fun setResultAndFinish(
        result: Int,
        resultIntent: Intent,
    ) {
        setResult(result, resultIntent)
        finish()
    }

    override fun finishAutofill() {
        finishAffinity()
    }
}
