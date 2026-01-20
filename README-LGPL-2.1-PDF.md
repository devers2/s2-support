## GNU Lesser General Public License, Version 2.1 (LGPL 2.1) Compliance Guide

### 1. License Notice and Written Offer for Source Code Provision

This product uses the OpenHTMLToPDF library (Modules: openhtmltopdf-core, openhtmltopdf-pdfbox), which is licensed under the **LGPL 2.1 License**.
Since this library is used via dynamic linking, the source code of your final product is not affected by the LGPL.

In compliance with the requirements of LGPL 2.1 Section 6, we provide the following written offer:

> **Source Code Provision Offer:**
>
> To anyone who receives this distribution, the complete source code for the LGPL library (OpenHTMLToPDF) included in this product can be obtained for the cost of distribution only (actual costs for preparing and shipping the media).
>
> To request the source code, please contact the following email address, which will be valid for a minimum of **three years** from the date this product was distributed.
>
> **Source Code Request Email:** eseungsu.dev@gmail.com

---

### 2. ❗ Important: OpenHTMLToPDF Dependency Notice (`compileOnly` Method)

This S2Util Library uses the OpenHTMLToPDF library via the **`compileOnly`** method, which means the **OpenHTMLToPDF JAR files are NOT included in this product.**

**To use the S2PdfUtil functionality, the end-user project must explicitly add the following dependencies to be available at runtime.** Failure to include these dependencies will result in a `java.lang.NoClassDefFoundError` at runtime.

**[For Gradle Users]**

```groovy
dependencies {
  // Essential runtime dependencies for S2PdfUtil functionality
  implementation 'org.jsoup:jsoup:1.18.3'
  implementation 'io.github.openhtmltopdf:openhtmltopdf-core:1.1.24'
  implementation 'io.github.openhtmltopdf:openhtmltopdf-pdfbox:1.1.24'
}
```
