package sfotakos.itos

import android.app.Application
import android.content.Context

class ItosApp : Application() {
    companion object {
        private lateinit var appContext : Context
        fun getContext() : Context {
            return appContext
        }
    }

    init {
        appContext = this
    }
}