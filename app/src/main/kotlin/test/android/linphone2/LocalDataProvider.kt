package test.android.linphone2

import android.content.Context

internal class LocalDataProvider(context: Context) {
    private val preferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)

    var domain: Domain?
        get() {
            return Domain(
                host = preferences.getString("host", null) ?: return null,
                port = preferences.takeIf { it.contains("port") }?.getInt("port", 5060)
            )
        }
        set(value) {
            preferences.edit().also {
                it.putString("host", value?.host)
                val port = value?.port
                if (port == null) {
                    it.remove("port")
                } else {
                    it.putInt("port", port)
                }
            }.commit()
        }

    var userCredentials: UserCredentials?
        get() {
            return UserCredentials(
                login = preferences.getString("login", null) ?: return null,
                password = preferences.getString("password", null).orEmpty()
            )
        }
        set(value) {
            preferences.edit()
                .putString("login", value?.login)
                .putString("password", value?.password)
                .commit()
        }
}
