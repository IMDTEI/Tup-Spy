fun sendLogToFirebase(log: String) {
    val logsRef = FirebaseDatabase.getInstance().getReference("logs")
    val logData = mapOf("log" to log, "timestamp" to System.currentTimeMillis())
    logsRef.push().setValue(logData).addOnSuccessListener {
        Log.d("Log", "Log enviado com sucesso")
    }.addOnFailureListener { e ->
        Log.e("Log", "Erro ao enviar log", e)
    }
}
