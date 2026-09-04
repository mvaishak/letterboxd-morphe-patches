extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "app.template.extension"
}

dependencies {
    // Present in the Letterboxd APK at runtime; not bundled.
    compileOnly("androidx.fragment:fragment:1.8.5")
}
