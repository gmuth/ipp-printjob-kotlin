plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.21"
}

repositories {
    mavenCentral()
}

defaultTasks("clean", "build")

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "11"
    }
}

tasks.withType<Jar> {
    enabled = false
}

val fatJar = tasks.create("fatJar", Jar::class) {
    archiveBaseName.set("printjob")
    manifest {
        attributes["Main-Class"] = "ipp.PrintJobStreamVersionKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map(::zipTree))
    with(tasks.jar.get())
    enabled = true
}

tasks {
    build {
        dependsOn(fatJar)
    }
}