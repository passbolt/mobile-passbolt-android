import com.android.build.api.dsl.BuildType
import com.google.firebase.appdistribution.gradle.AppDistributionExtension
import org.gradle.api.plugins.ExtensionAware

fun BuildType.firebaseDistribution(appId: String) {
    (this as ExtensionAware).extensions.configure<AppDistributionExtension>("firebaseAppDistribution") {
        this.appId = appId
        groups = "qa"
    }
}
