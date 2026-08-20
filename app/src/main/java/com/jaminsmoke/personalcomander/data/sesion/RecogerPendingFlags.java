package com.jaminsmoke.personalcomander.data.sesion;

import android.app.PendingIntent;

/**
 * Flags del PendingIntent de recoger con {@code |} Java.
 * El {@code or} infix de Kotlin no es un {@code BitwiseExpr} para CodeQL
 * ({@code java/android/implicit-pendingintents}), y el extractor trata el
 * PendingIntent como mutable.
 */
final class RecogerPendingFlags {
    static final int UPDATE_IMMUTABLE =
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

    private RecogerPendingFlags() {}
}
