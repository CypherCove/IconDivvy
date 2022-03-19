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

import org.gradle.api.Project
import java.io.File

open class ResourceBatchJobConfiguration(val name: String) {
    var stagingDir: String? = null
    var resourceDir: String = "app/src/main/res"
    var resourceType: String = "drawable"
    var overwriteExisting: Boolean = true

    /**
     * Checks whether the job has a valid configuration. Returns job parameters or a throwable with an explanatory
     * message.
     */
    internal open fun validate(
        project: Project,
        config: ResourceBatchJobConfiguration
    ): Result<ResourceBatchJobParams> {
        val source = config.stagingDir
            ?: return Result.failure(Throwable("sourceDir must be specified."))
        val resourceDir = File(project.projectDir, config.resourceDir)
        if (!resourceDir.isDirectory) {
            return Result.failure(Throwable("The destResDir $resourceDir is invalid."))
        }
        if (config.resourceType !in RESOURCE_TYPES) {
            return Result.failure(Throwable("The resourceType '${config.resourceType}' is invalid."))
        }
        return Result.success(ResourceBatchJobParams(File(project.projectDir, source), resourceDir))
    }
}

@Suppress("MemberVisibilityCanBePrivate")
open class ResourceBatchJobParams(val sourceDirectory: File, val resourceDirectory: File) {
    operator fun component1(): File = sourceDirectory
    operator fun component2(): File = resourceDirectory
}




private val RESOURCE_TYPES = listOf("drawable", "mipmap")