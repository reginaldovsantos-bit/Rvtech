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

private val modules = listOf(
    "Máquinas", "Paradas", "Ordens de serviço", "Apontamentos",
    "Preventivas", "Estoque", "Memória técnica IA", "Indicadores e relatórios"
)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var selected by remember { mutableStateOf("Início") }
                Scaffold(topBar = { TopAppBar(title = { Text("RVTEC 2.0") }) }) { padding ->
                    Row(Modifier.fillMaxSize().padding(padding)) {
                        LazyColumn(Modifier.width(170.dp).fillMaxHeight().padding(8.dp)) {
                            item { Text("MENU", style = MaterialTheme.typography.labelLarge) }
                            items(listOf("Início") + modules) { item ->
                                TextButton(onClick = { selected = item }, modifier = Modifier.fillMaxWidth()) {
                                    Text(item)
                                }
                            }
                        }
                        Column(Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                            Text(selected, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (selected == "Início")
                                    "Base limpa RVTEC 2.0. Os módulos serão liberados após implementação e testes."
                                else
                                    "Módulo planejado. Nenhum dado da versão 1.8.5 foi alterado."
                            )
                        }
                    }
                }
            }
        }
    }
}
