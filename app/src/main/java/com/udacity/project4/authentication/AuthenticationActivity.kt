package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import kotlinx.android.synthetic.main.activity_authentication.*

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private val loginLauncher = loginResult()
    private val providers = listOf(
        AuthUI.IdpConfig.EmailBuilder().build(),
        AuthUI.IdpConfig.GoogleBuilder().build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser != null) {
            startActivity(Intent(this, RemindersActivity::class.java))
            finish()
        } else {
            setContentView(R.layout.activity_authentication)
        }

        login_button.setOnClickListener {
            launchSignInFlow()
        }

    }

    private fun loginResult(): ActivityResultLauncher<Intent> {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val idpResponse = IdpResponse.fromResultIntent(result.data)

            if (result.resultCode == Activity.RESULT_OK) {
                startActivity(Intent(this, RemindersActivity::class.java))
                finish()
            } else {
                if (idpResponse == null) {
                    snackBar("Login cancelled")
                } else {
                    val message = idpResponse.error?.message
                    if (message != null) {
                        (if (message.contains("Code: 7")) {
                            "internet connection is required"
                        } else {
                            message
                        }).let {
                            snackBar(
                                it
                            )
                        }
                    }
                }
            }
        }
    }

    private fun launchSignInFlow() {
        loginLauncher.launch(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.drawable.map)
                .build(),
        )
    }

    private fun snackBar(message: String, retryAction: ((View) -> Unit)? = null) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry, retryAction)
            .show()
    }
}
