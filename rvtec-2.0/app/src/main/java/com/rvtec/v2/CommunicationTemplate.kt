package com.rvtec.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class CommunicationTab(val label: String) {
    CHATS("Conversas"), GROUPS("Grupos"), TEAM("Equipe")
}

private data class CommunicationContact(
    val id: String,
    val name: String,
    val subtitle: String,
    val initials: String,
    val online: Boolean,
    val group: Boolean = false,
    val unread: Int = 0
)

private data class CommunicationMessage(
    val id: Long,
    val sender: String,
    val body: String,
    val time: String,
    val mine: Boolean
)

/**
 * RVTEC communication UI starter.
 * This is a local interactive prototype; it does not send or sync messages yet.
 * Connect the repository to Firebase before enabling production use.
 */
@Composable
fun CommunicationTemplateScreen(
    modifier: Modifier = Modifier
) {
    val contacts = remember {
        listOf(
            CommunicationContact("maintenance", "Manutenção", "Grupo geral da manutenção", "MN", true, true, 2),
            CommunicationContact("shift-b", "Turno B", "Equipe do turno 14:30–22:30", "B", true, true),
            CommunicationContact("marlon", "Marlon", "Eletricista • disponível", "MA", true, false, 1),
            CommunicationContact("erick", "Erick", "Eletricista • em atendimento", "ER", true),
            CommunicationContact("production", "Produção", "Canal com a produção", "PR", false, true)
        )
    }
    var selectedTab by remember { mutableStateOf(CommunicationTab.CHATS) }
    var selectedContact by remember { mutableStateOf<CommunicationContact?>(null) }
    var draft by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            CommunicationMessage(1, "Marlon", "Alguém consegue verificar a extrusora 108?", "14:02", false),
            CommunicationMessage(2, "Você", "Estou terminando uma OS e já verifico.", "14:04", true),
            CommunicationMessage(3, "Erick", "Registrei a ocorrência no RVTEC.", "14:06", false)
        )
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (selectedContact == null) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxWidth().background(Color(0xFF071A33)).padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Text("RVTEC • Comunicação", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Converse com a equipe de manutenção", color = Color(0xFFB9D0E8), fontSize = 12.sp)
                }
                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    CommunicationTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.label) }
                        )
                    }
                }
                val visibleContacts = when (selectedTab) {
                    CommunicationTab.CHATS -> contacts
                    CommunicationTab.GROUPS -> contacts.filter { it.group }
                    CommunicationTab.TEAM -> contacts.filter { !it.group }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(visibleContacts, key = { it.id }) { contact ->
                        Row(
                            Modifier.fillMaxWidth().clickable { selectedContact = contact }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(48.dp).background(Color(0xFF1B4774), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(contact.initials, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(contact.name, fontWeight = FontWeight.SemiBold)
                                    if (contact.online) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(Modifier.size(7.dp).background(Color(0xFF28B66A), CircleShape))
                                    }
                                }
                                Text(contact.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            if (contact.unread > 0) {
                                Box(
                                    Modifier.background(Color(0xFF16834B), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(contact.unread.toString(), color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF071A33)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("‹", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable { selectedContact = null }.padding(horizontal = 8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(selectedContact!!.name, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(selectedContact!!.subtitle, color = Color(0xFFB9D0E8), fontSize = 11.sp)
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start
                        ) {
                            Column(
                                Modifier.widthIn(max = 290.dp)
                                    .background(
                                        if (message.mine) Color(0xFF174D3A) else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                if (!message.mine) Text(message.sender, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(message.body)
                                Text(message.time, Modifier.align(Alignment.End), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Digite uma mensagem") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val text = draft.trim()
                            if (text.isNotEmpty()) {
                                messages.add(CommunicationMessage(
                                    System.currentTimeMillis(), "Você", text, "agora", true
                                ))
                                draft = ""
                            }
                        },
                        enabled = draft.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text("Enviar")
                    }
                }
                Text(
                    "Protótipo local: mensagens não são enviadas nem sincronizadas.",
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}
