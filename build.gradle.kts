import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun config(name: String) = project.findProperty(name).toString()

repositories {
    mavenCentral()
}

plugins {
    java
    kotlin("jvm") version "1.5.10"
    id("org.jetbrains.intellij") version "0.7.2"
}

group = "dev.meanmail"
version = "${config("version")}-${config("postfix")}"


dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("junit:junit:4.13.2")
}

intellij {
    pluginName = config("pluginName")
    version = if (config("ideVersion") == "eap") {
        "LATEST-EAP-SNAPSHOT"
    } else {
        config("ideVersion")
    }
    type = config("ideType")
    val languages = config("languages").split(',').map {
        it.trim().toLowerCase()
    }
    if ("python" in languages) {
        when (type) {
            "PY" -> {
                setPlugins("python")
            }
            "PC" -> {
                setPlugins("PythonCore")
            }
            else -> {
                setPlugins("PythonCore:${config("PythonCore")}")
            }
        }
    }
}

fun readChangeNotes(pathname: String): String {
    val lines = file(pathname).readLines()

    val notes: MutableList<MutableList<String>> = mutableListOf()

    var note: MutableList<String>? = null

    for (line in lines) {
        if (line.startsWith('#')) {
            if (notes.size == 3) {
                break
            }
            note = mutableListOf()
            notes.add(note)
            val header = line.trimStart('#')
            note.add("<b>$header</b>")
        } else if (line.isNotBlank()) {
            note?.add(line)
        }
    }

    return notes.joinToString(
        "</p><br><p>",
        prefix = "<p>",
        postfix = "</p><br>"
    ) {
        it.joinToString("<br>")
    } +
            "See the full change notes on the <a href='" +
            config("repository") +
            "/blob/master/CHANGES.md'>github</a>"
}
tasks {
    withType<JavaCompile> {
        sourceCompatibility = config("jvmVersion")
        targetCompatibility = config("jvmVersion")
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = config("jvmVersion")
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = config("jvmVersion")
    }
    withType<Wrapper> {
        distributionType = Wrapper.DistributionType.ALL
        gradleVersion = config("gradleVersion")
    }

    test {
        useJUnit()

        maxHeapSize = "1G"
    }

    patchPluginXml {
        setPluginDescription(file("description.html").readText())
        setChangeNotes(readChangeNotes("CHANGES.md"))
    }

    publishPlugin {
        dependsOn("buildPlugin")
        setToken(System.getenv("PUBLISH_TOKEN"))
        setChannels(listOf(config("publishChannel")))
    }
}
