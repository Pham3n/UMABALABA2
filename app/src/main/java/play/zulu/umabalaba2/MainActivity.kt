package play.zulu.umabalaba2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import play.zulu.umabalaba2.ui.theme.UMABALABA2Theme

// Data class representing a single position (node) on the Umabalaba board.
// Node properties are immutable; we update state by replacing Node instances in the list.
data class Node(
    val id: Int, // Unique identifier (0-23)
    val occupant: Player? = null, // Which player owns the cow here, if any
    val connections: List<Int>, // Adjacent nodes for movement and mill detection
    val x: Int, // Horizontal grid coordinate (0 to 6)
    val y: Int  // Vertical grid coordinate (0 to 6)
)

// Enum defining the two players in the Umabalaba game
enum class Player {
    PLAYER_1, // Red Cows
    PLAYER_2  // Blue Cows
}

// Enum defining the phases of the game
enum class GamePhase {
    PLACEMENT, // Phase 1: Placing 12 cows each
    MOVEMENT   // Phase 2: Moving cows on the board
}

// Class to manage the state and logic of the Umabalaba game
class GameState {
    // Observable list of nodes to trigger UI recomposition when a node changes
    val nodes = mutableStateListOf<Node>()
    
    // Observable current player
    var currentPlayer by mutableStateOf(Player.PLAYER_1)

    // Observable player names
    var player1Name by mutableStateOf("Player 1")
    var player2Name by mutableStateOf("Player 2")
    
    // Observable state to track if a player needs to remove an opponent's piece
    var mustRemovePiece by mutableStateOf(false)

    // Observable current game phase
    var phase by mutableStateOf(GamePhase.PLACEMENT)

    // Tracks the currently selected node for movement
    var selectedNodeId by mutableStateOf<Int?>(null)

    // Observable winner
    var winner by mutableStateOf<Player?>(null)
    
    // Observable piece counts for the placement phase
    var piecesToPlace by mutableStateOf(mapOf(
        Player.PLAYER_1 to 12,
        Player.PLAYER_2 to 12
    ))

    // Define all possible mills (triplets of node IDs)
    private val MILLS = listOf(
        // Outer Square
        listOf(0, 1, 2), listOf(2, 3, 4), listOf(4, 5, 6), listOf(6, 7, 0),
        // Middle Square
        listOf(8, 9, 10), listOf(10, 11, 12), listOf(12, 13, 14), listOf(14, 15, 8),
        // Inner Square
        listOf(16, 17, 18), listOf(18, 19, 20), listOf(20, 21, 22), listOf(22, 23, 16),
        // Cross lines (top, right, bottom, left)
        listOf(1, 9, 17), listOf(3, 11, 19), listOf(5, 13, 21), listOf(7, 15, 23),
        // Diagonals (top-left, top-right, bottom-right, bottom-left)
        listOf(0, 8, 16), listOf(2, 10, 18), listOf(4, 12, 20), listOf(6, 14, 22)
    )

    init {
        initializeBoard()
    }

    private fun initializeBoard() {
        // (same coordinates and adjacency as before)
        val coords = listOf(
            Pair(0, 0), Pair(3, 0), Pair(6, 0), Pair(6, 3), Pair(6, 6), Pair(3, 6), Pair(0, 6), Pair(0, 3),
            Pair(1, 1), Pair(3, 1), Pair(5, 1), Pair(5, 3), Pair(5, 5), Pair(3, 5), Pair(1, 5), Pair(1, 3),
            Pair(2, 2), Pair(3, 2), Pair(4, 2), Pair(4, 3), Pair(4, 4), Pair(3, 4), Pair(2, 4), Pair(2, 3)
        )

        val adj = mapOf(
            0 to listOf(1, 7, 8), 1 to listOf(0, 2, 9), 2 to listOf(1, 3, 10), 3 to listOf(2, 4, 11),
            4 to listOf(3, 5, 12), 5 to listOf(4, 6, 13), 6 to listOf(5, 7, 14), 7 to listOf(6, 0, 15),
            8 to listOf(0, 9, 15, 16), 9 to listOf(1, 8, 10, 17), 10 to listOf(2, 9, 11, 18), 11 to listOf(3, 10, 12, 19),
            12 to listOf(4, 11, 13, 20), 13 to listOf(5, 12, 14, 21), 14 to listOf(6, 13, 15, 22), 15 to listOf(7, 14, 8, 23),
            16 to listOf(8, 17, 23), 17 to listOf(9, 16, 18), 18 to listOf(10, 17, 19), 19 to listOf(11, 18, 20),
            20 to listOf(12, 19, 21), 21 to listOf(13, 20, 22), 22 to listOf(14, 21, 23), 23 to listOf(15, 22, 16)
        )

        repeat(24) { i ->
            nodes.add(Node(id = i, connections = adj[i]!!, x = coords[i].first, y = coords[i].second))
        }
    }

    // Handles node clicks based on the current game phase
    fun handleNodeClick(nodeId: Int) {
        if (winner != null) return

        if (mustRemovePiece) {
            removePiece(nodeId)
            return
        }

        if (phase == GamePhase.PLACEMENT) {
            placePiece(nodeId)
        } else {
            handleMovement(nodeId)
        }
    }

    private fun placePiece(nodeId: Int): Boolean {
        val index = nodes.indexOfFirst { it.id == nodeId }
        if (index == -1 || nodes[index].occupant != null) return false

        val remaining = piecesToPlace[currentPlayer] ?: 0
        if (remaining <= 0) return false

        // Place the piece
        nodes[index] = nodes[index].copy(occupant = currentPlayer)
        piecesToPlace = piecesToPlace.toMutableMap().apply { put(currentPlayer, remaining - 1) }

        // Check if all pieces are placed to transition phase
        if (piecesToPlace.values.all { it == 0 }) {
            phase = GamePhase.MOVEMENT
        }

        // Check for new mill
        if (isNodeInMill(nodeId, currentPlayer)) {
            mustRemovePiece = true // Trigger removal phase
        } else {
            switchTurn()
        }
        return true
    }

    private fun handleMovement(nodeId: Int) {
        val node = nodes.find { it.id == nodeId } ?: return
        
        if (selectedNodeId == null) {
            // First click: Select one of your own pieces
            if (node.occupant == currentPlayer) {
                selectedNodeId = nodeId
            }
        } else {
            // Second click: Try to move or change selection
            if (node.occupant == currentPlayer) {
                // Change selection
                selectedNodeId = nodeId
            } else if (node.occupant == null) {
                // Try to move to the empty node
                if (movePiece(selectedNodeId!!, nodeId)) {
                    selectedNodeId = null
                }
            }
        }
    }

    private fun movePiece(fromId: Int, toId: Int): Boolean {
        val fromIndex = nodes.indexOfFirst { it.id == fromId }
        val toIndex = nodes.indexOfFirst { it.id == toId }
        val fromNode = nodes[fromIndex]
        val playerCows = nodes.count { it.occupant == currentPlayer }

        // Rule: Can move to adjacent node OR fly if you have exactly 3 cows left
        val isAdjacent = fromNode.connections.contains(toId)
        val canFly = playerCows == 3

        if (isAdjacent || canFly) {
            // Perform movement
            nodes[fromIndex] = nodes[fromIndex].copy(occupant = null)
            nodes[toIndex] = nodes[toIndex].copy(occupant = currentPlayer)

            // Check for new mill at the destination
            if (isNodeInMill(toId, currentPlayer)) {
                mustRemovePiece = true
            } else {
                switchTurn()
            }
            return true
        }
        return false
    }

    private fun removePiece(nodeId: Int): Boolean {
        val index = nodes.indexOfFirst { it.id == nodeId }
        if (index == -1) return false
        val node = nodes[index]

        // Can only remove opponent's piece
        val opponent = if (currentPlayer == Player.PLAYER_1) Player.PLAYER_2 else Player.PLAYER_1
        if (node.occupant != opponent) return false

        // Basic rule: You can't remove a piece that's in a mill unless all are in mills
        // (For simplicity in this version, we allow any opponent piece removal)
        nodes[index] = node.copy(occupant = null)
        mustRemovePiece = false
        
        checkWinCondition()
        if (winner == null) {
            switchTurn()
        }
        return true
    }

    private fun checkWinCondition() {
        val p1Total = (piecesToPlace[Player.PLAYER_1] ?: 0) + nodes.count { it.occupant == Player.PLAYER_1 }
        val p2Total = (piecesToPlace[Player.PLAYER_2] ?: 0) + nodes.count { it.occupant == Player.PLAYER_2 }

        if (p2Total < 3) {
            winner = Player.PLAYER_1
        } else if (p1Total < 3) {
            winner = Player.PLAYER_2
        }
    }

    fun resetGame() {
        nodes.clear()
        initializeBoard()
        currentPlayer = Player.PLAYER_1
        mustRemovePiece = false
        phase = GamePhase.PLACEMENT
        selectedNodeId = null
        piecesToPlace = mapOf(
            Player.PLAYER_1 to 12,
            Player.PLAYER_2 to 12
        )
        winner = null
    }

    fun getPlayerName(player: Player): String {
        return if (player == Player.PLAYER_1) player1Name else player2Name
    }

    fun getPlayerColor(player: Player): Color {
        return if (player == Player.PLAYER_1) Color.Red else Color.Blue
    }

    private fun isNodeInMill(nodeId: Int, player: Player): Boolean {
        // Find all mills that include this node
        return MILLS.filter { it.contains(nodeId) }.any { mill ->
            mill.all { id -> nodes.find { it.id == id }?.occupant == player }
        }
    }

    private fun switchTurn() {
        currentPlayer = if (currentPlayer == Player.PLAYER_1) Player.PLAYER_2 else Player.PLAYER_1
    }
}

// Visual representation of a single node
@Composable
fun BoardNode(node: Node, isSelected: Boolean, onClick: () -> Unit) {
    val color = when (node.occupant) {
        Player.PLAYER_1 -> Color.Red
        Player.PLAYER_2 -> Color.Blue
        null -> Color.LightGray
    }

    // Outer box for the "selection" ring
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp) // Slightly larger to allow for selection ring
            .clickable { onClick() }
    ) {
        if (isSelected) {
            // Selection ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Yellow, shape = CircleShape)
            )
        }
        
        // The actual cow/hole
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color, shape = CircleShape)
        )
    }
}

// Main game screen containing the board and UI info
@Composable
fun GameScreen(gameState: GameState) {
    var showNameDialog by remember { mutableStateOf(false) }

    if (showNameDialog) {
        NameDialog(
            initialP1 = gameState.player1Name,
            initialP2 = gameState.player2Name,
            onConfirm = { p1: String, p2: String ->
                gameState.player1Name = p1
                gameState.player2Name = p2
                gameState.resetGame()
                showNameDialog = false
            },
            onDismiss = { showNameDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Player Turn and Status Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            val currentPlayerName = gameState.getPlayerName(gameState.currentPlayer)
            val currentPlayerColor = gameState.getPlayerColor(gameState.currentPlayer)

            when {
                gameState.winner != null -> {
                    val winnerName = gameState.getPlayerName(gameState.winner!!)
                    val winnerColor = gameState.getPlayerColor(gameState.winner!!)
                    Text("GAME OVER: ", fontWeight = FontWeight.Bold)
                    Text(winnerName, color = winnerColor, fontWeight = FontWeight.Bold)
                    Text(" WINS!", fontWeight = FontWeight.Bold)
                }
                gameState.mustRemovePiece -> {
                    Text("MILL! ", color = Color.Magenta, fontWeight = FontWeight.Bold)
                    Text(currentPlayerName, color = currentPlayerColor, fontWeight = FontWeight.Bold)
                    Text(" shoot a cow!")
                }
                gameState.phase == GamePhase.PLACEMENT -> {
                    Text("PLACEMENT: ")
                    Text(currentPlayerName, color = currentPlayerColor, fontWeight = FontWeight.Bold)
                }
                else -> {
                    Text("MOVEMENT: ")
                    Text(currentPlayerName, color = currentPlayerColor, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row {
            Text(gameState.player1Name, color = Color.Red)
            Text(" Cows: ")
            if (gameState.phase == GamePhase.PLACEMENT) {
                Text("${gameState.piecesToPlace[Player.PLAYER_1]}")
            } else {
                Text("${gameState.nodes.count { it.occupant == Player.PLAYER_1 }}")
            }
            Text(" | ")
            Text(gameState.player2Name, color = Color.Blue)
            Text(" Cows: ")
            if (gameState.phase == GamePhase.PLACEMENT) {
                Text("${gameState.piecesToPlace[Player.PLAYER_2]}")
            } else {
                Text("${gameState.nodes.count { it.occupant == Player.PLAYER_2 }}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showNameDialog = true }) {
            Text(if (gameState.winner != null) "Play Again" else "Reset Game")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nested Grid Board Layout
        BoxWithConstraints(
            modifier = Modifier
                .aspectRatio(1f) // Ensure the board is square
                .fillMaxWidth()
                .padding(32.dp) // Leave space for nodes at the edges
        ) {
            val boardSize = maxWidth
            val step = boardSize / 6 // Calculate distance between points on the 7x7 grid

            // Render each node at its specific grid coordinate
            gameState.nodes.forEach { node ->
                Box(
                    modifier = Modifier
                        .offset(
                            x = step * node.x - 20.dp, // Center the 40.dp node on the point
                            y = step * node.y - 20.dp
                        )
                ) {
                    BoardNode(
                        node = node, 
                        isSelected = gameState.selectedNodeId == node.id
                    ) {
                        gameState.handleNodeClick(node.id)
                    }
                }
            }
        }
    }
}

@Composable
fun NameDialog(
    initialP1: String,
    initialP2: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var p1Name by remember { mutableStateOf(initialP1) }
    var p2Name by remember { mutableStateOf(initialP2) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Player Names") },
        text = {
            Column {
                OutlinedTextField(
                    value = p1Name,
                    onValueChange = { p1Name = it },
                    label = { Text("Player 1 (Red)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = p2Name,
                    onValueChange = { p2Name = it },
                    label = { Text("Player 2 (Blue)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(p1Name, p2Name) }) {
                Text("Start Game")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize state once per activity lifecycle
        val gameState = GameState()
        
        setContent {
            UMABALABA2Theme {
                GameScreen(gameState)
            }
        }
    }
}
