package com.shubhamthorat.flint.presentation

import com.shubhamthorat.flint.presentation.theme.DarkColorScheme
import com.shubhamthorat.flint.presentation.theme.FlintColorTokens
import com.shubhamthorat.flint.presentation.theme.FlintElevation
import com.shubhamthorat.flint.presentation.theme.FlintRadius
import com.shubhamthorat.flint.presentation.theme.FlintSpacing
import com.shubhamthorat.flint.presentation.theme.FlintTypography
import com.shubhamthorat.flint.presentation.theme.LightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeTest {

    @Test
    fun testLightColorSchemeTokens() {
        val scheme = LightColorScheme
        assertFalse(scheme.isDark)
        assertEquals(FlintColorTokens.PrimaryAmber, scheme.primary)
        assertEquals(FlintColorTokens.SecondaryViolet, scheme.secondary)
        assertEquals(FlintColorTokens.AccentCoral, scheme.accent)
        assertEquals(FlintColorTokens.BackgroundLight, scheme.background)
        assertEquals(FlintColorTokens.SurfaceLight, scheme.surface)
    }

    @Test
    fun testDarkColorSchemeTokens() {
        val scheme = DarkColorScheme
        assertTrue(scheme.isDark)
        assertEquals(FlintColorTokens.PrimaryAmberVariant, scheme.primary)
        assertEquals(FlintColorTokens.SecondaryVioletLight, scheme.secondary)
        assertEquals(FlintColorTokens.AccentCoral, scheme.accent)
        assertEquals(FlintColorTokens.BackgroundDark, scheme.background)
        assertEquals(FlintColorTokens.SurfaceDark, scheme.surface)
    }

    @Test
    fun testTypographyTokens() {
        val typography = FlintTypography()
        assertEquals(32, typography.displayLarge.fontSize.value.toInt())
        assertEquals(24, typography.headlineLarge.fontSize.value.toInt())
        assertEquals(16, typography.bodyLarge.fontSize.value.toInt())
        assertEquals(12, typography.labelSmall.fontSize.value.toInt())
    }

    @Test
    fun testSpacingRadiusAndElevationDefaults() {
        val spacing = FlintSpacing()
        val radius = FlintRadius()
        val elevation = FlintElevation()

        assertEquals(8, spacing.small.value.toInt())
        assertEquals(16, spacing.medium.value.toInt())

        assertEquals(12, radius.medium.value.toInt())
        assertEquals(16, radius.large.value.toInt())

        assertEquals(2, elevation.low.value.toInt())
        assertEquals(4, elevation.medium.value.toInt())
    }
}