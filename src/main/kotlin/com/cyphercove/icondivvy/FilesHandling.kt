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

import org.gradle.api.logging.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

internal val Path.isRasterImageFile: Boolean
    get() = fileName.toString().run { endsWith(".png") || endsWith(".jpg") || endsWith(".jpeg") }

internal val Path.isSvgFile: Boolean
    get() = fileName.toString().endsWith(".svg")

internal val Path.hasValidName: Boolean
    get() = fileName.toString().substringBeforeLast('.').let { name ->
        name.all { it == '_' || it.isDigit() || it.isLowerCase() } &&
                name !in JAVA_KEYWORDS
    }

private val JAVA_KEYWORDS = """
abstract assert boolean break byte case catch char class const continue
default do double else enum extends final finally float for goto if
implements import instanceof int interface long native new package
private protected public return short static strictfp super switch
synchronized this throw throws transient try void volatile while 
""".trim().split(" ", "\n")

internal fun findSourceFiles(logger: Logger, jobName: String, sourceDir: File, fileTypeFilter: (Path)->Boolean): List<File> {
    if (!sourceDir.exists()) {
        return emptyList()
    }
    return Files.walk(sourceDir.absoluteFile.toPath(), 1)
        .asSequence()
        .filter(fileTypeFilter)
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