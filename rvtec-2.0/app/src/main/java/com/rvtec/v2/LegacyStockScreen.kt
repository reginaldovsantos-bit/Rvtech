package com.rvtec.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rvtec.v2.data.LegacyModuleRepository
import com.rvtec.v2.domain.StockItem
import com.rvtec.v2.domain.StockMovementType
import java.util.UUID

@Composable
fun LegacyStockSection(repository: LegacyModuleRepository, revision: Int, onChanged: () -> Unit) {
    val stock = remember(revision) { repository.listStock() }
    val movements = remember(revision) { repository.listMovements() }
    var showNewItem by remember { mutableStateOf(false) }
    var movementItem by remember { mutableStateOf<StockItem?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Estoque", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showNewItem = true }) { Text("Nova peça") }
        }
        if (stock.isEmpty()) Text("Nenhuma peça cadastrada.")
        stock.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleSmall)
                    Text("Código: ${item.code.ifBlank { "—" }} • Saldo: ${item.quantity} ${item.unit}")
                    Text("Mínimo: ${item.minimumQuantity} • Local: ${item.location.ifBlank { "—" }}")
                    if (item.quantity <= item.minimumQuantity) {
                        Text("ATENÇÃO: estoque no mínimo ou abaixo", color = MaterialTheme.colorScheme.error)
                    }
                    OutlinedButton(onClick = { movementItem = item }) { Text("Movimentar estoque") }
                }
            }
        }
        Text("Últimas movimentações", style = MaterialTheme.typography.titleSmall)
        if (movements.isEmpty()) Text("Nenhuma movimentação registrada.")
        movements.take(20).forEach { movement ->
            val itemName = stock.firstOrNull { it.id == movement.itemId }?.name ?: "Peça removida"
            ListItem(
                headlineContent = { Text(itemName) },
                supportingContent = { Text("${movement.type.name} • ${movement.quantity} • ${movement.reason} • ${movement.responsible}") }
            )
        }
    }
    if (showNewItem) {
        NewStockItemDialog(
            onDismiss = { showNewItem = false },
            onSave = { code, name, description, unit, quantity, minimum, location ->
                repository.addStock(StockItem(UUID.randomUUID().toString(), code, name, description, unit, quantity, minimum, location))
                showNewItem = false
                onChanged()
            }
        )
    }
    movementItem?.let { item ->
        StockMovementDialog(
            item = item,
            onDismiss = { movementItem = null },
            onSave = { type, quantity, reason, responsible, workOrder ->
                repository.moveStock(item.id, type, quantity, reason, workOrder.ifBlank { null }, responsible)
                movementItem = null
                onChanged()
            }
        )
    }
}

@Composable
private fun NewStockItemDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Double, Double, String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("un") }
    var quantity by remember { mutableStateOf("0") }
    var minimum by remember { mutableStateOf("0") }
    var location by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cadastrar peça") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome da peça") })
                OutlinedTextField(code, { code = it }, label = { Text("Código") })
                OutlinedTextField(description, { description = it }, label = { Text("Descrição") })
                OutlinedTextField(unit, { unit = it }, label = { Text("Unidade") })
                OutlinedTextField(quantity, { quantity = it }, label = { Text("Saldo inicial") })
                OutlinedTextField(minimum, { minimum = it }, label = { Text("Estoque mínimo") })
                OutlinedTextField(location, { location = it }, label = { Text("Localização") })
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && (quantity.toDoubleOrNull() ?: -1.0) >= 0 && (minimum.toDoubleOrNull() ?: -1.0) >= 0,
                onClick = { onSave(code.trim(), name.trim(), description.trim(), unit.trim().ifBlank { "un" }, quantity.toDouble(), minimum.toDouble(), location.trim()) }
            ) { Text("Salvar peça") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun StockMovementDialog(
    item: StockItem,
    onDismiss: () -> Unit,
    onSave: (StockMovementType, Double, String, String, String) -> Unit
) {
    var type by remember { mutableStateOf(StockMovementType.ENTRY) }
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var responsible by remember { mutableStateOf("") }
    var workOrder by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Movimentar: ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Saldo atual: ${item.quantity} ${item.unit}")
                StockMovementType.values().forEach { option ->
                    Row {
                        RadioButton(selected = type == option, onClick = { type = option })
                        Text(
                            when (option) {
                                StockMovementType.ENTRY -> "Entrada"
                                StockMovementType.EXIT -> "Saída"
                                StockMovementType.ADJUSTMENT -> "Ajuste de saldo"
                            },
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
                OutlinedTextField(quantity, { quantity = it }, label = { Text(if (type == StockMovementType.ADJUSTMENT) "Novo saldo" else "Quantidade") })
                OutlinedTextField(reason, { reason = it }, label = { Text("Motivo obrigatório") })
                OutlinedTextField(responsible, { responsible = it }, label = { Text("Responsável") })
                OutlinedTextField(workOrder, { workOrder = it }, label = { Text("Número da OS (opcional)") })
            }
        },
        confirmButton = {
            Button(
                enabled = (quantity.toDoubleOrNull() ?: 0.0) > 0 && reason.isNotBlank(),
                onClick = { onSave(type, quantity.toDouble(), reason.trim(), responsible.trim(), workOrder.trim()) }
            ) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
