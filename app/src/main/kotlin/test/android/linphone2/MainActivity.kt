package test.android.linphone2

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.core.Account
import org.linphone.core.Call
import org.linphone.core.RegistrationState

internal class MainActivity : AppCompatActivity() {
    private val TAG = "[${this::class.java.simpleName}|${hashCode()}]"

    private var accountTextView: TextView? = null
    private var actionButton: TextView? = null
    private var exitButton: TextView? = null
    private var host: EditText? = null
    private var port: EditText? = null
    private var username: EditText? = null
    private var password: EditText? = null
    private var whom: EditText? = null
    private var waiter: View? = null

    private fun renderWaiter() {
        listOf(
            waiter
        ).forEach {
            checkNotNull(it).visibility = View.VISIBLE
        }
        listOf(
            accountTextView,
            whom,
            host,
            port,
            username,
            password,
            actionButton,
            exitButton
        ).forEach {
            checkNotNull(it).visibility = View.GONE
        }
    }

    private fun call() {
        // todo
    }

    private fun exit() {
        val intent = Intent(this, CallService::class.java).also {
            it.action = CallService.ACTION_EXIT
        }
        startService(intent)
    }

    private fun renderRegistered(account: Account) {
        listOf(
            accountTextView,
            whom,
            actionButton,
            exitButton
        ).forEach {
            checkNotNull(it).visibility = View.VISIBLE
        }
        listOf(
            host,
            port,
            username,
            password,
            waiter
        ).forEach {
            checkNotNull(it).visibility = View.GONE
        }
        val username = account.params.identityAddress?.username
        checkNotNull(accountTextView).text = "account: $username"
        val actionButton = checkNotNull(actionButton)
        actionButton.text = "call"
        actionButton.setOnClickListener {
            call()
        }
        val exitButton = checkNotNull(exitButton)
        exitButton.text = "exit"
        exitButton.setOnClickListener {
            exit()
        }
    }

    private fun renderRegistration() {
        listOf(
            host,
            port,
            username,
            password,
            actionButton
        ).forEach {
            checkNotNull(it).visibility = View.VISIBLE
        }
        listOf(
            accountTextView,
            whom,
            waiter,
            exitButton
        ).forEach {
            checkNotNull(it).visibility = View.GONE
        }
        val actionButton = checkNotNull(actionButton)
        actionButton.text = "register"
        lifecycleScope.launch {
            val domain = withContext(Dispatchers.IO) {
                App.ldp.domain
            }
            if (domain != null) {
                checkNotNull(host).setText(domain.host)
                checkNotNull(port).setText(domain.port?.toString())
            }
            val userCredentials = withContext(Dispatchers.IO) {
                App.ldp.userCredentials
            }
            if (userCredentials != null) {
                checkNotNull(username).setText(userCredentials.login)
                checkNotNull(password).setText(userCredentials.password)
            }
        }
        actionButton.setOnClickListener {
            register()
        }
    }

    private fun onBroadcast(broadcast: CallService.Broadcast) {
        when (broadcast) {
            is CallService.Broadcast.OnRegistrationState -> {
                when (broadcast.state) {
                    RegistrationState.Ok -> {
                        println("$TAG: on registration ok")
                        val account = broadcast.account
                        if (account == null) {
                            println("$TAG: no account")
                            TODO()
                        }
                        renderRegistered(account)
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                val domain = Domain(
                                    host = checkNotNull(host).text.toString(),
                                    port = checkNotNull(port).text.toString().toIntOrNull(),
                                )
                                if (domain != App.ldp.domain) App.ldp.domain = domain
                                val userCredentials = UserCredentials(
                                    login = checkNotNull(username).text.toString(),
                                    password = checkNotNull(password).text.toString(),
                                )
                                if (userCredentials != App.ldp.userCredentials) App.ldp.userCredentials = userCredentials
                            }
                        }
                    }
                    null -> {
                        println("$TAG: on registration null")
                        renderRegistration()
                    }
                    RegistrationState.Progress -> {
                        println("$TAG: on registration progress")
                        renderWaiter()
                    }
                    else -> {
                        println("$TAG: on registration: ${broadcast.state}")
                    }
                }
            }
            is CallService.Broadcast.OnCallState -> {
                // noop
            }
        }
    }

    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)
        val context: Context = this
        FrameLayout(context).also { root ->
            root.background = ColorDrawable(Color.BLACK)
            root.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            LinearLayout(context).also { rows ->
                rows.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL
                )
                rows.orientation = LinearLayout.VERTICAL
                TextView(context).also {
                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    accountTextView = it
                    rows.addView(it)
                }
                EditText(context).also {
                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    it.hint = "host"
                    host = it
                    rows.addView(it)
                }
                EditText(context).also {
                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    it.hint = "port"
                    port = it
                    rows.addView(it)
                }
                EditText(context).also {
                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    it.hint = "username"
                    username = it
                    rows.addView(it)
                }
                EditText(context).also {
                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    it.hint = "password"
                    password = it
                    rows.addView(it)
                }
                EditText(context).also {
                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    it.hint = "to"
                    whom = it
                    rows.addView(it)
                }
                Button(context).also {
                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    actionButton = it
                    rows.addView(it)
                }
                Button(context).also {
                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    exitButton = it
                    rows.addView(it)
                }
                ProgressBar(context).also {
                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    waiter = it
                    rows.addView(it)
                }
                root.addView(rows)
            }
            setContentView(root)
        }
        CallService.broadcast
            .flowWithLifecycle(lifecycle)
            .onEach(::onBroadcast)
            .launchIn(lifecycleScope)
    }

    private fun register() {
        val username = checkNotNull(username).text.toString()
        if (username.isEmpty()) {
            showToast("Username is empty!")
            return
        }
        val host = checkNotNull(host).text.toString()
        if (host.isEmpty()) {
            showToast("Host is empty!")
            return
        }
        val password = checkNotNull(password).text.toString()
        val port = checkNotNull(port).text.toString().let {
            if (it.isEmpty()) {
                5060
            } else {
                it.toIntOrNull()
            }
        }
        if (port == null) {
            showToast("Wrong port!")
            return
        }
        val intent = Intent(this, CallService::class.java).also {
            it.action = CallService.ACTION_REGISTER
            it.putExtra("username", username)
            it.putExtra("host", host)
            it.putExtra("password", password)
            it.putExtra("port", port)
        }
        startService(intent)
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, CallService::class.java).also {
            it.action = CallService.ACTION_REQUEST_REGISTRATION_STATE
        }
        startService(intent)
    }
}
