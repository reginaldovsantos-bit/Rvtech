package com.rvtec.v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rvtec.v2.data.MaintenanceRepository
import com.rvtec.v2.data.LegacyModuleRepository
import com.rvtec.v2.data.FirebaseServices
import com.google.firebase.auth.FirebaseAuth
import com.rvtec.v2.domain.*

private val pages = listOf("Início", "Máquinas", "Paradas", "Ordens de serviço", "Preventivas", "Relatórios", "Apontamentos", "Memória Técnica IA", "Sincronização", "Módulos legados", "Configurações")

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firebaseServices = FirebaseServices(applicationContext)
        firebaseServices.installAppCheck()
        val repository = MaintenanceRepository(applicationContext)
        val legacyRepository = LegacyModuleRepository(applicationContext)
        val auth = FirebaseAuth.getInstance()
        val settings = getSharedPreferences("rvtec_settings", MODE_PRIVATE)
        setContent {
            var appearance by remember { mutableStateOf(settings.getString("appearance", "Atual/Referência") ?: "Atual/Referência") }
            var appName by remember { mutableStateOf(settings.getString("app_name", "RVTEC 2.0") ?: "RVTEC 2.0") }
            var workSchedule by remember { mutableStateOf(settings.getString("work_schedule", "") ?: "") }
            val rvtecColors = when (appearance) {
                "Atual/Referência" -> lightColorScheme(primary=Color(0xFF3989D4),secondary=Color(0xFF17375E),background=Color(0xFFE2EFFB),surface=Color(0xFFF8FCFF),surfaceVariant=Color(0xFFD5E6F5),onBackground=Color(0xFF17324F),onSurface=Color(0xFF17324F))
                "Antigo/Clássico" -> lightColorScheme(primary=Color(0xFF2F5D50),secondary=Color(0xFFA0784A),background=Color(0xFFF3EFE7),surface=Color(0xFFFFFDF8),surfaceVariant=Color(0xFFE8E0D2))
                else -> darkColorScheme(primary=Color(0xFFD6A84B),secondary=Color(0xFFB88A32),background=Color(0xFF101318),surface=Color(0xFF202832),surfaceVariant=Color(0xFF303B48),onBackground=Color(0xFFF4F7FB),onSurface=Color(0xFFF4F7FB))
            }
            MaterialTheme(colorScheme = rvtecColors) {
                var signedIn by remember { mutableStateOf(auth.currentUser != null) }
                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { signedIn = it.currentUser != null }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }
                if (!signedIn) {
                    FirebaseAuthScreen(auth) { signedIn = true }
                } else {
                var page by remember { mutableStateOf("Início") }
                var visiblePages by remember {
                    mutableStateOf(settings.getStringSet("visible_pages", null)?.toSet()?.plus("Módulos legados") ?: pages.toSet())
                }
                val menuPages = pages.filter { it == "Início" || it == "Configurações" || it in visiblePages }
                var revision by remember { mutableIntStateOf(0) }
                var error by remember { mutableStateOf<String?>(null) }
                var showMachine by remember { mutableStateOf(false) }
                var showEditMachineId by remember { mutableStateOf<String?>(null) }
                var showDeleteMachineId by remember { mutableStateOf<String?>(null) }
                var selectedMachineId by remember { mutableStateOf<String?>(null) }
                var showStopFor by remember { mutableStateOf<String?>(null) }
                var showOrder by remember { mutableStateOf(false) }
                var showEntryFor by remember { mutableStateOf<String?>(null) }
                var showCompleteFor by remember { mutableStateOf<String?>(null) }
                fun refresh() { revision++ }
                fun perform(action: () -> Unit) {
                    try { action(); error = null; refresh() } catch (e: Exception) { error = e.message ?: "Falha na operação." }
                }
                val referenceStyle = appearance == "Atual/Referência"
                val menuColor = Color(0xFF102849)
                Scaffold(containerColor=MaterialTheme.colorScheme.background) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        if (referenceStyle) {
                            Row(Modifier.fillMaxWidth().padding(horizontal=10.dp, vertical=8.dp).height(70.dp)
                                .background(menuColor, MaterialTheme.shapes.extraLarge).padding(5.dp),
                                horizontalArrangement=Arrangement.spacedBy(3.dp), verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                                val mainNav = listOf(Triple("Início","⌂","Início"),Triple("Ordens de serviço","🔧","OS"),Triple("Máquinas","⚙","Máquinas"),Triple("Relatórios","▥","Indicadores"),Triple("Configurações","⚙","Ajustes"))
                                mainNav.forEach { (target,icon,label) ->
                                    val selected = page == target
                                    Column(Modifier.weight(1f).fillMaxHeight().clickable { page=target }
                                        .background(if(selected) Color(0xFF3989D4) else menuColor, MaterialTheme.shapes.large),
                                        horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,
                                        verticalArrangement=Arrangement.Center) {
                                        Text(icon,color=Color.White,style=MaterialTheme.typography.titleLarge)
                                        Text(label,color=Color.White,style=MaterialTheme.typography.labelMedium,maxLines=1)
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth().padding(horizontal=10.dp, vertical=4.dp), horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                                listOf(Triple("Memória Técnica IA","🤖 IA RVTEC","Memória Técnica IA"),Triple("Ordens de serviço","• Solicitações","Ordens de serviço"),Triple("Configurações","⚙ Configurações","Configurações")).forEach { (target,label,_) ->
                                    Card(Modifier.weight(1f).height(48.dp).clickable { page=target },
                                        colors=CardDefaults.cardColors(containerColor=menuColor),shape=MaterialTheme.shapes.large) {
                                        Box(Modifier.fillMaxSize(),contentAlignment=androidx.compose.ui.Alignment.Center) {
                                            Text(label,color=Color.White,style=MaterialTheme.typography.labelLarge,maxLines=1)
                                        }
                                    }
                                }
                            }
                        } else {
                            TopAppBar(title={ Text(appName.ifBlank { "RVTEC 2.0" }) })
                            ScrollableTabRow(selectedTabIndex=menuPages.indexOf(page).coerceAtLeast(0),containerColor=menuColor) {
                                menuPages.forEach { item -> Tab(selected=page==item,onClick={page=item},text={Text(item,maxLines=1)}) }
                            }
                        }
                        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                        if (repository.hasStorageError()) {
                            Text(
                                "ATENÇÃO: os dados locais não puderam ser lidos. O armazenamento foi bloqueado para evitar sobrescrever informações. Procure recuperar os dados antes de continuar.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) }
                        when (page) {
                            "Configurações" -> AppearanceSettingsScreen(
                                selected = appearance,
                                appName = appName,
                                workSchedule = workSchedule,
                                onAppNameChange = { value ->
                                    appName = value
                                    settings.edit().putString("app_name", value).apply()
                                },
                                onWorkScheduleChange = { value ->
                                    workSchedule = value
                                    settings.edit().putString("work_schedule", value).apply()
                                },
                                visiblePages = visiblePages,
                                onAppearanceSelect = { selected ->
                                    appearance = selected
                                    settings.edit().putString("appearance", selected).apply()
                                },
                                onModuleToggle = { module, enabled ->
                                    if (module != "Início" && module != "Configurações") {
                                        val updated = if (enabled) visiblePages + module else visiblePages - module
                                        visiblePages = updated + "Início" + "Configurações"
                                        settings.edit().putStringSet("visible_pages", visiblePages.toSet()).apply()
                                        if (!enabled && page == module) page = "Início"
                                    }
                                }
                            )
                            "Início" -> {
                                val machines = remember(revision) { repository.listMachines() }
                                val stops = remember(revision) { repository.listStops() }
                                val orders = remember(revision) { repository.listOrders() }
                                val now = System.currentTimeMillis()
                                val indicators = remember(revision) { MaintenanceIndicatorsCalculator.calculate(stops, now - 30L * 24 * 60 * 60 * 1000, now) }
                                DashboardScreen(
                                    machines, stops.count { it.status != StopStatus.CLOSED },
                                    orders.count { it.status != WorkOrderStatus.COMPLETED && it.status != WorkOrderStatus.CANCELLED },
                                    indicators.availabilityPercent, indicators.mtbfHours, indicators.mttrHours,
                                    { page = "Máquinas" }, { page = "Paradas" }, { page = "Ordens de serviço" },
                                    { page = "Relatórios" }, { page = "Memória Técnica IA" }, { page = "Preventivas" },
                                    { if (machines.isNotEmpty()) showStopFor = machines.first().id else page = "Máquinas" },
                                    { if (machines.isNotEmpty()) showOrder = true else page = "Máquinas" },
                                    { page = "Apontamentos" }
                                )
                            }
                            "Máquinas" -> {
                                val machines = remember(revision) { repository.listMachines() }
                                val selectedMachine = machines.firstOrNull { it.id == selectedMachineId }
                                    ?: machines.firstOrNull()
                                Column(Modifier.fillMaxSize()) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Máquinas", style = MaterialTheme.typography.titleLarge)
                                            Text("Situação das máquinas", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Button(onClick = { showMachine = true }) { Text("Nova") }
                                    }
                                    Row(
                                        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        StatusCount("OPERAÇÃO", machines.count { it.status == MachineStatus.RUNNING }, Color(0xFF16834B), Modifier.weight(1f))
                                        StatusCount("ATEND.", machines.count { it.status == MachineStatus.MAINTENANCE }, Color(0xFFD9A000), Modifier.weight(1f))
                                        StatusCount("PARADA", machines.count { it.status == MachineStatus.STOPPED }, Color(0xFFC62828), Modifier.weight(1f))
                                    }
                                    if (machines.isEmpty()) {
                                        Text("Nenhuma máquina cadastrada. Toque em Nova para começar.", modifier = Modifier.padding(vertical = 16.dp))
                                    } else {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(4),
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 330.dp),
                                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                                            verticalArrangement = Arrangement.spacedBy(7.dp)
                                        ) {
                                            items(machines.size) { index ->
                                                val machine = machines[index]
                                                val tileColor = when (machine.status) {
                                                    MachineStatus.RUNNING -> Color(0xFF16834B)
                                                    MachineStatus.MAINTENANCE -> Color(0xFFD9A000)
                                                    MachineStatus.STOPPED -> Color(0xFFC62828)
                                                }
                                                val pulse = if (machine.status == MachineStatus.STOPPED) {
                                                    rememberInfiniteTransition(label = "stopped-machine-${machine.id}")
                                                        .animateFloat(
                                                            initialValue = 0.48f,
                                                            targetValue = 1f,
                                                            animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
                                                            label = "stopped-pulse"
                                                        ).value
                                                } else 1f
                                                val tileText = if (machine.status == MachineStatus.MAINTENANCE) Color(0xFF342500) else Color.White
                                                val tileLabel = machine.code.filter { it.isDigit() }.take(4)
                                                    .ifBlank { machine.name.filter { it.isDigit() }.take(4) }
                                                    .ifBlank { machine.name.take(4) }
                                                Card(
                                                    modifier = Modifier.fillMaxWidth().height(70.dp)
                                                        .clickable { selectedMachineId = machine.id },
                                                    colors = CardDefaults.cardColors(containerColor = tileColor.copy(alpha = pulse)),
                                                    border = if (selectedMachine?.id == machine.id) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null
                                                ) {
                                                    Column(
                                                        Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 7.dp),
                                                        verticalArrangement = Arrangement.Center,
                                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            tileLabel,
                                                            color = tileText,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            maxLines = 1
                                                        )
                                                        Text(
                                                            when (machine.status) {
                                                                MachineStatus.RUNNING -> "OPERAÇÃO"
                                                                MachineStatus.MAINTENANCE -> "ATEND."
                                                                MachineStatus.STOPPED -> "PARADA"
                                                            },
                                                            color = tileText,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        selectedMachine?.let { machine ->
                                            Card(
                                                Modifier.fillMaxWidth().padding(top = 12.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                    Text(machine.name, style = MaterialTheme.typography.titleMedium)
                                                    Text("Código: ${machine.code.ifBlank { "—" }}")
                                                    Text("Setor: ${machine.sector.ifBlank { "—" }}")
                                                    Text("Produção: ${machine.productionKgPerHour} kg/h")
                                                    Text("Estado: ${statusText(machine.status)}")
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        OutlinedButton(onClick = { showEditMachineId = machine.id }) { Text("Editar") }
                                                        TextButton(onClick = { showDeleteMachineId = machine.id }) { Text("Excluir") }
                                                        if (machine.status == MachineStatus.MAINTENANCE) {
                                                            Button(onClick = { perform { repository.releaseMachine(machine.id) } }) { Text("Liberar") }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "Paradas" -> {
                                val machines = remember(revision) { repository.listMachines() }
                                val stops = remember(revision) { repository.listStops() }
                                val orders = remember(revision) { repository.listOrders() }
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
                                        val linkedOrder = orders.firstOrNull { it.id == s.workOrderId }
                                        ListItem(headlineContent = { Text(machineName) }, supportingContent = {
                                            Column {
                                                Text("${s.cause} • ${when (s.status) {
                                                    StopStatus.OPEN -> "Aberta"
                                                    StopStatus.LINKED_TO_ORDER -> "Vinculada à OS"
                                                    StopStatus.CLOSED -> "Encerrada"
                                                }}")
                                                if (linkedOrder != null) Text("OS: ${linkedOrder.title}")
                                                if (s.solution.isNotBlank()) Text("Solução: ${s.solution}")
                                                Text("Início: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR")).format(java.util.Date(s.startedAt))}")
                                                s.endedAt?.let { ended ->
                                                    Text("Fim: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR")).format(java.util.Date(ended))}")
                                                }
                                            }
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
                            "Memória Técnica IA" -> TechnicalAiScreen()
                            "Sincronização" -> FirebaseSyncScreen(firebaseServices, repository)
                            "Módulos legados" -> LegacyModulesScreen(legacyRepository, revision) { refresh() }
                            "Apontamentos" -> {
                                val entries = remember(revision) { repository.listEntries() }
                                val orders = remember(revision) { repository.listOrders() }
                                Text("Histórico de execução", style = MaterialTheme.typography.titleLarge)
                                LazyColumn { items(entries.reversed()) { e ->
                                    val title = orders.firstOrNull { it.id == e.workOrderId }?.title ?: e.workOrderId
                                    ListItem(headlineContent = { Text(title) }, supportingContent = {
                                        Text("${e.technician}: ${e.description} • ${e.durationMinutes} min")
                                    })
                                } }
                            }
                        }
                        }
                    }
                }
                if (showMachine) MachineDialog(onDismiss = { showMachine = false }) { name, code, sector, rate ->
                    perform { repository.addMachine(Machine(java.util.UUID.randomUUID().toString(), name, code, sector, rate)) }
                    showMachine = false
                }
                showEditMachineId?.let { machineId ->
                    val machine = repository.listMachines().firstOrNull { it.id == machineId }
                    if (machine != null) MachineDialog(
                        initial = machine,
                        onDismiss = { showEditMachineId = null }
                    ) { name, code, sector, rate ->
                        perform { repository.updateMachine(machineId, name, code, sector, rate) }
                        showEditMachineId = null
                    }
                }
                showDeleteMachineId?.let { machineId ->
                    val machineName = repository.listMachines().firstOrNull { it.id == machineId }?.name ?: machineId
                    AlertDialog(
                        onDismissRequest = { showDeleteMachineId = null },
                        title = { Text("Excluir máquina?") },
                        text = { Text("Deseja excluir $machineName? O sistema bloqueia a exclusão se houver paradas, ordens ou manutenção ativa vinculadas.") },
                        confirmButton = {
                            Button(onClick = {
                                perform { repository.deleteMachine(machineId) }
                                showDeleteMachineId = null
                            }) { Text("Excluir") }
                        },
                        dismissButton = { TextButton(onClick = { showDeleteMachineId = null }) { Text("Cancelar") } }
                    )
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
}

@Composable
private fun DashboardScreen(
    machines: List<Machine>, stopsOpen: Int, ordersOpen: Int, availability: Double,
    mtbf: Double?, mttr: Double?, onMachines: () -> Unit, onStops: () -> Unit,
    onOrders: () -> Unit, onReports: () -> Unit, onAi: () -> Unit,
    onPreventive: () -> Unit, onNewStop: () -> Unit, onNewOrder: () -> Unit, onEntries: () -> Unit
) {
    val running = machines.count { it.status == MachineStatus.RUNNING }
    val stopped = machines.count { it.status == MachineStatus.STOPPED }
    val attending = machines.count { it.status == MachineStatus.MAINTENANCE }
    val panel = Color(0xFFF8FCFF)
    val edge = Color(0xFFC9D9E8)
    val ink = Color(0xFF17324F)
    val capacity = machines.sumOf { it.productionKgPerHour }
    val metrics = listOf(
        "Máquinas cadastradas" to machines.size.toString(),
        "Máquinas paradas agora" to stopped.toString(),
        "Máquinas em atendimento" to attending.toString(),
        "Paradas hoje" to stopsOpen.toString(),
        "Ordens pendentes" to ordersOpen.toString(),
        "Preventivas atrasadas" to "—",
        "Preventivas cadastradas" to "—"
    )
    LazyColumn(Modifier.fillMaxSize().background(Color(0xFFE2EFFB)),
        contentPadding=PaddingValues(start=12.dp,end=12.dp,top=14.dp,bottom=24.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text("Painel geral",color=ink,style=MaterialTheme.typography.headlineSmall)
                Text("Visão geral da manutenção",color=ink.copy(alpha=0.68f),style=MaterialTheme.typography.titleMedium)
            }
        }
        item {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(columns=GridCells.Fixed(2),
                modifier=Modifier.fillMaxWidth().heightIn(max=560.dp),userScrollEnabled=false,
                horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                items(metrics.size) { index ->
                    val (label,value)=metrics[index]
                    Card(Modifier.fillMaxWidth().height(112.dp),colors=CardDefaults.cardColors(containerColor=panel),
                        border=androidx.compose.foundation.BorderStroke(1.dp,edge),shape=MaterialTheme.shapes.large,
                        elevation=CardDefaults.cardElevation(defaultElevation=3.dp)) {
                        Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.SpaceBetween) {
                            Text(label,color=ink.copy(alpha=0.68f),style=MaterialTheme.typography.titleSmall)
                            Text(value,color=ink,style=MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=panel),
                border=androidx.compose.foundation.BorderStroke(1.dp,edge)) {
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("Indicadores de manutenção",color=ink,style=MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("Disponibilidade",color=ink); Text(String.format(java.util.Locale("pt","BR"),"%.1f%%",availability),color=ink)
                    }
                    HorizontalDivider(color=edge)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("MTBF",color=ink); Text(mtbf?.let { String.format(java.util.Locale("pt","BR"),"%.1f h",it) } ?: "—",color=ink)
                    }
                    HorizontalDivider(color=edge)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("MTTR",color=ink); Text(mttr?.let { String.format(java.util.Locale("pt","BR"),"%.1f h",it) } ?: "—",color=ink)
                    }
                    Text("Capacidade cadastrada: "+String.format(java.util.Locale("pt","BR"),"%.0f kg/h",capacity),color=ink)
                }
            }
        }
        item {
            Text("Máquinas",color=ink,style=MaterialTheme.typography.titleMedium)
            if(machines.isEmpty()) Text("Cadastre as máquinas para visualizar a situação.",color=ink)
            else androidx.compose.foundation.lazy.grid.LazyVerticalGrid(columns=GridCells.Fixed(4),
                modifier=Modifier.fillMaxWidth().heightIn(max=190.dp),userScrollEnabled=false,
                horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                items(machines.size) { i ->
                    val m=machines[i]
                    val tile=when(m.status) { MachineStatus.RUNNING -> Color(0xFF16834B); MachineStatus.STOPPED -> Color(0xFFC62828); MachineStatus.MAINTENANCE -> Color(0xFFE0B323) }
                    val pulse=if(m.status==MachineStatus.STOPPED) rememberInfiniteTransition(label="machinePulse").animateFloat(0.55f,1f,infiniteRepeatable(tween(650),RepeatMode.Reverse),label="pulse").value else 1f
                    Card(Modifier.fillMaxWidth().height(66.dp).clickable(onClick=onMachines),
                        colors=CardDefaults.cardColors(containerColor=tile.copy(alpha=pulse))) {
                        Column(Modifier.fillMaxSize().padding(3.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                            Text(m.code.ifBlank { m.name.take(5) },color=Color.White,style=MaterialTheme.typography.titleMedium,maxLines=1)
                            Text(when(m.status) { MachineStatus.RUNNING -> "Operando"; MachineStatus.STOPPED -> "Parada"; MachineStatus.MAINTENANCE -> "Atend." },color=Color.White,style=MaterialTheme.typography.labelSmall,maxLines=1)
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                DashboardAction("＋","Nova OS",onNewOrder,Modifier.weight(1f),panel,edge,ink)
                DashboardAction("■","Nova parada",onNewStop,Modifier.weight(1f),panel,edge,ink)
                DashboardAction("✦","IA técnica",onAi,Modifier.weight(1f),panel,edge,ink)
            }
        }
    }
}
@Composable private fun DashboardMetric(label:String,value:String,valueColor:Color,modifier:Modifier,panel:Color,edge:Color,ink:Color){
    Card(modifier,colors=CardDefaults.cardColors(containerColor=panel),border=androidx.compose.foundation.BorderStroke(1.dp,edge)){
        Column(Modifier.fillMaxWidth().padding(9.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally){
            Text(value,color=valueColor,style=MaterialTheme.typography.titleLarge);Text(label,color=ink,style=MaterialTheme.typography.labelSmall,maxLines=1)
        }
    }
}
@Composable private fun DashboardAction(symbol:String,label:String,action:()->Unit,modifier:Modifier,panel:Color,edge:Color,ink:Color){
    Card(modifier.height(88.dp).clickable(onClick=action),colors=CardDefaults.cardColors(containerColor=panel),border=androidx.compose.foundation.BorderStroke(1.dp,edge)){
        Column(Modifier.fillMaxSize().padding(5.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
            Text(symbol,color=Color.White,style=MaterialTheme.typography.headlineSmall);Text(label,color=ink,style=MaterialTheme.typography.labelSmall,maxLines=2)
        }
    }
}

@Composable
private fun StatusCount(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.13f))) {
        Column(Modifier.padding(horizontal = 7.dp, vertical = 8.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(count.toString(), color = color, style = MaterialTheme.typography.titleMedium)
            Text(label, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable private fun Summary(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(10.dp)) { Text(value, style = MaterialTheme.typography.headlineSmall); Text(label, style = MaterialTheme.typography.labelSmall) } }
}
private fun statusText(s: MachineStatus) = when (s) {
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

@Composable private fun MachineDialog(
    initial: Machine? = null,
    onDismiss: () -> Unit,
    onSave: (String,String,String,Double) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var code by remember(initial?.id) { mutableStateOf(initial?.code ?: "") }
    var sector by remember(initial?.id) { mutableStateOf(initial?.sector ?: "") }
    var rate by remember(initial?.id) { mutableStateOf((initial?.productionKgPerHour ?: 0.0).toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "Nova máquina" else "Editar máquina") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nome da máquina") })
            OutlinedTextField(code, { code = it }, label = { Text("Código") })
            OutlinedTextField(sector, { sector = it }, label = { Text("Setor") })
            OutlinedTextField(rate, { rate = it }, label = { Text("Produção kg/h") })
        }
    }, confirmButton = {
        Button(
            enabled = name.isNotBlank() && (rate.toDoubleOrNull() ?: -1.0) >= 0,
            onClick = { onSave(name.trim(), code.trim(), sector.trim(), rate.toDoubleOrNull() ?: 0.0) }
        ) { Text("Salvar") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
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


@Composable
private fun AppearanceSettingsScreen(
    selected: String,
    appName: String,
    workSchedule: String,
    onAppNameChange: (String) -> Unit,
    onWorkScheduleChange: (String) -> Unit,
    visiblePages: Set<String>,
    onAppearanceSelect: (String) -> Unit,
    onModuleToggle: (String, Boolean) -> Unit
) {
    val styles = listOf(
        "Atual/Referência" to "Visual claro, cabeçalho azul e cartões suaves — modelo de referência aprovado.",
        "Moderno" to "Visual escuro sofisticado, com detalhes dourados.",
        "Antigo/Clássico" to "Visual clássico inspirado nas versões anteriores do RVTEC."
    )
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Configurações", style = MaterialTheme.typography.headlineSmall)
            Text("Identificação e jornada", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = appName,
                onValueChange = onAppNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nome exibido do aplicativo") },
                singleLine = true
            )
            OutlinedTextField(
                value = workSchedule,
                onValueChange = onWorkScheduleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Horário / escala de trabalho") },
                placeholder = { Text("Ex.: 6x2 • turnos de 8 horas") },
                singleLine = true
            )
            Text("Aparência do aplicativo", style = MaterialTheme.typography.titleMedium)
            Text("Escolha um dos três estilos. A preferência fica salva neste aparelho e é aplicada em todas as telas.")
        }
        items(styles) { style ->
            val (name, description) = style
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected == name) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected == name, onClick = { onAppearanceSelect(name) })
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text(description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            HorizontalDivider()
            Text("Módulos do menu", style = MaterialTheme.typography.titleMedium)
            Text("Desmarque para ocultar uma aba. Os dados e as funções permanecem salvos.")
        }
        items(pages) { module ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = module == "Início" || module == "Configurações" || module in visiblePages,
                    enabled = module != "Início" && module != "Configurações",
                    onCheckedChange = { checked -> onModuleToggle(module, checked) }
                )
                Text(module, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            Text("Início e Configurações permanecem disponíveis. Ocultar outros módulos não exclui máquinas, paradas, ordens ou apontamentos.")
        }
}
}
