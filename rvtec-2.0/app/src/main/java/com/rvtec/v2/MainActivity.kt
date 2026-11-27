package com.rvtec.v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                "Atual/Referência" -> lightColorScheme(primary=Color(0xFF3985D4),secondary=Color(0xFF102B4D),background=Color(0xFFDCE8F7),surface=Color(0xFFF7FBFF),surfaceVariant=Color(0xFFEAF3FC),onBackground=Color(0xFF17304B),onSurface=Color(0xFF17304B),onSurfaceVariant=Color(0xFF607386),outlineVariant=Color(0xFFCBD8E6))
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
                    mutableStateOf(settings.getStringSet("visible_pages", null)?.toSet() ?: pages.toSet())
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
                            Card(
                                Modifier.fillMaxWidth().padding(horizontal=14.dp, vertical=8.dp),
                                colors=CardDefaults.cardColors(containerColor=Color(0xFFEAF2FC)),
                                shape=MaterialTheme.shapes.extraLarge,
                                elevation=CardDefaults.cardElevation(defaultElevation=2.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min=120.dp).padding(horizontal=22.dp, vertical=18.dp),
                                    verticalAlignment=androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier=Modifier.size(54.dp).clickable { page = "Início" },
                                        color=Color.White, shape=MaterialTheme.shapes.large,
                                        shadowElevation=2.dp
                                    ) {
                                        Box(contentAlignment=androidx.compose.ui.Alignment.Center) {
                                            Text("☰", color=Color(0xFF102849), style=MaterialTheme.typography.headlineLarge)
                                        }
                                    }
                                    Column(Modifier.weight(1f).padding(start=16.dp)) {
                                        Text("MANUTENÇÃO INDUSTRIAL", color=Color(0xFF61758B), style=MaterialTheme.typography.labelLarge)
                                        Text("RVTEC", color=Color(0xFF9AAABD), style=MaterialTheme.typography.headlineLarge)
                                        Text("Manutenção Industrial", color=Color(0xFF64788D), style=MaterialTheme.typography.bodyMedium)
                                    }
                                    Surface(color=Color(0xFFEAF2FC), shape=MaterialTheme.shapes.extraLarge) {
                                        Text("v2.0", modifier=Modifier.padding(horizontal=15.dp, vertical=10.dp), color=Color(0xFF60758A), style=MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                            val mainNav=listOf("Início","Ordens de serviço","Máquinas","Relatórios","Configurações").filter { it=="Início" || it=="Configurações" || it in visiblePages }
                            Surface(
                                Modifier.fillMaxWidth().padding(horizontal=14.dp, vertical=0.dp),
                                color=Color(0xFF102B4D), shape=MaterialTheme.shapes.extraLarge,
                                shadowElevation=4.dp
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=8.dp, vertical=8.dp),
                                    horizontalArrangement=Arrangement.spacedBy(5.dp),
                                    verticalAlignment=androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    mainNav.forEach { target ->
                                        val active = page == target
                                        val (glyph,label)=when(target) {
                                            "Início" -> "⌂" to "Início"
                                            "Ordens de serviço" -> "🔧" to "OS"
                                            "Máquinas" -> "⚙" to "Máquinas"
                                            "Relatórios" -> "▥" to "Indicadores"
                                            else -> "⚙" to "Ajustes"
                                        }
                                        Surface(
                                            modifier=Modifier.widthIn(min=82.dp).clickable { page=target },
                                            color=if(active) Color(0xFF3985D4) else Color.Transparent,
                                            shape=MaterialTheme.shapes.large
                                        ) {
                                            Column(
                                                Modifier.heightIn(min=72.dp).padding(horizontal=8.dp, vertical=7.dp),
                                                horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,
                                                verticalArrangement=Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(glyph, color=Color.White, style=MaterialTheme.typography.headlineSmall)
                                                Text(label, color=Color.White, style=MaterialTheme.typography.labelMedium, maxLines=1, softWrap=false)
                                            }
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
                            "Preventivas" -> PreventivesScreen(
                                repository = legacyRepository,
                                machines = remember(revision) { repository.listMachines() },
                                revision = revision,
                                onRefresh = { refresh() }
                            )
                            "Relatórios" -> ReportsScreen(
                                machines = remember(revision) { repository.listMachines() },
                                stops = remember(revision) { repository.listStops() },
                                orders = remember(revision) { repository.listOrders() }
                            )
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
                                val entries = remember(revision) { repository.listEntries() }
                                DashboardScreen(
                                    machines, stops, orders, entries,
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
                                val selectedMachine = machines.firstOrNull { it.id == selectedMachineId } ?: machines.firstOrNull()
                                Column(Modifier.fillMaxSize()) {
                                    Row(Modifier.fillMaxWidth().padding(top=12.dp,bottom=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("⚙ Máquinas",style=MaterialTheme.typography.headlineMedium,color=Color(0xFF102849))
                                            Text("Situação das máquinas",style=MaterialTheme.typography.bodyLarge,color=Color(0xFF34495E))
                                        }
                                        Button(onClick={showMachine=true}) { Text("＋ Nova") }
                                    }
                                    Card(Modifier.fillMaxWidth().padding(bottom=10.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFEAF2FC)),shape=MaterialTheme.shapes.extraLarge,border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFFCBD8E6))) {
                                        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=10.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                                            StatusLegend("Operação",Color(0xFF078B4B))
                                            StatusLegend("Atendimento",Color(0xFFE0B000))
                                            StatusLegend("Parada",Color(0xFFE3222B))
                                        }
                                    }
                                    if(machines.isEmpty()) {
                                        Text("Nenhuma máquina cadastrada. Toque em Nova para começar.",modifier=Modifier.padding(vertical=16.dp))
                                    } else {
                                        LazyVerticalGrid(columns=GridCells.Fixed(4),modifier=Modifier.fillMaxWidth().weight(1f),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(7.dp),contentPadding=PaddingValues(bottom=10.dp)) {
                                            items(machines.size) { index ->
                                                val machine=machines[index]
                                                val tileColor=when(machine.status) {
                                                    MachineStatus.RUNNING -> Color(0xFF078B4B)
                                                    MachineStatus.MAINTENANCE -> Color(0xFFE0B000)
                                                    MachineStatus.STOPPED -> Color(0xFFE3222B)
                                                }
                                                val pulse=if(machine.status==MachineStatus.STOPPED) rememberInfiniteTransition(label="stopped-machine-${machine.id}").animateFloat(0.35f,1f,infiniteRepeatable(tween(2500),RepeatMode.Reverse),label="stopped-pulse").value else 1f
                                                val tileText=if(machine.status==MachineStatus.MAINTENANCE) Color(0xFF302600) else Color.White
                                                val tileLabel=machine.code.filter { it.isDigit() }.take(4).ifBlank { machine.name.filter { it.isDigit() }.take(4) }.ifBlank { machine.name.take(4) }
                                                Card(Modifier.fillMaxWidth().height(78.dp).clickable { selectedMachineId=machine.id },colors=CardDefaults.cardColors(containerColor=tileColor.copy(alpha=pulse)),shape=MaterialTheme.shapes.large,border=androidx.compose.foundation.BorderStroke(1.5.dp,Color.White),elevation=CardDefaults.cardElevation(defaultElevation=2.dp)) {
                                                    Column(Modifier.fillMaxSize().padding(horizontal=1.dp,vertical=4.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                                                        Text(tileLabel,color=tileText,style=MaterialTheme.typography.headlineSmall,maxLines=1)
                                                        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                                                            if(machine.status==MachineStatus.STOPPED) Text("● ",color=Color.White,style=MaterialTheme.typography.labelSmall)
                                                            Text(when(machine.status) { MachineStatus.RUNNING -> "OPERAÇÃO"; MachineStatus.MAINTENANCE -> "ATENDIMENTO"; MachineStatus.STOPPED -> "PARADA" },color=tileText,style=MaterialTheme.typography.labelSmall,maxLines=1,softWrap=false)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        selectedMachine?.let { machine ->
                                            Card(Modifier.fillMaxWidth().padding(top=8.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                                                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                                    Text(machine.name,style=MaterialTheme.typography.titleMedium)
                                                    Text("Código: ${machine.code.ifBlank { "—" }}")
                                                    Text("Setor: ${machine.sector.ifBlank { "—" }}")
                                                    Text("Produção: ${machine.productionKgPerHour} kg/h")
                                                    Text("Estado: ${statusText(machine.status)}")
                                                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                                        OutlinedButton(onClick={showEditMachineId=machine.id}) { Text("Editar") }
                                                        TextButton(onClick={showDeleteMachineId=machine.id}) { Text("Excluir") }
                                                        if(machine.status==MachineStatus.MAINTENANCE) Button(onClick={perform { repository.releaseMachine(machine.id) }}) { Text("Liberar") }
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
    machines: List<Machine>, stops: List<StopRecord>, orders: List<WorkOrder>,
    entries: List<ExecutionEntry>, availability: Double, mtbf: Double?, mttr: Double?,
    onMachines: () -> Unit, onStops: () -> Unit, onOrders: () -> Unit,
    onReports: () -> Unit, onAi: () -> Unit, onPreventive: () -> Unit,
    onNewStop: () -> Unit, onNewOrder: () -> Unit, onEntries: () -> Unit
) {
    val running = machines.count { it.status == MachineStatus.RUNNING }
    val activeOrders = orders.filter { it.status == WorkOrderStatus.IN_PROGRESS }
    val attendingMachines = activeOrders.mapNotNull { order ->
        machines.firstOrNull { it.id == order.machineId }?.let { machine -> order to machine }
    }.distinctBy { it.second.id }
    // Paradas e atendimentos são indicadores independentes: uma máquina pode aparecer nos dois cartões.
    val stoppedMachines = machines.filter { it.status == MachineStatus.STOPPED }
    val pendingOrders = orders.count { it.status == WorkOrderStatus.OPEN || it.status == WorkOrderStatus.IN_PROGRESS }
    val todayStart = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val stopsToday = stops.count { it.startedAt >= todayStart }
    val ink = Color(0xFF17324D)
    val muted = Color(0xFF687B8D)
    val panel = Color(0xFFF8FBFF)
    val border = Color(0xFFC9D8E7)
    LazyColumn(
        Modifier.fillMaxSize().background(Color(0xFFE4EFFB)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Painel geral", color = ink, style = MaterialTheme.typography.headlineSmall)
                Text("Visão geral da manutenção", color = muted, style = MaterialTheme.typography.titleSmall)
            }
        }
        item {
            DashboardCarouselCard(
                title = "MÁQUINAS EM PARADA",
                emptyMessage = "Nenhuma máquina em parada agora.",
                count = stoppedMachines.size,
                accent = Color(0xFFC62828),
                onClick = onStops
            ) {
                val machinesOnStop = stoppedMachines
                var index by remember(machinesOnStop.map { it.id }) { mutableIntStateOf(0) }
                LaunchedEffect(machinesOnStop.map { it.id }) {
                    while (machinesOnStop.size > 1) {
                        kotlinx.coroutines.delay(10_000)
                        index = (index + 1) % machinesOnStop.size
                    }
                }
                if (machinesOnStop.isNotEmpty()) {
                    val machine = machinesOnStop[index.coerceIn(0, machinesOnStop.lastIndex)]
                    val pulse = rememberInfiniteTransition(label = "dashboard-stop-${machine.id}")
                        .animateFloat(0.55f, 1f,
                            infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                            label = "dashboard-stop-pulse").value
                    Column(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
                            .background(Color(0xFFC62828).copy(alpha = pulse)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(machine.name.uppercase(), color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text(machine.sector.ifBlank { "Especialidade não informada" }.uppercase(),
                            color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("EM PARADA", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                    if (machinesOnStop.size > 1) {
                        Text("Máquina ${index + 1} de ${machinesOnStop.size} • alternância a cada 10 segundos",
                            color = muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            DashboardCarouselCard(
                title = "EM ATENDIMENTO",
                emptyMessage = "Nenhuma ordem em atendimento.",
                count = attendingMachines.size,
                accent = Color(0xFFB88600),
                onClick = onOrders
            ) {
                val services = attendingMachines
                var index by remember(services.map { it.first.id to it.second.id }) { mutableIntStateOf(0) }
                LaunchedEffect(services.map { it.first.id to it.second.id }) {
                    while (services.size > 1) {
                        kotlinx.coroutines.delay(10_000)
                        index = (index + 1) % services.size
                    }
                }
                if (services.isNotEmpty()) {
                    val (order, machine) = services[index.coerceIn(0, services.lastIndex)]
                    val technician = entries.filter { it.workOrderId == order.id }
                        .maxByOrNull { it.createdAt }?.technician ?: "Técnico não informado"
                    Column(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
                            .background(Color(0xFFFFD54F)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("OS nº ${order.id.takeLast(6).uppercase()}", color = Color(0xFF342500),
                            style = MaterialTheme.typography.titleLarge)
                        Text("Técnico: $technician", color = Color(0xFF342500),
                            style = MaterialTheme.typography.titleMedium)
                        Text("Máquina: ${machine.name}", color = Color(0xFF342500),
                            style = MaterialTheme.typography.titleMedium)
                    }
                    if (services.size > 1) {
                        Text("Máquina ${index + 1} de ${services.size} • alternância a cada 10 segundos",
                            color = muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardMetric("Máquinas cadastradas", machines.size.toString(), panel, Modifier.weight(1f), onMachines)
                    DashboardMetric("Máquinas paradas agora", stoppedMachines.size.toString(), panel, Modifier.weight(1f), onStops)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardMetric("Máquinas em atendimento", attendingMachines.size.toString(), panel, Modifier.weight(1f), onOrders)
                    DashboardMetric("Paradas hoje", stopsToday.toString(), panel, Modifier.weight(1f), onStops)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardMetric("Ordens pendentes", pendingOrders.toString(), panel, Modifier.weight(1f), onOrders)
                    DashboardMetric("Máquinas em operação", running.toString(), panel, Modifier.weight(1f), onMachines)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = panel),
                border = androidx.compose.foundation.BorderStroke(1.dp, border), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Indicadores de manutenção", color = ink, style = MaterialTheme.typography.titleLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Disponibilidade", color = muted, style = MaterialTheme.typography.labelMedium)
                            Text(String.format(java.util.Locale("pt", "BR"), "%.1f%%", availability),
                                color = ink, style = MaterialTheme.typography.titleLarge)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("MTBF", color = muted, style = MaterialTheme.typography.labelMedium)
                            Text(mtbf?.let { String.format(java.util.Locale("pt", "BR"), "%.1f h", it) } ?: "—",
                                color = ink, style = MaterialTheme.typography.titleLarge)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("MTTR", color = muted, style = MaterialTheme.typography.labelMedium)
                            Text(mttr?.let { String.format(java.util.Locale("pt", "BR"), "%.1f h", it) } ?: "—",
                                color = ink, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = panel),
                border = androidx.compose.foundation.BorderStroke(1.dp, border), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Acesso rápido", color = ink, style = MaterialTheme.typography.titleLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardQuickButton("＋ Registrar parada", Modifier.weight(1f), true, onNewStop)
                        DashboardQuickButton("＋ Nova ordem", Modifier.weight(1f), false, onNewOrder)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardQuickButton("＋ Apontamento", Modifier.weight(1f), false, onEntries)
                        DashboardQuickButton("▥ Relatórios", Modifier.weight(1f), false, onReports)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardQuickButton("Preventivas", Modifier.weight(1f), false, onPreventive)
                        DashboardQuickButton("Memória Técnica IA", Modifier.weight(1f), false, onAi)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCarouselCard(
    title: String, emptyMessage: String, count: Int, accent: Color,
    onClick: () -> Unit, content: @Composable () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FBFF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC9D8E7)),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(title, color = accent, style = MaterialTheme.typography.titleMedium)
                Text(count.toString(), color = accent, style = MaterialTheme.typography.labelLarge)
            }
            if (count == 0) {
                Text(emptyMessage, color = Color(0xFF687B8D), modifier = Modifier.padding(vertical = 8.dp))
            } else {
                content()
            }
        }
    }
}

@Composable
private fun DashboardMetric(label:String,value:String,color:Color,modifier:Modifier=Modifier,onClick:()->Unit) {
    Card(modifier=modifier.heightIn(min=88.dp).clickable(onClick=onClick),
        colors=CardDefaults.cardColors(containerColor=color),shape=MaterialTheme.shapes.extraLarge,
        border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFFC9D8E7)),
        elevation=CardDefaults.cardElevation(defaultElevation=2.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text(label,color=Color(0xFF687B8D),style=MaterialTheme.typography.labelMedium,minLines=2)
            Text(value,color=Color(0xFF17324D),style=MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun DashboardQuickButton(label:String,modifier:Modifier=Modifier,primary:Boolean=false,onClick:()->Unit) {
    Button(onClick=onClick,modifier=modifier.heightIn(min=54.dp),
        colors=ButtonDefaults.buttonColors(
            containerColor=if(primary) Color(0xFF2478B9) else Color(0xFFE6F0FA),
            contentColor=if(primary) Color.White else Color(0xFF173B5E)),
        shape=MaterialTheme.shapes.large,contentPadding=PaddingValues(horizontal=10.dp,vertical=10.dp)) {
        Text(label,style=MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun DashboardShortcut(icon:String,label:String,modifier:Modifier=Modifier,onClick:()->Unit) {
    Card(modifier.height(70.dp).clickable(onClick=onClick),colors=CardDefaults.cardColors(containerColor=Color(0xFF173451)),shape=MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxSize().padding(5.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Text(icon,color=Color.White,style=MaterialTheme.typography.titleLarge)
            Text(label,color=Color.White,style=MaterialTheme.typography.labelSmall,maxLines=2)
        }
    }
}

@Composable
private fun StatusLegend(label:String,color:Color) {
    Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        Surface(Modifier.size(13.dp),color=color,shape=MaterialTheme.shapes.small) {}
        Text(label,color=Color(0xFF17324D),style=MaterialTheme.typography.labelMedium)
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
        "Atual/Referência" to "Painel industrial azul-marinho, cartões de estado e indicadores — referência aprovada.",
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