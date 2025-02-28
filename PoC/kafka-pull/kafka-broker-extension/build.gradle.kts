dependencies {
    implementation("org.eclipse.edc:transfer-spi:0.11.0-20250110-SNAPSHOT")
    implementation("org.eclipse.edc:validator-spi:0.11.0-20250110-SNAPSHOT")
    implementation("org.apache.kafka:kafka-clients:3.7.0")
    implementation("org.eclipse.edc:util-lib:0.11.0-20250110-SNAPSHOT")
    implementation(project(":data-address-kafka"))
    implementation(project(":validator-data-address-kafka"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.eclipse.edc:junit:0.11.0-20250110-SNAPSHOT")
    testImplementation("org.mockito:mockito-core:5.2.0")
}

tasks.test {
    useJUnitPlatform()
}