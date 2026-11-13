package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.ArmorCard
import com.example.ui.theme.BrightFlame
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Box(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
    Image(
      painter = painterResource(id = R.drawable.img_hero_battlefield),
      contentDescription = "Free Fire Bihar Battlefield",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              ObsidianVoid.copy(alpha = 0.55f),
              ObsidianVoid.copy(alpha = 0.72f),
              ObsidianVoid.copy(alpha = 0.96f),
            )
          )
        )
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 520.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Image(
          painter = painterResource(id = R.drawable.ffb_app_icon),
          contentDescription = "Free Fire Bihar Emblem",
          modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(22.dp)),
        )
        Spacer(Modifier.height(14.dp))
        Text(
          text = "FREE FIRE BIHAR",
          style = MaterialTheme.typography.displayLarge,
          color = TextPrimary,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "SEASON 1 • PATNA DELTA 3D BATTLE ROYALE",
          style = MaterialTheme.typography.labelLarge,
          color = BrightFlame,
          textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FeatureBadge(Icons.Default.SportsEsports, "3D Combat", Modifier.weight(1f))
          FeatureBadge(Icons.Default.Groups, "50-Player Rooms", Modifier.weight(1f))
          FeatureBadge(Icons.Default.CloudDone, "Cloud Save", Modifier.weight(1f))
        }

        Spacer(Modifier.height(22.dp))

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = ArmorCard.copy(alpha = 0.92f),
          border = BorderStroke(1.dp, SteelBorder),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Security, contentDescription = null, tint = TacticalCyan, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text(
                text = "OPERATIVE DEPLOYMENT LOGIN",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )
            }
            Spacer(Modifier.height(8.dp))
            Text(
              text = "Sign in securely with your Google Account to sync your Player ID, rank, loadout, squad friends, and battleground progress.",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
              textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))

            Button(
              onClick = {
                errorMsg = null
                isLoading = true
                onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                  },
                  onAuthError = { msg ->
                    isLoading = false
                    errorMsg = msg
                  },
                  scope = scope,
                  onAuthCancelled = {
                    isLoading = false
                    errorMsg = context.getString(R.string.error_auth_cancelled)
                  },
                )
              },
              enabled = !isLoading,
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("google_sign_in_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = EmberOrange,
                contentColor = ObsidianVoid,
              ),
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = ObsidianVoid,
                  strokeWidth = 2.5.dp,
                )
              } else {
                Text(
                  text = "SIGN IN WITH GOOGLE",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
              }
            }

            if (errorMsg != null) {
              Spacer(Modifier.height(12.dp))
              Text(
                text = errorMsg ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = DangerCrimson,
                textAlign = TextAlign.Center,
              )
            }

            Spacer(Modifier.height(12.dp))
            Text(
              text = "Facebook OAuth login requires external Meta App configuration in Firebase Console.",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary.copy(alpha = 0.78f),
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureBadge(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = ObsidianVoid.copy(alpha = 0.82f),
    border = BorderStroke(1.dp, SteelBorder),
  ) {
    Column(
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(icon, contentDescription = label, tint = EmberOrange, modifier = Modifier.size(20.dp))
      Spacer(Modifier.height(4.dp))
      Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, textAlign = TextAlign.Center)
    }
  }
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    onUnauthenticated()
    return
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(token, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder()
    .addCredentialOption(signInOption)
    .build()

  scope.launch {
    try {
      val activity = context as? Activity
      if (activity == null) {
        onAuthError("Activity context required for Google Sign-In")
        return@launch
      }
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(token, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type received")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed")
    }
  }
}
