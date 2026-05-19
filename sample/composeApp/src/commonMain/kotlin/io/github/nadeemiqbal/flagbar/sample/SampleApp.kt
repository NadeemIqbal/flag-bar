package io.github.nadeemiqbal.flagbar.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.nadeemiqbal.debugbar.DebugBar
import io.github.nadeemiqbal.debugbar.DebugBarActivation
import io.github.nadeemiqbal.debugbar.rememberDebugBarState
import io.github.nadeemiqbal.flagbar.BoolFlag
import io.github.nadeemiqbal.flagbar.EnumFlag
import io.github.nadeemiqbal.flagbar.FlagBarSection
import io.github.nadeemiqbal.flagbar.FlagSource
import io.github.nadeemiqbal.flagbar.IntFlag
import io.github.nadeemiqbal.flagbar.StringFlag
import io.github.nadeemiqbal.flagbar.VariantFlag
import io.github.nadeemiqbal.flagbar.collectFlagValue
import io.github.nadeemiqbal.flagbar.rememberFlagBar

/** Theme enum used by the demo's Enum flag. */
private enum class Theme { System, Light, Dark }

/** All demo flags as a typed object so call sites are type-safe. */
private object DemoFlags {
    val newCheckout = BoolFlag("new_checkout", default = false)
    val maxRetries = IntFlag("max_retries", default = 3)
    val apiBaseUrl = StringFlag("api_base_url", default = "https://api.example.com")
    val theme = EnumFlag("theme", default = Theme.System, options = Theme.entries)
    val checkoutVariant = VariantFlag(
        key = "checkout_variant",
        variants = listOf("control", "v1", "v2"),
        weights = listOf(0.5, 0.25, 0.25),
        default = "control",
    )
}

@Composable
fun SampleApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            DemoScreen()
        }
    }
}

@Composable
private fun DemoScreen() {
    val flags = rememberFlagBar(
        flags = listOf(
            DemoFlags.newCheckout,
            DemoFlags.maxRetries,
            DemoFlags.apiBaseUrl,
            DemoFlags.theme,
            DemoFlags.checkoutVariant,
        ),
        userId = "demo-user-42",
        remoteSource = FlagSource.static(
            mapOf(
                // Pretend these came from a remote endpoint. Note `new_checkout` is true on the
                // server but the user can override it locally via the drawer.
                "new_checkout" to true,
                "max_retries" to 5,
            ),
        ),
    )

    // Wrap the whole app in debug-bar so the flag drawer becomes a tab.
    val debugState = rememberDebugBarState()

    DebugBar(
        enabled = true,
        activation = DebugBarActivation.LongPressCorner() + DebugBarActivation.KeyboardShortcut(),
        state = debugState,
        sections = listOf(
            FlagBarSection(flags),  // <-- the integration: flags get a tab inside debug-bar
        ),
    ) {
        // Reactive reads — the screen recomposes when the user toggles overrides in the drawer.
        val newCheckout by remember(flags) { flags.overrides }.let { _ ->
            // collectFlagValue is the reactive read; we wrap in `by` for syntactic sugar.
            flags.collectFlagValueState(DemoFlags.newCheckout)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("FlagBar — Compose Multiplatform", style = MaterialTheme.typography.titleLarge)
            Text(
                "Long-press top-right corner (or press Cmd+Shift+D) to open the debug drawer; " +
                    "the Flags tab lets you toggle every flag at runtime. Tap a switch / change a " +
                    "value → the demo screen below reacts immediately.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { debugState.openSection("Flags") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            ) { Text("Open Flags drawer") }

            FlagSnapshotCard(flags)

            Spacer(modifier = Modifier.height(8.dp))

            CheckoutCard(
                showNewCheckout = newCheckout,
                variant = flags.collectFlagValue(DemoFlags.checkoutVariant),
                maxRetries = flags.collectFlagValue(DemoFlags.maxRetries),
                theme = flags.collectFlagValue(DemoFlags.theme),
                apiBaseUrl = flags.collectFlagValue(DemoFlags.apiBaseUrl),
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Variant is hash-assigned from user '${flags.userId}'. Toggle 'override' in the " +
                    "drawer to force a specific variant for testing.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FlagSnapshotCard(flags: io.github.nadeemiqbal.flagbar.FlagBar) {
    // Re-derive on overrides/remote change.
    val overrides by flags.overrides.collectAsState()
    val remote by flags.remote.collectAsState()
    val snapshot = remember(overrides, remote) { flags.snapshot() }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Live flag snapshot", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            snapshot.forEach { line ->
                Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CheckoutCard(
    showNewCheckout: Boolean,
    variant: String,
    maxRetries: Int,
    theme: Theme,
    apiBaseUrl: String,
) {
    Surface(
        color = if (showNewCheckout) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                if (showNewCheckout) "✨ New checkout (variant: $variant)" else "Old checkout (variant: $variant)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = if (showNewCheckout) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "maxRetries=$maxRetries · theme=$theme · api=$apiBaseUrl",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = if (showNewCheckout) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Tiny helper that wraps [collectFlagValue] so the call site can use `by` delegation. Returns a
 * `State<T>` rather than `T` directly.
 */
@Composable
private fun <T> io.github.nadeemiqbal.flagbar.FlagBar.collectFlagValueState(
    flag: io.github.nadeemiqbal.flagbar.Flag<T>,
): androidx.compose.runtime.State<T> {
    val v = collectFlagValue(flag)
    val s = remember { androidx.compose.runtime.mutableStateOf(v) }
    s.value = v
    return s
}

@Suppress("unused")
private fun ensureBgImport(m: Modifier) = m.background(androidx.compose.ui.graphics.Color.Unspecified)

@Suppress("unused")
private fun ensureBoxImport() = Box(modifier = Modifier) { }
