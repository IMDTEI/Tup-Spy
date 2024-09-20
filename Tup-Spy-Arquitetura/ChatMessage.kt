data class ChatMessage(
    val message: String = "",
    val sender: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
