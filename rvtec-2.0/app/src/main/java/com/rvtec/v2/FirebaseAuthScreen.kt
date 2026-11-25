package com.rvtec.v2

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun FirebaseAuthScreen(auth: FirebaseAuth, onAuthenticated: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("RVTEC 2.0", style = MaterialTheme.typography.headlineLarge)
            Text("Acesse sua conta para habilitar a sincronização segura.", modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
            OutlinedTextField(email, { email = it.trim() }, label = { Text("E-mail") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Button(
                enabled = !busy && email.contains("@") && password.length >= 6,
                onClick = {
                    scope.launch {
                        busy = true
                        message = null
                        try {
                            if (creating) auth.createUserWithEmailAndPassword(email, password).await()
                            else auth.signInWithEmailAndPassword(email, password).await()
                            onAuthenticated()
                        } catch (e: Exception) {
                            message = e.localizedMessage ?: "Não foi possível autenticar."
                        } finally { busy = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) { Text(if (busy) "Aguarde..." else if (creating) "Criar conta" else "Entrar") }
            TextButton(onClick = { creating = !creating; message = null }, enabled = !busy) {
                Text(if (creating) "Já tenho conta — entrar" else "Primeiro acesso — criar conta")
            }
            TextButton(onClick = {
                if (!email.contains("@")) {
                    message = "Informe seu e-mail para receber o link de redefinição."
                } else {
                    scope.launch {
                        busy = true
                        try {
                            auth.sendPasswordResetEmail(email).await()
                            message = "Link de redefinição enviado. Verifique seu e-mail."
                        } catch (e: Exception) {
                            message = e.localizedMessage ?: "Não foi possível enviar o link."
                        } finally { busy = false }
                    }
                }
            }, enabled = !busy) { Text("Esqueci minha senha") }
        }
    }
}
