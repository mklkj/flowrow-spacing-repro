package repro.flowrow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FlowRow converts verticalArrangement spacing to px twice, differently:
 *  - breakDownItems (fit check):            ceil(spacing.toPx())   -> 8.dp @ 2.8875 = 24 px
 *  - placeHelper / intrinsics (reported):   spacing.roundToPx()    -> 8.dp @ 2.8875 = 23 px
 * So FlowRow reports a height 1 px smaller than its own fit check needs. Measured again with
 * exactly that height, the second line "doesn't fit" and is dropped (overflow is always Clip).
 *
 * Densities and TwoLineFlowRow are shared with MainActivity, which shows the same scenarios on screen.
 */
@RunWith(AndroidJUnit4::class)
class FlowRowSpacingRoundingTest {

    @get:Rule
    val rule = createComposeRule()

    // What View parents do (e.g. FrameLayout re-measuring a match_parent ComposeView with
    // EXACTLY(measuredHeight)): measure loose, then again with the reported height as a fixed size.
    @Test
    fun fractionalDensity_remeasureWithReportedHeight_keepsSecondLine() =
        remeasureWithReportedHeight(density = FRACTIONAL_DENSITY) // FAILS

    @Test
    fun integralSpacingDensity_remeasureWithReportedHeight_keepsSecondLine() =
        remeasureWithReportedHeight(density = INTEGRAL_SPACING_DENSITY) // passes

    // Pure-Compose variant: intrinsic height uses roundToPx, the measure uses ceil.
    @Test
    fun fractionalDensity_intrinsicMinHeight_keepsSecondLine() =
        intrinsicMinHeight(density = FRACTIONAL_DENSITY) // FAILS

    @Test
    fun integralSpacingDensity_intrinsicMinHeight_keepsSecondLine() =
        intrinsicMinHeight(density = INTEGRAL_SPACING_DENSITY) // passes

    private fun remeasureWithReportedHeight(density: Float) {
        var fixedHeightPx by mutableStateOf<Int?>(null)
        var reportedHeightPx = -1

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density)) {
                Box(
                    Modifier.layout { measurable, constraints ->
                        val loose = constraints.copy(minWidth = 0, minHeight = 0)
                        val placeable = measurable.measure(
                            fixedHeightPx?.let { loose.copy(minHeight = it, maxHeight = it) } ?: loose
                        )
                        if (fixedHeightPx == null) reportedHeightPx = placeable.height
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                ) {
                    TwoLineFlowRow()
                }
            }
        }

        // Loose constraints: both lines are placed.
        rule.onNodeWithTag(SECOND).assertIsDisplayed()

        // Same FlowRow, same content, given exactly the height it just reported.
        rule.runOnIdle { fixedHeightPx = reportedHeightPx }
        rule.onNodeWithTag(SECOND).assertIsDisplayed()
    }

    private fun intrinsicMinHeight(density: Float) {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density)) {
                // Width on the Box too: intrinsic-height queries pass the parent's width through
                // Modifier.width, so without it FlowRow would be asked at full screen width.
                Box(Modifier.width(200.dp).height(IntrinsicSize.Min)) {
                    TwoLineFlowRow(Modifier.fillMaxHeight())
                }
            }
        }

        rule.onNodeWithTag(SECOND).assertIsDisplayed()
    }
}
