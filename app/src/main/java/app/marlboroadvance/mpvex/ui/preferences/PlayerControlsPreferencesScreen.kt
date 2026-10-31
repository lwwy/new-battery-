package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.PlayerButton
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.ConfirmDialog
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.koin.compose.koinInject

@Serializable
enum class ControlRegion {
  TOP_RIGHT,
  BOTTOM_RIGHT,
  BOTTOM_LEFT,
  PORTRAIT_BOTTOM,
}

@Serializable
object PlayerControlsPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val appearancePrefs = koinInject<AppearancePreferences>()
    val playerPrefs = koinInject<PlayerPreferences>()

    val topRState by appearancePrefs.topRightControls.collectAsState()
    val bottomRState by appearancePrefs.bottomRightControls.collectAsState()
    val bottomLState by appearancePrefs.bottomLeftControls.collectAsState()
    val portraitBottomState by appearancePrefs.portraitBottomControls.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
      ConfirmDialog(
        title = stringResource(id = R.string.pref_layout_reset_title),
        subtitle = stringResource(id = R.string.pref_layout_reset_summary),
        onConfirm = {
          appearancePrefs.topRightControls.delete()
          appearancePrefs.bottomRightControls.delete()
          appearancePrefs.bottomLeftControls.delete()
          appearancePrefs.portraitBottomControls.delete()
          showResetDialog = false
        },
        onCancel = { showResetDialog = false },
      )
    }

    val (topRightButtons, bottomRightButtons, bottomLeftButtons) =
      remember(topRState, bottomRState, bottomLState) {
        val usedButtons = mutableSetOf<PlayerButton>()
        val topR = appearancePrefs.parseButtons(topRState, usedButtons)
        val bottomR = appearancePrefs.parseButtons(bottomRState, usedButtons)
        val bottomL = appearancePrefs.parseButtons(bottomLState, usedButtons)
        listOf(topR, bottomR, bottomL)
      }

    val topLeftButtons = remember {
      listOf(PlayerButton.BACK_ARROW, PlayerButton.VIDEO_TITLE)
    }

    val portraitBottomButtons = remember(portraitBottomState) {
      appearancePrefs.parseButtons(portraitBottomState, mutableSetOf())
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text(text = stringResource(id = R.string.pref_layout_title)) },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            }
          },
          actions = {
            IconButton(onClick = { showResetDialog = true }) {
              Icon(Icons.Outlined.Restore, contentDescription = stringResource(id = R.string.pref_layout_reset_default))
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
          item {
            PreferenceCategoryWithEditButton(
              title = stringResource(id = R.string.pref_layout_top_right_controls),
              onClick = { backstack.add(ControlLayoutEditorScreen(ControlRegion.TOP_RIGHT)) },
            )
            PreferenceIconSummary(buttons = topRightButtons)
          }

          item {
            PreferenceCategoryWithEditButton(
              title = stringResource(id = R.string.pref_layout_bottom_right_controls),
              onClick = { backstack.add(ControlLayoutEditorScreen(ControlRegion.BOTTOM_RIGHT)) },
            )
            PreferenceIconSummary(buttons = bottomRightButtons)
          }

          item {
            PreferenceCategoryWithEditButton(
              title = stringResource(id = R.string.pref_layout_bottom_left_controls),
              onClick = { backstack.add(ControlLayoutEditorScreen(ControlRegion.BOTTOM_LEFT)) },
            )
            PreferenceIconSummary(buttons = bottomLeftButtons)
          }

          item {
            PreferenceCategoryWithEditButton(
              title = stringResource(id = R.string.pref_layout_portrait_bottom_controls),
              onClick = { backstack.add(ControlLayoutEditorScreen(ControlRegion.PORTRAIT_BOTTOM)) },
            )
            PreferenceIconSummary(buttons = portraitBottomButtons)
          }

          item {
            PreferenceCategory(title = { Text(stringResource(id = R.string.pref_layout_preview)) })
            ControlLayoutPreview(
              modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
              topLeftButtons = topLeftButtons,
              topRightButtons = topRightButtons,
              bottomRightButtons = bottomRightButtons,
              bottomLeftButtons = bottomLeftButtons,
              portraitBottomButtons = portraitBottomButtons,
            )
          }
        }
      }
    }
  }

  @Composable
  private fun PreferenceCategoryWithEditButton(
    title: String,
    onClick: () -> Unit,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 24.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = onClick) {
        Icon(
          imageVector = Icons.Outlined.Edit,
          contentDescription = "编辑 $title",
          tint = MaterialTheme.colorScheme.secondary,
        )
      }
    }
  }

  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  private fun PreferenceIconSummary(buttons: List<PlayerButton>) {
    FlowRow(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
      if (buttons.isEmpty()) {
        Text(
          "无",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        buttons.forEach { button ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            if (button != PlayerButton.VIDEO_TITLE) {
              Icon(
                imageVector = button.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            when (button) {
              PlayerButton.VIDEO_TITLE -> {
                Text(
                  "视频标题",
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              PlayerButton.CURRENT_CHAPTER -> {
                Text(
                  "1:06 • 第1章",
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              else -> {
                // Just show the icon
              }
            }
          }
        }
      }
    }
  }
}
