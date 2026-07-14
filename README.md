# GodotForge Installer

تطبيق Android صغير لتثبيت إضافة **GodotForge AI** داخل مشروع Godot 4 يختاره المستخدم.

## ما الذي يفعله التطبيق؟

1. يفتح منتقي المجلدات الرسمي في Android عبر Storage Access Framework.
2. يتحقق من وجود `project.godot` مباشرة داخل المجلد المحدد.
3. يتحقق من إمكانية القراءة والكتابة.
4. ينشئ المسار `addons/godotforge_ai` عند الحاجة.
5. ينسخ الملفات المضمّنة:
   - `plugin.cfg`
   - `godotforge_ai_plugin.gd`
   - `README.md`
6. إذا كانت الإضافة موجودة، يطلب تأكيدًا ثم ينشئ نسخة احتياطية باسم شبيه بـ:
   - `addons/godotforge_ai_backup_20260713_221500`

التطبيق **لا يستخدم Root**، ولا يطلب صلاحية وصول شاملة للتخزين، ولا يصل إلى أي مكان خارج المجلد الذي اختاره المستخدم.

> الإضافة المضمّنة هي قالب Godot 4 EditorPlugin فعلي وبسيط. لا تتضمن مزود ذكاء اصطناعي ولا مفاتيح API ولا طلبات شبكة.

## حالة التحقق الحالية

أُجريت فحوصات بنيوية وترجمة Kotlin ثابتة، لكن لم يُنتج APK داخل بيئة التسليم لأن Android SDK غير مثبت واتصال الطرفية لم يتمكن من تنزيل Gradle. التفاصيل الدقيقة في `BUILD_STATUS.md`.

**مسار APK المتوقع بعد نجاح البناء:** `app/build/outputs/apk/debug/app-debug.apk`.

## البناء محليًا

المتطلبات:
- JDK 17
- Android SDK Platform 35
- Android Build Tools 35.0.0

الأوامر:

```bash
chmod +x gradlew
./gradlew testDebugUnitTest assembleDebug
```

مسار APK المتوقع بعد نجاح البناء:

```text
app/build/outputs/apk/debug/app-debug.apk
```

نسخة Debug تُوقّع تلقائيًا بمفتاح Debug القياسي، ولذلك تكون قابلة للتثبيت مباشرة للاختبار.

## البناء عبر GitHub Actions

الملف:

```text
.github/workflows/build-apk.yml
```

يعمل عند الدفع إلى `main` أو `master`، وعند فتح Pull Request، أو يدويًا. بعد النجاح يرفع APK كـ Artifact باسم:

```text
GodotForge-Installer-debug-apk
```

## استخدام التطبيق

1. ثبّت APK وافتح التطبيق.
2. اضغط **اختيار مشروع Godot**.
3. اختر المجلد الذي يحتوي مباشرة على `project.godot`.
4. بعد نجاح الفحص، اضغط **تثبيت الإضافة**.
5. افتح المشروع في Godot.
6. انتقل إلى **Project > Project Settings > Plugins** وفعّل **GodotForge AI**.

## تغيير اسم التطبيق والحزمة

- اسم التطبيق: `app/src/main/res/values/strings.xml` و`values-ar/strings.xml`.
- الحزمة و`applicationId`: المتغير `appPackage` في `app/build.gradle.kts`.
- غيّر كذلك مسار ملفات Kotlin ليتوافق مع الحزمة الجديدة.

## ملاحظات أمان

- التطبيق يكتب أسماء ثابتة فقط داخل الشجرة المحددة.
- يتحقق من الأسماء قبل إنشاء الملفات أو نسخ النسخة الاحتياطية.
- إذا وُجد ملف بدل مجلد في `addons` أو `addons/godotforge_ai`، يتوقف ولا يحذفه.
- إعادة التثبيت لا تبدأ إلا بعد موافقة المستخدم.
- النسخة الاحتياطية تُنشأ بالكامل قبل استبدال الملفات المُدارة.
