// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // This plugin provides the `alias()` function for the version catalog (libs.versions.toml)
    // It should be 'id' not 'alias' at the top level.
    id("com.android.application") version "8.13.0" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
