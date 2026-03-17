import org.gradle.kotlin.dsl.from

plugins {
    id("java")
}

group = "org.arepo"

java{
    setSourceCompatibility(20)
    setTargetCompatibility(20)
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(20))
    }
}
repositories {
    mavenCentral()
}

dependencies {
    implementation ("info.picocli:picocli:4.7.4")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("org.xerial:sqlite-jdbc:3.50.2.0")
    implementation("org.codejargon:fluentjdbc:1.8.6")
    implementation("io.github.java-diff-utils:java-diff-utils:4.16")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
tasks.withType<Jar>() {
    manifest {
        attributes["Main-Class"] = "org.arepo.Main2"
        //attributes["Main-Class"] = "org.example.test.Main"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
}