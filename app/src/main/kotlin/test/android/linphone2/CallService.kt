package test.android.linphone2

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.linphone.core.Account
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListener
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.LogCollectionState
import org.linphone.core.LogLevel
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType

class CallService : Service() {
    sealed interface Broadcast {
        class OnRegistrationState(val account: Account?) : Broadcast
        class OnCallState(val call: Call?) : Broadcast
    }

    companion object {
        val ACTION_REGISTER = "${this::class.java.name}:ACTION_REGISTER"
        val ACTION_REQUEST_REGISTRATION_STATE = "${this::class.java.name}:ACTION_REQUEST_REGISTRATION_STATE"
        val ACTION_REQUEST_CALL_STATE = "${this::class.java.name}:ACTION_REQUEST_CALL_STATE"
        val ACTION_EXIT = "${this::class.java.name}:ACTION_EXIT"
        val _broadcast = MutableSharedFlow<Broadcast>()
        val broadcast = _broadcast.asSharedFlow()
    }

    private val TAG = "[${this::class.java.simpleName}|${hashCode()}]"
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var core: Core? = null
    private val coreListener: CoreListener = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            scope.launch {
                _broadcast.emit(Broadcast.OnRegistrationState(account))
            }
            when (state) {
                RegistrationState.Ok -> {
                    println("$TAG: on account registration ok")
                    // todo
                }
                else -> {
                    println("$TAG: on account registration: $state")
                }
            }
        }

        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String
        ) {
            scope.launch {
                _broadcast.emit(Broadcast.OnCallState(call))
            }
            when (state) {
                Call.State.IncomingReceived -> {
                    println("$TAG: on call incoming")
                    onIncoming(call)
                }
                else -> {
                    println("$TAG: on call: $state")
                }
            }
        }
    }

    private fun onIncoming(call: Call) {
        startActivity(Intent(this, CallActivity::class.java))
    }

    private fun onRegister(intent: Intent) {
        println("$TAG: on register")
        val username = intent.getStringExtra("username")
        if (username.isNullOrEmpty()) return
        val host = intent.getStringExtra("host")
        if (host.isNullOrEmpty()) return
        val password = intent.getStringExtra("password").orEmpty()
        val port = intent.getIntExtra("port", 5060)
        println("$TAG: address($host, $port)")
        println("$TAG: credentials($username, $password)")
        val core = checkNotNull(core)
        val userid: String? = null
        val ha1: String? = null
        val realm: String? = null
        val algorithm: String? = null
        val domain = "$host:$port"
        val authInfo = Factory.instance().createAuthInfo(
            username,
            userid,
            password,
            ha1,
            realm,
            domain,
            algorithm
        )
        core.addAuthInfo(authInfo)
        val params = core.createAccountParams()
        params.identityAddress = Factory.instance().createAddress("sip:$username@$domain")
        val serverAddress = Factory.instance().createAddress("sip:$domain")
        if (serverAddress == null) {
            println("$TAG: server address null!")
            return
        }
        serverAddress.transport = TransportType.Udp
        params.serverAddress = serverAddress
        params.isRegisterEnabled = true
        val account = core.createAccount(params)
        core.addAccount(account)
        core.defaultAccount = account
        core.addListener(coreListener)
        core.start()
    }

    private fun onStartCommand(intent: Intent) {
        when (intent.action) {
            ACTION_REGISTER -> {
                onRegister(intent)
            }
            ACTION_REQUEST_REGISTRATION_STATE -> {
                println("$TAG: on request registration state")
                scope.launch {
                    _broadcast.emit(Broadcast.OnRegistrationState(core?.defaultAccount))
                }
            }
            ACTION_REQUEST_CALL_STATE -> {
                println("$TAG: on request call state")
                scope.launch {
                    _broadcast.emit(Broadcast.OnCallState(core?.currentCall))
                }
            }
            ACTION_EXIT -> {
                println("$TAG: on exit")
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) onStartCommand(intent)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        println("$TAG: on create")
        super.onCreate()
        Factory.instance().also {
            it.enableLogCollection(LogCollectionState.Enabled)
            it.enableLogcatLogs(true)
            it.loggingService.setLogLevel(LogLevel.Message)
            it.setLoggerDomain("[Linphone|${it.hashCode()}]")
        }
        val configPath: String? = null
        val factoryConfigPath: String? = null
        val systemContext: Any = this
        core = Factory.instance().createCore(configPath, factoryConfigPath, systemContext).also {
            it.setUserAgent(BuildConfig.APPLICATION_ID, BuildConfig.VERSION_NAME)
        }
    }

    override fun onDestroy() {
        println("$TAG: on destroy")
        super.onDestroy()
        try {
            core?.also {
                it.clearAccounts()
                it.clearAllAuthInfo()
                it.removeListener(coreListener)
                it.stop()
            }
        } catch (e: Throwable) {
            println("$TAG: core stop error: $e")
        }
        scope.launch {
            _broadcast.emit(Broadcast.OnRegistrationState(null))
        }
    }
}
