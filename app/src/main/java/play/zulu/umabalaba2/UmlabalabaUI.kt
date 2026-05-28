package play.zulu.umabalaba2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun UmlabalabaScreen(gameState: GameState) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (gameState.showNameDialog) {
        NameDialog(
            initialP1 = gameState.player1Name,
            initialP2 = gameState.player2Name,
            onConfirm = { p1: String, p2: String ->
                gameState.player1Name = p1
                gameState.player2Name = p2
                gameState.resetGame()
                gameState.showNameDialog = false
            },
            onDismiss = { gameState.showNameDialog = false }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1B120B),
                drawerShape = RoundedCornerShape(0.dp)
            ) {
                SidebarContent(gameState)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1B120B))
                .padding(12.dp)
        ) {
            // ===== TOP SECTION: CHAT BOX & MENU =====
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFFD6B37A))
                }
                ChatBox(modifier = Modifier.weight(1f).height(110.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ===== MAIN CONTENT: BOARD + SIDE PANEL =====
            Row(
                modifier = Modifier.weight(1f)
            ) {
                // ===== BOARD =====
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            Color(0xFF8B5E3C),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    UmlabalabaBoard(
                        modifier = Modifier.aspectRatio(1f).fillMaxSize()
                    )
                    
                    // Render nodes on top of the board
                    BoxWithConstraints(modifier = Modifier.aspectRatio(1f).fillMaxSize()) {
                        val boardSize = maxWidth
                        val step = boardSize / 6
                        
                        gameState.nodes.forEach { node ->
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = step * node.x - 20.dp,
                                        y = step * node.y - 20.dp
                                    )
                            ) {
                                BoardNode(
                                    node = node, 
                                    gameState = gameState,
                                    isSelected = gameState.selectedNodeId == node.id
                                ) {
                                    gameState.handleNodeClick(node.id)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // ===== SIDE PANEL =====
                Column(
                    modifier = Modifier.width(110.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SideCard(
                        title = "PHASE",
                        content = if (gameState.mustRemovePiece) "SHOOT" else gameState.phase.name
                    )

                    SideCard(
                        title = "TURN",
                        content = if (gameState.winner != null) "OVER" else gameState.getPlayerName(gameState.currentPlayer).uppercase()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ===== PLAYER INDICATORS (Row of 2) =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val p1Active = gameState.currentPlayer == Player.PLAYER_1
                val p2Active = gameState.currentPlayer == Player.PLAYER_2

                PlayerPanel(
                    modifier = Modifier.weight(1f),
                    name = gameState.player1Name,
                    pieces = if (gameState.phase == GamePhase.PLACEMENT) 
                        gameState.piecesToPlace[Player.PLAYER_1] ?: 0 
                        else gameState.nodes.count { it.occupant == Player.PLAYER_1 },
                    mills = 0,
                    isActive = p1Active,
                    pieceRes = gameState.getPlayerPieceRes(Player.PLAYER_1),
                    textColor = gameState.getPlayerTextColor(Player.PLAYER_1)
                )

                PlayerPanel(
                    modifier = Modifier.weight(1f),
                    name = gameState.player2Name,
                    pieces = if (gameState.phase == GamePhase.PLACEMENT) 
                        gameState.piecesToPlace[Player.PLAYER_2] ?: 0 
                        else gameState.nodes.count { it.occupant == Player.PLAYER_2 },
                    mills = 0,
                    isActive = p2Active,
                    pieceRes = gameState.getPlayerPieceRes(Player.PLAYER_2),
                    textColor = gameState.getPlayerTextColor(Player.PLAYER_2)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ===== ACTION BUTTONS =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GameButton("NEW") { gameState.showNameDialog = true }
                GameButton("UNDO") { }
                GameButton("PASS") { }
                GameButton("RESIGN") { }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ===== BOTTOM INFO CARDS =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BottomInfoCard(
                    modifier = Modifier.weight(1f),
                    title = "MODE",
                    content = gameState.gameMode.name
                )
                BottomInfoCard(
                    modifier = Modifier.weight(1f),
                    title = "STATUS",
                    content = gameState.connectionStatus
                )
            }
        }
    }
}

@Composable
fun SidebarContent(gameState: GameState) {
    var expandedMode by remember { mutableStateOf<GameMode?>(null) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .padding(24.dp)
    ) {
        Text(
            "GAME MODES",
            color = Color(0xFFD6B37A),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        ModeItem(
            title = "LOCAL PLAY",
            icon = Icons.Default.Person,
            isSelected = gameState.gameMode == GameMode.LOCAL,
            onClick = {
                gameState.gameMode = GameMode.LOCAL
                gameState.connectionStatus = "Local Game"
                expandedMode = null
            }
        )

        ModeItem(
            title = "BLUETOOTH",
            icon = Icons.Default.Bluetooth,
            isSelected = gameState.gameMode == GameMode.BLUETOOTH,
            onClick = {
                expandedMode = if (expandedMode == GameMode.BLUETOOTH) null else GameMode.BLUETOOTH
            }
        )
        AnimatedVisibility(visible = expandedMode == GameMode.BLUETOOTH) {
            Column(modifier = Modifier.padding(start = 32.dp)) {
                SubModeItem("Host Game") {
                    gameState.gameMode = GameMode.BLUETOOTH
                    gameState.isHost = true
                    gameState.bluetoothManager.hostGame()
                }
                SubModeItem("Join Game") {
                    gameState.gameMode = GameMode.BLUETOOTH
                    gameState.isHost = false
                    gameState.bluetoothManager.joinGame()
                }
            }
        }

        ModeItem(
            title = "WIFI DIRECT",
            icon = Icons.Default.Wifi,
            isSelected = gameState.gameMode == GameMode.WIFI,
            onClick = {
                expandedMode = if (expandedMode == GameMode.WIFI) null else GameMode.WIFI
            }
        )
        AnimatedVisibility(visible = expandedMode == GameMode.WIFI) {
            Column(modifier = Modifier.padding(start = 32.dp)) {
                SubModeItem("Host Game") {
                    gameState.gameMode = GameMode.WIFI
                    gameState.isHost = true
                    gameState.wifiManager.hostGame()
                }
                SubModeItem("Join Game") {
                    gameState.gameMode = GameMode.WIFI
                    gameState.isHost = false
                    gameState.wifiManager.joinGame()
                }
            }
        }

        ModeItem(
            title = "ONLINE PLAY",
            icon = Icons.Default.Public,
            isSelected = gameState.gameMode == GameMode.ONLINE,
            onClick = {
                gameState.gameMode = GameMode.ONLINE
                gameState.onlineManager.connect()
                expandedMode = null
            }
        )
    }
}

@Composable
fun ModeItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF5A3822) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else Color(0xFFD6B37A))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SubModeItem(title: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowRight, contentDescription = null, tint = Color(0xFFD6B37A))
            Text(title, color = Color.LightGray)
        }
    }
}

@Composable
fun ChatBox(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B12)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("GAME CHAT", color = Color(0xFFD6B37A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            ChatMessage("System", "Match started!", "12:05")
            ChatMessage("Opponent", "Let's play!", "12:06")
        }
    }
}

@Composable
fun ChatMessage(sender: String, message: String, time: String) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text("[$time] ", color = Color.Gray, fontSize = 11.sp)
        Text("$sender: ", color = Color(0xFFE7C58A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(message, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun UmlabalabaBoard(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        val boardColor = Color(0xFF2E1D10)
        val stroke = 6f
        val step = size.width / 6

        // Draw Squares
        for (i in 0..2) {
            val offset = i * step
            val s = size.width - 2 * offset
            drawRect(
                color = boardColor,
                topLeft = Offset(offset, offset),
                size = androidx.compose.ui.geometry.Size(s, s),
                style = Stroke(stroke)
            )
        }

        // Connecting lines
        drawLine(color = boardColor, start = Offset(3 * step, 0f), end = Offset(3 * step, 2 * step), strokeWidth = stroke)
        drawLine(color = boardColor, start = Offset(3 * step, 4 * step), end = Offset(3 * step, 6 * step), strokeWidth = stroke)
        drawLine(color = boardColor, start = Offset(0f, 3 * step), end = Offset(2 * step, 3 * step), strokeWidth = stroke)
        drawLine(color = boardColor, start = Offset(4 * step, 3 * step), end = Offset(6 * step, 3 * step), strokeWidth = stroke)
        
        // Diagonals
        drawLine(color = boardColor, start = Offset(0f, 0f), end = Offset(2 * step, 2 * step), strokeWidth = stroke)
        drawLine(color = boardColor, start = Offset(6 * step, 0f), end = Offset(4 * step, 2 * step), strokeWidth = stroke)
        drawLine(color = boardColor, start = Offset(0f, 6 * step), end = Offset(2 * step, 4 * step), strokeWidth = stroke)
        drawLine(color = boardColor, start = Offset(6 * step, 6 * step), end = Offset(4 * step, 4 * step), strokeWidth = stroke)
    }
}

@Composable
fun PlayerPanel(
    modifier: Modifier = Modifier,
    name: String,
    pieces: Int,
    mills: Int,
    isActive: Boolean,
    pieceRes: Int,
    textColor: Color
) {
    val borderColor = if (isActive) Color(0xFFFFC857) else Color.Transparent
    
    Card(
        modifier = modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B12)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = pieceRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = name, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(text = "Cows: $pieces", color = Color.White, fontSize = 12.sp)
                if (isActive) {
                    Text(text = "YOUR TURN", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun SideCard(title: String, content: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B12))) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = title, color = Color(0xFFE7C58A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text = content, color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
fun BottomInfoCard(modifier: Modifier = Modifier, title: String, content: String) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B12))) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = title, color = Color(0xFFE7C58A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(text = content, color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
fun GameButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A3822)),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = Color(0xFFE7C58A), fontSize = 12.sp)
    }
}
