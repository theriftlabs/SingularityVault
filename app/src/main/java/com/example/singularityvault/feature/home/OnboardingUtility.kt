package com.riftlabs.singularityvault.feature.home

import android.content.Context
import com.riftlabs.singularityvault.feature.auth.MasterPasswordRepository

private const val PREFS_NAME = "singularity_vault_prefs"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

fun isOnboardingDone(context: Context, masterPasswordRepository: MasterPasswordRepository): Boolean {
    // If user already set a master password, skip onboarding forever
    if (masterPasswordRepository.isMasterPasswordSet()) {
        return true
    }

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_ONBOARDING_DONE, false)
}

fun setOnboardingDone(context: Context) {
    context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_ONBOARDING_DONE, true)
        .apply()
}