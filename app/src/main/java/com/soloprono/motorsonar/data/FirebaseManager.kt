package com.soloprono.motorsonar.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    fun isFirebaseInitialized(context: Context): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase not initialized: ${e.message}")
            false
        }
    }

    suspend fun syncScanToCloud(context: Context, scan: EngineScan): Boolean {
        return try {
            if (!isFirebaseInitialized(context)) return false
            val db = FirebaseFirestore.getInstance()
            val scanMap = mapOf(
                "vehicleName" to scan.vehicleName,
                "vehicleType" to scan.vehicleType,
                "healthScore" to scan.healthScore,
                "timestamp" to scan.timestamp,
                "isBaseline" to scan.isBaseline,
                "issueName" to scan.issueName,
                "urgency" to scan.urgency,
                "repairCostEstimate" to scan.repairCostEstimate,
                "mechanicPhrase" to scan.mechanicPhrase
            )
            db.collection("motor_sonar_scans")
                .add(scanMap)
                .await()
            Log.d(TAG, "Scan synced to Firestore successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync scan to Firestore: ${e.message}")
            false
        }
    }

    suspend fun fetchAiExpertInsight(context: Context, scan: EngineScan): String {
        return try {
            if (!isFirebaseInitialized(context)) {
                return "Firebase is not configured. Please add google-services.json to enable Firebase Vertex AI expert diagnostics."
            }
            
            val model = Firebase.vertexAI.generativeModel("gemini-1.5-flash")
            
            val prompt = """
                You are an expert master automotive diagnostic mechanic and acoustic engineer.
                Analyze the following vehicle engine scan report:
                - Vehicle: ${scan.vehicleName} (${scan.vehicleType})
                - Health Score: ${scan.healthScore}/100
                - Detected Issue: ${scan.issueName} (${scan.issueDescription})
                - Urgency Level: ${scan.urgency}
                - Estimated Repair Cost: ${scan.repairCostEstimate}
                - Mechanic Phrasing: ${scan.mechanicPhrase}
                
                Provide a professional, concise diagnostic analysis (in 2-3 short paragraphs) with actionable repair steps and what parts to inspect (e.g., tensioners, bearings, valves, exhaust, fuel injectors).
            """.trimIndent()

            val response = model.generateContent(prompt)
            response.text ?: "No expert insight generated."
        } catch (e: Exception) {
            Log.e(TAG, "AI Vertex Analysis failed: ${e.message}")
            "AI Expert analysis currently unavailable (Ensure google-services.json and Firebase AI Vertex API are configured): ${e.localizedMessage}"
        }
    }
}
