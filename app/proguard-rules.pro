# do not obfuscate code - Open Source
-dontobfuscate
-keepattributes SourceFile,LineNumberTable

# keep all the code from the app
-keep class com.passbolt.mobile.android.** { *; }

# General config
-keepattributes InnerClasses,Signature,Exceptions,EnclosingMethod,SourceFile,LineNumberTable,*Annotation*
-renamesourcefileattribute SourceFile

# Room & SqlCipher
-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.sqlcipher.** { *; }

# Gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit
 -keep,allowobfuscation,allowshrinking interface retrofit2.Call
 -keep,allowobfuscation,allowshrinking class retrofit2.Response
 -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Jwt
-keep class io.fusionauth.jwt.domain.** { *; }

# Yubikit
-keep class com.yubico.yubikit.android.** { *; }
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
