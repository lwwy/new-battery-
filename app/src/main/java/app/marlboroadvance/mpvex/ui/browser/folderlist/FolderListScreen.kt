package app.marlboroadvance.mpvex.ui.browser.folderlist

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import my.nanihadesuka.compose.LazyColumnScrollbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.FolderSortType
import app.marlboroadvance.mpvex.preferences.FolderViewMode
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.SortOrder
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.pullrefresh.PullRefreshBox
import app.marlboroadvance.mpvex.repository.MediaFileRepository
import app.marlboroadvance.mpvex.ui.browser.cards.FolderCard
import app.marlboroadvance.mpvex.ui.browser.cards.VideoCard
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteConfirmationDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.SortDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.ViewModeSelector
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle
import app.marlboroadvance.mpvex.ui.browser.fab.MediaActionFab
import app.marlboroadvance.mpvex.ui.browser.filesystem.FileSystemBrowserRootScreen
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.browser.sheets.PlayLinkSheet
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.ui.browser.states.LoadingState
import app.marlboroadvance.mpvex.ui.browser.states.PermissionDeniedState
import app.marlboroadvance.mpvex.ui.browser.videolist.VideoListScreen
import app.marlboroadvance.mpvex.ui.preferences.PreferencesScreen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import app.marlboroadvance.mpvex.utils.permission.PermissionUtils
import app.marlboroadvance.mpvex.utils.sort.SortUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.koinInject
import java.io.File

@Serializable
object FolderListScreen : Screen {
  @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val browserPreferences = koinInject<BrowserPreferences>()
    val folderViewMode by browserPreferences.folderViewMode.collectAsState()

    when (folderViewMode) {
      FolderViewMode.FileManager -> FileSystemBrowserRootScreen.Content()
      FolderViewMode.AlbumView -> MediaStoreFolderListContent()
    }
  }

  @Composable
  private fun MediaStoreFolderListContent() {
    val context = LocalContext.current
    val viewModel: FolderListViewModel =
      viewModel(factory = FolderListViewModel.factory(context.applicationContext as android.app.Application))
    val videoFolders by viewModel.videoFolders.collectAsState()
    val foldersWithNewCount by viewModel.foldersWithNewCount.collectAsState()
    val recentlyPlayedFilePath by viewModel.recentlyPlayedFilePath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasCompletedInitialLoad by viewModel.hasCompletedInitialLoad.collectAsState()
    val foldersWereDeleted by viewModel.foldersWereDeleted.collectAsState()
    val backstack = LocalBackStack.current
    val coroutineScope = rememberCoroutineScope()
    val browserPreferences = koinInject<BrowserPreferences>()
    val foldersPreferences = koinInject<app.marlboroadvance.mpvex.preferences.FoldersPreferences>()
    val advancedPreferences = koinInject<app.marlboroadvance.mpvex.preferences.AdvancedPreferences>()
    val enableRecentlyPlayed by advancedPreferences.enableRecentlyPlayed.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val listState = rememberLazyListState()
    val isRefreshing = remember { mutableStateOf(false) }
    val showLinkDialog = remember { mutableStateOf(false) }
    val sortDialogOpen = rememberSaveable { mutableStateOf(false) }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var hasRecentlyPlayed by remember { mutableStateOf(false) }
    val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var allVideos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var videosLoaded by remember { mutableStateOf(false) }

    val folderSortType by browserPreferences.folderSortType.collectAsState()
    val folderSortOrder by browserPreferences.folderSortOrder.collectAsState()

    val sortedFolders =
      remember(videoFolders, folderSortType, folderSortOrder) {
        SortUtils.sortFolders(videoFolders, folderSortType, folderSortOrder)
      }
    val filteredFolders = if (isSearching && searchQuery.isNotBlank()) {
      sortedFolders.filter { folder ->
        folder.name.contains(searchQuery, ignoreCase = true) ||
          folder.path.contains(searchQuery, ignoreCase = true)
      }
    } else {
      sortedFolders
    }

    val selectionManager =
      rememberSelectionManager(
        items = sortedFolders,
        getId = { it.bucketId },
        onDeleteItems = { folders ->
          val ids = folders.map { it.bucketId }.toSet()
          val videos = MediaFileRepository.getVideosForBuckets(context, ids)
          viewModel.deleteVideos(videos)
          Pair(videos.size, 0)
        },
        onOperationComplete = { viewModel.refresh() },
      )

    val permissionState =
      PermissionUtils.handleStoragePermission(
        onPermissionGranted = { viewModel.refresh() },
      )

    val filePicker =
      rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
      ) { uri ->
        uri?.let {
          runCatching {
            context.contentResolver.takePersistableUriPermission(
              it,
              Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
          }
          MediaUtils.playFile(it.toString(), context, "open_file")
        }
      }

    LaunchedEffect(Unit) {
      hasRecentlyPlayed =
        app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps
          .hasRecentlyPlayed()
    }

    DisposableEffect(lifecycleOwner) {
      val observer =
        LifecycleEventObserver { _, event ->
          if (event == Lifecycle.Event.ON_RESUME) {
            viewModel.recalculateNewVideoCounts()
          }
        }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
      }
    }

    LaunchedEffect(fabMenuExpanded) {
      if (fabMenuExpanded) {
        hasRecentlyPlayed =
          app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps
            .hasRecentlyPlayed()
      }
    }

    LaunchedEffect(isSearching) {
      if (isSearching && !videosLoaded) {
        val bucketIds = videoFolders.map { it.bucketId }.toSet()
        allVideos = MediaFileRepository.getVideosForBuckets(context, bucketIds)
        videosLoaded = true
      }
      if (!isSearching) {
        videosLoaded = false
        allVideos = emptyList()
      }
      if (isSearching) {
        focusRequester.requestFocus()
        keyboardController?.show()
      }
    }

    val filteredVideos = if (isSearching && searchQuery.isNotBlank() && videosLoaded) {
      allVideos.filter { video ->
        video.title.contains(searchQuery, ignoreCase = true) ||
          video.displayName.contains(searchQuery, ignoreCase = true) ||
          video.path.contains(searchQuery, ignoreCase = true)
      }
    } else emptyList()

    androidx.activity.compose.BackHandler(enabled = selectionManager.isInSelectionMode || isSearching) {
      when {
        selectionManager.isInSelectionMode -> selectionManager.clear()
        isSearching -> {
          isSearching = false
          searchQuery = ""
        }
      }
    }

    Scaffold(
      topBar = {
        if (isSearching) {
          SearchBar(
            inputField = {
              SearchBarDefaults.InputField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { },
                expanded = false,
                onExpandedChange = { },
                placeholder = { Text("搜索视频...") },
                leadingIcon = {
                  Icon(imageVector = Icons.Filled.Search, contentDescription = "搜索")
                },
                trailingIcon = {
                  IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "取消")
                  }
                },
                modifier = Modifier.focusRequester(focusRequester),
              )
            },
            expanded = false,
            onExpandedChange = { },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
          ) {}
        } else {
          BrowserTopBar(
            title = stringResource(app.marlboroadvance.mpvex.R.string.app_name),
            isInSelectionMode = selectionManager.isInSelectionMode,
            selectedCount = selectionManager.selectedCount,
            totalCount = videoFolders.size,
            onBackClick = null,
            onCancelSelection = { selectionManager.clear() },
            onSortClick = { sortDialogOpen.value = true },
            onSettingsClick = { backstack.add(PreferencesScreen) },
            onSearchClick = { isSearching = !isSearching },
            onDeleteClick = { deleteDialogOpen.value = true },
            onRenameClick = null,
            isSingleSelection = selectionManager.isSingleSelection,
            onInfoClick = null,
            onShareClick = {
              coroutineScope.launch {
                val selectedIds = selectionManager.getSelectedItems().map { it.bucketId }.toSet()
                val allVideos = MediaFileRepository.getVideosForBuckets(context, selectedIds)
                if (allVideos.isNotEmpty()) {
                  MediaUtils.shareVideos(context, allVideos)
                }
              }
            },
            onPlayClick = {
              coroutineScope.launch {
                val selectedIds = selectionManager.getSelectedItems().map { it.bucketId }.toSet()
                val allVideos = MediaFileRepository.getVideosForBuckets(context, selectedIds)
                if (allVideos.isNotEmpty()) {
                  if (allVideos.size == 1) {
                    MediaUtils.playFile(allVideos.first(), context)
                  } else {
                    val intent = Intent(Intent.ACTION_VIEW, allVideos.first().uri)
                    intent.setClass(context, app.marlboroadvance.mpvex.ui.player.PlayerActivity::class.java)
                    intent.putExtra("internal_launch", true)
                    intent.putParcelableArrayListExtra("playlist", ArrayList(allVideos.map { it.uri }))
                    intent.putExtra("playlist_index", 0)
                    intent.putExtra("launch_source", "playlist")
                    context.startActivity(intent)
                  }
                  selectionManager.clear()
                }
              }
            },
            onBlacklistClick = {
              coroutineScope.launch {
                val selectedFolders = selectionManager.getSelectedItems()
                val blacklistedFolders = foldersPreferences.blacklistedFolders.get().toMutableSet()
                selectedFolders.forEach { folder ->
                  blacklistedFolders.add(folder.path)
                }
                foldersPreferences.blacklistedFolders.set(blacklistedFolders)
                selectionManager.clear()
                viewModel.refresh()
                android.widget.Toast.makeText(
                  context,
                  context.getString(app.marlboroadvance.mpvex.R.string.pref_folders_blacklisted),
                  android.widget.Toast.LENGTH_SHORT,
                ).show()
              }
            },
            onSelectAll = { selectionManager.selectAll() },
            onInvertSelection = { selectionManager.invertSelection() },
            onDeselectAll = { selectionManager.clear() },
          )
        }
      },
      floatingActionButton = {
        // 悬浮播放按钮已被移除
      },
    ) { padding ->
      when (permissionState.status) {
        PermissionStatus.Granted -> {
          if (isSearching) {
            if (searchQuery.isNotBlank() && videosLoaded) {
              if (filteredVideos.isEmpty()) {
                Box(
                  modifier = Modifier.fillMaxSize().padding(padding).padding(bottom = 80.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  EmptyState(
                    icon = Icons.Filled.Folder,
                    title = "未找到视频",
                    message = "请尝试其他搜索关键词。",
                  )
                }
              } else {
                val searchListState = rememberLazyListState()
                val isAtTop by remember { derivedStateOf { searchListState.firstVisibleItemIndex == 0 && searchListState.firstVisibleItemScrollOffset == 0 } }
                val hasEnoughItems = filteredVideos.size > 20
                val scrollbarAlpha by androidx.compose.animation.core.animateFloatAsState(
                  targetValue = if (isAtTop || !hasEnoughItems) 0f else 1f,
                  animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
                  label = "scrollbarAlpha",
                )
                LazyColumnScrollbar(
                  state = searchListState,
                  settings = ScrollbarSettings(
                    thumbUnselectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f * scrollbarAlpha),
                    thumbSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = scrollbarAlpha),
                  ),
                ) {
                  LazyColumn(
                    state = searchListState,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 88.dp),
                  ) {
                    items(filteredVideos) { video ->
                      VideoCard(
                        video = video,
                        progressPercentage = null,
                        isRecentlyPlayed = false,
                        isSelected = false,
                        onClick = { MediaUtils.playFile(video, context, "search") },
                        onLongClick = {},
                        onThumbClick = {},
                      )
                    }
                  }
                }
              }
            }
          } else {
            FolderListContent(
              folders = filteredFolders,
              foldersWithNewCount = foldersWithNewCount,
              listState = listState,
              isRefreshing = isRefreshing,
              isLoading = isLoading,
              hasCompletedInitialLoad = hasCompletedInitialLoad,
              foldersWereDeleted = foldersWereDeleted,
              recentlyPlayedFilePath = recentlyPlayedFilePath,
              onRefresh = { viewModel.refresh() },
              selectionManager = selectionManager,
              onFolderClick = { folder ->
                if (selectionManager.isInSelectionMode) {
                  selectionManager.toggle(folder)
                } else {
                  fabMenuExpanded = false
                  backstack.add(VideoListScreen(folder.bucketId, folder.name))
                }
              },
              onFolderLongClick = { folder -> selectionManager.toggle(folder) },
              modifier = Modifier.padding(padding),
            )
          }
        }
        is PermissionStatus.Denied -> {
          PermissionDeniedState(
            onRequestPermission = { permissionState.launchPermissionRequest() },
            modifier = Modifier.padding(padding),
          )
        }
      }
            PlayLinkSheet(
        isOpen = showLinkDialog.value,
        onDismiss = { showLinkDialog.value = false },
        onPlayLink = { url -> MediaUtils.playFile(url, context, "play_link") },
      )

      FolderSortDialog(
        isOpen = sortDialogOpen.value,
        onDismiss = { sortDialogOpen.value = false },
        sortType = folderSortType,
        sortOrder = folderSortOrder,
        onSortTypeChange = { browserPreferences.folderSortType.set(it) },
        onSortOrderChange = { browserPreferences.folderSortOrder.set(it) },
      )

      DeleteConfirmationDialog(
        isOpen = deleteDialogOpen.value,
        onDismiss = { deleteDialogOpen.value = false },
        onConfirm = { selectionManager.deleteSelected() },
        itemType = "folder",
        itemCount = selectionManager.selectedCount,
      )
    }
  }
}

@Composable
private fun FolderListContent(
  folders: List<VideoFolder>,
  foldersWithNewCount: List<FolderWithNewCount>,
  listState: LazyListState,
  isRefreshing: MutableState<Boolean>,
  isLoading: Boolean,
  hasCompletedInitialLoad: Boolean,
  foldersWereDeleted: Boolean,
  recentlyPlayedFilePath: String?,
  onRefresh: suspend () -> Unit,
  selectionManager: app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager<VideoFolder, String>,
  onFolderClick: (VideoFolder) -> Unit,
  onFolderLongClick: (VideoFolder) -> Unit,
  modifier: Modifier = Modifier,
) {
  val gesturePreferences = koinInject<GesturePreferences>()
  val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()

  val showEmpty = folders.isEmpty() && !isLoading && hasCompletedInitialLoad
  val showLoading = isLoading && folders.isEmpty()

  val isAtTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 } }
  val hasEnoughItems = folders.size > 20
  val scrollbarAlpha by androidx.compose.animation.core.animateFloatAsState(
    targetValue = if (isAtTop || !hasEnoughItems) 0f else 1f,
    animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
    label = "scrollbarAlpha",
  )

  PullRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    listState = listState,
    modifier = modifier.fillMaxSize(),
  ) {
    if (showLoading || showEmpty) {
      Box(
        modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
        contentAlignment = Alignment.Center,
      ) {
        if (showLoading) {
          LoadingState(
            icon = Icons.Filled.Folder,
            title = "正在扫描视频...",
            message = "正在搜索您的设备，请稍候",
          )
        } else if (showEmpty) {
          EmptyState(
            icon = Icons.Filled.Folder,
            title = "未找到视频文件夹",
            message = "请将视频文件添加到您的设备中",
          )
        }
      }
    } else {
      LazyColumnScrollbar(
        state = listState,
        settings = ScrollbarSettings(
          thumbUnselectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f * scrollbarAlpha),
          thumbSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = scrollbarAlpha),
        ),
      ) {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 88.dp),
        ) {
          items(folders) { folder ->
            val isRecentlyPlayed =
              recentlyPlayedFilePath?.let { filePath ->
                val file = File(filePath)
                file.parent == folder.path
              } ?: false

            val newCount = foldersWithNewCount.find { it.folder.bucketId == folder.bucketId }?.newVideoCount ?: 0

            FolderCard(
              folder = folder,
              isSelected = selectionManager.isSelected(folder),
              isRecentlyPlayed = isRecentlyPlayed,
              onClick = { onFolderClick(folder) },
              onLongClick = { onFolderLongClick(folder) },
              onThumbClick = if (tapThumbnailToSelect) {
                { onFolderLongClick(folder) }
              } else {
                { onFolderClick(folder) }
              },
              newVideoCount = newCount,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FolderSortDialog(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  sortType: FolderSortType,
  sortOrder: SortOrder,
  onSortTypeChange: (FolderSortType) -> Unit,
  onSortOrderChange: (SortOrder) -> Unit,
) {
  val browserPreferences = koinInject<BrowserPreferences>()
  val appearancePreferences = koinInject<AppearancePreferences>()
  val showTotalVideosChip by browserPreferences.showTotalVideosChip.collectAsState()
  val showTotalDurationChip by browserPreferences.showTotalDurationChip.collectAsState()
  val showTotalSizeChip by browserPreferences.showTotalSizeChip.collectAsState()
  val showFolderPath by browserPreferences.showFolderPath.collectAsState()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
  val folderViewMode by browserPreferences.folderViewMode.collectAsState()

  val isAlbumView = folderViewMode == FolderViewMode.AlbumView

  SortDialog(
    isOpen = isOpen,
    onDismiss = onDismiss,
    title = if (isAlbumView) "排序与视图选项" else "视图选项",
    sortType = sortType.displayName,
    onSortTypeChange = { typeName ->
      FolderSortType.entries.find { it.displayName == typeName }?.let(onSortTypeChange)
    },
    sortOrderAsc = sortOrder.isAscending,
    onSortOrderChange = { isAsc ->
      onSortOrderChange(if (isAsc) SortOrder.Ascending else SortOrder.Descending)
    },
    types = listOf(
        FolderSortType.Title.displayName,
        FolderSortType.Date.displayName,
        FolderSortType.Size.displayName,
      ),
    icons = listOf(
        Icons.Filled.Title,
        Icons.Filled.CalendarToday,
        Icons.Filled.SwapVert,
      ),
    getLabelForType = { type, _ ->
      when (type) {
        FolderSortType.Title.displayName -> Pair("A-Z", "Z-A")
        FolderSortType.Date.displayName -> Pair("最旧", "最新")
        FolderSortType.Size.displayName -> Pair("最小", "最大")
        else -> Pair("升序", "降序")
      }
    },
    showSortOptions = isAlbumView,
    viewModeSelector = ViewModeSelector(
        label = "视图模式",
        firstOptionLabel = "文件夹视图",
        secondOptionLabel = "树状视图",
        firstOptionIcon = Icons.Filled.ViewModule,
        secondOptionIcon = Icons.Filled.AccountTree,
        isFirstOptionSelected = folderViewMode == FolderViewMode.AlbumView,
        onViewModeChange = { isFirstOption ->
          browserPreferences.folderViewMode.set(
            if (isFirstOption) FolderViewMode.AlbumView else FolderViewMode.FileManager,
          )
        },
      ),
    visibilityToggles = listOf(
        VisibilityToggle(label = "完整名称", checked = unlimitedNameLines, onCheckedChange = { appearancePreferences.unlimitedNameLines.set(it) }),
        VisibilityToggle(label = "路径", checked = showFolderPath, onCheckedChange = { browserPreferences.showFolderPath.set(it) }),
        VisibilityToggle(label = "视频总数", checked = showTotalVideosChip, onCheckedChange = { browserPreferences.showTotalVideosChip.set(it) }),
        VisibilityToggle(label = "总时长", checked = showTotalDurationChip, onCheckedChange = { browserPreferences.showTotalDurationChip.set(it) }),
        VisibilityToggle(label = "文件夹大小", checked = showTotalSizeChip, onCheckedChange = { browserPreferences.showTotalSizeChip.set(it) }),
      ),
  )
}
