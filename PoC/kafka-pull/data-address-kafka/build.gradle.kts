dependencies {
    implementation("org.eclipse.edc:core-spi:0.11.0-20250110-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
}