package com.lockplay.ui.onboarding

import com.lockplay.ui.permissions.AppPermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingLogicTest {

    @Test
    fun `permission order maps notification listener first and runtime perm last`() {
        assertEquals(AppPermission.NotificationListener, OnboardingPerms.first())
        assertEquals(AppPermission.PostNotifications, OnboardingPerms.last())
        assertEquals(AppPermission.NotificationListener, CorePerm)
    }

    @Test
    fun `step indices are contiguous and bracket the perm steps`() {
        assertEquals(FIRST_PERM_STEP, permStepIndex(OnboardingPerms.first()))
        assertEquals(STEP_SUMMARY - 1, permStepIndex(OnboardingPerms.last()))
        // permAtStep round-trips, and the fixed steps are not perm steps.
        OnboardingPerms.forEach { assertEquals(it, permAtStep(permStepIndex(it))) }
        assertEquals(null, permAtStep(STEP_WELCOME))
        assertEquals(null, permAtStep(STEP_SUMMARY))
        assertEquals(null, permAtStep(STEP_END))
        assertEquals(STEP_END + 1, STEP_COUNT)
    }

    @Test
    fun `core missing blocks the end screen until notification listener is granted`() {
        val none = emptyMap<AppPermission, Boolean>()
        assertTrue(isCoreMissing(none))

        val coreOnly = mapOf(AppPermission.NotificationListener to true)
        assertFalse(isCoreMissing(coreOnly))
        assertEquals(1, grantedCount(coreOnly))
    }

    @Test
    fun `degraded list reports skipped battery perm but never the core perm`() {
        // Core granted, optional battery perm missing → degraded mentions it, never the core perm.
        val granted = OnboardingPerms.associateWith { it != AppPermission.BatteryOptimization }
        val degraded = degradedPerms(granted)
        assertEquals(listOf(AppPermission.BatteryOptimization), degraded)
        assertFalse(degraded.contains(CorePerm))
        assertEquals(OnboardingPerms.size - 1, grantedCount(granted))
    }

    @Test
    fun `degraded list warns about every skipped non-core perm, not just battery`() {
        // Only the core perm is required; skipping any other (e.g. overlay) must raise a warning, so
        // the "you're all set" End screen never silently hides a missing recommended perm.
        val granted = mapOf(CorePerm to true) // everything else skipped
        val degraded = degradedPerms(granted)
        assertTrue(degraded.contains(AppPermission.DisplayOverApps))
        assertTrue(degraded.contains(AppPermission.FullScreenIntent))
        assertTrue(degraded.contains(AppPermission.PostNotifications))
        assertTrue(degraded.contains(AppPermission.BatteryOptimization))
        assertFalse(degraded.contains(CorePerm))
        // Core-only is enough to leave the blocked state, but it is still a degraded setup.
        assertFalse(isCoreMissing(granted))
    }
}
