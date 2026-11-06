package com.brajesh.gaas.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.brajesh.gaas.BuildConfig
import com.brajesh.gaas.auth.GoogleSignInManager
import com.brajesh.gaas.auth.GoogleSignInResult
import com.brajesh.gaas.data.GoogleAccount
import kotlinx.coroutines.launch

/**
 * Optional Google sign-in. Kept in one place so onboarding and Settings behave
 * identically, including the "not configured in this build" state for anyone
 * building the repo without their own OAuth Web client id.
 */
@Composable
fun GoogleSignInButton(
    account: GoogleAccount?,
    onSignedIn: (GoogleAccount) -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val configured = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    Column(modifier.fillMaxWidth()) {
        if (account != null) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(
                            account.name ?: "Google account",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            account.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    TextButton(onClick = onSignedOut) { Text("Sign out") }
                }
            }
        } else {
            Button(
                onClick = {
                    val activity = context as? ComponentActivity
                    if (activity == null) {
                        error = "Google sign-in needs an Activity context."
                        return@Button
                    }
                    error = null
                    busy = true
                    scope.launch {
                        when (val result = GoogleSignInManager(activity).signIn()) {
                            is GoogleSignInResult.Success -> onSignedIn(result.account)
                            is GoogleSignInResult.Failed -> error = result.message
                            is GoogleSignInResult.NotConfigured ->
                                error = "Add a Google Web Client ID to enable sign-in."
                            GoogleSignInResult.Cancelled -> Unit
                        }
                        busy = false
                    }
                },
                enabled = !busy && configured,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (busy) "Opening Google…" else "Continue with Google")
            }
        }

        error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (!configured) {
            Text(
                "Sign-in is optional. It's enabled once a Web Client ID is configured in the build.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
