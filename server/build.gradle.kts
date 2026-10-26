plugins { application }
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
application { mainClass = "com.pulse.server.FitnessServer" }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
dependencies {
    implementation(project(":core"))
    runtimeOnly("com.mysql:mysql-connector-j:26.10.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.h2database:h2:2.3.232")
}
