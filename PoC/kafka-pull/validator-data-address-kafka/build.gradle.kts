plugins {
    `java-library`
}

dependencies {
    implementation("org.eclipse.edc:core-spi:0.11.0-20250110-SNAPSHOT")
    implementation("org.eclipse.edc:validator-spi:0.11.0-20250110-SNAPSHOT")
    implementation(project(":data-address-kafka"))

    testImplementation("org.eclipse.edc:junit-base:0.11.0-20250110-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

tasks.test {
    useJUnitPlatform()
}