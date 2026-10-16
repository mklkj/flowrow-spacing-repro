package repro.flowrow

import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ReproScreen() }
    }
}

@Composable
private fun ReproScreen() {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(16.dp),
    ) {
        BasicText(
            "FlowRow(verticalArrangement = spacedBy(8.dp)) with two items; the orange one wraps " +
                "to line 2. Grey = the FlowRow's container.",
            style = TextStyle(fontSize = 14.sp),
        )
        Scenario("1. Measured once (baseline)") { onSecondPlaced ->
            TwoLineFlowRow(onSecondPlaced = onSecondPlaced)
        }
        Scenario("2. Measured again with exactly the height it reported") { onSecondPlaced ->
            RemeasuredWithReportedHeight(onSecondPlaced)
        }
        Scenario("3. Inside a FrameLayout (View re-measure, what Snackbar does)") { onSecondPlaced ->
            InsideFrameLayout(onSecondPlaced)
        }
        Scenario("4. Box(Modifier.height(IntrinsicSize.Min)) + fillMaxHeight()") { onSecondPlaced ->
            InsideIntrinsicMinHeight(onSecondPlaced)
        }
    }
}

@Composable
private fun Scenario(title: String, content: @Composable (onSecondPlaced: () -> Unit) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
        DensityCell(
            label = "density $FRACTIONAL_DENSITY (420 dpi @ 110%): 8.dp = 23.1 px → ceil 24 / round 23",
            density = FRACTIONAL_DENSITY,
            content = content,
        )
        DensityCell(
            label = "density $INTEGRAL_SPACING_DENSITY (420 dpi): 8.dp = 21 px → ceil 21 / round 21",
            density = INTEGRAL_SPACING_DENSITY,
            content = content,
        )
    }
}

@Composable
private fun DensityCell(
    label: String,
    density: Float,
    content: @Composable (onSecondPlaced: () -> Unit) -> Unit,
) {
    var secondPlaced by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(label, style = TextStyle(fontSize = 12.sp, color = Color.DarkGray))
        CompositionLocalProvider(LocalDensity provides Density(density)) {
            Box(Modifier.background(Color(0xFFE0E0E0))) {
                content { secondPlaced = true }
            }
        }
        BasicText(
            if (secondPlaced) "✅ orange item placed" else "❌ orange item dropped",
            style = TextStyle(
                fontSize = 12.sp,
                color = if (secondPlaced) Color(0xFF2E7D32) else Color(0xFFC62828),
            ),
        )
    }
}

/** Measures a copy loosely, then the real one with exactly the height the copy reported. */
@Composable
private fun RemeasuredWithReportedHeight(onSecondPlaced: () -> Unit) {
    SubcomposeLayout { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val reportedHeight = subcompose("probe") { TwoLineFlowRow() }
            .first()
            .measure(loose)
            .height
        val placeable = subcompose("real") { TwoLineFlowRow(onSecondPlaced = onSecondPlaced) }
            .first()
            .measure(loose.copy(minHeight = reportedHeight, maxHeight = reportedHeight))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}

/**
 * A FrameLayout with more than one match_parent child re-measures them with EXACTLY(its own
 * height), the same thing Snackbar's layout does to a ComposeView added to it.
 */
@Composable
private fun InsideFrameLayout(onSecondPlaced: () -> Unit) {
    val density = LocalDensity.current
    AndroidView(
        // FrameLayout re-measures the ComposeView with EXACTLY its width too; keep that at the
        // FlowRow's 200.dp, or the items fit on one line and nothing wraps.
        modifier = Modifier.width(200.dp),
        factory = { context ->
            FrameLayout(context).apply {
                addView(View(context), FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                addView(
                    ComposeView(context).apply {
                        setContent {
                            CompositionLocalProvider(LocalDensity provides density) {
                                TwoLineFlowRow(onSecondPlaced = onSecondPlaced)
                            }
                        }
                    },
                    FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT),
                )
            }
        },
    )
}

/** Intrinsic height uses roundToPx, the measure's fit check uses ceil. */
@Composable
private fun InsideIntrinsicMinHeight(onSecondPlaced: () -> Unit) {
    // Width on the Box too: intrinsic-height queries pass the parent's width through
    // Modifier.width, so without it FlowRow would be asked at full screen width.
    Box(Modifier.width(200.dp).height(IntrinsicSize.Min)) {
        TwoLineFlowRow(Modifier.fillMaxHeight(), onSecondPlaced)
    }
}
