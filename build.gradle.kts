// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

val bouncyCastleBcprov = libs.versions.bouncycastleBcprov.get()
val bouncyCastlePkix = libs.versions.bouncycastlePkix.get()

subprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group != "org.bouncycastle") return@eachDependency
            val artifact = requested.name
            when {
                artifact.startsWith("bcprov") -> {
                    useVersion(bouncyCastleBcprov)
                    because("GHSA GOST/LDAP: bcprov 1.85.2; el APK no empaqueta BouncyCastle")
                }
                artifact.startsWith("bcpkix") || artifact.startsWith("bcutil") -> {
                    useVersion(bouncyCastlePkix)
                    because("Alinear pkix/util con BC 1.85 (1.85.2 solo existe en bcprov)")
                }
            }
        }
    }
}