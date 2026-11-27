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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val DAY_MS = 86_400_000L

@Composable
fun PreventivesScreen(
    repository: LegacyModuleRepository,
    machines: List<Machine>,
    revision: Int,
    onRefresh: () -> Unit
) {
    val plans = remember(revision) { repository.listPreventives().sortedBy { it.nextDueAt ?: Long.MAX_VALUE } }
    var showCreate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Manutenção preventiva", style = MaterialTheme.typography.titleLarge)
                Text("Planos, periodicidade e vencimentos", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { showCreate = true }, enabled = machines.isNotEmpty()) { Text("Nova") }
        }
        if (machines.isEmpty()) {
            Text("Cadastre uma máquina antes de criar um plano preventivo.", modifier = Modifier.padding(12.dp))
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 6.dp)) }
        if (plans.isEmpty()) {
            Text("Nenhum plano preventivo cadastrado.", modifier = Modifier.padding(vertical = 16.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(plans, key = { it.id }) { plan ->
                    val machineName = machines.firstOrNull { it.id == plan.machineId }?.name ?: plan.machineId
                    val dueAt = plan.nextDueAt ?: plan.lastPerformedAt?.plus(plan.intervalDays * DAY_MS)
                    val overdue = plan.active && dueAt != null && dueAt < System.currentTimeMillis()
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(plan.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Text(
                                    when {
                                        !plan.active -> "INATIVA"
                                        overdue -> "VENCIDA"
                                        dueAt == null -> "SEM DATA"
                                        else -> "EM DIA"
                                    },
                                    color = when {
                                        !plan.active -> MaterialTheme.colorScheme.onSurfaceVariant
                                        overdue -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Text("Máquina: $machineName")
                            if (plan.description.isNotBlank()) Text(plan.description)
                            Text("Periodicidade: a cada ${plan.intervalDays} dias")
                            Text("Última execução: ${plan.lastPerformedAt?.let(::formatDate) ?: "Não registrada"}")
                            Text("Próximo vencimento: ${dueAt?.let(::formatDate) ?: "Não definido"}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    enabled = plan.active,
                                    onClick = {
                                        val now = System.currentTimeMillis()
                                        runCatching {
                                            repository.savePreventive(plan.copy(lastPerformedAt = now, nextDueAt = now + plan.intervalDays * DAY_MS))
                                            error = null
                                            onRefresh()
                                        }.onFailure { error = it.message ?: "Falha ao registrar preventiva." }
                                    }
                                ) { Text("Registrar execução") }
                                TextButton(onClick = {
                                    runCatching {
                                        repository.savePreventive(plan.copy(active = !plan.active))
                                        error = null
                                        onRefresh()
                                    }.onFailure { error = it.message ?: "Falha ao atualizar plano." }
                                }) { Text(if (plan.active) "Desativar" else "Ativar") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        PreventiveDialog(
            machines = machines,
            onDismiss = { showCreate = false },
            onSave = { machineId, title, description, interval ->
                runCatching {
                    repository.savePreventive(
                        PreventivePlan(
                            id = UUID.randomUUID().toString(),
                            machineId = machineId,
                            title = title.trim(),
                            description = description.trim(),
                            intervalDays = interval,
                            nextDueAt = System.currentTimeMillis() + interval * DAY_MS
                        )
                    )
                    error = null
                    onRefresh()
                    showCreate = false
                }.onFailure { error = it.message ?: "Não foi possível salvar o plano." }
            }
        )
    }
}

@Composable
private fun PreventiveDialog(
    machines: List<Machine>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int) -> Unit
) {
    var machineId by remember { mutableStateOf(machines.firstOrNull()?.id.orEmpty()) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var interval by remember { mutableStateOf("30") }
    var machineMenu by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo plano preventivo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { machineMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(machines.firstOrNull { it.id == machineId }?.name ?: "Selecionar máquina")
                    }
                    DropdownMenu(expanded = machineMenu, onDismissRequest = { machineMenu = false }) {
                        machines.forEach { machine ->
                            DropdownMenuItem(
                                text = { Text(machine.name) },
                                onClick = { machineId = machine.id; machineMenu = false }
                            )
                        }
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Nome da preventiva") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição / tarefas") }, minLines = 2)
                OutlinedTextField(value = interval, onValueChange = { interval = it.filter(Char::isDigit).take(4) }, label = { Text("Intervalo em dias") }, singleLine = true)
                validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val days = interval.toIntOrNull()
                when {
                    machineId.isBlank() -> validation = "Selecione uma máquina."
                    title.isBlank() -> validation = "Informe o nome da preventiva."
                    days == null || days <= 0 -> validation = "Informe um intervalo maior que zero."
                    else -> onSave(machineId, title, description, days)
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ReportsScreen(machines: List<Machine>, stops: List<StopRecord>, orders: List<WorkOrder>) {
    val end = System.currentTimeMillis()
    val start = end - 30L * DAY_MS
    val total = remember(stops, end) { MaintenanceIndicatorsCalculator.calculate(stops, start, end) }

    Column(Modifier.fillMaxSize()) {
        Text("Relatórios de manutenção", style = MaterialTheme.typography.titleLarge)
        Text("Período analisado: últimos 30 dias • baseado nos registros de paradas e OS.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportMetric("Disponibilidade", "${formatNumber(total.availabilityPercent)}%", Modifier.weight(1f))
            ReportMetric("Paradas", total.failureCount.toString(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportMetric("Horas paradas", formatNumber(total.downtimeHours), Modifier.weight(1f), "h")
            ReportMetric("MTTR", total.mttrHours?.let(::formatNumber) ?: "—", Modifier.weight(1f), "h")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportMetric("MTBF", total.mtbfHours?.let(::formatNumber) ?: "—", Modifier.weight(1f), "h")
            ReportMetric("OS concluídas", orders.count { order -> order.status == WorkOrderStatus.COMPLETED && order.completedAt?.let { it in start..end } == true }.toString(), Modifier.weight(1f))
        }
        Text("Indicadores por máquina", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
        if (machines.isEmpty()) {
            Text("Cadastre máquinas e registre paradas para gerar relatórios.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(machines, key = { it.id }) { machine ->
                    val indicator = remember(stops, machine.id, end) {
                        MaintenanceIndicatorsCalculator.calculate(stops.filter { it.machineId == machine.id }, start, end)
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(machine.name, style = MaterialTheme.typography.titleMedium)
                            Text("Disponibilidade: ${formatNumber(indicator.availabilityPercent)}%")
                            Text("Paradas: ${indicator.failureCount} • Horas paradas: ${formatNumber(indicator.downtimeHours)} h")
                            Text("MTTR: ${indicator.mttrHours?.let(::formatNumber) ?: "—"} h • MTBF: ${indicator.mtbfHours?.let(::formatNumber) ?: "—"} h")
                        }
                    }
                }
            }
        }
        Text(
            "Nota: cálculo preliminar por janela de 30 dias. Não desconta paradas programadas nem calendário de turnos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

@Composable
private fun ReportMetric(label: String, value: String, modifier: Modifier = Modifier, suffix: String = "") {
    Card(modifier.padding(vertical = 4.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(if (suffix.isBlank()) value else "$value $suffix", style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(timestamp))

private fun formatNumber(value: Double): String = String.format(Locale("pt", "BR"), "%.1f", value)
