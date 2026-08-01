package com.dylandos.iptv.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dylandos.iptv.data.entity.CategoryEntity
import com.dylandos.iptv.ui.components.GlassPanel
import com.dylandos.iptv.ui.components.tvFocus
import com.dylandos.iptv.ui.theme.BrandRed
import com.dylandos.iptv.ui.theme.OnDarkHigh
import com.dylandos.iptv.ui.theme.OnDarkMid

@Composable
fun ManageCategoriesScreen(viewModel: ManageCategoriesViewModel) {
    val categories by viewModel.categories.collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text(
            text = "Manage Categories",
            style = MaterialTheme.typography.displayMedium,
            color = OnDarkHigh
        )
        Text(
            text = "Press CENTER / long-press to hide or show a category.",
            style = MaterialTheme.typography.bodyLarge,
            color = OnDarkMid,
            modifier = Modifier.padding(top = 4.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                CategoryRow(category) { viewModel.toggle(category) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryRow(category: CategoryEntity, onToggle: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .then(Modifier.tvFocus(scale = 1.02f))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onLongClick = onToggle,
                    onClick = onToggle
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(category.categoryName, style = MaterialTheme.typography.titleMedium, color = OnDarkHigh)
                Text(category.type, style = MaterialTheme.typography.labelMedium, color = OnDarkMid)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (category.isUserVisible) "VISIBLE" else "HIDDEN",
                style = MaterialTheme.typography.titleSmall,
                color = if (category.isUserVisible) BrandRed else OnDarkMid
            )
        }
    }
}
