import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
import org.jsoup.Jsoup
import kotlin.streams.asSequence

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java") // Java support
    id("checkstyle")
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.errorpronePlugin)
    alias(libs.plugins.grammarKit)
    alias(libs.plugins.asciidoctorConvert)
}

version = if (System.getenv("VERSION") != null) System.getenv("VERSION") else "0.0.1"

// Configure project's dependencies
repositories {
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()

        // Needed when I download EAP versions which are only available on Maven.
        // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1638#issuecomment-2151527333
        jetbrainsRuntime()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    errorprone("com.google.errorprone:error_prone_core:2.34.0")
    compileOnly("com.google.errorprone:error_prone_core:2.34.0")
    /* snakeyaml is s used by asciidoctorj-pdf, but is actually provided within jruby-stdlib
     * a snakeyaml version in the classpath takes precedence, but IntelliJ includes a version that is too old
     * therefore this plugin includes the same version of snakeyaml that is already included in jruby-stdlib
     * to prevent loading the older version from IntelliJ.
     * When a different version than jruby-stdlib 9.4.7.0 is used after upgrading asciidoctorj,
     * double check the snakeyaml version.
     * https://github.com/asciidoctor/asciidoctorj-pdf/issues/25
     */
    implementation("org.snakeyaml:snakeyaml-engine:2.7")
    implementation("org.asciidoctor:asciidoctorj:3.0.0") // WARNING: when upgrading asciidoctorj, see comment above about snakeyaml!
    implementation("commons-io:commons-io:2.17.0")
    implementation("io.github.markdown-asciidoc:markdown-to-asciidoc:2.0.1") {
        // Exclude the Kotlin dependency, as this plugin uses only the Java version of the converter.
        // This saves ~1.6 MB from the final plugin"s bundle.
        exclude("org.jetbrains.kotlin", "kotlin-stdlib")
    }

    implementation("org.apache.commons:commons-text:1.12.0")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("io.sentry:sentry:7.15.0")

    // when updating the versions here, also update them in AsciiDocDownloaderUtil for dynamic download
    testImplementation("org.asciidoctor:asciidoctorj-diagram:2.3.1")
    testImplementation("org.asciidoctor:asciidoctorj-diagram-plantuml:1.2024.5")
    testImplementation("org.asciidoctor:asciidoctorj-diagram-batik:1.17")
    testImplementation("org.asciidoctor:asciidoctorj-pdf:2.3.19")

    testImplementation(gradleTestKit())
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.3")

    testImplementation("com.tngtech.archunit:archunit-junit4:1.3.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.3")

    // implementation(libs.exampleLibrary)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1693
        intellijIdeaCommunity(properties("platformVersion"), useInstaller = false)

        // Needed when I download EAP versions which are only available on Maven.
        // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1638#issuecomment-2151527333
        jetbrainsRuntime()

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(listOf(
            "tanvd.grazi", // used for spell and grammar checking checking
            "com.intellij.java", // used to integrate into the build via AsciiDocTargetScopeProvider
            "com.intellij.properties", // dependency to the Java plugin
            "org.jetbrains.plugins.yaml", // used to inject file references to Antora YAML files
            "com.jetbrains.sh",
            "org.intellij.intelliLang"
        ))

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        // https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html
        plugins(listOf(
            "PsiViewer:243.7768", // used for debugging
            // "PlantUML integration:6.3.0-IJ2023.2", // used to test highlighting of plantuml diagrams
            // "com.intellij.platform.images", // dependency for PlantUML plugin
            "com.intellij.javafx:1.0.4",
            // "com.google.ide-perf:1.2.0", // performance tracing
            // see https://github.com/google/ide-perf/blob/master/docs/user-guide.md
            // "com.jetbrains.performancePlugin:213.5744.122" // used run YourKit CPU profiling in test IDE
            // see https://intellij-support.jetbrains.com/hc/en-us/articles/207241235
            // https://plugins.jetbrains.com/plugin/16136-grazie-professional/
            "com.intellij.grazie.pro:0.3.320"
        ))

        instrumentationTools()
        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        description = providers.fileContents(layout.projectDirectory.file("src/main/resources/META-INF/description.html")).asText

        changeNotes = provider { Jsoup.parse(file("build/docs/asciidoc/CHANGELOG.html").readText(Charsets.UTF_8))
            .select("#releasenotes").get(0).nextElementSibling()!!.children()
            .subList(0, 20)
            .stream().map { e ->
                e.html()
                    .replace(Regex("\\(work in progress\\)"), "")
                    .replace(Regex("\\(preview, available from GitHub releases\\)"), "")
                    .replace(Regex("#([0-9]+)"), "<a href=\"https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/$1\">#$1</a>")
                    // regex for GitHub user names from https://github.com/shinnn/github-username-regex
                    .replace(Regex("(?i)@([a-z\\d](?:[a-z\\d]|-(?=[a-z\\d])){0,38})"), "<a href=\"https://github.com/$1\">@$1</a>")
            }
            .asSequence().joinToString("\n") }
    }

    publishing {
        token = environment("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(if ("true" == environment("PRE_RELEASE").getOrElse("false")) "eap" else "default"))
    }

    pluginVerification {
        failureLevel = listOf(VerifyPluginTask.FailureLevel.INVALID_PLUGIN, VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS, VerifyPluginTask.FailureLevel.NOT_DYNAMIC)
        freeArgs = listOf("-mute", "TemplateWordInPluginId")
        ides {
            // recommended()
            ides( properties("pluginVerifierIdeVersions").get().split(',') )
        }
    }
}

checkstyle {
    toolVersion = "9.3"
}

java {
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
}

grammarKit {}

sourceSets {
    main {
        java.srcDirs(project.files(file("gen")))
        resources.exclude("META-INF/description.html")
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    publishPlugin {
    }

    generateLexer {
        sourceFile.set(file("src/main/java/org/asciidoc/intellij/lexer/asciidoc.flex"))
        targetOutputDir.set(file("gen/org/asciidoc/intellij/lexer"))
    }

    test {
        testLogging {
            // this shows the full exception on failed tests on the build server
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    runIde {
        // should not automatically reload plugin on change in IDE, as JRuby is not very good at this
        systemProperty("idea.auto.reload.plugins", "false")
        systemProperty("jdk.attach.allowAttachSelf", "true") // necessary to run plugin com.google.ide-perf
        systemProperty("ide.plugins.snapshot.on.unload.fail", "true")
    }

    compileJava {
        options.errorprone.excludedPaths = ".*(_AsciiDocLexer).*"
        options.errorprone.error("StreamResourceLeak") // enforce errors where there would be warning in the standard only
        options.errorprone.disable("MissingSummary", "NullableOnContainingClass", "CanIgnoreReturnValueSuggester")
        options.compilerArgs.addAll(listOf("--release", "17"))
        // will print link to contributor guide at the start of each build
        dependsOn (generateLexer, "showLinkToContributorGuide", "checkJavaVersion")
    }

    checkstyleMain {
        dependsOn (instrumentTestCode)
    }

    patchPluginXml {
        dependsOn (asciidoctor)
        sinceBuild = "242.20224.159"
        untilBuild = provider { null }
    }

    asciidoctor {
        sourceDir(file("."))
        sources {
            include("CHANGELOG.adoc")
        }
    }

}

tasks.register("checkJavaVersion") {
    if (JavaVersion.current() != JavaVersion.VERSION_21) {
        val message = "As of IntellIJ 2024.2, this build must be run with Java 21, see:\n" +
                "https://intellij-asciidoc-plugin.ahus1.de/docs/contributors-guide/coder/setup-environment.html"
        println("\n" + message + "\n")
        throw GradleException(message)
    }
}

tasks.register("showLinkToContributorGuide") {
    println("\nFirst time contributing to this plugin? Have a look at the IntelliJ AsciiDoc Plugin contributor's guide for coders:\n" +
            "https://intellij-asciidoc-plugin.ahus1.de/docs/contributors-guide/contribute-as-a-coder.html\n")
}

