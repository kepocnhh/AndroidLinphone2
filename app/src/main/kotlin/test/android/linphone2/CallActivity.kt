package test.android.linphone2

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.linphone.core.Call

class CallActivity : AppCompatActivity() {
    private val TAG = "[${this::class.java.simpleName}|${hashCode()}]"

    private var hangUpButton: TextView? = null
    private var pickUpButton: TextView? = null
    private var statusTextView: TextView? = null
    private var addressTextView: TextView? = null
    private var accountTextView: TextView? = null

    private fun onIncoming(call: Call) {
        val address = call.remoteAddress.asStringUriOnly()
        checkNotNull(addressTextView).text = "address: $address"
        val username = call.remoteAddress.username
        checkNotNull(accountTextView).text = "account: $username"
        checkNotNull(hangUpButton).setOnClickListener {
            call.terminate()
        }
    }

    private fun onBroadcast(broadcast: CallService.Broadcast) {
        when (broadcast) {
            is CallService.Broadcast.OnCallState -> {
                val call = broadcast.call
                if (call == null) {
                    println("$TAG: no call")
                    finish()
                    return
                }
                checkNotNull(statusTextView).text = "call state: ${call.state}"
                when (call.state) {
                    Call.State.IncomingReceived -> {
                        println("$TAG: on call incoming")
                        onIncoming(call)
                    }
                    Call.State.Released -> {
                        println("$TAG: on call released")
                        finish()
                    }
                    else -> {
                        println("$TAG: on call ${call.state}")
                    }
                }
            }
            is CallService.Broadcast.OnRegistrationState -> {
                // noop
            }
        }
    }

    override fun onCreate(inState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
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
                    statusTextView = it
                    rows.addView(it)
                }
                TextView(context).also {
                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    addressTextView = it
                    rows.addView(it)
                }
                TextView(context).also {
                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    accountTextView = it
                    rows.addView(it)
                }
                LinearLayout(context).also { columns ->
                    Button(context).also {
                        it.layoutParams = LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                        it.text = "pick up"
                        pickUpButton = it
                        columns.addView(it)
                    }
                    Button(context).also {
                        it.layoutParams = LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                        it.text = "hang up"
                        hangUpButton = it
                        columns.addView(it)
                    }
                    rows.addView(columns)
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

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, CallService::class.java).also {
            it.action = CallService.ACTION_REQUEST_CALL_STATE
        }
        startService(intent)
    }
}
