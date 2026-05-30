package play.zulu.umabalaba2

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Manager for Online play connecting to a Python backend.
 */
class OnlineServerManager(private val gameState: GameState) {

    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val SERVER_URL = "http://your-python-server-ip:8000" // Aligned with typical FastAPI port

    fun connect(onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
        gameState.connectionStatus = "Online: Connecting..."
        
        val request = Request.Builder()
            .url("$SERVER_URL/health") // Assuming a health check endpoint
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    onFailure()
                    gameState.connectionStatus = "Online: Connection Failed"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                Handler(Looper.getMainLooper()).post {
                    if (response.isSuccessful) {
                        onSuccess()
                        gameState.connectionStatus = "Online: Ready"
                    } else {
                        onFailure()
                        gameState.connectionStatus = "Online: Server Error"
                    }
                }
            }
        })
    }

    fun submitMove(fromId: Int, toId: Int) {
        val json = """{"action": "move", "payload": {"from": $fromId, "to": $toId}}"""
        sendAction(json)
    }

    fun submitPlacement(nodeId: Int) {
        val json = """{"action": "place", "payload": {"position": $nodeId}}"""
        sendAction(json)
    }

    private fun sendAction(json: String) {
        val body = json.toRequestBody(JSON)
        val request = Request.Builder()
            .url("$SERVER_URL/game/action")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OnlineServer", "Action failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                // Parse response and update gameState.nodes
            }
        })
    }
}

    fun submitMove(fromId: Int, toId: Int) {
        // Send move to server for validation
        // Server will respond with updated board state or error
    }

    fun submitPlacement(nodeId: Int) {
        // Send placement to server for validation
    }
}
