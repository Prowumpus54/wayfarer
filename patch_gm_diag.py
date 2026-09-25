from pathlib import Path

root = Path(r"C:\Wayfarer")

# Version bump
p = root / "app" / "build.gradle"
s = p.read_text(encoding="utf-8")
s = s.replace("versionCode 14", "versionCode 15", 1)
s = s.replace("versionName '0.12.0'", "versionName '0.12.1'", 1)
p.write_text(s, encoding="utf-8")

# GeminiGameMaster diagnostics
p = root / "app" / "src" / "main" / "java" / "com" / "wayfarer" / "rpg" / "GeminiGameMaster.kt"
s = p.read_text(encoding="utf-8")
s = s.replace("package com.wayfarer.rpg\n\nimport com.google.firebase.Firebase", "package com.wayfarer.rpg\n\nimport android.util.Log\nimport com.google.firebase.Firebase", 1)
old = '''                } catch (error: Exception) {\n                    lastError = error\n                    if (!isTransient(error)) throw error\n                    if (attempt == 0) delay(900)\n                }'''
new = '''                } catch (error: Exception) {\n                    lastError = error\n                    Log.e(\n                        "WayfarerGM",\n                        "generate failed model=$modelName attempt=${attempt + 1} type=${error::class.java.name} message=${error.message}",\n                        error\n                    )\n                    if (!isTransient(error)) throw error\n                    if (attempt == 0) delay(900)\n                }'''
assert old in s, "Gemini catch block not found"
s = s.replace(old, new, 1)
p.write_text(s, encoding="utf-8")

# PlayScreen diagnostics
p = root / "app" / "src" / "main" / "java" / "com" / "wayfarer" / "rpg" / "PlayScreen.kt"
s = p.read_text(encoding="utf-8")
s = s.replace("package com.wayfarer.rpg\n\nimport androidx.compose.foundation.background", "package com.wayfarer.rpg\n\nimport android.util.Log\nimport androidx.compose.foundation.background", 1)
old = '''            } catch (cancel: CancellationException) { throw cancel\n            } catch (error: Exception) {\n                failedAction = clean\n                gmStatus = "GM temporarily offline"'''
new = '''            } catch (cancel: CancellationException) { throw cancel\n            } catch (error: Exception) {\n                Log.e("WayfarerGM", "adjudicate failed type=${error::class.java.name} message=${error.message}", error)\n                failedAction = clean\n                gmStatus = "GM error • " + (error.message ?: error::class.java.simpleName).take(90)'''
assert old in s, "Play adjudicate catch not found"
s = s.replace(old, new, 1)
old2 = '''            } catch (cancel: CancellationException) { throw cancel\n            } catch (error: Exception) {\n                gmStatus = "GM temporarily offline"\n            } finally {'''
new2 = '''            } catch (cancel: CancellationException) { throw cancel\n            } catch (error: Exception) {\n                Log.e("WayfarerGM", "resolve failed type=${error::class.java.name} message=${error.message}", error)\n                gmStatus = "GM error • " + (error.message ?: error::class.java.simpleName).take(90)\n            } finally {'''
assert old2 in s, "Play resolve catch not found"
s = s.replace(old2, new2, 1)
p.write_text(s, encoding="utf-8")

print("patched")
# End patch
