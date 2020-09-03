package ir.cafebazaar.poolakey.exception

import android.os.RemoteException

class ConsumeFailedException : RemoteException() {

    override val message: String?
        get() = "Consume request failed: It's from Bazaar"

}
