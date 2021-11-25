package ir.cafebazaar.poolakey.callback

import android.os.Bundle
import java.lang.Exception

internal class GetFeatureConfigCallback {

    internal var getFeatureConfigSucceed: (Bundle) -> Unit = {}

    internal var getFeatureConfigFailed: (exception: Exception) -> Unit = {}

    fun getFeatureConfigSucceed(block: (Bundle) -> Unit) {
        getFeatureConfigSucceed = block
    }

    fun getFeatureConfigFailed(block: (exception: Exception) -> Unit) {
        getFeatureConfigFailed = block
    }
}