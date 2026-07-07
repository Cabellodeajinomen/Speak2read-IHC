package com.example.speak2read.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.speak2read.R
import com.example.speak2read.data.local.Speak2ReadPrefs
import com.example.speak2read.ui.main.HomeActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val TAG = "LoginActivity"

    private val googleSignInLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "googleSignInLauncher: result code = ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            Log.e(TAG, "Google Sign-In cancelado. ResultCode: ${result.resultCode}")
            Toast.makeText(this, "Inicio con Google cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Speak2ReadPrefs.applySettings(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            Log.d(TAG, "Sesion detectada: ${auth.currentUser?.email}")
            goToHome()
        }

        val clientId = "598440352422-qbe21f4vo54li0dapjlhjqgoh78nu4q6.apps.googleusercontent.com"
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(clientId) 
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val etEmail = findViewById<EditText>(R.id.etUsername)
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
                            goToHome()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Ingresa correo y clave", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoogle.setOnClickListener {
            Log.d(TAG, "Iniciando flujo Google Login")
            googleSignInLauncher.launch(mGoogleSignInClient.signInIntent)
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)!!
            val idToken = account.idToken
            if (idToken != null) {
                Log.d(TAG, "Token de Google obtenido con exito")
                firebaseAuthWithGoogle(idToken)
            } else {
                Log.e(TAG, "El Token de Google es NULO")
                Toast.makeText(this, "Error: No se pudo obtener el token de Google", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            Log.e(TAG, "handleSignInResult fallido code=" + e.statusCode)
            val msg = when(e.statusCode) {
                10 -> "Error 10: Verifica el SHA-1 en Firebase Console y el Client ID."
                7 -> "Error de red. Revisa tu internet."
                else -> "Error Google: ${e.statusCode}"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "Login exitoso en Firebase: ${user?.email}")
                    Speak2ReadPrefs.setLoggedUser(this, user?.displayName ?: "Usuario Google", "Sordo")
                    goToHome()
                } else {
                    Log.e(TAG, "Error en Firebase con Google", task.exception)
                    Toast.makeText(this, "Error Firebase: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
