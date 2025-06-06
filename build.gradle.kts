import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

// 定义获取 Gradle 属性和环境变量的函数
fun gradleProperty(key: String) = providers.gradleProperty(key)
fun environmentVariable(key: String) = providers.environmentVariable(key)

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
    alias(libs.plugins.changelog)
    alias(libs.plugins.qodana)
    alias(libs.plugins.kover)
}

// 配置项目的分组和版本
group = gradleProperty("pluginGroup").get()
version = gradleProperty("pluginVersion").get()

// 配置项目的依赖
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://mirrors.cloud.tencent.com/repository/maven") }
    maven { url = uri("https://mirrors.huaweicloud.com/repository/maven") }
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)

    intellijPlatform {
        create(gradleProperty("platformType").get(), gradleProperty("platformVersion").get())

        bundledPlugins(gradleProperty("platformBundledPlugins").map { it.split(',') })
        plugins(gradleProperty("platformPlugins").map { it.split(',') })
    }
}


// 配置 Kotlin 编译工具链
kotlin {
    jvmToolchain(21)
}

// 配置 IntelliJ Platform Gradle Plugin
intellijPlatform {
    pluginConfiguration {
        name = gradleProperty("pluginName").get()
        version = gradleProperty("pluginVersion").get()

        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val startTag = "<!-- Plugin description -->"
            val endTag = "<!-- Plugin description end -->"

            with(it.lines()) {
                require(startTag in this && endTag in this) { "Plugin description section not found in README.md" }
                subList(indexOf(startTag) + 1, indexOf(endTag)).joinToString("\n").let(::markdownToHTML)
            }
        }

        changeNotes = gradleProperty("pluginVersion").map { pluginVersion ->
            with(project.changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = gradleProperty("pluginSinceBuild").get()
            // untilBuild = gradleProperty("pluginUntilBuild").get() // 如需配置 untilBuild，可取消注释并设置相应属性
        }
    }

    signing {
        certificateChain = environmentVariable("CERTIFICATE_CHAIN")
        privateKey = environmentVariable("PRIVATE_KEY")
        password = environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = environmentVariable("PUBLISH_TOKEN")
        channels = gradleProperty("pluginVersion").map { version ->
            listOf(if (version.contains('-')) version.substringAfter('-').substringBefore('.') else "default")
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

// 配置 Gradle Changelog Plugin
changelog {
    groups = emptyList() // 清空默认分组
    repositoryUrl = gradleProperty("pluginRepositoryUrl").get()
}

// 配置 Gradle Kover Plugin
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

// 配置 Gradle Wrapper
tasks.wrapper {
    gradleVersion = gradleProperty("gradleVersion").get()
}

// 配置发布插件任务依赖
tasks.publishPlugin {
    dependsOn(tasks.patchChangelog)
}

// 配置 IntelliJ Platform 测试
intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }
            plugins {
                robotServerPlugin()
            }
        }
    }
}

// 配置 Groovy 编译任务
tasks.withType<GroovyCompile> {
    options.encoding = "UTF-8"
}