package app.marlboroadvance.mpvex.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BatteryIndicator(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var batteryLevel by remember { mutableIntStateOf(100) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level != -1 && scale != -1) {
                        batteryLevel = level * 100 / scale
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
        if (intent != null) {
            val initLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val initScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (initLevel != -1 && initScale != -1) {
                batteryLevel = initLevel * 100 / initScale
            }
        }

        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val color = if (batteryLevel > 20) Color(0xFF4CAF50) else Color(0xFFF44336)

    Row(
        modifier = modifier.pointerInput(Unit) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(82.dp) // 👈 恢复 82 宽度
                .height(40.dp) // 👈 恢复 40 高度
                .background(
                    color = Color.Black.copy(alpha = 0.6f), 
                    shape = RoundedCornerShape(10.dp), 
                )
                .border(
                    width = 3.dp, // 👈 恢复 3dp 边框
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                )
                .padding(3.dp), 
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(batteryLevel / 100f)
                    .background(
                        color = color,
                    )
                    .align(Alignment.CenterStart),
            )

            Text(
                text = "$batteryLevel",
                color = Color.White,
                fontSize = 28.sp, // 👈 恢复 28sp 字体
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-3.5).dp), 
                style = TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false,
                    ),
                    lineHeight = 28.sp, 
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(1.5f, 1.5f),
                        blurRadius = 3f,
                    ),
                ),
            )
        }

        Box(
            modifier = Modifier
                .width(6.dp)
                .height(16.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(
                        topEnd = 3.dp,
                        bottomEnd = 3.dp,
                    ),
                ),
        )
    }
}
