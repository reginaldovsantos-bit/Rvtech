package com.rvtec.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rvtec.v2.data.LegacyModuleRepository
import com.rvtec.v2.domain.RvtecSettings

@Composable
fun LegacySettingsScreen(repository: LegacyModuleRepository, onSaved: () -> Unit) {
    val initial = remember { repository.getSettings() }
    var company by remember { mutableStateOf(initial.companyName) }
    var s1Start by remember { mutableStateOf(initial.shift1Start) }
    var s1End by remember { mutableStateOf(initial.shift1End) }
    var s2Start by remember { mutableStateOf(initial.shift2Start) }
    var s2End by remember { mutableStateOf(initial.shift2End) }
    var s3Start by remember { mutableStateOf(initial.shift3Start) }
    var s3End by remember { mutableStateOf(initial.shift3End) }
    var dark by remember { mutableStateOf(initial.darkTheme) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Configurações", style = MaterialTheme.typography.headlineSmall)
        Text("Dados da empresa e horários dos turnos")
        OutlinedTextField(company, { company = it }, label = { Text("Nome da empresa") }, modifier = Modifier.fillMaxWidth())
        Text("Turno 1", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(s1Start, { s1Start = it }, label = { Text("Início HH:mm") }, modifier = Modifier.weight(1f))
            OutlinedTextField(s1End, { s1End = it }, label = { Text("Fim HH:mm") }, modifier = Modifier.weight(1f))
        }
        Text("Turno 2", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(s2Start, { s2Start = it }, label = { Text("Início HH:mm") }, modifier = Modifier.weight(1f))
            OutlinedTextField(s2End, { s2End = it }, label = { Text("Fim HH:mm") }, modifier = Modifier.weight(1f))
        }
        Text("Turno 3", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(s3Start, { s3Start = it }, label = { Text("Início HH:mm") }, modifier = Modifier.weight(1f))
            OutlinedTextField(s3End, { s3End = it }, label = { Text("Fim HH:mm") }, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Preferir tema escuro", modifier = Modifier.weight(1f))
            Switch(checked = dark, onCheckedChange = { dark = it })
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val times = listOf(s1Start, s1End, s2Start, s2End, s3Start, s3End)
                val valid = times.all { it.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$")) }
                if (!valid) {
                    message = "Use horários válidos no formato HH:mm."
                } else {
                    repository.saveSettings(RvtecSettings(
                        companyName = company.trim(),
                        shift1Start = s1Start, shift1End = s1End,
                        shift2Start = s2Start, shift2End = s2End,
                        shift3Start = s3Start, shift3End = s3End,
                        darkTheme = dark,
                        enabledScreens = initial.enabledScreens
                    ))
                    message = null
                    onSaved()
                }
            }
        ) { Text("Salvar configurações") }
        Text("Os horários ficam salvos neste aparelho. A aplicação automática dos turnos e do tema em todas as telas ainda precisa ser integrada.", style = MaterialTheme.typography.bodySmall)
    }
}
