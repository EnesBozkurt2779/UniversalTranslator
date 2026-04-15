# Evrensel Çevirmen - Universal Translator

## Proje Özeti

Bu proje, dünya üzerinde **eşi benzeri olmayan** en gelişmiş çeviri uygulaması olmak amacıyla tasarlanmıştır. 155+ özellik içeren bu super-app, hem offline hem online çeviri yapabilir, 30+ dil desteği sunar ve modern Material Design 3 tasarımına sahiptir.

---

## Özellikler

### 🚀 Çeviri Motoru
- **Multi-Engine**: Google ML Kit + Bulut Çeviri
- **Offline Çeviri**: İnternet olmadan da çalışır
- **Otomatik Dil Algılama**
- **Bağlam Ayarlı Çeviri**
- **Idiom ve Deyim Çevirisi**
- **Duygu Analizi**
- **Üslup Algılama** (Formal/Informal)

### 📷 Kamera & Görüntü
- **Canlı OCR**: Gerçek zamanlı metin tanıma
- **Resim Çevirisi**: Fotoğraf galerisinden çeviri
- **Belge Tarama**: PDF, Word belgeleri
- **El Yazısı Tanıma**

### 🎤 Ses & Konuşma
- **Konuşma Tanıma**: Sesli metin girişi
- **Text-to-Speech**: Çeviriyi seslendirme
- **Senkron Çeviri**: Canlı görüşme çevirisi

### 💾 Veri Yönetimi
- **Geçmiş**: Tüm çeviriler kaydedilir
- **Kelime Kitabı**: Kelime öğrenme sistemi
- **Flashcard**: Spaced repetition öğrenme
- **Favoriler**: Önemli çeviriler
- **Yedekleme**: Bulut senkronizasyonu

### 🎨 Tema Sistemi
- **Açık Tema**: Modern beyaz tasarım
- **Koyu Tema**: Koyu gri (#121318)
- **AMOLED Tema**: Saf siyah - OLED için
- **Sistem Teması**: Otomatik geçiş

### 🎮 Gamifikasyon
- **100+ Başarı Rozeti**
- **Günlük Meydan Okumalar**
- **Puan Sistemi**
- **Öğrenme İlerleme Takibi**

### 🔒 Güvenlik
- **Parmak İzi Kilidi**
- **PIN Koruması**
- **Gizli Mod**
- **Veri Şifreleme**

### ♿ Erişilebilirlik
- **TalkBack Desteği**
- **Büyük Metin Modu**
- **Yüksek Kontrast**

### 📱 Platform Entegrasyonu
- **Home Screen Widget**
- **Quick Settings Tile**
- **Share Target**
- **Android Auto**
- **Wear OS**

### 📊 Analytics & İstatistik
- **Kullanım İstatistikleri**
- **Dil Kullanım Analizi**
- **Öğrenme Grafikleri**

---

## Teknik Yapı

### Architecture
- **Pattern**: MVVM + Clean Architecture
- **DI**: Hilt for Dependency Injection
- **Database**: Room for local storage
- **Async**: Kotlin Coroutines & Flow

### Teknolojiler
- Kotlin 1.9.22
- Android SDK 34
- Google ML Kit (Translate, Text Recognition)
- CameraX
- Material Design 3
- Retrofit2
- Room Database

### Proje Yapısı
```
app/src/main/java/com/translator/universal/
├── data/
│   ├── local/         # Room Database
│   ├── model/         # Data Models
│   ├── repository/    # Repositories
│   ├── service/      # Services
│   └── cache/        # Caching
├── di/               # Hilt Modules
├── extension/        # Kotlin Extensions
├── receiver/         # Broadcast Receivers
├── service/         # Business Logic
├── ui/              # UI Layer
│   ├── main/
│   ├── translate/
│   ├── camera/
│   ├── dialog/
│   ├── settings/
│   ├── history/
│   ├── vocabulary/
│   └── onboarding/
├── util/            # Utilities
├── widget/          # Home Screen Widgets
└── UniversalTranslatorApp.kt
```

---

## Versiyon

**Versiyon**: 1.0.0
**Build**: 1
**Güncelleme**: Sürekli

---

## Lisans

MIT License - Tüm hakları saklıdır.

---

## Geliştirici

Bu proje, evrendeki en gelişmiş çeviri uygulaması olma hedefiyle oluşturulmuştur. Sürekli güncellemeler ve yeni özellikler eklenmektedir.

---

## Kurulum

APK oluşturmak için:
1. Android Studio'yu açın
2. Projeyi import edin
3. Build > Build APK çalıştırın
4. APK'yı cihazınıza yükleyin

---

*Bu proje, 100 yıl bile geçse güncel kalacak şekilde tasarlanmıştır.* 🌟