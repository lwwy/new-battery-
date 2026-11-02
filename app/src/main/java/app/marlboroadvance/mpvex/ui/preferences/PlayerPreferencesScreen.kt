package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.SeekbarStyle
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.player.PlayerOrientation
import app.marlboroadvance.mpvex.ui.player.controls.components.SeekbarPreview
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.toFixed
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject

@Serializable
object PlayerPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val preferences = koinInject<PlayerPreferences>()
    val appearancePreferences = koinInject<AppearancePreferences>()
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(text = stringResource(id = R.string.pref_player)) },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        Column(
          modifier =
            Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(padding),
        ) {
          val orientation by preferences.orientation.collectAsState()
          ListPreference(
            value = orientation,
            onValueChange = preferences.orientation::set,
            values = PlayerOrientation.entries,
            valueToText = { AnnotatedString(context.getString(it.titleRes)) },
            title = { Text(text = stringResource(id = R.string.pref_player_orientation)) },
            summary = { Text(text = stringResource(id = orientation.titleRes)) },
          )
          val savePositionOnQuit by preferences.savePositionOnQuit.collectAsState()
          SwitchPreference(
            value = savePositionOnQuit,
            onValueChange = preferences.savePositionOnQuit::set,
            title = { Text(stringResource(R.string.pref_player_save_position_on_quit)) },
          )
          val closeAfterEndOfVideo by preferences.closeAfterReachingEndOfVideo.collectAsState()
          SwitchPreference(
            value = closeAfterEndOfVideo,
            onValueChange = preferences.closeAfterReachingEndOfVideo::set,
            title = { Text(stringResource(id = R.string.pref_player_close_after_eof)) },
          )
          val playlistMode by preferences.playlistMode.collectAsState()
          SwitchPreference(
            value = playlistMode,
            onValueChange = preferences.playlistMode::set,
            title = { Text(text = "播放列表模式") },
            summary = {
              Text(
                text =
                  if (playlistMode) {
                    "自动为文件夹中的所有视频启用上一个/下一个导航"
                  } else {
                    "单独播放视频（选择多个视频以创建播放列表）"
                  },
              )
            },
          )
          val rememberBrightness by preferences.rememberBrightness.collectAsState()
          SwitchPreference(
            value = rememberBrightness,
            onValueChange = preferences.rememberBrightness::set,
            title = { Text(text = stringResource(R.string.pref_player_remember_brightness)) },
          )
          PreferenceCategory(
            title = { Text(stringResource(R.string.pref_player_seeking_title)) },
          )
          val horizontalSeekGesture by preferences.horizontalSeekGesture.collectAsState()
          SwitchPreference(
            value = horizontalSeekGesture,
            onValueChange = preferences.horizontalSeekGesture::set,
            title = { Text(text = "水平滑动定位") },
          )
          val showSeekbarWhenSeeking by preferences.showSeekBarWhenSeeking.collectAsState()
          SwitchPreference(
            value = showSeekbarWhenSeeking,
            onValueChange = preferences.showSeekBarWhenSeeking::set,
            title = { Text(text = "定位时显示进度条") },
          )
          val showDoubleTapOvals by preferences.showDoubleTapOvals.collectAsState()
          SwitchPreference(
            value = showDoubleTapOvals,
            onValueChange = preferences.showDoubleTapOvals::set,
            title = { Text(stringResource(R.string.show_splash_ovals_on_double_tap_to_seek)) },
          )
          val showSeekTimeWhileSeeking by preferences.showSeekTimeWhileSeeking.collectAsState()
          SwitchPreference(
            value = showSeekTimeWhileSeeking,
            onValueChange = preferences.showSeekTimeWhileSeeking::set,
            title = { Text(stringResource(R.string.show_time_on_double_tap_to_seek)) },
          )
          val usePreciseSeeking by preferences.usePreciseSeeking.collectAsState()
          SwitchPreference(
            value = usePreciseSeeking,
            onValueChange = preferences.usePreciseSeeking::set,
            title = { Text(stringResource(R.string.pref_player_use_precise_seeking)) },
          )
          val seekbarStyle by appearancePreferences.seekbarStyle.collectAsState()

          Column(Modifier.padding(vertical = 8.dp)) {
            Text(
              text = "进度条样式",
              style = MaterialTheme.typography.titleSmall, // Smaller font as requested
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
              color = MaterialTheme.colorScheme.primary,
            )
            SeekbarStyle.entries.forEach { style ->
              ListItem(
                headlineContent = {
                  Text(
                    text =
                      when (style.name) {
                        "Standard" -> "标准"
                        "Wavy" -> "波浪"
                        "Simple" -> "简约"
                        "Circular" -> "圆形"
                        else -> style.name
                      },
                  )
                },
                supportingContent = {
                  SeekbarPreview(
                    style = style,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                  )
                },
                trailingContent = {
                  RadioButton(
                    selected = seekbarStyle == style,
                    onClick = null,
                  )
                },
                modifier =
                  Modifier
                    .clickable { appearancePreferences.seekbarStyle.set(style) },
              )
            }
          }
          PreferenceCategory(
            title = { Text(stringResource(R.string.pref_player_gestures)) },
          )
          val brightnessGesture by preferences.brightnessGesture.collectAsState()
          SwitchPreference(
            value = brightnessGesture,
            onValueChange = preferences.brightnessGesture::set,
            title = { Text(stringResource(R.string.pref_player_gestures_brightness)) },
          )
          val volumeGesture by preferences.volumeGesture.collectAsState()
          SwitchPreference(
            value = volumeGesture,
            onValueChange = preferences.volumeGesture::set,
            title = { Text(stringResource(R.string.pref_player_gestures_volume)) },
          )
          val pinchToZoomGesture by preferences.pinchToZoomGesture.collectAsState()
          SwitchPreference(
            value = pinchToZoomGesture,
            onValueChange = preferences.pinchToZoomGesture::set,
            title = { Text(stringResource(R.string.pref_player_gestures_pinch_to_zoom)) },
          )
          val holdForMultipleSpeed by preferences.holdForMultipleSpeed.collectAsState()
          SliderPreference(
            value = holdForMultipleSpeed,
            onValueChange = { preferences.holdForMultipleSpeed.set(it.toFixed(2)) },
            title = { Text(stringResource(R.string.pref_player_gestures_hold_for_multiple_speed)) },
            valueRange = 0f..6f,
            summary = {
              Text(
                if (holdForMultipleSpeed == 0F) {
                  stringResource(R.string.generic_disabled)
                } else {
                  "%.2fx".format(holdForMultipleSpeed)
                },
              )
            },
            onSliderValueChange = { preferences.holdForMultipleSpeed.set(it.toFixed(2)) },
            sliderValue = holdForMultipleSpeed,
          )
          val showDynamicSpeedOverlay by preferences.showDynamicSpeedOverlay.collectAsState()
          SwitchPreference(
            value = showDynamicSpeedOverlay,
            onValueChange = preferences.showDynamicSpeedOverlay::set,
            title = { Text("动态倍速浮层") },
            summary = { Text("长按并滑动调速时显示增强控制浮层") },
          )
          PreferenceCategory(
            title = { Text(stringResource(R.string.pref_player_controls)) },
          )
          val allowGesturesInPanels by preferences.allowGesturesInPanels.collectAsState()
          SwitchPreference(
            value = allowGesturesInPanels,
            onValueChange = preferences.allowGesturesInPanels::set,
            title = {
              Text(
                text = stringResource(id = R.string.pref_player_controls_allow_gestures_in_panels),
              )
            },
          )
          val displayVolumeAsPercentage by preferences.displayVolumeAsPercentage.collectAsState()
          SwitchPreference(
            value = displayVolumeAsPercentage,
            onValueChange = preferences.displayVolumeAsPercentage::set,
            title = { Text(stringResource(R.string.pref_player_controls_display_volume_as_percentage)) },
          )
          val swapVolumeAndBrightness by preferences.swapVolumeAndBrightness.collectAsState()
          SwitchPreference(
            value = swapVolumeAndBrightness,
            onValueChange = preferences.swapVolumeAndBrightness::set,
            title = { Text(stringResource(R.string.swap_the_volume_and_brightness_slider)) },
          )
          val showLoadingCircle by preferences.showLoadingCircle.collectAsState()
          SwitchPreference(
            value = showLoadingCircle,
            onValueChange = preferences.showLoadingCircle::set,
            title = { Text(stringResource(R.string.pref_player_controls_show_loading_circle)) },
          )
          PreferenceCategory(
            title = { Text(stringResource(R.string.pref_player_display)) },
          )
          val showSystemStatusBar by preferences.showSystemStatusBar.collectAsState()
          SwitchPreference(
            value = showSystemStatusBar,
            onValueChange = preferences.showSystemStatusBar::set,
            title = { Text(stringResource(R.string.pref_player_display_show_status_bar)) },
          )
          val reduceMotion by preferences.reduceMotion.collectAsState()
          SwitchPreference(
            value = reduceMotion,
            onValueChange = preferences.reduceMotion::set,
            title = { Text(stringResource(R.string.pref_player_display_reduce_player_animation)) },
          )
          val playerTimeToDisappear by preferences.playerTimeToDisappear.collectAsState()
          val predefinedTimeValues = listOf(500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000)
          val isCustomTimeValue = !predefinedTimeValues.contains(playerTimeToDisappear)

          var showCustomTimeDialog by remember { mutableStateOf(false) }
          var customTimeValue by remember { mutableStateOf("") }

          ListPreference(
            value = if (isCustomTimeValue) -1 else playerTimeToDisappear,
            onValueChange = { newValue ->
              if (newValue == -1) {
                customTimeValue = playerTimeToDisappear.toString()
                showCustomTimeDialog = true
              } else {
                preferences.playerTimeToDisappear.set(newValue)
              }
            },
            values = predefinedTimeValues + listOf(-1),
            valueToText = { value ->
              if (value == -1) {
                AnnotatedString("自定义")
              } else {
                AnnotatedString("$value 毫秒")
              }
            },
            title = { Text(text = stringResource(R.string.pref_player_display_hide_player_control_time)) },
            summary = {
              Text(
                text =
                  if (isCustomTimeValue) {
                    "自定义 ($playerTimeToDisappear 毫秒)"
                  } else {
                    "$playerTimeToDisappear 毫秒"
                  },
              )
            },
          )

          if (showCustomTimeDialog) {
            AlertDialog(
              onDismissRequest = { showCustomTimeDialog = false },
              title = { Text(text = stringResource(R.string.pref_player_display_hide_player_control_time)) },
              text = {
                Column {
                  Text(
                    text = "输入自定义隐藏时间（毫秒）",
                    modifier = Modifier.padding(bottom = 8.dp),
                  )
                  OutlinedTextField(
                    value = customTimeValue,
                    onValueChange = { customTimeValue = it },
                    label = { Text("毫秒") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                  )
                }
              },
              confirmButton = {
                TextButton(
                  onClick = {
                    val value = customTimeValue.toIntOrNull()
                    if (value != null && value in 100..1000000000000) {
                      preferences.playerTimeToDisappear.set(value)
                      showCustomTimeDialog = false
                    }
                  },
                ) {
                  Text(stringResource(R.string.generic_ok))
                }
              },
              dismissButton = {
                TextButton(onClick = { showCustomTimeDialog = false }) {
                  Text(stringResource(R.string.generic_cancel))
                }
              },
            )
          }
        }
      }
    }
  }
}
