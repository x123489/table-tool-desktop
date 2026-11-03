import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.compose") version "1.6.10"
}

group = "com.example.tabletool"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("com.github.doyaaaaaken:kotlin-csv:1.9.1")
}

compose.desktop {
    application {
        mainClass = "com.example.tabletool.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi)
            packageName = "table-tool"
            packageVersion = "1.0.0"
            description = "表格拆分 / 合并 / 筛选工具（单机版）"
            copyright = "Copyright (c) 2026"
        }
    }
}
