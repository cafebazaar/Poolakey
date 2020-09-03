package ir.cafebazaar.poolakey.thread

import android.os.Handler
import android.os.Looper
import android.os.Message

internal class MainThread : Handler(Looper.getMainLooper()), PoolakeyThread<() -> Unit> {

    override fun handleMessage(message: Message) {
        super.handleMessage(message)
        (message.obj as? Function0<*>)?.invoke()
            ?: throw IllegalArgumentException("Can't run on main thread: Message is corrupted!")
    }

    override fun execute(task: () -> Unit) {
        Message.obtain().apply { obj = task }.also { message ->
            sendMessage(message)
        }
    }

    override fun dispose() {
        // Nothing to dispose in here :/
    }

}
