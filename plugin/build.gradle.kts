plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    google()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation("com.android.tools.build:gradle:8.4.1")
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")
    compileOnly("org.ow2.asm:asm-tree:9.7")
    compileOnly("org.ow2.asm:asm-util:9.6")
//    implementation("com.android.tools.build:gradle:8.4.1")
//    implementation(libs.gradlePlugin.android)
//    implementation(libs.gradlePlugin.kotlin)

    runtimeOnly("org.aspectj:aspectjrt:1.9.22.1")
    runtimeOnly("org.aspectj:aspectjweaver:1.9.22.1")
}

gradlePlugin {
    plugins {
        create("aspectj") {
            group = "com.pujh"
            id = "com.pujh.aspectj"
            implementationClass = "com.pujh.plugin.AspectjPlugin"
            version = "1.0"
        }
    }
}

publishing {
    repositories {
        maven(url = "../repo")    // 发布到本地根目录下的 repositories 目录下，发布的 groupId, artifactsId, version 共用插件的字符
    }
}