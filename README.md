<p align="center"><img src="https://raw.githubusercontent.com/CypherCove/IconDivvy/master/img/banner.png"></p>
----

IconDivvy is a Gradle plugin that converts a high-resolution raster source image into appropriate drawable resources 
at various sizes and places them in their corresponding resource directories. The resized images are generated using
[Thumbnailator](https://github.com/coobird/thumbnailator).

![version](https://img.shields.io/badge/version-0.2-red)

## Usage

Add IconDivvy as a build script dependency:

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath ("com.cyphercove.icondivvy:icondivvy:0.2")
    }
}
```

Import the plugin and configure how to distribute the resized icon files:

```groovy
apply plugin: "com.cyphercove.icondivvy"

iconDivvy {
    jobs {
        launcherLegacy {
            stagingDir = "icons/launcher-legacy"
            resourceType = "mipmap"
            sizeDip = 48
        }
        launcherAdaptive {
            stagingDir = "icons/launcher-adaptive"
            resourceType = "drawable"
            sizeDip = 108
        }
    }
}
```

Then run the Gradle task `divvyIcons` (located in the `build` group). There is also `divvyIconsLogOnly` task that can be
run to preview which files will be written without actually writing them.

Source high resolution images should be placed in a staging directory in the project. Each staging directory corresponds 
with one job. Each job finds all the image files in the source staging directory, resizes it for all the different drawable
densities, and places them in their appropriate places in the app resources.

Each job's name is used only for logging purposes. In the example above, there are two jobs named "`launcherLegacy`" and
"`launcherAdaptive`". There are separate jobs (and staging directories) because Android legacy and adaptive icon images
have different sizes, and legacy icons should preferably be `mipmap` resources. 

`sizeDip` is the size in pixels of the drawable at **mdpi** density in Android density-independent-pixel units. If the 
image is not square, its aspect ratio is preserved and `sizeDip` corresponds with the width. The image will be resized 
for each input density and placed in corresponding resource directories with the same file name as the source image 
file. By default, the densities **mdpi** through **xxxdpi** are produced. These can be customized with the `densities` 
property:

```groovy
iconDivvy {
    jobs {
        launcherLegacy {
            stagingDir = "icons/launcher-legacy"
            densities = ["ldpi", "mdpi", "hdpi"]
        }
    }
}
```

The default Android resources directory is set to '"app/src/main/res"'. This can be customized using the `resourceDir`
property, which may be necessary for an unconventional project structure, resource directories for specific flavors,
or a multi-module project.

```groovy
iconDivvy {
    jobs {
        launcherLegacyFreeFlavor {
            stagingDir = "icons/launcher-legacy"
            resourceDir = "app/src/main/res-freeFlavor"
        }
    }
}
```

There is also a Boolean property, `overwriteExisting` (default **true**), which can be set to **false** to avoid 
overwriting existing files.

## License

IconDivvy is covered by the [Apache 2.0 license](LICENSE.md).

IconDivvy uses [Thumbnailator](https://github.com/coobird/thumbnailator), covered by 
[this license](https://github.com/coobird/thumbnailator/blob/master/LICENSE).

The Android robot is reproduced or modified from work created and shared by Google and used according to terms described 
in the [Creative Commons](https://creativecommons.org/licenses/by/3.0/) 3.0 Attribution License
