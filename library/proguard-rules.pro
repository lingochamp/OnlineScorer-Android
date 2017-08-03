# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/wcw/dev/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keep class com.liulishuo.jni.SpeexEncoder { *; }
-keep class com.liulishuo.engzo.onlinescorer.OnlineScorer { *; }
-keep enum *{*;}
-keep class com.liulishuo.engzo.onlinescorer.OnlineScorerRecorder {
    public <methods>;
}
-keep class com.liulishuo.engzo.onlinescorer.OnlineScorerRecorder$* {
    *;
}
-keep public class * extends com.liulishuo.engzo.onlinescorer.BaseExercise {
    public <methods>;
}
-keep public class com.liulishuo.engzo.onlinescorer.RequestLogCallback {
    *;
}