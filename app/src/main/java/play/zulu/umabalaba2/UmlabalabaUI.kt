package play.zulu.umabalaba2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UmlabalabaScreen(gameState: GameState) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B120B))
            .padding(12.dp)
    ) {

        // ===== TOP BAR =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = null,
                    tint = Color(0xFFD6B37A)
                )
            }

            Text(
                text = "PlayUMLABALABA",
                color = Color(0xFFD6B37A),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(onClick = { }) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        tint = Color(0xFFD6B37A)
                    )
                }

                IconButton(onClick = { }) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFFD6B37A)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== PLAYER INFO (Opponent) =====
        val opponent = if (gameState.currentPlayer == Player.PLAYER_1) Player.PLAYER_2 else Player.PLAYER_1
        PlayerPanel(
            name = gameState.getPlayerName(opponent),
            pieces = if (gameState.phase == GamePhase.PLACEMENT) 
                gameState.piecesToPlace[opponent] ?: 0 
                else gameState.nodes.count { it.occupant == opponent },
            mills = 0, // Mill tracking not fully implemented in GameState yet
            isTop = true,
            pieceRes = gameState.getPlayerPieceRes(opponent),
            textColor = gameState.getPlayerTextColor(opponent)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ===== MAIN CONTENT =====
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
                modifier = Modifier.width(120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SideCard(
                    title = "TURN",
                    content = if (gameState.winner != null) "GAME OVER" else gameState.getPlayerName(gameState.currentPlayer).uppercase()
                )

                SideCard(
                    title = "PHASE",
                    content = if (gameState.mustRemovePiece) "SHOOT" else gameState.phase.name
                )

                SideCard(
                    title = "MILLS",
                    content = "P1: 0\nP2: 0" // Placeholder
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== PLAYER INFO (Current Player) =====
        PlayerPanel(
            name = gameState.getPlayerName(gameState.currentPlayer),
            pieces = if (gameState.phase == GamePhase.PLACEMENT) 
                gameState.piecesToPlace[gameState.currentPlayer] ?: 0 
                else gameState.nodes.count { it.occupant == gameState.currentPlayer },
            mills = 0,
            isTop = false,
            pieceRes = gameState.getPlayerPieceRes(gameState.currentPlayer),
            textColor = gameState.getPlayerTextColor(gameState.currentPlayer)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ===== ACTION BUTTONS =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GameButton("NEW") { gameState.showNameDialog = true }
            GameButton("UNDO") { /* Not implemented */ }
            GameButton("HINT") { /* Not implemented */ }
            GameButton("PASS") { /* Not implemented */ }
            GameButton("RESIGN") { /* Not implemented */ }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ===== BOTTOM INFO =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BottomInfoCard(
                modifier = Modifier.weight(1f),
                title = "SCORE",
                content = "Winner: ${gameState.winner?.name ?: "None"}"
            )

            BottomInfoCard(
                modifier = Modifier.weight(1f),
                title = "HOW TO WIN",
                content = "Reduce opponent to 2 pieces"
            )

            BottomInfoCard(
                modifier = Modifier.weight(1f),
                title = "GAME INFO",
                content = "Local Match"
            )
        }
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
        
        // We need to match the 7x7 grid coordinates (0 to 6)
        // So step is size / 6
        val step = size.width / 6

        // Outer Square
        drawRect(
            color = boardColor,
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(size.width, size.height),
            style = Stroke(stroke)
        )

        // Middle Square
        drawRect(
            color = boardColor,
            topLeft = Offset(step, step),
            size = androidx.compose.ui.geometry.Size(size.width - 2 * step, size.height - 2 * step),
            style = Stroke(stroke)
        )

        // Inner Square
        drawRect(
            color = boardColor,
            topLeft = Offset(2 * step, 2 * step),
            size = androidx.compose.ui.geometry.Size(size.width - 4 * step, size.height - 4 * step),
            style = Stroke(stroke)
        )

        // Connecting lines (Crosses)
        // Top
        drawLine(color = boardColor, start = Offset(3 * step, 0f), end = Offset(3 * step, 2 * step), strokeWidth = stroke)
        // Bottom
        drawLine(color = boardColor, start = Offset(3 * step, 4 * step), end = Offset(3 * step, 6 * step), strokeWidth = stroke)
        // Left
        drawLine(color = boardColor, start = Offset(0f, 3 * step), end = Offset(2 * step, 3 * step), strokeWidth = stroke)
        // Right
        drawLine(color = boardColor, start = Offset(4 * step, 3 * step), end = Offset(6 * step, 3 * step), strokeWidth = stroke)
        
        // Diagonals (Typical in Umabalaba/Morabaraba)
        // Top-Left
        drawLine(color = boardColor, start = Offset(0f, 0f), end = Offset(2 * step, 2 * step), strokeWidth = stroke)
        // Top-Right
        drawLine(color = boardColor, start = Offset(6 * step, 0f), end = Offset(4 * step, 2 * step), strokeWidth = stroke)
        // Bottom-Left
        drawLine(color = boardColor, start = Offset(0f, 6 * step), end = Offset(2 * step, 4 * step), strokeWidth = stroke)
        // Bottom-Right
        drawLine(color = boardColor, start = Offset(6 * step, 6 * step), end = Offset(4 * step, 4 * step), strokeWidth = stroke)
    }
}

@Composable
fun PlayerPanel(
    name: String,
    pieces: Int,
    mills: Int,
    isTop: Boolean,
    pieceRes: Int,
    textColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1B12)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = pieceRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = name,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Text(
                    text = "Pieces: $pieces",
                    color = Color.White
                )

                Text(
                    text = "Mills: $mills",
                    color = Color(0xFFFFC857)
                )
            }
        }
    }
}

@Composable
fun SideCard(
    title: String,
    content: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1B12)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFFE7C58A),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = content,
                color = Color.White
            )
        }
    }
}

@Composable
fun BottomInfoCard(
    modifier: Modifier = Modifier,
    title: String,
    content: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A1B12)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFFE7C58A),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = content,
                color = Color.White
            )
        }
    }
}

@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF5A3822)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFE7C58A)
        )
    }
}
