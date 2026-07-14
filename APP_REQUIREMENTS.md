# متطلبات GodotForge Installer

## الهدف
تطبيق Android Setup يختار المستخدم من خلاله المجلد الجذر لمشروع Godot، ويتحقق من وجود `project.godot`، ثم يثبت إضافة Godot 4 داخل:

`addons/godotforge_ai`

## السلوك الوظيفي
- اختيار مجلد عبر Storage Access Framework فقط.
- الاحتفاظ بصلاحية URI الدائمة للقراءة والكتابة.
- رفض المجلد إذا لم يوجد `project.godot` مباشرة داخله.
- عدم الوصول إلى أي مسار خارج الشجرة التي اختارها المستخدم.
- إنشاء `addons` و`godotforge_ai` عند عدم وجودهما.
- عند وجود الإضافة مسبقًا: طلب تأكيد صريح، ثم نسخها احتياطيًا داخل `addons` قبل الكتابة.
- عدم حذف الملفات الإضافية أو أي نسخة احتياطية.
- تضمين إضافة Godot 4 أساسية بلا شبكة وبلا مفاتيح API.

## المتطلبات التقنية
- Kotlin وAndroid Views.
- Gradle Kotlin DSL.
- Java 17.
- `minSdk 26`، و`targetSdk 35`، و`compileSdk 35`.
- دون Root ودون صلاحيات تخزين عامة.
- GitHub Actions لبناء APK بتوقيع Debug ورفعه كـ Artifact.
