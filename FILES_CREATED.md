# الملفات المُنشأة

## إعداد المشروع والبناء
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml`
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper-src/org/gradle/wrapper/GradleWrapperMain.java` (مصدر bootstrap المضمّن)
- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `.github/workflows/build-apk.yml`

## مصدر Android
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/godotforge/installer/data/GodotProjectRepository.kt`
- `app/src/main/java/com/godotforge/installer/data/ProjectSelectionStore.kt`
- `app/src/main/java/com/godotforge/installer/domain/AddonManifest.kt`
- `app/src/main/java/com/godotforge/installer/domain/InstallerModels.kt`
- `app/src/main/java/com/godotforge/installer/ui/InstallerUiState.kt`
- `app/src/main/java/com/godotforge/installer/ui/MainViewModel.kt`
- `app/src/main/java/com/godotforge/installer/ui/MainActivity.kt`

## موارد الواجهة
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-ar/strings.xml`
- موارد أيقونة المشغل داخل `drawable` و`mipmap-anydpi-v26`

## إضافة Godot المضمّنة
- `app/src/main/assets/godotforge_ai/plugin.cfg`
- `app/src/main/assets/godotforge_ai/godotforge_ai_plugin.gd`
- `app/src/main/assets/godotforge_ai/README.md`

## الاختبار والتوثيق
- `app/src/test/java/com/godotforge/installer/domain/AddonManifestTest.kt`
- `APP_REQUIREMENTS.md`
- `BUILD_STATUS.md`
- `README.md`
- `README_AR.md`
- `FILES_CREATED.md`
- `.gitignore`
