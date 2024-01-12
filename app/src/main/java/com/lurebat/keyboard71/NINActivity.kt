package com.lurebat.keyboard71

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.alorma.compose.settings.storage.datastore.composeSettingsDataStore
import com.alorma.compose.settings.storage.datastore.rememberPreferenceDataStoreBooleanSettingState
import com.alorma.compose.settings.ui.SettingsCheckbox
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import com.lurebat.keyboard71.BuildConfig;

@Composable
fun ComposeSettingsTheme(
    darkThemePreference: Boolean,
    dynamicThemePreference: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (Build.VERSION.SDK_INT >= 31 && dynamicThemePreference) {
            if (darkThemePreference) {
                dynamicDarkColorScheme(LocalContext.current)
            } else {
                dynamicLightColorScheme(LocalContext.current)
            }
        } else {
            if (darkThemePreference) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }
        },
        content = content
    )
}


class NINActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val darkThemePreference = rememberPreferenceDataStoreBooleanSettingState(
                key = "darkThemePreference",
                defaultValue = true,
            )

            val dynamicThemePreference = rememberPreferenceDataStoreBooleanSettingState(
                key = "dynamicThemePreference",
                defaultValue = true,
            )

            val hapticFeedbackPreference = rememberPreferenceDataStoreBooleanSettingState(
                key = HAPTIC_FEEDBACK_PREFERENCE,
                defaultValue = true,
            )

            ComposeSettingsTheme(
                darkThemePreference = darkThemePreference.value,
                dynamicThemePreference = dynamicThemePreference.value
            ) {
                Scaffold(
                ) {
                    Column(
                        modifier = Modifier.padding(it)
                    ) {
                        Text(
                            "Keyboard 71 - Version " + BuildConfig.VERSION_NAME ,
                            style = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Box(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.error,
                                    RoundedCornerShape(8.dp)
                                ),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "NOTE",
                                    style = MaterialTheme.typography.headlineMedium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "This is a modified version of Nintype / keyboard 69 by jormy",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Please support the original developer @jormy in any way you can",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(it).fillMaxWidth().align(
                                Alignment.CenterHorizontally)
                        ) {
                            Button(
                                onClick = {
                                    this@NINActivity.startActivity(
                                        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS),
                                    )
                                },
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                Text("Click here to enable the keyboard")
                            }

                            // textbutton for changing the keyboard
                            Button(
                                onClick = { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() },
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                Text("Click here to switch to the keyboard")
                            }
                        }


                        Text(
                            "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                        SettingsCheckbox(title = { Text("Haptic Feedback") }, subtitle = {
                            Text("Clickity clackity")
                        }, state = hapticFeedbackPreference)
                        SettingsCheckbox(title = { Text("Dark Theme") }, subtitle = {
                            Text("Only for this page - no effect on the keyboard")
                        }, state = darkThemePreference)
                        SettingsCheckbox(
                            title = { Text("Material You") },
                            subtitle = {
                                Text("[Android 12+] Only for this page - no effect on the keyboard")
                            },
                            state = dynamicThemePreference
                        )

                    }
                }

            }
        }

    }

    companion object {
        const val HAPTIC_FEEDBACK_PREFERENCE = "hapticFeedbackPreference"
    }
}

class Preferences(val context: Context) {
    val store = context.composeSettingsDataStore

    private val hapticFeedback = store.data.map { s ->
        s[booleanPreferencesKey(NINActivity.HAPTIC_FEEDBACK_PREFERENCE)] ?: true
    }

    fun hapticFeedbackBlocking() = runBlocking { hapticFeedback.first() }

}
