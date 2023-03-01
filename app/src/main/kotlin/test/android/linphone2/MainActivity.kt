package test.android.linphone2

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

internal class MainActivity : AppCompatActivity() {
    private val TAG = "[${this::class.java.simpleName}|${hashCode()}]"
    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)
        val context: Context = this
        FrameLayout(context).also { root ->
            root.background = ColorDrawable(Color.BLACK)
            root.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContentView(root)
        }
        lifecycleScope.launch {
            println("$TAG: launch...")
            val state = Lifecycle.State.STARTED
            repeatOnLifecycle(state) {
                println("$TAG: repeatOnLifecycle($state)...")
            }
        }
    }
}
