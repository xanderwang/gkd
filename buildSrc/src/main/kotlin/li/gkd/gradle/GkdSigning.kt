package li.gkd.gradle

import com.android.build.api.dsl.ApkSigningConfig
import java.io.File
import java.util.Properties
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

// Keep the result independent of AGP's runtime classloader as well.
class GkdSigningConfigs<T>(val gkd: T, val play: T)

/**
 * `local.properties` is git-ignored, so local release keys can stay out of the repository.
 * Java properties unescape Windows paths (`C\:\\path`), so parse it instead of splitting lines.
 */
@PublishedApi
internal fun readLocalProperties(rootDir: File): Map<String, String> {
    val file = File(rootDir, "local.properties")
    if (!file.isFile) return emptyMap()
    val properties = Properties()
    file.inputStream().use(properties::load)
    return properties.stringPropertyNames().associateWith { properties.getProperty(it) }
}

/** Environment variable and Gradle property win over the git-ignored local development file. */
@PublishedApi
internal fun Project.signingProperty(key: String, localProperties: Map<String, String>): String? =
    buildProperty(key).orNull ?: localProperties[key]?.takeIf { it.isNotBlank() }

// Inline keeps AGP type access in the caller's plugin classloader.
@Suppress("NOTHING_TO_INLINE")
inline fun Project.getOrCreateSigningConfigs(
    configs: NamedDomainObjectContainer<out ApkSigningConfig>,
): GkdSigningConfigs<ApkSigningConfig> {
    // Preserve each signing property's existing relative-path base.
    val gkd =
        resolveSigningConfig(
            configs,
            "gkd",
            "GKD",
            rootProject.file("gkd-android"),
            configs.getByName("debug"),
        )
    val play = resolveSigningConfig(configs, "play", "PLAY", projectDir, gkd)
    return GkdSigningConfigs(gkd, play)
}

@PublishedApi
@Suppress("NOTHING_TO_INLINE")
internal inline fun Project.resolveSigningConfig(
    configs: NamedDomainObjectContainer<out ApkSigningConfig>,
    name: String,
    prefix: String,
    baseDir: File,
    fallback: ApkSigningConfig,
): ApkSigningConfig {
    configs.findByName(name)?.let {
        return it
    }
    // Environment variable and Gradle property win over the local development file.
    val localProperties = readLocalProperties(rootProject.projectDir)
    val path = signingProperty("${prefix}_STORE_FILE", localProperties) ?: return fallback
    return configs.create(name).apply {
        storeFile = baseDir.resolve(path)
        storePassword = signingProperty("${prefix}_STORE_PASSWORD", localProperties)
        keyAlias = signingProperty("${prefix}_KEY_ALIAS", localProperties)
        keyPassword = signingProperty("${prefix}_KEY_PASSWORD", localProperties)
    }
}
