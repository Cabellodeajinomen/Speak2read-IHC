package com.example.speak2read

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    private val googleSignInLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            Toast.makeText(this, "Google Sign-In cancelado o fallido (${result.resultCode})", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Speak2ReadPrefs.applySettings(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Persistencia: Revisar si ya hay sesión activa
        if (auth.currentUser != null) {
            val user = auth.currentUser
            Toast.makeText(this, "Sesión activa: ${user?.email}", Toast.LENGTH_SHORT).show()
            Speak2ReadPrefs.setLoggedUser(this, user?.displayName ?: user?.email?.split("@")?.get(0) ?: "Usuario", "Sordo")
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id)) 
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val etEmail = findViewById<EditText>(R.id.etUsername) // Reutilizamos el ID anterior para el correo
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnGoogle = findViewById<Button>(R.id.btnGoogleLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Speak2ReadPrefs.setLoggedUser(this, email.split("@")[0], "Sordo")
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoogle.setOnClickListener {
            val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
            if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                googleApiAvailability.getErrorDialog(this, resultCode, 2404)?.show()
                return@setOnClickListener
            }

            try {
                if (!::mGoogleSignInClient.isInitialized) {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestIdToken(getString(R.string.default_web_client_id)) 
                        .build()
                    mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
                }
                googleSignInLauncher.launch(mGoogleSignInClient.signInIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al iniciar Google: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val name = account?.displayName ?: "Usuario Google"
            Speak2ReadPrefs.setLoggedUser(this, name, "Sordo")
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } catch (e: ApiException) {
            val errorMessage = when (e.statusCode) {
                7 -> "Parece que no tienes internet. Por favor, revisa tu conexión."
                10 -> "Hay un pequeño error técnico (Configuración). Intenta de nuevo más tarde."
                12500 -> "Hubo un problema interno con Google. Intenta reiniciar la app."
                12501 -> "Cancelaste el inicio de sesión."
                else -> "No pudimos conectar con Google en este momento."
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }
}
