# Third party notices

* Symja / matheclipse-core 3.2.0: https://github.com/axkr/symja_android_library ; LGPL-3.0 / GPL-3.0, as specified by the upstream source and transitive components.
* Guava 33.5.0-android: Apache-2.0.
* Log4j API (declared 2.17.2, resolved 2.25.4 through transitive dependencies) and log4j2-android 1.0.0: Apache-2.0.
* desugar_jdk_libs: upstream Android Open Source Project licenses.
* Google ML Kit Text Recognition 16.0.1 (bundled Latin model): Google SDK and model terms apply; see https://developers.google.com/ml-kit/vision/text-recognition/v2/android . This component is not included in the calculator's source-code license. OCR runs on device. The local tabs/update test build requests INTERNET to check public GitHub release metadata on demand; ACCESS_NETWORK_STATE remains excluded.
* org.json 20240303: test dependency only, not added to the APK.

The Gradle dependency graph records all transitive dependencies. Original license resources are retained inside the APK (duplicate resources use the first available copy). For redistribution, consult each upstream component's full license.
