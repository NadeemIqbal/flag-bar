package io.github.nadeemiqbal.flagbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.nadeemiqbal.debugbar.DebugBarSection

/**
 * `debug-bar` plugin: surfaces the [FlagOverrideDrawer] as a tab inside the debug drawer.
 *
 * Usage:
 * ```
 * DebugBar(
 *     sections = listOf(
 *         FlagBarSection(flags),           // ← this library's debug-bar plugin
 *         NetworkLogSection(networkStore),
 *         LogViewerSection(logStore),
 *         // ...
 *     ),
 * ) { MainAppContent() }
 * ```
 *
 * The tab's badge shows the count of currently-active overrides, so a glance at the debug-bar
 * tab strip tells you whether anyone has temporarily tweaked any flag.
 */
class FlagBarSection(
    private val bar: FlagBar,
    override val title: String = "Flags",
) : DebugBarSection {

    override val icon: ImageVector get() = Icons.Outlined.Flag

    override val badgeCount: Int?
        get() = bar.overrides.value.size.takeIf { it > 0 }

    @Composable
    override fun Content() {
        // Trigger recomposition when overrides change so the badge stays current.
        val overrides by bar.overrides.collectAsState()
        @Suppress("UNUSED_VARIABLE")
        val _trigger = overrides
        FlagOverrideDrawer(bar = bar)
    }
}
