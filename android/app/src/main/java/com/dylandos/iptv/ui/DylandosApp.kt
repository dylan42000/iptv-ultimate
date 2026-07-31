package com.dylandos.iptv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.foundation.focusGroup
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.dylandos.iptv.ui.components.CollapsingNavRail
import com.dylandos.iptv.ui.navigation.AppSection
import com.dylandos.iptv.ui.navigation.Destination
import com.dylandos.iptv.ui.player.PlayerController
import com.dylandos.iptv.ui.player.PlayerScreen
import com.dylandos.iptv.ui.screens.ContentGridScreen
import com.dylandos.iptv.ui.screens.ContentViewModel
import com.dylandos.iptv.ui.screens.DvrScreen
import com.dylandos.iptv.ui.screens.DvrViewModel
import com.dylandos.iptv.ui.screens.EpgScreen
import com.dylandos.iptv.ui.screens.EpgViewModel
import com.dylandos.iptv.ui.screens.ManageCategoriesScreen
import com.dylandos.iptv.ui.screens.ManageCategoriesViewModel
import com.dylandos.iptv.ui.screens.SettingsScreen
import com.dylandos.iptv.ui.screens.SettingsViewModel
import com.dylandos.iptv.ui.theme.DylandosTheme

/**
 * Root application scaffold. Routes between the main section layout and the
 * detail/player destinations, and manages the collapsing nav rail focus.
 */
@Composable
fun DylandosApp(
    playerController: PlayerController,
    appViewModel: AppViewModel = hiltViewModel(),
    contentViewModel: ContentViewModel = hiltViewModel(),
    dvrViewModel: DvrViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    DylandosTheme {
        val destination by appViewModel.destination.collectAsState()
        val navState by appViewModel.navState.collectAsState()

        when (val dest = destination) {
            is Destination.Player -> {
                PlayerScreen(
                    channelId = dest.channelId,
                    controller = playerController,
                    onBack = { appViewModel.navigateBack() }
                )
            }
            Destination.EpgGuide -> {
                val epg = hiltViewModel<EpgViewModel>()
                LaunchedEffect(Unit) { epg.refreshEpg() }
                Box(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                    if (event.key == Key.Back) { appViewModel.navigateBack(); true } else false
                }) {
                    EpgScreen(epg)
                }
            }
            Destination.ManageCategories -> {
                val mgmt = hiltViewModel<ManageCategoriesViewModel>()
                Box(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                    if (event.key == Key.Back) { appViewModel.navigateBack(); true } else false
                }) {
                    ManageCategoriesScreen(mgmt)
                }
            }
            else -> MainScaffold(
                appViewModel = appViewModel,
                contentViewModel = contentViewModel,
                dvrViewModel = dvrViewModel,
                settingsViewModel = settingsViewModel,
                playerController = playerController
            )
        }
    }
}

@Composable
private fun MainScaffold(
    appViewModel: AppViewModel,
    contentViewModel: ContentViewModel,
    dvrViewModel: DvrViewModel,
    settingsViewModel: SettingsViewModel,
    playerController: PlayerController
) {
    val navState by appViewModel.navState.collectAsState()
    val section = navState.section
    val railFocusRequester = remember { FocusRequester() }
    var railExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    // Route content section to the correct paging type.
    LaunchedEffect(section) {
        contentViewModel.setSection(
            when (section) {
                AppSection.LIVE_TV -> "live"
                AppSection.MOVIES -> "movie"
                AppSection.SERIES -> "series"
                else -> "live"
            }
        )
    }

    Row(Modifier.fillMaxSize().focusGroup()) {
        CollapsingNavRail(
            selected = section,
            onSelect = {
                appViewModel.selectSection(it)
                railExpanded = false
            },
            forceExpanded = railExpanded,
            firstItemFocusRequester = railFocusRequester,
            modifier = Modifier
        )

        Box(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    when (event.key) {
                        Key.DirectionLeft -> {
                            railExpanded = true
                            railFocusRequester.requestFocus()
                            true
                        }
                        Key.Back -> {
                            if (railExpanded) {
                                railExpanded = false
                                true
                            } else false
                        }
                        else -> false
                    }
                }
        ) {
            when (section) {
                AppSection.LIVE_TV,
                AppSection.MOVIES,
                AppSection.SERIES -> {
                    val pagingItems = contentViewModel.channels.collectAsLazyPagingItems()
                    ContentGridScreen(
                        viewModel = appViewModel,
                        items = pagingItems,
                        section = section,
                        onChannelClick = { channel ->
                            appViewModel.openPlayer(channel.id)
                        }
                    )
                }
                AppSection.DVR -> DvrScreen(dvrViewModel)
                AppSection.SETTINGS -> SettingsScreen(settingsViewModel)
            }
        }
    }
}
