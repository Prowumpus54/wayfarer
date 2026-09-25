from pathlib import Path

root = Path(r'C:\Wayfarer')

# build.gradle
p = root/'app'/'build.gradle'
s = p.read_text(encoding='utf-8')
if 'def localProps = new Properties()' not in s:
    s = "def localProps = new Properties()\ndef localPropsFile = rootProject.file('local.properties')\nif (localPropsFile.exists()) localPropsFile.withInputStream { localProps.load(it) }\n\n" + s
s = s.replace("versionCode 15", "versionCode 16").replace("versionName '0.12.1'", "versionName '0.12.2'")
needle = "        targetSdk 36\n        versionCode 16\n        versionName '0.12.2'\n"
repl = needle + "        buildConfigField 'String', 'LOCAL_LLM_URL', '\"' + localProps.getProperty('WAYFARER_LOCAL_LLM_URL', '') + '\"'\n        buildConfigField 'String', 'LOCAL_LLM_TOKEN', '\"' + localProps.getProperty('WAYFARER_LOCAL_LLM_TOKEN', '') + '\"'\n"
s = s.replace(needle, repl)
p.write_text(s, encoding='utf-8')

# manifest
p = root/'app'/'src'/'main'/'AndroidManifest.xml'
s = p.read_text(encoding='utf-8')
if 'android:usesCleartextTraffic="true"' not in s:
    s = s.replace('android:allowBackup="false"', 'android:allowBackup="false"\n        android:usesCleartextTraffic="true"')
p.write_text(s, encoding='utf-8')

# GeminiGameMaster
p = root/'app'/'src'/'main'/'java'/'com'/'wayfarer'/'rpg'/'GeminiGameMaster.kt'
s = p.read_text(encoding='utf-8')
if 'import java.net.HttpURLConnection' not in s:
    s = s.replace('import org.json.JSONObject\n', 'import org.json.JSONObject\nimport java.net.HttpURLConnection\nimport java.net.URL\nimport java.io.IOException\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.withContext\n')
s = s.replace('    FLASH("Gemini 3.8 Flash", "3.8 Flash"),\n    FLASH_LITE("Gemini 3.5 Flash Lite", "3.5 Flash Lite")', '    LOCAL("Local Qwen 3.5 9B", "Local"),\n    LOCAL_FAST("Local Qwen 3.5 4B", "Local Fast"),\n    FLASH("Gemini 3.8 Flash", "3.8 Flash"),\n    FLASH_LITE("Gemini 3.5 Flash Lite", "3.5 Flash Lite")')
s = s.replace('            GmModelChoice.FLASH -> listOf("gemini-3.8-flash")\n            GmModelChoice.FLASH_LITE -> listOf("gemini-3.5-flash-lite")', '            GmModelChoice.LOCAL, GmModelChoice.LOCAL_FAST -> emptyList()\n            GmModelChoice.FLASH -> listOf("gemini-3.8-flash")\n            GmModelChoice.FLASH_LITE -> listOf("gemini-3.5-flash-lite")')
needle = '    private suspend fun generateWithFallback(prompt: String): Pair<String, String> {\n        var lastError: Exception? = null\n'
repl = '''    private suspend fun generateWithFallback(prompt: String): Pair<String, String> {
        if (modelChoice == GmModelChoice.LOCAL) return generateLocal(prompt, "gm")
        if (modelChoice == GmModelChoice.LOCAL_FAST) return generateLocal(prompt, "fast")
        if (modelChoice == GmModelChoice.AUTO && BuildConfig.LOCAL_LLM_URL.isNotBlank()) {
            try {
                return generateLocal(prompt, "gm")
            } catch (error: Exception) {
                Log.e("WayfarerGM", "local GM failed; falling back to Gemini: ${error.message}", error)
            }
        }
        var lastError: Exception? = null
'''
s = s.replace(needle, repl)
insert_before = '    private fun isTransient(error: Exception): Boolean {'
local_fn = '''    private suspend fun generateLocal(prompt: String, profile: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val base = BuildConfig.LOCAL_LLM_URL.trimEnd('/')
        if (base.isBlank()) throw IOException("Local LLM URL is not configured")
        val connection = (URL("$base/v1/chat").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5000
            readTimeout = 180000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${BuildConfig.LOCAL_LLM_TOKEN}")
        }
        val body = JSONObject().put("profile", profile).put("prompt", prompt).toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw IOException("Local LLM HTTP $code: ${raw.take(240)}")
        val json = JSONObject(raw)
        val text = json.optString("text")
        if (text.isBlank()) throw IOException("Local LLM returned an empty response")
        text to ("local:" + json.optString("model", profile))
    }

'''
if 'private suspend fun generateLocal' not in s:
    s = s.replace(insert_before, local_fn + insert_before)
p.write_text(s, encoding='utf-8')
print('patched local LLM')
