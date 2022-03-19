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
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.round

class RasterJobConfiguration(name: String): ResourceBatchJobConfiguration(name) {
    var sizeDip: Int = 48
    var densities: List<String> = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")

    override fun validate(project: Project, config: ResourceBatchJobConfiguration): Result<ResourceBatchJobParams> {
        for (density in densities) {
            if (density !in SCALE_BY_DENSITY.keys) {
                return failureMessage("Unknown density $density.")
            }
        }
        if (densities.isEmpty()) {
            return failureMessage("No densities provided.")
        }
        if (sizeDip <= 0) {
            return failureMessage("The sizeDip of $sizeDip is invalid.")
        }
        return super.validate(project, config)
    }
}

fun executeRasterJobs(project: Project, jobs: Iterable<RasterJobConfiguration>, logOnly: Boolean) {
    val logger = project.logger

    jobs@ for (config in jobs) {
        val job = config.name

        val (sourceDir, resourceDir) = config.validate(project, config)
            .onFailure { logger.warn("${it.message} Raster job '$job' will not run.") }
            .getOrNull() ?: continue@jobs

        val sourceFiles = findSourceFiles(logger, job, sourceDir, Path::isRasterImageFile)
        if (sourceFiles.isEmpty()) {
            logger.lifecycle("Raster job '$job' does not have any valid source image files or staging directory does not exist.")
            continue
        }

        val resDirectoryNames = config.densities.joinToString(", ") { "${config.resourceType}-$it" }
        logger.lifecycle("Raster job '$job' will place the following image(s) in the subdirectories of '$resourceDir'\n($resDirectoryNames):")
        for (file in sourceFiles) {
            logger.lifecycle("    ${file.name}")
            if (logOnly) {
                continue
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

private val SCALE_BY_DENSITY = mapOf(
    "ldpi" to 0.75f,
    "mdpi" to 1f,
    "hdpi" to 1.5f,
    "xhdpi" to 2f,
    "xxhdpi" to 3f,
    "xxxhdpi" to 4f
)