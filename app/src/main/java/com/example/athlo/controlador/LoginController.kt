package com.example.athlo.controlador

// Importaciones necesarias para el contexto de la app, navegación, logs, toasts y Firebase
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider

// Este controlador gestiona toda la lógica relacionada con el login
object LoginController {

    /**
     * Inicia sesión con correo y contraseña.
     *
     * @param context Contexto actual de la app (para acceder a SharedPreferences y mostrar toasts)
     * @param correo Correo introducido por el usuario
     * @param contrasena Contraseña introducida por el usuario
     * @param recordarUsuario Booleano para guardar sesión si se marca la casilla
     * @param navController Controlador de navegación para redirigir tras login
     * @param onError Callback para devolver mensajes de error a los campos
     */
    fun loginConCorreo(
        context: Context,
        correo: String,
        contrasena: String,
        recordarUsuario: Boolean,
        navController: NavController,
        onError: (String?, String?) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val sharedPref = context.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)

        auth.signInWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        if (recordarUsuario) {
                            sharedPref.edit()
                                .putString("correo", correo)
                                .putBoolean("recordarSesion", true)
                                .apply()
                        } else {
                            sharedPref.edit()
                                .remove("correo")
                                .putBoolean("recordarSesion", false)
                                .apply()
                        }
                        Toast.makeText(context, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                        SesionController.comprobarRutaYRedirigir(context, navController)

                    } else {
                        auth.signOut()
                        Toast.makeText(context, "Debes verificar tu correo antes de iniciar sesión", Toast.LENGTH_LONG).show()
                    }
                } else {
                    when (val exception = task.exception) {
                        is FirebaseAuthInvalidUserException -> onError("Este usuario no existe", null)
                        is FirebaseAuthInvalidCredentialsException -> onError(null, "Contraseña incorrecta o la cuenta no existe")
                        else -> {
                            Toast.makeText(context, "Error: ${exception?.message}", Toast.LENGTH_LONG).show()
                            Log.e("Login", "Error al iniciar sesión", exception)
                        }
                    }
                }
            }
    }


    /**
     * Inicia sesión usando una cuenta de Google.
     *
     * @param context Contexto actual de la app
     * @param account Cuenta de Google obtenida tras el intento de login
     * @param navController Navegación tras login exitoso
     */
    fun loginConGoogle(
        context: Context,
        account: GoogleSignInAccount,
        navController: NavController
    ) {
        val auth = FirebaseAuth.getInstance()
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val sharedPref = context.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)

        // Se intenta autenticar con las credenciales de Google
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    sharedPref.edit().putBoolean("recordarSesion", true).apply()
                    Toast.makeText(context, "Inicio de sesión con Google exitoso", Toast.LENGTH_SHORT).show()
                    SesionController.comprobarRutaYRedirigir(context, navController)

                } else {
                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * Envía un correo de recuperación de contraseña.
     *
     * @param context Contexto para mostrar toasts
     * @param correo Correo al que se enviará el enlace de recuperación
     */
    fun enviarCorreoRecuperacion(context: Context, correo: String) {
        val auth = FirebaseAuth.getInstance()
        auth.sendPasswordResetEmail(correo)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Correo de recuperación enviado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No se pudo enviar el correo: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
