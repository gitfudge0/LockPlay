package com.musiclock.ui.onboarding

import com.musiclock.ui.permissions.AppPermission

/**
 * Pure, Compose-free flow logic for the onboarding wizard: step ordering, summary status, and the
 * core-missing/blocked rule. Kept separate from [OnboardingFlow] so it can be unit-tested on the JVM.
 */

/**
 * The permission steps in wizard order: required notification access first (Phase 1 · make it work),
 * then the lockscreen-display perms (Phase 2), then the optional quick-tap runtime perm (Phase 3).
 */
val OnboardingPerms: List<AppPermission> = listOf(
    AppPermission.NotificationListener,
    AppPermission.DisplayOverApps,
    AppPermission.FullScreenIntent,
    AppPermission.BatteryOptimization,
    AppPermission.PostNotifications,
)

/** The single core permission without which the app cannot see any music. */
val CorePerm: AppPermission = AppPermission.NotificationListener

// Fixed wizard steps surrounding the per-permission steps.
const val STEP_WELCOME = 0
const val STEP_WELCOME2 = 1
const val FIRST_PERM_STEP = 2
val STEP_SUMMARY = FIRST_PERM_STEP + OnboardingPerms.size
val STEP_END = STEP_SUMMARY + 1
val STEP_COUNT = STEP_END + 1

/** Wizard step index for a given permission. */
fun permStepIndex(permission: AppPermission): Int = FIRST_PERM_STEP + OnboardingPerms.indexOf(permission)

/** The permission shown at [stepIndex], or null if the step is not a permission step. */
fun permAtStep(stepIndex: Int): AppPermission? =
    OnboardingPerms.getOrNull(stepIndex - FIRST_PERM_STEP)

/** Number of granted permissions for the summary title. */
fun grantedCount(granted: Map<AppPermission, Boolean>): Int =
    OnboardingPerms.count { granted[it] == true }

/** True when the required core permission ([CorePerm]) is not granted → the End screen is blocked. */
fun isCoreMissing(granted: Map<AppPermission, Boolean>): Boolean = granted[CorePerm] != true

/**
 * Optional permissions the user skipped (or simply did not grant) — drives the "degraded" warning on
 * a ready End screen. Excludes the core perm (its absence blocks instead).
 */
fun degradedPerms(granted: Map<AppPermission, Boolean>): List<AppPermission> =
    OnboardingPerms.filter { it.optional && granted[it] != true }
