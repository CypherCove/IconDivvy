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
import org.gradle.api.logging.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.round
import kotlin.streams.toList

fun executeDivvyIcons(logger: Logger, jobs: Iterable<DivvyJobConfiguration>, logOnly: Boolean) {
    if (logOnly){
        logger.lifecycle("Running IconDivvy in 'logOnly' mode. No files will be written.")
    }
    jobs@for (config in jobs) {
        val job = config.name
        val errorMsg = "Job '$job' will not run."
        val source = config.stagingDir
        if (source == null) {
            logger.warn("sourceDir must be specified. $errorMsg")
            continue
        }
        val resourceDir = File(config.resourceDir)
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
        val sourceFiles = findSourceImageFiles(logger, job, File(source))
        if (sourceFiles.isEmpty()) {
            logger.lifecycle("Job '$job' does not have any source image files or staging directory does not exist.")
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
                        .width(round( config.sizeDip * scale).toInt())
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
        .filter { path ->
            path.isImageFile &&
                    path.hasValidName.also {
                        if (!it) logger.warn("File $path in job '$jobName' has an invalid resource name and will be skipped." +
                                "Names must contain only lowercase a-z, 0-9, or underscore")
                    }
        }
        .toList()
        .map(Path::toFile)
}

private val Path.isImageFile: Boolean
    get() = fileName.toString().run { endsWith(".png") || endsWith(".jpg") || endsWith(".jpeg") }

private val Path.hasValidName: Boolean
    get() = fileName.toString().substringBeforeLast('.').all { it == '_' || it.isDigit() || it.isLowerCase() }

private val SCALE_BY_DENSITY = mapOf(
    "ldpi" to 0.75f,
    "mdpi" to 1f,
    "hdpi" to 1.5f,
    "xhdpi" to 2f,
    "xxhdpi" to 3f,
    "xxxhdpi" to 4f
)

private val RESOURCE_TYPES = listOf("drawable", "mipmap")
