package play.zulu.umabalaba2 // Package declaration for the game project

import android.os.Bundle // Import for activity state handling
import androidx.activity.ComponentActivity // Base class for Compose activities
import androidx.activity.compose.setContent // Function to set the Compose UI content
import androidx.activity.enableEdgeToEdge // Function to enable edge-to-edge display
import androidx.compose.foundation.layout.fillMaxSize // Modifier to fill screen size
import androidx.compose.foundation.layout.padding // Modifier for spacing
import androidx.compose.material3.Scaffold // Basic layout structure for screens
import androidx.compose.material3.Text // Composable for displaying text
import androidx.compose.runtime.Composable // Annotation for UI functions
import androidx.compose.ui.Modifier // Utility for UI layout/styling
import androidx.compose.foundation.background // Modifier for background colors
import androidx.compose.foundation.clickable // Modifier for click interactions
import androidx.compose.foundation.layout.Box // Layout for stacking elements
import androidx.compose.foundation.layout.Column // Layout for vertical sequencing
import androidx.compose.foundation.layout.size // Modifier for fixed dimensions
import androidx.compose.foundation.shape.CircleShape // Shape for circular nodes
import androidx.compose.ui.graphics.Color // Utility for color definitions
import androidx.compose.ui.unit.dp // Utility for density-independent pixels
import androidx.compose.ui.tooling.preview.Preview // Annotation for UI previews
import play.zulu.umabalaba2.ui.theme.UMABALABA2Theme // Custom theme for the app

// Data class representing a single position (node) on the Umabalaba board
data class Node(
    val id: Int, // Unique identifier for the node (0-23 for a 24-node board)
    var occupant: Player? = null, // Tracks which player's "cow" is on this node, or null if empty
    val connections: List<Int> // List of IDs for adjacent nodes to define movement paths and mills
)

// Enum defining the two players in the Umabalaba game
enum class Player {
    PLAYER_1, // Represents the first player (often Red cows)
    PLAYER_2  // Represents the second player (often Blue cows)
}

// Class to manage the state and logic of the Umabalaba game
class GameState {

    // List containing all 24 nodes that make up the Umabalaba board
    val nodes = mutableListOf<Node>()

    // Tracks which player is currently allowed to make a move
    var currentPlayer = Player.PLAYER_1
    
    // Tracks how many pieces (cows) each player has left to place during the first phase
    var piecesToPlace = mapOf(
        Player.PLAYER_1 to 12, // Umabalaba typically starts with 12 pieces per player
        Player.PLAYER_2 to 12
    )

    // Function to handle placing a piece on the board during the placement phase
    fun placePiece(nodeId: Int): Boolean {
        // Identify the specific node clicked
        val node = nodes[nodeId]

        // If the node is already occupied, the placement is invalid
        if (node.occupant != null) return false

        // Check how many pieces the current player has left to place
        val remaining = piecesToPlace[currentPlayer] ?: 0
        
        // If no pieces remain for this player, they cannot place more in this phase
        if (remaining <= 0) return false

        // Occupy the node with the current player's piece
        node.occupant = currentPlayer

        // Update the count of remaining pieces for the current player
        piecesToPlace = piecesToPlace.toMutableMap().apply {
            put(currentPlayer, remaining - 1)
        }

        // Switch the turn to the other player after a successful placement
        switchTurn()
        
        // Return true to indicate a successful move
        return true
    }

    // Private helper function to toggle between players
    private fun switchTurn() {
        // Change the current player to the opponent
        currentPlayer =
            if (currentPlayer == Player.PLAYER_1) Player.PLAYER_2
            else Player.PLAYER_1
    }
}

// Composable function to visually represent a single node on the board
@Composable
fun BoardNode(node: Node, onClick: () -> Unit) {

    // Determine the color of the node based on who occupies it
    val color = when (node.occupant) {
        Player.PLAYER_1 -> Color.Red // Player 1's cows are Red
        Player.PLAYER_2 -> Color.Blue // Player 2's cows are Blue
        null -> Color.Gray // Empty nodes are Gray (available for placement)
    }

    // UI element for the node: a clickable circle representing a "hole" or position
    Box(
        modifier = Modifier
            .size(40.dp) // Set the visual size of the board node
            .background(color, shape = CircleShape) // Apply color and circular shape
            .clickable { onClick() } // Listen for player interaction (tapping the node)
    )
}

// Composable function representing the main game screen
@Composable
fun GameScreen(gameState: GameState) {

    // Layout the game elements vertically
    Column {
        // Display text indicating which player's turn it is
        Text("Turn: ${gameState.currentPlayer}")

        // Iterate through all 24 nodes to render them on the board
        gameState.nodes.forEach { node ->
            // Draw each individual node
            BoardNode(node) {
                // When a node is clicked, attempt to place a piece via the GameState
                gameState.placePiece(node.id)
            }
        }
    }
}

// Main Activity class which serves as the entry point for the Umabalaba application
class MainActivity : ComponentActivity() {

    // Called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the game state when the activity starts
        val gameState = GameState().apply {

            // Create 24 nodes to form the standard Umabalaba board layout
            repeat(24) { index ->
                nodes.add(
                    Node(
                        id = index, // Assign a unique ID to each node
                        connections = emptyList() // Adjacency list for movement/mills (to be defined)
                    )
                )
            }
        }

        // Set the UI content using the application's theme
        setContent {
            // Apply the project's visual theme
            UMABALABA2Theme {
                // Launch the main GameScreen with the initialized game logic
                GameScreen(gameState)
            }
        }
    }
}
