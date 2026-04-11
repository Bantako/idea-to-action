package org.mrlem.composesample.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mrlem.composesample.theme.Theme

@Composable
internal fun SettingsScreen(
    onNavigateToThemes: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "設定",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(Theme.size.medium),
        )
        HorizontalDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToThemes)
                .padding(Theme.size.medium),
        ) {
            Text(
                text = "テーマを管理する",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
        HorizontalDivider()

        Text(
            text = "Claude API キー",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = Theme.size.medium, top = Theme.size.medium, bottom = Theme.size.small),
        )
        TextField(
            value = state.apiKey,
            onValueChange = { viewModel.onAction(SettingsViewAction.ApiKeyChange(it)) },
            placeholder = { Text("sk-ant-...") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.size.medium),
        )

        if (state.saved) {
            Text(
                text = "保存しました",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = Theme.size.medium, vertical = Theme.size.small),
            )
        }

        Button(
            onClick = { viewModel.onAction(SettingsViewAction.Save) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.size.medium),
        ) {
            Text("保存")
        }
    }
}
