package com.dylandos.iptv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.dylandos.iptv.data.entity.ChannelEntity
import com.dylandos.iptv.ui.components.ChannelCard
import com.dylandos.iptv.ui.navigation.AppSection
import com.dylandos.iptv.ui.theme.OnDarkMid
import com.dylandos.iptv.ui.AppViewModel

/**
 * Shared responsive grid for Live TV / Movies / Series. Paging 3 feeds items so
 * huge provider catalogs never balloon memory; each row is a lazy item.
 */
@Composable
fun ContentGridScreen(
    viewModel: AppViewModel,
    items: LazyPagingItems<ChannelEntity>,
    section: AppSection,
    onChannelClick: (ChannelEntity) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        when {
            items.loadState.refresh is LoadState.Loading && items.itemCount == 0 -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            items.loadState.refresh is LoadState.Error && items.itemCount == 0 -> {
                Text(
                    text = "Could not load ${section.name}",
                    color = OnDarkMid,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(items.itemCount, key = { index -> items[index]?.id ?: -index }) { index ->
                        val channel = items[index]
                        if (channel != null) {
                            ChannelCard(
                                channel = channel,
                                onClick = { onChannelClick(channel) }
                            )
                        }
                    }
                }
            }
        }
    }
}
