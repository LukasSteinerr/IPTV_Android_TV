package com.example.tv_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.tv_app.ui.theme.TV_APPTheme
import io.objectbox.Box
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    private lateinit var noteBox: Box<Note>

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        noteBox = ObjectBox.boxStore.boxFor(Note::class.java)

        // Example: Add a new note
        val newNote = Note(text = "Hello, ObjectBox!", date = System.currentTimeMillis())
        noteBox.put(newNote)

        // Example: Get all notes
        val allNotes = noteBox.all

        // Test Firebase connection
        testFirebaseConnection()

        setContent {
            TV_APPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    Greeting("Android")
                }
            }
        }
    }

    // Define a simple function to test the connection
    fun testFirebaseConnection() {
        val db = Firebase.firestore
        val user = hashMapOf(
            "first" to "Ada",
            "last" to "Lovelace",
            "born" to 1815
        )

        // Add a new document with a generated ID
        db.collection("testUsers")
            .add(user)
            .addOnSuccessListener { documentReference ->
                Log.d("FirebaseTest", "DocumentSnapshot added with ID: ${documentReference.id}")
                // Connection successful! You can show a Toast or update UI here
            }
            .addOnFailureListener { e ->
                Log.w("FirebaseTest", "Error adding document", e)
                // Connection failed. Handle the error (e.g., check for network issues, rules)
            }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TV_APPTheme {
        Greeting("Android")
    }
}