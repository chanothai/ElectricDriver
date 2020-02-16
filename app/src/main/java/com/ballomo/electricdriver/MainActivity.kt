package com.ballomo.electricdriver

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

    private val job = Job()

    private var firestore = FirebaseFirestore.getInstance()

    private val user by lazy {
        return@lazy User(
            "ada",
            "Lovelace",
            1815,
            "0949948249"
        )
    }

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        launch {
            addUser()
        }
    }

    private suspend fun addUser() {
        withContext(Dispatchers.IO) {
            Tasks.await(firestore.collection("users").document(user.phone).set(user))
        }
    }

    private suspend fun getUser(): List<User> {
        val users = mutableListOf<User>()
        val response = withContext(Dispatchers.IO) {
            Tasks.await(firestore.collection("users").get())
        }

        response.let {
            for (document in it) {
                val user = document.toObject(User::class.java)
                users.add(user)
            }
        }

        return users
    }

    private suspend fun deleteUser(user: User) {
        withContext(Dispatchers.IO) {
            Tasks.await(firestore.collection("users").document(user.phone).delete())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
