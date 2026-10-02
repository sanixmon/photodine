// Root build file: version catalog plugin declarations only (applied in modules).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
}

allprojects {
    tasks.matching { it.name.contains("AarMetadata") || it.name.startsWith("lintAnalyze") }.configureEach {
        enabled = false
    }
}

subprojects {
    afterEvaluate {
        extensions.findByType<com.android.build.gradle.BaseExtension>()?.apply {
            lintOptions {
                disable("NullSafeMutableLiveData")
                disable("FlowOperatorInvokedInComposition")
                isAbortOnError = false
                isCheckReleaseBuilds = false
            }
        }
    }
}

gradle.taskGraph.whenReady {
    allTasks.forEach { task ->
        if (task.name.contains("AarMetadata") || task.name.contains("lintAnalyze")) {
            task.enabled = false
        }
    }
}
