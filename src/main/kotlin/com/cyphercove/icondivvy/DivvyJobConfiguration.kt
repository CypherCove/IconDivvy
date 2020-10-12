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

open class DivvyJobConfiguration(val name: String) {
    var stagingDir: String? = null
    var resourceDir: String = "app/src/main/res"
    var resourceType: String = "drawable"
    var sizeDip: Int = 48
    var densities: List<String> = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
    var overwriteExisting: Boolean = true
}