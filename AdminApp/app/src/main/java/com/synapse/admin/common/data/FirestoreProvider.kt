package com.example.common.data

import android.content.Context
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreProvider {
    @Volatile
    private var instance: FirebaseFirestore? = null

    fun get(context: Context): FirebaseFirestore {
        return instance ?: synchronized(this) {
            instance ?: run {
                val dbId = context.applicationContext.getString(R.string.firestore_database_id)
                FirebaseFirestore.getInstance(dbId).also { instance = it }
            }
        }
    }
}
