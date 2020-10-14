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
import javax.lang.model.SourceVersion
import kotlin.math.round
import kotlin.streams.toList

fun executeDivvyIcons(logger: Logger, jobs: Iterable<DivvyJobConfiguration>, logOnly: Boolean) {
    if (logOnly){
        logger.lifecycle("Running IconDivvy in 'logOnly' mode. No files will be written.")
    }
    jobs.forEach { config ->
        val job = config.name
        val source = config.stagingDir
        if (source == null) {
            logger.warn("sourceDir must be specified. Job '$job' will not run.")
            return@forEach
        }
        val resourceDir = File(config.resourceDir)
        if (!resourceDir.isDirectory) {
            logger.warn("The destResDir $resourceDir is invalid. Job '$job' will not run.")
            return@forEach
        }
        val densities = config.densities.filter { density ->
            (density in scaleByDensity.keys)
                .also { if (!it) logger.warn("Unknown density $density in job '$job' will be skipped.") }
        }
        if (densities.isEmpty()) {
            logger.warn("Job '$job' does not have any valid densities defined and will be skipped.")
            return@forEach
        }
        if (!(config.resourceType == "drawable" || config.resourceType == "mipmap")) {
            logger.warn("The resourceType '${config.resourceType}' is invalid. Job '$job' will not run.")
            return@forEach
        }
        if (config.sizeDip <= 0) {
            logger.warn("The sizeDip of ${config.sizeDip} is invalid. Job '$job' will not run.")
            return@forEach
        }
        val sourceFiles = findSourceImageFiles(logger, job, File(source))
        if (sourceFiles.isEmpty()) {
            logger.lifecycle("Job '$job' does not have any source image files.")
            return@forEach
        }
        val resDirectoryNames = densities.map { "${config.resourceType}-$it" }.joinToString(", ")
        logger.lifecycle("Job '$job' will place image(s) in the subdirectories of '$resourceDir'\n($resDirectoryNames):")
        sourceFiles.forEach { file ->
            logger.lifecycle("    ${file.name}")
            if (logOnly) {
                return@forEach
            }
            val sourceImage = ImageIO.read(file)
            val heightScale = sourceImage.height.toFloat() / sourceImage.width.toFloat()
            for (density in densities) {
                val outDir = File(resourceDir, "${config.resourceType}-$density")
                    .apply { mkdirs() }
                val outFile = File(outDir, file.name)
                val scale = scaleByDensity[density] ?: error("Failed to filter unknown density.")
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

private val scaleByDensity = mapOf(
    "ldpi" to 0.75f,
    "mdpi" to 1f,
    "hdpi" to 1.5f,
    "xhdpi" to 2f,
    "xxhdpi" to 3f,
    "xxxhdpi" to 4f
)
