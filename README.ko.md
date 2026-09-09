# S2 Support Library

[English](README.md) | [한국어](README.ko.md)

[![Java CI](https://github.com/devers2/s2-support/actions/workflows/ci.yml/badge.svg)](https://github.com/devers2/s2-support/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.devers2.internal/s2-support?color=brightgreen&label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.devers2.internal/s2-support)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](./LICENSE)

> `s2-core` 및 `s2-validator` 기반으로 구축된 **주관적(Opinionated) 보조 라이브러리**로, 유용한 헬퍼 클래스와 편의 유틸리티 모음을 제공합니다.  
> 주로 **개인 개발 워크플로** 간소화와 반복적인 애플리케이션 패턴 지원을 목적으로 설계되었습니다.

---

## 📖 개요 (Overview)

`s2-support`는 실용적인 Java/Spring 애플리케이션 개발에 맞춰 자주 사용하는 헬퍼 모듈과 반복적인 보일러플레이트를 모아둔 라이브러리입니다. 파일 관리, 페이징, Spring 컨텍스트 유틸리티, JSON/암호화/이미지 헬퍼 등을 포함합니다.

---

## ✨ 주요 유틸리티

- **📁 파일 관리** — `FileManager`, `S2File`, `S2RemoteFile`을 통한 로컬 및 원격(SFTP/JSch) 파일 처리
- **📄 페이징** — 목록/검색 UI를 위한 `S2PaginationInfo`, `S2PaginationTag`, `S2SearchVO` 제공
- **🍃 Spring 유틸리티** — Spring 기반 앱을 위한 `S2ContextUtil`, `S2AutoConfiguration`, `S2AnnotationResolver`, `S2RestApiUtil`
- **🔧 범용 헬퍼** — `S2JsonUtil`, `S2HashUtil`, `S2EncryptionUtil`, `S2ImageUtil`, `S2TypeUtil`, `S2CollectionUtil`, `S2StreamUtil`, `S2ServletUtil`, `S2QueryStringUtil`, `S2Uuid`
- **🗂️ 자료구조** — `S2LruMap` (`LinkedHashMap` 기반 LRU 캐시)

---

## 🚀 설치 (Installation)

`build.gradle`에 다음 의존성을 추가합니다.

**[Gradle]**

```groovy
dependencies {
    implementation 'io.github.devers2.internal:s2-support:1.1.3'
}
```

**[Maven]**

```xml
<dependency>
    <groupId>io.github.devers2.internal</groupId>
    <artifactId>s2-support</artifactId>
    <version>1.1.3</version>
</dependency>
```

---

## ⚙️ 요구사항 (Requirements)

본 프로젝트는 **JDK 21** 환경에서 빌드되었으나, **Java 17 이상**의 모든 환경에서 안정적으로 사용할 수 있습니다.

---

## 📜 라이선스 및 저작권 (License & Copyright)

본 라이브러리는 **Apache License 2.0** 하에 제공됩니다. 사용자는 라이선스의 의무 사항(저작권 고지, 소스 코드 공개 범위 등)을 준수하는 조건 하에 자유롭게 사용, 수정 및 재배포가 가능합니다. 상세한 조건은 **[LICENSE](./LICENSE)** 파일을 반드시 확인해 주세요.

- **저작권 2020 - 2026 devers2 (이승수, 대한민국 대전)**
- 문의: [eseungsu.dev@gmail.com](mailto:eseungsu.dev@gmail.com)

**제3자 라이브러리 고지:** 본 프로젝트는 외부 라이브러리를 사용합니다. 상세한 제3자 라이브러리 고지사항은 **[licenses/NOTICE](./licenses/NOTICE)** 파일을 참조해 주세요.

---

s2-support Version: 1.1.3 (2026-09-09)

[//]: # 'S2_DEPS_INFO_START'

---

**특정 기능을 사용하려면 런타임에 다음 의존성을 엔드유저 프로젝트에 명시적으로 추가해야 합니다.** 이 의존성이 누락되면 런타임에 `java.lang.NoClassDefFoundError`가 발생합니다.

**[Gradle 사용자]**

```groovy
dependencies {
    // 선택적 기능을 위한 필수 런타임 의존성
    implementation 'io.github.devers2:s2-core:1.1.7'
    implementation 'org.springframework:spring-context:6.1.1'
    implementation 'org.springframework:spring-web:6.1.1'
    implementation 'org.springframework.integration:spring-integration-sftp:6.1.1'
    implementation 'jakarta.servlet:jakarta.servlet-api:6.1.0'
    implementation 'jakarta.servlet.jsp:jakarta.servlet.jsp-api:4.0.0'
    implementation 'org.aspectj:aspectjweaver:1.9.25.1'
}
```

[//]: # 'S2_DEPS_INFO_END'
