[![](https://jitpack.io/v/XuQK/KDQrScanner.svg)](https://jitpack.io/#XuQK/KDQrScanner)

本库是利用 Google 出品的 ML Kit 中的 Barcode Scanning 组件，封装成的一个二维码识别库，速度极快，识别精准，具体信息可参考[链接](https://developers.google.com/ml-kit/vision/barcode-scanning)。

**注意：**本库采用了 alpha 版本的的 cameraX 组件，请谨慎使用。

主要功能：

1. 扫描识别单个/多个识别码，并提供其位置标注，可点击选择使用哪个识别码。
   > 由于识别速度过快，扫描识别的时候，默认识别4次(可通过修改totalAnalyseTimes进行改变)，取结果合集，降低反应速度，提高识别准确度。
2. 可继承 `github.xuqk.kdqrscanner.ScanResultView` 自定义结果展示页面。
3. 可识别相册图片中的识别码。

效果图：

![](demo.webp)

# 使用方式：

## 将JitPack存储库添加到构建文件(项目根目录下build.gradle文件)

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

## 添加依赖项

```groovy
// 版本号参看Release
implementation 'com.github.XuQK:KDQrScanner:versionCode'
```

## 使用说明

参考 demo 中的 `github.xuqk.kdqrscanner.ScanFragment`。
