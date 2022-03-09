/*******************************************************************************
 * Copyright 2020 Cypher Cove LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cyphercove.icondivvy

import net.coobird.thumbnailator.Thumbnails
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.round
import kotlin.streams.asSequence
import kotlin.streams.toList

fun executeDivvyIcons(project: Project, jobs: Iterable<DivvyJobConfiguration>, logOnly: Boolean) {
    val logger = project.logger
    if (logOnly) {
        logger.lifecycle("Running IconDivvy in 'logOnly' mode. No files will be written.")
    }
    jobs@ for (config in jobs) {
        val job = config.name
        val errorMsg = "Job '$job' will not run."
        val source = config.stagingDir
        if (source == null) {
            logger.warn("sourceDir must be specified. $errorMsg")
            continue
        }
        val resourceDir = File(project.projectDir, config.resourceDir)
        if (!resourceDir.isDirectory) {
            logger.warn("The destResDir $resourceDir is invalid. $errorMsg")
            continue
        }
        for (density in config.densities) {
            if (density !in SCALE_BY_DENSITY.keys) {
                logger.warn("Unknown density $density. $errorMsg")
                continue@jobs
            }
        }
        if (config.densities.isEmpty()) {
            logger.warn("No densities provided. $errorMsg")
            continue
        }
        if (config.resourceType !in RESOURCE_TYPES) {
            logger.warn("The resourceType '${config.resourceType}' is invalid. $errorMsg")
            continue
        }
        if (config.sizeDip <= 0) {
            logger.warn("The sizeDip of ${config.sizeDip} is invalid. $errorMsg")
            continue
        }
        val sourceFiles = findSourceImageFiles(logger, job, File(project.projectDir, source))
        if (sourceFiles.isEmpty()) {
            logger.lifecycle("Job '$job' does not have any valid source image files or staging directory does not exist.")
            continue
        }
        val resDirectoryNames = config.densities.joinToString(", ") { "${config.resourceType}-$it" }
        logger.lifecycle("Job '$job' will place the following image(s) in the subdirectories of '$resourceDir'\n($resDirectoryNames):")
        sourceFiles.forEach { file ->
            logger.lifecycle("    ${file.name}")
            if (logOnly) {
                return@forEach
            }
            val sourceImage = ImageIO.read(file)
            val heightScale = sourceImage.height.toFloat() / sourceImage.width.toFloat()
            for (density in config.densities) {
                val outDir = File(resourceDir, "${config.resourceType}-$density")
                    .apply { mkdirs() }
                val outFile = File(outDir, file.name)
                val scale = SCALE_BY_DENSITY[density] ?: error("Unknown density $density.")
                if (config.overwriteExisting || !outFile.exists()) {
                    Thumbnails.of(sourceImage)
                        .width(round(config.sizeDip * scale).toInt())
                        .height(round(config.sizeDip * scale * heightScale).toInt())
                        .toFile(outFile)
                }
            }
            sourceImage.flush()
        }
    }
}

private fun findSourceImageFiles(logger: Logger, jobName: String, sourceDir: File): List<File> {
    if (!sourceDir.exists()) {
        return emptyList()
    }
    return Files.walk(sourceDir.absoluteFile.toPath(), 1)
        .asSequence()
        .filter(Path::isImageFile)
        .filter { path ->
            path.hasValidName.also {
                if (!it) {
                    logger.warn(
                        "File $path in job '$jobName' has an invalid resource name and will be skipped." +
                                "Names must contain only lowercase a-z, 0-9, or underscore, and must not be a Java keyword.")
                }
            }
        }
        .map(Path::toFile)
        .toList()
}

private val Path.isImageFile: Boolean
    get() = fileName.toString().run { endsWith(".png") || endsWith(".jpg") || endsWith(".jpeg") }

private val Path.hasValidName: Boolean
    get() = fileName.toString().substringBeforeLast('.').let { name ->
        name.all { it == '_' || it.isDigit() || it.isLowerCase() } &&
                name !in JAVA_KEYWORDS
    }

private val SCALE_BY_DENSITY = mapOf(
    "ldpi" to 0.75f,
    "mdpi" to 1f,
    "hdpi" to 1.5f,
    "xhdpi" to 2f,
    "xxhdpi" to 3f,
    "xxxhdpi" to 4f
)

private val RESOURCE_TYPES = listOf("drawable", "mipmap")

private val JAVA_KEYWORDS = """
abstract assert boolean break byte case catch char class const continue
default do double else enum extends final finally float for goto if
implements import instanceof int interface long native new package
private protected public return short static strictfp super switch
synchronized this throw throws transient try void volatile while 
""".trim().split(" ", "\n")

