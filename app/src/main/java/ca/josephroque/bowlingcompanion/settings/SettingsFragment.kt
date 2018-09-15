package ca.josephroque.bowlingcompanion.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.FragmentTransaction
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import ca.josephroque.bowlingcompanion.BuildConfig
import ca.josephroque.bowlingcompanion.R
import ca.josephroque.bowlingcompanion.common.ScrollableTextDialog
import ca.josephroque.bowlingcompanion.utils.Analytics
import ca.josephroque.bowlingcompanion.utils.Email
import ca.josephroque.bowlingcompanion.utils.Facebook
import ca.josephroque.bowlingcompanion.utils.Files
import ca.josephroque.bowlingcompanion.utils.toSpanned

/**
 * Copyright (C) 2018 Joseph Roque
 *
 * Display user preferences and allow manipulation.
 */
class SettingsFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        /** Logging identifier. */
        @Suppress("unused")
        private const val TAG = "SettingsFragment"

        /**
         * Create a new instance of the fragment.
         *
         * @return the new instance
         */
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }

    /** Handle preference changes. */
    private val onPreferenceChangedListener = SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
        when (key) {
            Settings.EnableAutoAdvance.prefName -> {
                val enabled = preferences.getBoolean(Settings.EnableAutoAdvance.prefName, Settings.EnableAutoAdvance.booleanDefault)
                if (enabled) {
                    Analytics.trackEnableAutoAdvance()
                } else {
                    Analytics.trackDisableAutoAdvance()
                }
            }
            Settings.EnableAutoLock.prefName -> {
                val enabled = preferences.getBoolean(Settings.EnableAutoLock.prefName, Settings.EnableAutoLock.booleanDefault)
                if (enabled) {
                    Analytics.trackEnableAutoLock()
                } else {
                    Analytics.trackDisableAutoLock()
                }
            }
            else -> {} // Do nothing
        }
    }

    /** Handle special cases for preferences interactions. */
    private val onPreferenceClickListener = Preference.OnPreferenceClickListener {
        when (it.key) {
            Settings.ReportBug.prefName -> {
                sendBugReportEmail()
                true
            }
            Settings.SendFeedback.prefName -> {
                sendFeedbackEmail()
                true
            }
            Settings.Rate.prefName -> {
                displayPlayStoreListing()
                true
            }
            Settings.Attributions.prefName -> {
                displayAttributions()
                true
            }
            Settings.Facebook.prefName -> {
                displayFacebookPage()
                true
            }
            Settings.DeveloperWebsite.prefName -> {
                Analytics.trackViewWebsite()
                true
            }
            Settings.ViewSource.prefName -> {
                Analytics.trackViewSource()
                true
            }
            Settings.PrivacyPolicy.prefName -> {
                Analytics.trackViewPrivacyPolicy()
                true
            }
            else -> false // Does nothing
        }
    }

    /** @Override */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(onPreferenceChangedListener)
    }

    /** @Override */
    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(onPreferenceChangedListener)
    }

    /** @Override */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_app)

        findPreference(Settings.ReportBug.prefName).onPreferenceClickListener = onPreferenceClickListener
        findPreference(Settings.SendFeedback.prefName).onPreferenceClickListener = onPreferenceClickListener
        findPreference(Settings.Rate.prefName).onPreferenceClickListener = onPreferenceClickListener
        findPreference(Settings.Attributions.prefName).onPreferenceClickListener = onPreferenceClickListener
        findPreference(Settings.Facebook.prefName).onPreferenceClickListener = onPreferenceClickListener
        findPreference(Settings.DeveloperWebsite.prefName).onPreferenceClickListener = onPreferenceClickListener
        findPreference(Settings.ViewSource.prefName).onPreferenceClickListener = onPreferenceClickListener
    }

    override fun onStart() {
        super.onStart()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        updatePreferenceSummaries()
    }

    /** @Override */
    override fun onStop() {
        super.onStop()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    /** @Override */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        updatePreferenceSummaries()
    }

    /**
     * Prompts the user to send a bug report email.
     */
    private fun sendBugReportEmail() {
        activity?.let {
            Email.sendEmail(
                    it,
                    resources.getString(R.string.bug_email_recipient),
                    resources.getString(R.string.bug_email_subject),
                    resources.getString(R.string.bug_email_body)
            )
        }

        Analytics.trackReportBug()
    }

    /**
     * Prompts the user to send a feedback email.
     */
    private fun sendFeedbackEmail() {
        activity?.let {
            Email.sendEmail(
                    it,
                    resources.getString(R.string.feedback_email_recipient),
                    resources.getString(R.string.feedback_email_subject),
                    null
            )
        }

        Analytics.trackSendFeedback()
    }

    /**
     * Displays the app's Play Store listing, in either the Google Play app, or the web browser.
     */
    private fun displayPlayStoreListing() {
        activity?.let {
            val appPackageName = it.packageName
            val marketIntent = try {
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
            } catch (ex: android.content.ActivityNotFoundException) {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName"))
            }

            startActivity(marketIntent)
        }

        Analytics.trackRate()
    }

    /**
     * Displays the open source library attributions for the app.
     */
    private fun displayAttributions() {
        activity?.let { activity ->
            val licenseText = Files.retrieveTextFileAsset(activity, "licenses.txt")

            licenseText?.let {
                val fragment = ScrollableTextDialog.newInstance(
                        R.string.open_source_libraries,
                        it.replace("\n", "<br />").toSpanned()
                )

                activity.supportFragmentManager.beginTransaction().apply {
                    setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    add(android.R.id.content, fragment)
                    addToBackStack(null)
                    commit()
                }
            }
        }

        Analytics.trackViewAttributions()
    }

    /**
     * Opens the Bowling Companion Facebook page.
     */
    private fun displayFacebookPage() {
        activity?.let {
            it.startActivity(Facebook.getFacebookPageIntent(it))
        }

        Analytics.trackViewFacebook()
    }

    /**
     * Update summaries of user preferences.
     */
    private fun updatePreferenceSummaries() {
        val prefs = preferenceScreen.sharedPreferences
        findPreference(Settings.VersionName.prefName).summary = BuildConfig.VERSION_NAME

        val autoAdvanceTime = prefs.getString(Settings.AutoAdvanceTime.prefName, Settings.AutoAdvanceTime.stringDefault)
        findPreference(Settings.AutoAdvanceTime.prefName).summary = resources.getString(R.string.pref_auto_advance_time_summary_seconds, autoAdvanceTime)
    }
}
