package com.rvtec.v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rvtec.v2.data.MaintenanceRepository
import com.rvtec.v2.data.LegacyModuleRepository
import com.rvtec.v2.data.FirebaseServices
import com.rvtec.v2.domain.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val pages = listOf("Início", "Máquinas", "Paradas", "Ordens de serviço", "Apontamentos", "Módulos", "Memória Técnica IA", "Configurações")

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firebaseServices = FirebaseServices(applicationContext)
        firebaseServices.installAppCheck()
        val repository = MaintenanceRepository(applicationContext)
        val legacyRepository = LegacyModuleRepository(applicationContext)
        setContent {
            MaterialTheme {
                var page by remember { mutableStateOf("Início") }
                var revision by remember { mutableIntStateOf(0) }
                var error by remember { mutableStateOf<String?>(null) }
                var showMachine by remember { mutableStateOf(false) }
                var showStopFor by remember { mutableStateOf<String?>(null) }
                var showOrder by remember { mutableStateOf(false) }
                var showEntryFor by remember { mutableStateOf<String?>(null) }
                var showCompleteFor by remember { mutableStateOf<String?>(null) }
                fun refresh() { revision++ }
                fun perform(action: () -> Unit) {
                    try { action(); error = null; refresh() } catch (e: Exception) { error = e.message ?: "Falha na operação." }
                }
                Scaffold(topBar = { TopAppBar(title = { Text("RVTEC 2.0") }) }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
                        ScrollableTabRow(selectedTabIndex = pages.indexOf(page).coerceAtLeast(0)) {
                            pages.forEach { item ->
                                Tab(selected = page == item, onClick = { page = item }, text = { Text(item) })
                            }
                        }
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) }
                        when (page) {
                            "Início" -> {
                                val machines = remember(revision) { repository.listMachines() }
                                val stops = remember(revision) { repository.listStops() }
                                val orders = remember(revision) { repository.listOrders() }
                                val periodEnd = System.currentTimeMillis()
                                val periodStart = periodEnd - 30L * 24L * 60L * 60L * 1000L
                                val indicators = remember(revision) {
                                    MaintenanceIndicatorsCalculator.calculate(stops, periodStart, periodEnd)
                                }
                                Text("Painel operacional", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Summary("Máquinas", machines.size.toString(), Modifier.weight(1f))
                                    Summary("Paradas abertas", stops.count { it.status != StopStatus.CLOSED }.toString(), Modifier.weight(1f))
                                    Summary("OS abertas", orders.count { it.status != WorkOrderStatus.COMPLETED && it.status != WorkOrderStatus.CANCELLED }.toString(), Modifier.weight(1f))
                                }
                                Text("Indicadores • últimos 30 dias corridos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Summary("Disponibilidade*", String.format("%.1f%%", indicators.availabilityPercent), Modifier.weight(1f))
                                    Summary("MTTR*", indicators.mttrHours?.let { String.format("%.1f h", it) } ?: "—", Modifier.weight(1f))
                                    Summary("MTBF*", indicators.mtbfHours?.let { String.format("%.1f h", it) } ?: "—", Modifier.weight(1f))
                                }
                                Text("* Cálculo em horas corridas, não desconta turnos ou paradas programadas. MTTR considera paradas encerradas; MTBF é tempo operacional dividido pelas falhas iniciadas no período.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                                Text("Cadastre as máquinas para iniciar os apontamentos de manutenção.", modifier = Modifier.padding(top = 16.dp))
                            }
                            "Máquinas" -> {
                                val machines = remember(revision) { repository.listMachines() }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Cadastro de máquinas", style = MaterialTheme.typography.titleLarge)
                                    Button(onClick = { showMachine = true }) { Text("Nova máquina") }
                                }
                                LazyColumn { items(machines) { m ->
                                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(m.name, style = MaterialTheme.typography.titleMedium)
                                            Text("Código: ${m.code.ifBlank { "—" }} | Setor: ${m.sector.ifBlank { "—" }}")
                                            Text("Produção: ${m.productionKgPerHour} kg/h")
                                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {\n                                                Text("Estado: ")\n                                                MachineStatusIndicator(m.status)\n                                            }
                                            if (m.status == MachineStatus.MAINTENANCE) {
                                                Button(onClick = { perform { repository.releaseMachine(m.id) } }) {
                                                    Text("Liberar máquina")
                                                }
                                            }
                                        }
                                    }
                                } }
                            }
                            "Paradas" -> {
                                val machines = remember(revision) { repository.listMachines() }
                                val stops = remember(revision) { repository.listStops() }
                                Text("Registrar parada", style = MaterialTheme.typography.titleLarge)
                                LazyColumn {
                                    items(machines) { m ->
                                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(m.name, style = MaterialTheme.typography.titleMedium)
                                                    Text(statusText(m.status))
                                                }
                                                Button(enabled = m.status == MachineStatus.RUNNING, onClick = { showStopFor = m.id }) { Text("Parar") }
                                            }
                                        }
                                    }
                                    item { Text("Histórico de paradas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
                                    items(stops.reversed()) { s ->
                                        val machineName = machines.firstOrNull { it.id == s.machineId }?.name ?: s.machineId
                                        ListItem(headlineContent = { Text(machineName) }, supportingContent = {
                                            Text("${s.cause} • ${if (s.status == StopStatus.CLOSED) "Encerrada" else "Aberta"}")
                                        })
                                    }
                                }
                            }
                            "Ordens de serviço" -> {
                                val machines = remember(revision) { repository.listMachines() }
                                val orders = remember(revision) { repository.listOrders() }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Ordens de serviço", style = MaterialTheme.typography.titleLarge)
                                    Button(enabled = machines.isNotEmpty(), onClick = { showOrder = true }) { Text("Nova OS") }
                                }
                                LazyColumn { items(orders.reversed()) { o ->
                                    val mName = machines.firstOrNull { it.id == o.machineId }?.name ?: o.machineId
                                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(o.title, style = MaterialTheme.typography.titleMedium)
                                            Text("$mName • ${orderText(o.status)}")
                                            if (o.description.isNotBlank()) Text(o.description)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                if (o.status == WorkOrderStatus.OPEN) Button(onClick = { perform { repository.startWork(o.id) } }) { Text("Iniciar") }
                                                if (o.status == WorkOrderStatus.IN_PROGRESS) {
                                                    OutlinedButton(onClick = { showEntryFor = o.id }) { Text("Apontar") }
                                                    Button(onClick = { showCompleteFor = o.id }) { Text("Concluir") }
                                                }
                                            }
                                        }
                                    }
                                } }
                            }
                            "Módulos" -> LegacyModulesScreen(legacyRepository, revision) { refresh() }
                            "Configurações" -> LegacySettingsScreen(legacyRepository) { refresh() }
                            "Memória Técnica IA" -> TechnicalAiScreen(firebaseServices.firebaseApp)
                            "Apontamentos" -> {
                                val entries = remember(revision) { repository.listEntries() }
                                val orders = remember(revision) { repository.listOrders() }
                                Text("Histórico de execução", style = MaterialTheme.typography.titleLarge)
                                LazyColumn { items(entries) { e ->
                                    val title = orders.firstOrNull { it.id == e.workOrderId }?.title ?: e.workOrderId
                                    ListItem(headlineContent = { Text(title) }, supportingContent = {
                                        Text("${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(e.createdAt))} • ${e.technician}: ${e.description} • ${e.durationMinutes} min")
                                    })
                                } }
                            }
                        }
                    }
                }
                if (showMachine) MachineDialog(onDismiss = { showMachine = false }) { name, code, sector, rate ->
                    perform { repository.addMachine(Machine(java.util.UUID.randomUUID().toString(), name, code, sector, rate)) }
                    showMachine = false
                }
                showStopFor?.let { machineId ->
                    val machineName = repository.listMachines().firstOrNull { it.id == machineId }?.name ?: machineId
                    StopDialog(machineName, onDismiss = { showStopFor = null }) { cause ->
                        perform { repository.registerStop(machineId, cause) }
                        showStopFor = null
                    }
                }
                if (showOrder) OrderDialog(machines = repository.listMachines(), stops = repository.listStops(), onDismiss = { showOrder = false }) { machineId, title, description, stopId ->
                    perform { repository.openWorkOrder(machineId, title, description, stopId) }
                    showOrder = false
                }
                showEntryFor?.let { orderId ->
                    EntryDialog(onDismiss = { showEntryFor = null }) { technician, description, parts, minutes ->
                        perform { repository.addExecution(orderId, technician, description, parts, minutes) }
                        showEntryFor = null
                    }
                }
                showCompleteFor?.let { orderId ->
                    val order = repository.listOrders().firstOrNull { it.id == orderId }
                    if (order != null) {
                        CompleteWorkDialog(
                            linkedToStop = order.stopId != null,
                            onDismiss = { showCompleteFor = null }
                        ) { solution, releaseMachine ->
                            perform { repository.completeWork(orderId, solution, releaseMachine) }
                            showCompleteFor = null
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun Summary(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(10.dp)) { Text(value, style = MaterialTheme.typography.headlineSmall); Text(label, style = MaterialTheme.typography.labelSmall) } }
}

@Composable
private fun MachineStatusIndicator(status: MachineStatus) {
    val color = when (status) {
        MachineStatus.RUNNING -> MaterialTheme.colorScheme.primary
        MachineStatus.STOPPED -> {
            val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "machine-stopped")
            transition.animateColor(
                initialValue = MaterialTheme.colorScheme.error,
                targetValue = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(650),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "stopped-blink"
            ).value
        }
        MachineStatus.MAINTENANCE -> MaterialTheme.colorScheme.tertiary
    }
    val label = statusText(status)
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(Modifier.size(10.dp)) {
            drawCircle(color = color)
        }
        Text(" $label", color = color, style = MaterialTheme.typography.bodyMedium)
    }
}
\nprivate fun statusText(s: MachineStatus) = when (s) {
    MachineStatus.RUNNING -> "Em operação"
    MachineStatus.STOPPED -> "Parada"
    MachineStatus.MAINTENANCE -> "Aguardando liberação"
}
private fun orderText(s: WorkOrderStatus) = when (s) {
    WorkOrderStatus.OPEN -> "Aberta"
    WorkOrderStatus.IN_PROGRESS -> "Em execução"
    WorkOrderStatus.COMPLETED -> "Concluída"
    WorkOrderStatus.CANCELLED -> "Cancelada"
}

@Composable private fun MachineDialog(onDismiss: () -> Unit, onSave: (String,String,String,Double) -> Unit) {
    var name by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("") }; var rate by remember { mutableStateOf("0") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nova máquina") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nome da máquina") })
            OutlinedTextField(code, { code = it }, label = { Text("Código") })
            OutlinedTextField(sector, { sector = it }, label = { Text("Setor") })
            OutlinedTextField(rate, { rate = it }, label = { Text("Produção kg/h") })
        }
    }, confirmButton = { Button(enabled = name.isNotBlank() && (rate.toDoubleOrNull() ?: -1.0) >= 0, onClick = { onSave(name.trim(),code.trim(),sector.trim(),rate.toDoubleOrNull() ?: 0.0) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable private fun OrderDialog(machines: List<Machine>, stops: List<StopRecord>, onDismiss: () -> Unit, onSave: (String,String,String,String?) -> Unit) {
    var machineId by remember { mutableStateOf(machines.firstOrNull()?.id ?: "") }
    var title by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }
    val openStop = stops.firstOrNull { it.machineId == machineId && it.status != StopStatus.CLOSED && it.workOrderId == null }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nova ordem de serviço") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Máquina")
            machines.forEach { m -> Row { RadioButton(selected = machineId == m.id, onClick = { machineId = m.id }); Text(m.name, modifier = Modifier.padding(top = 12.dp)) } }
            OutlinedTextField(title, { title = it }, label = { Text("Título da OS") })
            OutlinedTextField(description, { description = it }, label = { Text("Descrição") })
            Text(if (openStop != null) "A OS será vinculada à parada aberta." else "OS sem parada vinculada.")
        }
    }, confirmButton = { Button(enabled = title.isNotBlank() && machineId.isNotBlank(), onClick = { onSave(machineId,title,description,openStop?.id) }) { Text("Criar OS") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable private fun EntryDialog(onDismiss: () -> Unit, onSave: (String,String,String,Int) -> Unit) {
    var technician by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }
    var parts by remember { mutableStateOf("") }; var minutes by remember { mutableStateOf("0") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Apontamento de execução") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(technician, { technician = it }, label = { Text("Técnico") })
            OutlinedTextField(description, { description = it }, label = { Text("O que foi feito") })
            OutlinedTextField(parts, { parts = it }, label = { Text("Peças utilizadas") })
            OutlinedTextField(minutes, { minutes = it }, label = { Text("Duração em minutos") })
        }
    }, confirmButton = { Button(enabled = technician.isNotBlank() && description.isNotBlank() && (minutes.toIntOrNull() ?: -1) >= 0,
        onClick = { onSave(technician,description,parts,minutes.toIntOrNull() ?: 0) }) { Text("Salvar apontamento") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}


@Composable private fun StopDialog(
    machineName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var cause by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar parada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Máquina: $machineName")
                OutlinedTextField(
                    value = cause,
                    onValueChange = { cause = it },
                    label = { Text("Causa da parada") },
                    placeholder = { Text("Ex.: falha no puxador, inversor em alarme...") }
                )
            }
        },
        confirmButton = {
            Button(enabled = cause.isNotBlank(), onClick = { onConfirm(cause.trim()) }) {
                Text("Confirmar parada")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}


@Composable private fun CompleteWorkDialog(
    linkedToStop: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var solution by remember { mutableStateOf("") }
    var releaseMachine by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Concluir ordem de serviço") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (linkedToStop) {
                    OutlinedTextField(
                        value = solution,
                        onValueChange = { solution = it },
                        label = { Text("Solução aplicada") },
                        placeholder = { Text("Descreva o reparo realizado") }
                    )
                    Text("Confirme a condição da máquina:")
                    Row {
                        RadioButton(selected = releaseMachine, onClick = { releaseMachine = true })
                        Text("Liberar para produção", modifier = Modifier.padding(top = 12.dp))
                    }
                    Row {
                        RadioButton(selected = !releaseMachine, onClick = { releaseMachine = false })
                        Text("Manter em manutenção", modifier = Modifier.padding(top = 12.dp))
                    }
                } else {
                    Text("Esta OS não está vinculada a uma parada. A conclusão não altera o estado da máquina.")
                }
            }
        },
        confirmButton = {
            Button(enabled = !linkedToStop || solution.isNotBlank(), onClick = {
                onConfirm(solution.trim(), linkedToStop && releaseMachine)
            }) { Text("Confirmar conclusão") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
