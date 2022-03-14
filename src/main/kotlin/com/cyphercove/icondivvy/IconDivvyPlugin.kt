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

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project

internal const val TASK_GROUP = "build"
internal const val PROJECT_EXTENSION = "iconDivvy"
internal const val DIVVY_ICONS_TASK_NAME = "divvyIcons"
internal const val DIVVY_ICONS_LOG_ONLY_TASK_NAME = "divvyIconsLogOnly"

class IconDivvyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(PROJECT_EXTENSION, IconDivvyExtension::class.java, project)
        project.tasks.create(DIVVY_ICONS_TASK_NAME) { task ->
            task.doLast {
                executeRasterJobs(project, extension.rasterJobs, false)
            }
            task.group = TASK_GROUP
        }
        project.tasks.create(DIVVY_ICONS_LOG_ONLY_TASK_NAME) { task ->
            task.doLast {
                project.logger.lifecycle("Running IconDivvy in 'logOnly' mode. No files will be written.")
                executeRasterJobs(project, extension.rasterJobs, true)
            }
            task.group = TASK_GROUP
        }
    }

}

open class IconDivvyExtension(project: Project) {
    val rasterJobs: NamedDomainObjectContainer<RasterJobConfiguration> = project.container(RasterJobConfiguration::class.java)

    fun rasterJobs(config: Closure<Unit>) {
        rasterJobs.configure(config)
    }
}