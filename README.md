<p align="center"><img src="https://raw.githubusercontent.com/CypherCove/IconDivvy/main/img/banner.png"></p>

----

IconDivvy is a Gradle plugin for batch converting:

 - high-resolution raster source images into appropriate Android drawables at multiple resolutions
 - source SVG images into Android vector drawables

and places them in their corresponding resource directories. The resized images are generated 
using [Thumbnailator](https://github.com/coobird/thumbnailator). The SVG images are pre-processed using
[svg-non-stop](https://github.com/14v/svg-non-stop) to make gradients created in Inkscape and some other sources compatible
with conversion to Android drawables.

[![gradle plugin version](https://img.shields.io/badge/gradle%20plugin-v1.0-02303A)](https://plugins.gradle.org/plugin/com.cyphercove.icondivvy)

## Usage

IconDivvy is available via the [Gradle plugins portal](https://plugins.gradle.org/plugin/com.cyphercove.icondivvy). Add IconDivvy in the plugins block:

```groovy
plugins {
    id "com.cyphercove.icondivvy" version "1.0"
}
```

Configure how to distribute the resized icon files:

```groovy
iconDivvy {
    rasterJobs {
        launcherLegacy {
            stagingDir = "icons/launcher-legacy"
            resourceType = "mipmap"
            sizeDip = 48
        }
        launcherAdaptiveForeground {
            stagingDir = "icons/launcher-adaptive"
            resourceType = "drawable"
            sizeDip = 108
        }
    }
    vectorJobs {
        launcherAdaptiveBackground {
            stagingDir = "icons/launcher-adaptive"
            resourceType = "drawable"
        }
    }
}
```

Then run the Gradle task `divvyIcons` (located in the `build` group). There is also a `divvyIconsLogOnly` task for 
previewing which files will be written without actually writing them.

Jobs come in two flavors: `rasterJobs` and `vectorJobs`. Each job defines a staging directory from which it will process
all images found (png/jpg/jpeg or svg for `rasterJobs` and `vectorJobs` respectively).

For raster jobs, source high resolution images should be placed in the staging directory. Each raster job creates resized
copies of each image in the source directory for all the different drawable densities, and places them in their appropriate 
directories in the app resources.

For vector jobs, source SVG files should be placed in the staging directory. Each SVG file is converted to an XML vector
drawable and placed in the resource directory specified by `resourceType`.

`resourceType` can be either `drawable` or `mipmap`. In the case of launcher icons, it is common to use mipmap resources
for legacy launcher icons at 48dp, and use 108dp drawable resources for the raster components of an adaptive icon.

`sizeDip` (only used for raster jobs) is the drawable size in Android density-independent-pixel units, *e.g.*, its
size in pixels at **mdpi**  density. If the image is not square, its aspect ratio is preserved and `sizeDip` corresponds
with the width. The image will be resized for each input density and placed in corresponding resource directories with
the same file name as the source image file. By default, the densities **mdpi** through **xxxdpi** are produced. These can
be customized with the `densities` property (only used for raster jobs):

```groovy
iconDivvy {
    rasterJobs {
        launcherLegacy {
            stagingDir = "icons/launcher-legacy"
            densities = ["ldpi", "mdpi", "hdpi"]
        }
    }
}
```

The default Android resources directory is set to `"app/src/main/res"`. This can be customized using the `resourceDir`
property, which may be necessary for an unconventional project structure, resource directories for specific flavors,
or a multi-module project:

```groovy
iconDivvy {
    rasterJobs {
        launcherLegacyFreeFlavor {
            stagingDir = "icons/launcher-legacy"
            resourceDir = "app/src/main/res-freeFlavor"
        }
    }
}
```

Additional properties:

 - `overwriteExisting` (default **true**), set to **false** to avoid overwriting existing files.
 - `repairMissingStops` (vector jobs only, default **true**), set to **false** to skip repairing SVG gradients.

## License

IconDivvy is covered by the [Apache 2.0 license](LICENSE.md).

IconDivvy uses:

 - [Thumbnailator](https://github.com/coobird/thumbnailator), covered by [this license](https://github.com/coobird/thumbnailator/blob/master/LICENSE).
 - [svg-non-stop](https://github.com/14v/svg-non-stop), covered by [this license](https://github.com/14v/svg-non-stop/blob/master/LICENSE).

The Android robot is reproduced or modified from work created and shared by Google and used according to terms described 
in the [Creative Commons](https://creativecommons.org/licenses/by/3.0/) 3.0 Attribution License
