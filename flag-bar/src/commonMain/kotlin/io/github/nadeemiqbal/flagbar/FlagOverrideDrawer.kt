package io.github.nadeemiqbal.flagbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Standalone Compose drawer that lists every flag in [bar] with the appropriate per-type
 * override editor:
 *  - [BoolFlag] → switch
 *  - [IntFlag] → numeric text field
 *  - [StringFlag] → free-form text field
 *  - [EnumFlag] → dropdown of `options`
 *  - [VariantFlag] → dropdown of `variants`
 *
 * Use this directly when you don't want `debug-bar` as a parent (just drop it into a
 * `ModalBottomSheet` / `Dialog` / `Drawer` of your own). When `debug-bar` IS in the picture, use
 * [FlagBarSection] instead so flags get a tab in the debug drawer.
 *
 * Test tags:
 *  - root: `flag_override_drawer`
 *  - per-flag row: `flag_row_{flag.key}`
 *  - override-badge: `flag_badge_{flag.key}` (only present when overridden)
 *  - reset-all: `flag_reset_all`
 *  - refresh: `flag_refresh`
 */
@Composable
fun FlagOverrideDrawer(bar: FlagBar, modifier: Modifier = Modifier) {
    val overrides by bar.overrides.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.testTag("flag_override_drawer").fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${bar.flags.size} flag(s) · ${overrides.size} overridden",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                IconButton(
                    onClick = { scope.launch { bar.refresh() } },
                    modifier = Modifier.testTag("flag_refresh"),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh remote flags")
                }
                TextButton(
                    onClick = { bar.clearAllOverrides() },
                    modifier = Modifier.testTag("flag_reset_all"),
                ) { Text("Reset all") }
            }
        }

        if (bar.flags.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No flags registered.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(bar.flags, key = { it.key }) { flag ->
                    FlagRow(bar = bar, flag = flag)
                }
            }
        }
    }
}

@Composable
private fun FlagRow(bar: FlagBar, flag: Flag<*>) {
    val overrides by bar.overrides.collectAsState()
    val remote by bar.remote.collectAsState()
    val hasOverride = flag.key in overrides
    @Suppress("UNCHECKED_CAST")
    val typedFlag = flag as Flag<Any?>
    val currentValue = remember(overrides, remote, flag, bar.userId) { bar.value(typedFlag) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().testTag("flag_row_${flag.key}"),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    flag.key,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                if (hasOverride) {
                    OverrideBadge(flag.key)
                }
            }
            Text(
                "default: ${flag.default}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(modifier = Modifier.padding(top = 6.dp).fillMaxWidth()) {
                when (flag) {
                    is BoolFlag -> BoolEditor(bar = bar, flag = flag, current = currentValue as Boolean)
                    is IntFlag -> IntEditor(bar = bar, flag = flag, current = currentValue as Int)
                    is StringFlag -> StringEditor(bar = bar, flag = flag, current = currentValue as String)
                    is EnumFlag<*> -> EnumEditor(bar = bar, flag = flag, current = currentValue as Enum<*>)
                    is VariantFlag -> VariantEditor(bar = bar, flag = flag, current = currentValue as String)
                }
            }
        }
    }
}

@Composable
private fun OverrideBadge(flagKey: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = CircleShape,
        modifier = Modifier.testTag("flag_badge_$flagKey"),
    ) {
        Text(
            "override",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BoolEditor(bar: FlagBar, flag: BoolFlag, current: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(
            checked = current,
            onCheckedChange = { bar.setOverride(flag, it) },
            modifier = Modifier.testTag("flag_switch_${flag.key}"),
        )
        Text("  $current", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun IntEditor(bar: FlagBar, flag: IntFlag, current: Int) {
    var text by remember(current) { mutableStateOf(current.toString()) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        BasicTextField(
            value = text,
            onValueChange = { new ->
                text = new
                new.toIntOrNull()?.let { bar.setOverride(flag, it) }
            },
            modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("flag_int_${flag.key}"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun StringEditor(bar: FlagBar, flag: StringFlag, current: String) {
    var text by remember(current) { mutableStateOf(current) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        BasicTextField(
            value = text,
            onValueChange = { new ->
                text = new
                bar.setOverride(flag, new)
            },
            modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("flag_string_${flag.key}"),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun EnumEditor(bar: FlagBar, flag: EnumFlag<*>, current: Enum<*>) {
    // EnumFlag<E : Enum<E>> doesn't accept Enum<*> as a type argument, so we hide the
    // type-parameter behind an internal helper that does the unsafe cast in one spot.
    val options: List<Enum<*>> = flag.options
    var expanded by remember { mutableStateOf(false) }
    Surface(
        onClick = { expanded = true },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().testTag("flag_enum_${flag.key}"),
    ) {
        Text(
            current.name,
            modifier = Modifier.padding(10.dp),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.name) },
                onClick = {
                    setEnumOverride(bar, flag, option)
                    expanded = false
                },
            )
        }
    }
}

/**
 * Internal helper: erases the type parameter so we can call `setOverride` on an `EnumFlag<*>`
 * without the F-bounded `EnumFlag<E : Enum<E>>` constraint biting us. Centralises the unsafe
 * cast so the editor itself stays type-flat.
 */
@Suppress("UNCHECKED_CAST")
private fun setEnumOverride(bar: FlagBar, flag: EnumFlag<*>, value: Enum<*>) {
    (bar as FlagBar).setOverride(flag as Flag<Enum<*>>, value)
}

@Composable
private fun VariantEditor(bar: FlagBar, flag: VariantFlag, current: String) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Surface(
            onClick = { expanded = true },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth().testTag("flag_variant_${flag.key}"),
        ) {
            Text(
                current,
                modifier = Modifier.padding(10.dp),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            flag.variants.forEachIndexed { i, variant ->
                val weight = flag.weights[i]
                DropdownMenuItem(
                    text = { Text("$variant  (weight ${weight})") },
                    onClick = {
                        bar.setOverride(flag, variant)
                        expanded = false
                    },
                )
            }
        }
        if (bar.userId != null) {
            Text(
                "user='${bar.userId}' · hash-assigned: ${assignVariant(bar.userId, flag)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Suppress("unused")
private fun ensureBgImport(m: Modifier) = m.background(Color.Unspecified)

@Suppress("unused")
private fun ensureSizeImport(m: Modifier) = m.size(0.dp)
