package play.zulu.umabalaba2

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Manager for Online play connecting to a Python backend using raw Sockets (aligned with KHASINA).
 */
class OnlineServerManager(private val gameState: GameState) {

    private var pendingOnSuccess: (() -> Unit)? = null
    private var pendingOnFailure: (() -> Unit)? = null

    private val onlineService = OnlineService(
        onConnected = {
            Handler(Looper.getMainLooper()).post {
                gameState.onlineConnected = true
                gameState.connectionStatus = "Online: Connected"
                pendingOnSuccess?.invoke()
                pendingOnSuccess = null
                pendingOnFailure = null
            }
        },
        onReceived = { message ->
            Handler(Looper.getMainLooper()).post {
                handleReceivedMessage(message)
            }
        }
    )

    private val SERVER_IP = "10.0.2.2" // Emulator default host IP
    private val SERVER_PORT = 9999

    fun connect(onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
        pendingOnSuccess = onSuccess
        pendingOnFailure = onFailure
        
        gameState.connectionStatus = "Online: Connecting..."
        try {
            onlineService.connect(SERVER_IP, SERVER_PORT)
            
            // Timeout if not connected in 5 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                if (!gameState.onlineConnected) {
                    onlineService.stop()
                    gameState.connectionStatus = "Online: Timeout"
                    pendingOnFailure?.invoke()
                    pendingOnSuccess = null
                    pendingOnFailure = null
                }
            }, 5000)
            
        } catch (e: Exception) {
            Log.e("OnlineServer", "Connection failed", e)
            gameState.connectionStatus = "Online: Error"
            onFailure()
        }
    }

    fun disconnect() {
        onlineService.stop()
        gameState.onlineConnected = false
        gameState.connectionStatus = "Online: Disconnected"
    }

    fun submitMove(fromId: Int, toId: Int) {
        val data = "MOVE:$fromId:$toId"
        onlineService.send(data)
    }

    fun submitPlacement(nodeId: Int) {
        val data = "PLACE:$nodeId"
        onlineService.send(data)
    }

    private fun handleReceivedMessage(message: String) {
        Log.d("OnlineServer", "Received: $message")
        // Logic to update gameState based on server messages (e.g. SYNC:...)
    }
}
