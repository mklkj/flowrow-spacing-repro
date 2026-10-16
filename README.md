# FlowRow drops wrapped items on fractional densities

`FlowRow` converts `verticalArrangement` spacing to pixels in two different ways:

| Where (foundation-layout 1.12.1) | Conversion | 8.dp at density 2.8875 |
|---|---|---|
| `FlowLayout.kt:1220-1221` `breakDownItems` — fit check (`leftOverCrossAxis`, line 1390 → `FlowLayoutBuildingBlocks.kt:88`) | `ceil(spacing.toPx())` | 24 px |
| `FlowLayout.kt:1552` `placeHelper` — reported size | `spacing.roundToPx()` | 23 px |
| `FlowLayout.kt:784` `minIntrinsicHeight` | `spacing.roundToPx()` | 23 px |

So `FlowRow` reports a height 1 px smaller than its own fit check needs. Measured again with exactly
that height (a View parent re-measuring with `EXACTLY(measuredHeight)`, or `IntrinsicSize.Min`), the
last line "doesn't fit" and is silently dropped — the non-deprecated `FlowRow` always uses
`FlowRowOverflow.Clip` (`FlowLayout.kt:170`).

It only happens when the spacing's pixel value has a fractional part below 0.5, e.g. 8.dp at
2.8875 (420 dpi with Display size 110%) or 5.dp at 2.625 (plain 420 dpi).

## See it

```
./gradlew installDebug
```

`MainActivity` shows the same `TwoLineFlowRow` in four setups, each at density 2.8875 and 2.625
(forced via `LocalDensity`, so any device works). The orange item is the one that wraps; each cell
says whether it was placed:

1. Measured once — baseline, always placed.
2. Measured again (`SubcomposeLayout`) with exactly the height it reported — dropped at 2.8875.
3. Inside a `FrameLayout` with two match_parent children (the View re-measure Snackbar does) — dropped at 2.8875.
4. `Box(Modifier.height(IntrinsicSize.Min))` + `fillMaxHeight()` — dropped at 2.8875.

## Test

```
./gradlew connectedDebugAndroidTest
```

Needs a connected device or emulator (density is forced via `LocalDensity`, so any device works).
Expected: the two `fractionalDensity_*` tests fail with "The component with TestTag = 'second' is not
displayed", the two `integralSpacingDensity_*` controls pass.

Versions: AGP 9.4.1, Kotlin 2.4.21, Compose BOM alpha 2026.10.00 (foundation-layout 1.13.0-beta01,
needs compileSdk 37.1). Also reproduces on stable BOM 2026.09.00 (foundation-layout 1.12.1). The same
code is still on `androidx-main` as of 2026-10-16. Line numbers above are from 1.12.1; in
1.13.0-beta01 they're 1222-1223 and 1566.
