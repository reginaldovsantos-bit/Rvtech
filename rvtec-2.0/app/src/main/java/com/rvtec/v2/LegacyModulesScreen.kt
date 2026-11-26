package com.rvtec.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rvtec.v2.data.LegacyModuleRepository
import com.rvtec.v2.domain.*

@Composable
fun LegacyModulesScreen(repository: LegacyModuleRepository, revision: Int, onRefresh: () -> Unit) {
    val preventives = remember(revision) { repository.listPreventives() }
    val diagnostics = remember(revision) { repository.listDiagnostics() }
    val memories = remember(revision) { repository.listMemories() }
    val stock = remember(revision) { repository.listStock() }
    val staff = remember(revision) { repository.listStaff() }
    val notifications = remember(revision) { repository.listNotifications() }
    val settings = remember(revision) { repository.getSettings() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Módulos recuperados", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onRefresh) { Text("Atualizar") }
        }
        Text("Dados locais importados da versão legada. Esta tela consulta os registros sem alterar o conteúdo.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Resumo", style = MaterialTheme.typography.titleMedium)
                        Text("Preventivas: ${preventives.size} • Diagnósticos: ${diagnostics.size}")
                        Text("Memórias técnicas: ${memories.size} • Peças: ${stock.size}")
                        Text("Equipe: ${staff.size} • Notificações: ${notifications.size}")
                        Text("Empresa: ${settings.companyName.ifBlank { "Não configurada" }}")
                        Text("Turnos: ${settings.shift1Start}–${settings.shift1End} | ${settings.shift2Start}–${settings.shift2End} | ${settings.shift3Start}–${settings.shift3End}")
                    }
                }
            }
            item { Text("Preventivas", style = MaterialTheme.typography.titleMedium) }
            if (preventives.isEmpty()) item { EmptyModule("Nenhuma preventiva importada.") }
            items(preventives, key = { "p:${it.id}" }) { p ->
                ModuleCard(p.title, "${p.description}\nMáquina: ${p.machineId}\nIntervalo: ${p.intervalDays} dias • ${if (p.active) "Ativa" else "Inativa"}")
            }
            item { Text("Diagnósticos", style = MaterialTheme.typography.titleMedium) }
            if (diagnostics.isEmpty()) item { EmptyModule("Nenhum diagnóstico importado.") }
            items(diagnostics, key = { "d:${it.id}" }) { d ->
                ModuleCard(d.title, "Máquina: ${d.machineId}\nSintomas: ${d.symptoms}\nCausas: ${d.probableCauses}\nVerificações: ${d.checks}\nSolução: ${d.solution}")
            }
            item { Text("Memória técnica", style = MaterialTheme.typography.titleMedium) }
            if (memories.isEmpty()) item { EmptyModule("Nenhuma memória técnica importada.") }
            items(memories, key = { "m:${it.id}" }) { m ->
                ModuleCard(m.title, "${m.content}\nTags: ${m.tags.joinToString()}")
            }
            item { LegacyStockSection(repository, revision, onRefresh) }
            item { Text("Equipe", style = MaterialTheme.typography.titleMedium) }
            if (staff.isEmpty()) item { EmptyModule("Nenhum usuário importado.") }
            items(staff, key = { "u:${it.id}" }) { u ->
                ModuleCard(u.name, "${u.email}\nPerfil: ${u.role} • ${if (u.active) "Ativo" else "Inativo"}")
            }
            item { Text("Notificações", style = MaterialTheme.typography.titleMedium) }
            if (notifications.isEmpty()) item { EmptyModule("Nenhuma notificação importada.") }
            items(notifications, key = { "n:${it.id}" }) { n ->
                ModuleCard(n.title, "${n.message}\n${if (n.read) "Lida" else "Não lida"}")
            }
        }
    }
}

@Composable
private fun ModuleCard(title: String, details: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(details, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmptyModule(message: String) {
    Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
}
