package com.soloprono.motorsonar.data

import android.content.Context
import android.util.Log
import com.soloprono.motorsonar.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object TranslationManager {
    private const val TAG = "TranslationManager"
    private const val PREFS_NAME = "motosonar_translations"
    private const val KEY_LANG = "selected_language_code"

    val languages = listOf(
        "en" to "English",
        "es" to "Español (Spanish)",
        "fr" to "Français (French)",
        "de" to "Deutsch (German)",
        "it" to "Italiano (Italian)",
        "pt" to "Português (Portuguese)",
        "ru" to "Русский (Russian)",
        "zh" to "简体中文 (Chinese Simplified)",
        "zh_TW" to "繁體中文 (Chinese Traditional)",
        "ja" to "日本語 (Japanese)",
        "ko" to "한국어 (Korean)",
        "ar" to "العربية (Arabic)",
        "hi" to "हिन्दी (Hindi)",
        "bn" to "বাংলা (Bengali)",
        "tr" to "Türkçe (Turkish)",
        "vi" to "Tiếng Việt (Vietnamese)",
        "pl" to "Polski (Polish)",
        "nl" to "Nederlands (Dutch)",
        "sv" to "Svenska (Swedish)",
        "no" to "Norsk (Norwegian)",
        "da" to "Dansk (Danish)",
        "fi" to "Suomi (Finnish)",
        "cs" to "Čeština (Czech)",
        "el" to "Ελληνικά (Greek)",
        "he" to "עברית (Hebrew)",
        "hu" to "Magyar (Hungarian)",
        "id" to "Bahasa Indonesia (Indonesian)",
        "ms" to "Bahasa Melayu (Malay)",
        "ro" to "Română (Romanian)",
        "sk" to "Slovenčina (Slovak)",
        "th" to "ไทย (Thai)",
        "uk" to "Українська (Ukrainian)",
        "tl" to "Filipino (Tagalog)",
        "fa" to "فارسی (Persian)",
        "hr" to "Hrvatski (Croatian)",
        "sr" to "Српски (Serbian)",
        "bg" to "Български (Bulgarian)",
        "lt" to "Lietuvių (Lithuanian)",
        "lv" to "Latviešu (Latvian)",
        "et" to "Eesti (Estonian)",
        "sl" to "Slovenščina (Slovenian)",
        "ca" to "Català (Catalan)",
        "gl" to "Galego (Galician)",
        "eu" to "Euskara (Basque)",
        "is" to "Íslenska (Icelandic)",
        "sw" to "Kiswahili (Swahili)",
        "zu" to "isiZulu (Zulu)",
        "af" to "Afrikaans",
        "am" to "አማርኛ (Amharic)",
        "az" to "Azərbaycanca (Azerbaijani)",
        "be" to "Беларуская (Belarusian)",
        "ka" to "ქართული (Georgian)",
        "hy" to "Հայերեն (Armenian)",
        "kk" to "Қазақша (Kazakh)",
        "mn" to "Монгол (Mongolian)",
        "ne" to "नेपाली (Nepali)",
        "si" to "සිංහල (Sinhala)",
        "ur" to "اردو (Urdu)",
        "ta" to "தமிழ் (Tamil)",
        "te" to "తెలుగు (Telugu)"
    )

    private val _selectedLanguage = MutableStateFlow("en")
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating

    // Core offline fallback dictionary for instant load without internet (Spanish, French, German, Vietnamese)
    private val offlineDictionary = mapOf(
        "es" to mapOf(
            "Diagnose" to "Diagnosticar",
            "Baseline" to "Línea Base",
            "History" to "Historial",
            "START DIAGNOSIS" to "INICIAR DIAGNÓSTICO",
            "STOP SCAN" to "DETENER ESCANEO"
        ),
        "fr" to mapOf(
            "Diagnose" to "Diagnostiquer",
            "Baseline" to "Référence",
            "History" to "Historique",
            "START DIAGNOSIS" to "DÉMARRER LE DIAGNOSTIC",
            "STOP SCAN" to "ARRÊTER LE SCAN"
        ),
        "de" to mapOf(
            "Diagnose" to "Diagnose",
            "Baseline" to "Basislinie",
            "History" to "Verlauf",
            "START DIAGNOSIS" to "DIAGNOSE STARTEN",
            "STOP SCAN" to "SCAN STOPPEN"
        ),
        "vi" to mapOf(
            "Diagnose" to "Chẩn đoán",
            "Baseline" to "Dữ liệu mẫu",
            "History" to "Lịch sử",
            "START DIAGNOSIS" to "BẮT ĐẦU CHẨN ĐOÁN",
            "STOP SCAN" to "DỪNG QUÉT"
        )
    )

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _selectedLanguage.value = prefs.getString(KEY_LANG, "en") ?: "en"
    }

    fun setLanguage(context: Context, langCode: String) {
        _selectedLanguage.value = langCode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, langCode).apply()
    }

    /**
     * Translates a string dynamically.
     * Looks up offline dictionary, then translation cache in SharedPreferences, and falls back to original.
     */
    fun getString(context: Context, englishText: String): String {
        val lang = _selectedLanguage.value
        if (lang == "en" || englishText.isBlank()) return englishText

        // 1. Check offline dictionary
        offlineDictionary[lang]?.get(englishText)?.let { return it }

        // 2. Check SharedPreferences cache
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString("${lang}_$englishText", null)
        if (cached != null) return cached

        return englishText
    }

    /**
     * Translates the entire app's primary UI strings in a single Gemini batch call and saves to SharedPreferences cache.
     */
    suspend fun translateAppUI(context: Context, langCode: String, onComplete: () -> Unit = {}) = withContext(Dispatchers.IO) {
        if (langCode == "en") {
            onComplete()
            return@withContext
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Check if already batch-translated
        if (prefs.getBoolean("batch_translated_$langCode", false)) {
            onComplete()
            return@withContext
        }

        _isTranslating.value = true
        val targetLangName = languages.find { it.first == langCode }?.second ?: langCode

        // Core UI text keys to translate
        val textsToTranslate = listOf(
            "High-End Acoustic Diagnostics",
            "START DIAGNOSIS",
            "STOP SCAN",
            "EST. IDLE SPEED",
            "SOUND POWER",
            "Analyzing...",
            "Calibrating...",
            "Acoustic Oscilloscope Offline",
            "Acoustic Scan Guide",
            "Engine Bay",
            "~ 8 Inches",
            "Your Phone",
            "Register as Reference Baseline",
            "Current vehicle already has a baseline",
            "Record reference sound of a healthy engine to track wear later",
            "Diagnostic Report",
            "ENGINE HEALTH INDEX",
            "Mechanic Card (Scam Shield)",
            "Local Market Cost Estimator",
            "Fair Market Rate",
            "Acoustic Wear",
            "Service Logs",
            "Engine Baseline Wear Tracking",
            "Track sub-audible mechanical wear over months",
            "How Wear Tracking Works",
            "Service Log & Planner",
            "Configure Current Vehicle",
            "Car",
            "Scooter",
            "Save",
            "Cancel",
            "Confirm",
            "Tap Start Diagnosis to Begin",
            "Acoustic Band-Pass Filter (60Hz-4.5kHz)",
            "Eco Battery Saver",
            "Export Formatted Text File (.txt)",
            "Save or share formatted diagnostic summary file",
            "Interactive Positioning Guide",
            "• Recommended Action: %1\$s",
            "• Estimated Cost: %1\$s",
            "• Urgency Level: %1\$s",
            "Get Full Gemini AI Expert Analysis",
            "Gemini AI Acoustic Processing...",
            "Analyzing engine sound harmonics, mechanical knock signatures & spectrum data...",
            "Gemini AI Master Mechanic Insights:",
            "Delete Diagnostic Scan",
            "Are you sure you want to permanently delete this report?",
            "Delete",
            "Share Diagnostic Report",
            "Choose how you'd like to share this diagnostic summary with your mechanic or contacts:",
            "Text Mechanic (SMS)",
            "Draft message with health metrics",
            "Share Full Report",
            "Send to WhatsApp, Email, or Slack",
            "Export PDF Document",
            "Compile a professional certificate",
            "Camera Inspection",
            "Scan belts & hoses",
            "Find Mechanics",
            "Google Maps search",
            "Engine Status: %1\$s",
            "Acoustic AI Diagnostic Confidence",
            "Acoustic Diagnostic & Symptom Notes",
            "Symptom Observations:",
            "Raw Audio Analysis Summary:",
            "AI Acoustic Expert Analysis",
            "Low Severity",
            "Moderate Risk",
            "Critical Urgency",
            "Plain English Acoustic Breakdown",
            "Detected Issue: %1\$s",
            "Repair Suggestions & Estimated Cost",
            "Interactive Repair Cost Simulator",
            "Adjust parameters to calculate dynamic parts & labor estimates",
            "MATCHED SYSTEM",
            "DIFFICULTY",
            "Hourly Labor Rate: ",
            "Parts Quality Tier",
            "Parts: %1\$s",
            "Labor: %1\$s (%2\$d hrs)",
            "Estimated Grand Total:",
            "Acoustic Shield Protection Advice",
            "MOTORAI DIAGNOSTICS",
            "OFFICIAL ANALYSIS REPORT",
            "REPORT ID: #MA-%1\$s-%2\$s",
            "DATE: %1\$s",
            "VEHICLE DETAILS",
            "HEALTH INDEX",
            "ACOUSTIC ANALYSIS",
            "Detected Symptom: %1\$s",
            "MECHANIC TECHNICAL NOTES:",
            "RECOMMENDED REPAIR PROCEDURE:",
            "EST. REPAIR COST:",
            "MotorAI Verified Seal",
            "Filter Diagnostics",
            "All",
            "Critical Issues",
            "Good Health",
            "Needs Maintenance",
            "Acoustic Diagnostic Log (%1\$d)",
            "No matching reports",
            "Try changing your filter selection above.",
            "Engine Sound Memo Recorder",
            "Capture engine sounds for subsequent AI analysis",
            "Recorded Clip: %1\$s",
            "Stop",
            "Record Sound Memo",
            "Acoustic Shield AI analyzing sound frequencies...",
            "Run Acoustic AI Diagnostics on Sound Memo",
            "Engine Health Trend Analysis",
            "Acoustic health degradation tracking over time",
            "Awaiting More Diagnoses",
            "Perform at least two acoustic scans to plot a historical degradation trend.",
            "Health Score: %1\$d%%",
            "Generating PDF Certificate...",
            "Rendering spectrogram signatures...",
            "Est. Repair: ",
            "Configure Vehicle Profile",
            "Name and configure your vehicle's engine or motor profile to refine acoustic diagnostics.",
            "%1\$s Engine/Motor Profile",
            "BASELINE",
            "HP",
            "Share",
            "Mechanic Voice Context Note",
            "Attach a voice-recorded note to provide additional context and symptoms for your mechanic.",
            "Record Voice Note",
            "Play Voice Note",
            "Stop Playback",
            "Re-record",
            "Delete Note",
            "Attach to Report",
            "Enjoying MotorAI Diagnostics?",
            "If MotorAI has helped you diagnose your vehicle or motorcycle engine, please take a moment to rate us on the Play Store!",
            "Rate 5 Stars",
            "Maybe Later",
            "Share MotorAI with Drivers",
            "Help fellow motorists and riders keep their engines healthy with instant acoustic AI diagnostics.",
            "Share App",
            "Thank you for rating MotorAI!"
        )

        try {
            val jsonPayloadBuilder = JSONObject()
            textsToTranslate.forEachIndexed { index, text ->
                jsonPayloadBuilder.put("k_$index", text)
            }

            val apiKey = BuildConfig.GEMINI_API_KEY
            val prompt = """
                You are a professional localization expert. Translate the values in the following JSON block from English to $targetLangName. 
                Keep the JSON keys (e.g. k_0, k_1...) exactly the same, only translate the values.
                Make sure translations are natural, highly accurate for an automotive / motorcycle diagnostic application, and complete.
                Return ONLY the translated JSON block, absolutely no markdown formatting, backticks, or extra text:
                
                ${jsonPayloadBuilder.toString()}
            """.trimIndent()

            var responseText: String? = null

            if (apiKey.isNotBlank() && apiKey != "DEFAULT_API_KEY") {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 15000
                    readTimeout = 20000
                    doOutput = true
                }

                val payload = JSONObject().apply {
                    put("contents", listOf(
                        JSONObject().apply {
                            put("parts", listOf(
                                JSONObject().apply { put("text", prompt) }
                            ))
                        }
                    ))
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(responseString)
                    val candidates = responseJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    responseText = parts?.optJSONObject(0)?.optString("text")?.trim()
                }
            }

            // Fallback to Firebase Vertex AI
            if (responseText == null && FirebaseManager.isFirebaseInitialized(context)) {
                try {
                    val model = Firebase.vertexAI.generativeModel("gemini-1.5-flash")
                    val response = model.generateContent(prompt)
                    responseText = response.text?.trim()
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase Vertex AI batch translation failed", e)
                }
            }

            // Clean the JSON string (strip markdown block wrappers if model added them)
            if (responseText != null) {
                var cleanJson = responseText
                if (cleanJson.startsWith("```json")) {
                    cleanJson = cleanJson.removePrefix("```json")
                }
                if (cleanJson.startsWith("```")) {
                    cleanJson = cleanJson.removePrefix("```")
                }
                if (cleanJson.endsWith("```")) {
                    cleanJson = cleanJson.removeSuffix("```")
                }
                cleanJson = cleanJson.trim()

                val resultJson = JSONObject(cleanJson)
                val editor = prefs.edit()
                textsToTranslate.forEachIndexed { index, englishText ->
                    val translatedText = resultJson.optString("k_$index", englishText)
                    if (translatedText.isNotBlank()) {
                        editor.putString("${langCode}_$englishText", translatedText)
                    }
                }
                editor.putBoolean("batch_translated_$langCode", true)
                editor.apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to batch translate to $targetLangName: ${e.message}", e)
        } finally {
            _isTranslating.value = false
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    /**
     * Translates a block of text (e.g., dynamic mechanic diagnostic report) dynamically using Gemini AI.
     */
    suspend fun translateDynamicBlock(context: Context, text: String, targetLangCode: String): String = withContext(Dispatchers.IO) {
        if (targetLangCode == "en" || text.isBlank()) return@withContext text

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cacheKey = "dynamic_${targetLangCode}_${text.hashCode()}"
        prefs.getString(cacheKey, null)?.let { return@withContext it }

        val targetLangName = languages.find { it.first == targetLangCode }?.second ?: targetLangCode
        val prompt = """
            You are a professional mechanic and translator. Translate the following diagnostic report and technical text to $targetLangName.
            Keep all technical terms clear and professional. Keep any safety warning symbols (🟢, 🟡, 🔴) as is.
            Return ONLY the translated text, no markdown block wrappers, no annotations, no preamble:
            
            $text
        """.trimIndent()

        val apiKey = BuildConfig.GEMINI_API_KEY
        var responseText: String? = null

        if (apiKey.isNotBlank() && apiKey != "DEFAULT_API_KEY") {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 10000
                    readTimeout = 15000
                    doOutput = true
                }

                val payload = JSONObject().apply {
                    put("contents", listOf(
                        JSONObject().apply {
                            put("parts", listOf(
                                JSONObject().apply { put("text", prompt) }
                            ))
                        }
                    ))
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(responseString)
                    val candidates = responseJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    responseText = parts?.optJSONObject(0)?.optString("text")?.trim()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Dynamic translation error: ${e.message}")
            }
        }

        if (responseText == null && FirebaseManager.isFirebaseInitialized(context)) {
            try {
                val model = Firebase.vertexAI.generativeModel("gemini-1.5-flash")
                val response = model.generateContent(prompt)
                responseText = response.text?.trim()
            } catch (e: Exception) {
                Log.e(TAG, "Firebase dynamic translation failed", e)
            }
        }

        if (responseText != null) {
            prefs.edit().putString(cacheKey, responseText).apply()
            return@withContext responseText
        }

        return@withContext text
    }
}
