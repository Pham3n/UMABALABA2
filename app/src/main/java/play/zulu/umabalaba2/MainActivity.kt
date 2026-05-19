package play.zulu.umabalaba2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

// Class to manage the state and logic of the Umabalaba game
class GameState {
    // Observable list of nodes to trigger UI recomposition when a node changes
    val nodes = mutableStateListOf<Node>()
    
    // Observable current player using delegated property
    var currentPlayer by mutableStateOf(Player.PLAYER_1)
    
    // Observable piece counts for the placement phase
    var piecesToPlace by mutableStateOf(mapOf(
        Player.PLAYER_1 to 12,
        Player.PLAYER_2 to 12
    ))

    init {
        initializeBoard()
    }

    // Sets up the 24 nodes with their Umabalaba grid coordinates and connections
    private fun initializeBoard() {
        // 7x7 Grid Coordinates for the three nested squares
        val coords = listOf(
            // Outer Square (0-7)
            Pair(0, 0), Pair(3, 0), Pair(6, 0), Pair(6, 3), Pair(6, 6), Pair(3, 6), Pair(0, 6), Pair(0, 3),
            // Middle Square (8-15)
            Pair(1, 1), Pair(3, 1), Pair(5, 1), Pair(5, 3), Pair(5, 5), Pair(3, 5), Pair(1, 5), Pair(1, 3),
            // Inner Square (16-23)
            Pair(2, 2), Pair(3, 2), Pair(4, 2), Pair(4, 3), Pair(4, 4), Pair(3, 4), Pair(2, 4), Pair(2, 3)
        )

        // Adjacency mapping for standard Morabaraba/Umabalaba board (including diagonals)
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

    // Handles the logic for placing a piece on the board
    fun placePiece(nodeId: Int): Boolean {
        val index = nodes.indexOfFirst { it.id == nodeId }
        if (index == -1) return false
        val node = nodes[index]

        // Invalid if already occupied
        if (node.occupant != null) return false

        // Invalid if no pieces left to place for current player
        val remaining = piecesToPlace[currentPlayer] ?: 0
        if (remaining <= 0) return false

        // Update the node in the observable list to trigger a UI refresh
        nodes[index] = node.copy(occupant = currentPlayer)

        // Update the piece count map with a new instance to trigger state change
        piecesToPlace = piecesToPlace.toMutableMap().apply {
            put(currentPlayer, remaining - 1)
        }

        switchTurn()
        return true
    }

    private fun switchTurn() {
        currentPlayer = if (currentPlayer == Player.PLAYER_1) Player.PLAYER_2 else Player.PLAYER_1
    }
}

// Visual representation of a single node
@Composable
fun BoardNode(node: Node, onClick: () -> Unit) {
    val color = when (node.occupant) {
        Player.PLAYER_1 -> Color.Red
        Player.PLAYER_2 -> Color.Blue
        null -> Color.LightGray
    }

    Box(
        modifier = Modifier
            .size(32.dp) // Size of the "cow" or hole
            .background(color, shape = CircleShape)
            .clickable { onClick() }
    )
}

// Main game screen containing the board and UI info
@Composable
fun GameScreen(gameState: GameState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Player Turn and Status Header
        Text("Turn: ${gameState.currentPlayer}", modifier = Modifier.padding(16.dp))
        Text("P1 Cows: ${gameState.piecesToPlace[Player.PLAYER_1]} | P2 Cows: ${gameState.piecesToPlace[Player.PLAYER_2]}")

        Spacer(modifier = Modifier.height(32.dp))

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
                            x = step * node.x - 16.dp, // Center the 32.dp node on the point
                            y = step * node.y - 16.dp
                        )
                ) {
                    BoardNode(node) {
                        gameState.placePiece(node.id)
                    }
                }
            }
        }
    }
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
