plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
//    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.jmdns:jmdns:3.5.7")

    implementation("com.github.serezhka:java-airplay-lib:1.0.5")

}

tasks.test {
    useJUnitPlatform()
}