package test.android.linphone2

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.linphone.core.Call
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CallActivity : AppCompatActivity() {
    private val TAG = "[${this::class.java.simpleName}|${hashCode()}]"

    private var hangUpButton: TextView? = null
    private var pickUpButton: TextView? = null
    private var statusTextView: TextView? = null
    private var addressTextView: TextView? = null
    private var accountTextView: TextView? = null
    private var timeTextView: TextView? = null

    private fun onIncoming(call: Call) {
        val address = call.remoteAddress.asStringUriOnly()
        checkNotNull(addressTextView).text = "address: $address"
        val username = call.remoteAddress.username
        checkNotNull(accountTextView).text = "account: $username"
        checkNotNull(hangUpButton).setOnClickListener {
            call.terminate()
        }
        checkNotNull(pickUpButton).also {
            it.isEnabled = true
            it.setOnClickListener {
                val isGranted = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (isGranted) {
                    call.accept()
                } else {
                    println("$TAG: no permission")
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 42)
                }
            }
        }
    }

    private fun onStreamsRunning(call: Call) {
        val address = call.remoteAddress.asStringUriOnly()
        checkNotNull(addressTextView).text = "address: $address"
        val username = call.remoteAddress.username
        checkNotNull(accountTextView).text = "account: $username"
        checkNotNull(hangUpButton).setOnClickListener {
            call.terminate()
        }
        checkNotNull(pickUpButton).also {
            it.isEnabled = false
            it.setOnClickListener(null)
        }
        lifecycleScope.launch {
            val timeTextView = checkNotNull(timeTextView)
            val startDate = call.callLog.startDate.seconds
            println("$TAG: call start: ${Date(startDate.inWholeMilliseconds)}")
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
            timeFormat.timeZone = TimeZone.getTimeZone("UTC")
            while (!isDestroyed) {
                val text = timeFormat.format(call.duration.seconds.inWholeMilliseconds)
                timeTextView.text = text
                delay(1.seconds)
            }
        }
    }

    private fun onBroadcast(broadcast: CallService.Broadcast) {
        when (broadcast) {
            is CallService.Broadcast.OnCallState -> {
                checkNotNull(statusTextView).text = "call state: ${broadcast.state}"
                when (broadcast.state) {
                    Call.State.IncomingReceived -> {
                        println("$TAG: on call incoming")
                        val call = broadcast.call ?: TODO()
                        onIncoming(call)
                    }
                    Call.State.StreamsRunning -> {
                        println("$TAG: on call streams running")
                        val call = broadcast.call ?: TODO()
                        onStreamsRunning(call)
                    }
                    Call.State.Released -> {
                        println("$TAG: on call released")
                        finish()
                    }
                    else -> {
                        println("$TAG: on call ${broadcast.state}")
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
                TextView(context).also {
                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    timeTextView = it
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

    override fun onBackPressed() {
        // todo
    }

    private fun requestTerminate() {
        val intent = Intent(this, CallService::class.java).also {
            it.action = CallService.ACTION_REQUEST_CALL_TERMINATE
        }
        startService(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            42 -> {
                val index = permissions.indexOf(Manifest.permission.RECORD_AUDIO)
                if (index < 0) {
                    requestTerminate()
                    return
                }
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    requestTerminate()
                    return
                }
            }
        }
    }
}
