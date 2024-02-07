
architectury {
    common(rootProject.property("enabled_platforms").toString().split(","))
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("fabric_loader_version")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric_api_version")}")
    // Remove the next line if you don't want to depend on the API
    modApi("dev.architectury:architectury-fabric:${rootProject.property("architectury_version")}")

    implementation("net.kyori:adventure-api:${property("minimessage_version")}")
    implementation("net.kyori:adventure-text-minimessage:${property("minimessage_version")}")
    implementation("net.kyori:adventure-text-serializer-gson:${property("minimessage_version")}")
    implementation("net.kyori:adventure-text-serializer-legacy:${property("minimessage_version")}")


    compileOnly(project(":common", "namedElements")) {
        isTransitive = false
    }
}
