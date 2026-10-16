package repro.flowrow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** 8.dp = 23.1 px: FlowRow's fit check uses ceil (24), its reported size uses roundToPx (23). 420 dpi @ 110%. */
const val FRACTIONAL_DENSITY = 2.8875f

/** 8.dp = 21.0 px: ceil == round. Plain 420 dpi, the control. */
const val INTEGRAL_SPACING_DENSITY = 2.625f

const val FIRST = "first"
const val SECOND = "second"

/** 150.dp + 150.dp > 200.dp, so the second item always wraps to line 2. */
@Composable
fun TwoLineFlowRow(
    modifier: Modifier = Modifier,
    onSecondPlaced: () -> Unit = {},
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.width(200.dp),
    ) {
        Box(
            Modifier
                .size(150.dp, 20.dp)
                .background(Color(0xFF1E88E5))
                .testTag(FIRST)
        )
        Box(
            Modifier
                .size(150.dp, 20.dp)
                .background(Color(0xFFFB8C00))
                .testTag(SECOND)
                .onGloballyPositioned { onSecondPlaced() }
        )
    }
}
