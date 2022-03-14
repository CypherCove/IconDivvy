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

import com.android.ide.common.vectordrawable.Svg2Vector
import io.github.fourteenv.NonStop
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class VectorJobConfiguration(name: String): ResourceBatchJobConfiguration(name) {
    var repairMissingStops: Boolean = true
}

fun executeVectorJobs(project: Project, jobs: Iterable<VectorJobConfiguration>, logOnly: Boolean) {
    val logger = project.logger

    jobs@ for (config in jobs) {
        val job = config.name

        val (sourceDir, resourceDir) = config.validate(project, config)
            .onFailure { logger.warn("${it.message} Vector job '$job' will not run.") }
            .getOrNull() ?: continue@jobs

        val sourceFiles = findSourceFiles(logger, job, sourceDir, Path::isSvgFile)
        if (sourceFiles.isEmpty()) {
            logger.lifecycle("Vector job '$job' does not have any valid source image files or staging directory does not exist.")
            continue
        }

        val outDir = File(resourceDir, config.resourceType)

        logger.lifecycle("Vector job '$job' will place the following image(s) in '$resourceDir\\${config.resourceType}':")
        for (file in sourceFiles) {
            val outputFileName = file.nameWithoutExtension + ".xml"
            logger.lifecycle("    $outputFileName")
            if (logOnly) {
                continue
            }
            var sourceFile = file
            if (config.repairMissingStops)  {
                sourceFile = file.repairMissingStops(logger) ?: continue
            }
            FileOutputStream(File(outDir, outputFileName)).use { outputStream ->
                val errorString = try {
                    Svg2Vector.parseSvgToXml(sourceFile, outputStream)
                } catch (e: IOException) {
                    e.message ?: "An IOException without message was encountered."
                }
                if (errorString.isNotEmpty()) {
                    logger.error("File ${file.name} had the following error during conversion, which may have prevented writing the new file:\n$errorString")
                }
            }
        }
    }
}

private fun File.repairMissingStops(logger: Logger): File? {
    try {
        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = documentBuilder.parse(this)
        val nodes = document.documentElement.childNodes
        if (!NonStop.processSvg(nodes)) {
            return this
        }
        val outFile = File.createTempFile("com.cyphercove.icondivvy", ".svg").apply {
            deleteOnExit()
        }
        val domSource = DOMSource(document)
        val streamResult = StreamResult(outFile)
        TransformerFactory.newInstance().newTransformer().transform(domSource, streamResult)
        return outFile
    } catch (e: Exception) { //IOException or TransformerException
        logger.error("Failed to repair the missing stops in file $this. It will be skipped.")
        return null
    }
}