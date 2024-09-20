import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ChatActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageList: MutableList<ChatMessage>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_activity)

        database = FirebaseDatabase.getInstance().getReference("chat")

        messageList = mutableListOf()
        chatAdapter = ChatAdapter(messageList)

        val recyclerView = findViewById<RecyclerView>(R.id.chatRecyclerView)
        recyclerView.adapter = chatAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadMessages()

        findViewById<Button>(R.id.sendButton).setOnClickListener {
            sendMessage()
        }

        findViewById<Button>(R.id.backupButton).setOnClickListener {
            backupData()
        }
    }

    private fun loadMessages() {
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(ChatMessage::class.java)
                if (message != null) {
                    messageList.add(message)
                    chatAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Chat", "Erro ao carregar mensagens", error.toException())
            }
        })
    }

    private fun sendMessage() {
        val messageText = findViewById<EditText>(R.id.messageEditText).text.toString()
        if (messageText.isNotEmpty()) {
            val message = ChatMessage(message = messageText, sender = "Usuário")
            database.push().setValue(message)
            findViewById<EditText>(R.id.messageEditText).text.clear()
        }
    }

    private fun collectFiles(): List<File> {
        val backupDir = Environment.getExternalStorageDirectory().absolutePath + "/Backup"
        val backupFiles = mutableListOf<File>()

        val directory = File(backupDir)
        if (directory.exists() && directory.isDirectory) {
            val files = directory.listFiles()
            if (files != null) {
                backupFiles.addAll(files)
            }
        }
        return backupFiles
    }

    private fun zipFiles(files: List<File>, zipFileName: String): File {
        val zipFile = File(Environment.getExternalStorageDirectory(), zipFileName)
        val zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))

        for (file in files) {
            val zipEntry = ZipEntry(file.name)
            zipOutputStream.putNextEntry(zipEntry)
            file.inputStream().copyTo(zipOutputStream)
            zipOutputStream.closeEntry()
        }

        zipOutputStream.close()
        return zipFile
    }

    private fun uploadBackupToFirebase(zipFile: File) {
        val storageRef = FirebaseStorage.getInstance().reference
        val backupRef = storageRef.child("backups/${zipFile.name}")

        val uploadTask = backupRef.putFile(Uri.fromFile(zipFile))
        uploadTask.addOnSuccessListener {
            Log.d("Firebase", "Backup enviado com sucesso: ${zipFile.name}")
        }.addOnFailureListener { e ->
            Log.e("Firebase", "Erro ao enviar backup", e)
        }
    }

    private fun backupData() {
        val files = collectFiles()
        val zipFile = zipFiles(files, "backup_${System.currentTimeMillis()}.zip")
        uploadBackupToFirebase(zipFile)
    }
}
