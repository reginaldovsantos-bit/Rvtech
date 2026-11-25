package com.rvtec.v2

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rvtec.v2.data.FirebaseServices
import com.rvtec.v2.data.MaintenanceRepository
import kotlinx.coroutines.launch

@Composable
fun FirebaseSyncScreen(firebase: FirebaseServices, repository: MaintenanceRepository) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Os dados são salvos primeiro neste aparelho. A nuvem é uma cópia vinculada à sua conta.") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sincronização segura", style = MaterialTheme.typography.headlineSmall)
        Text("Conta autenticada: sincronização individual por usuário. O envio substitui a cópia na nuvem pela fotografia atual deste aparelho.")
        Button(
            enabled = !busy && !repository.hasStorageError(),
            onClick = {
                scope.launch {
                    busy = true
                    message = "Enviando dados para a nuvem..."
                    try {
                        firebase.uploadSnapshot(repository.exportCloudSnapshot())
                        message = "Cópia enviada para a nuvem com sucesso."
                    } catch (e: Exception) {
                        message = e.localizedMessage ?: "Falha ao enviar a cópia."
                    } finally { busy = false }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Enviar dados deste aparelho") }
        OutlinedButton(
            enabled = !busy && !repository.hasStorageError(),
            onClick = {
                scope.launch {
                    busy = true
                    message = "Consultando cópia na nuvem..."
                    try {
                        val snapshot = firebase.downloadSnapshot()
                        if (snapshot == null) {
                            message = "Ainda não existe cópia na nuvem para esta conta."
                        } else {
                            repository.restoreCloudSnapshot(snapshot)
                            message = "Cópia restaurada. Feche e abra o RVTEC para atualizar todas as telas."
                        }
                    } catch (e: Exception) {
                        message = e.localizedMessage ?: "Falha ao restaurar a cópia."
                    } finally { busy = false }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Restaurar cópia da nuvem neste aparelho vazio") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Text(
            "Proteção: a restauração é bloqueada se este aparelho já tiver máquinas, paradas, OS ou apontamentos. Não há mesclagem automática entre aparelhos nesta etapa.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
