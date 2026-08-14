# Personal Comander — ProGuard/R8 rules for release builds

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * {
    <fields>;
}
-dontwarn androidx.room.paging.**

# ---- Gson (backup JSON + LAN Bar) ----
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.jaminsmoke.personalcomander.data.BackupJson* { *; }
-keep class com.jaminsmoke.personalcomander.data.sesion.TicketLan { *; }
-keep class com.jaminsmoke.personalcomander.data.sesion.LineaTicketLan { *; }
-keep class com.jaminsmoke.personalcomander.data.sesion.SalaEventLan { *; }
-keep class com.jaminsmoke.personalcomander.data.sesion.EstadoLan { *; }
-keepclassmembers class com.jaminsmoke.personalcomander.data.Producto { *; }
-keepclassmembers class com.jaminsmoke.personalcomander.data.Mesa { *; }
-keepclassmembers class com.jaminsmoke.personalcomander.data.Pedido { *; }
-keepclassmembers class com.jaminsmoke.personalcomander.data.LineaPedido { *; }

# ---- Enums (Room stores enums as strings) ----
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- Coroutines ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ---- Compose ----
-keep class androidx.compose.** { *; }

# ---- Keep line numbers for crash reports ----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
