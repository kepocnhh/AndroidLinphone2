package test.android.linphone2

import android.app.Application

internal class App : Application() {
    companion object {
        private var _ldp: LocalDataProvider? = null
        val ldp: LocalDataProvider get() = _ldp!!
    }

    override fun onCreate() {
        super.onCreate()
        _ldp = LocalDataProvider(this)
    }
}
