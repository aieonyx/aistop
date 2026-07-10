# AI Stop ProGuard rules
# Keep JNI bridge — loaded by name at runtime
-keep class com.aieonyx.aistop.jni.AiStopCore { *; }
-keep class com.aieonyx.aistop.ime.** { *; }
-keep class com.aieonyx.aistop.identity.** { *; }
