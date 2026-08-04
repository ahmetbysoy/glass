# GlassPro Proje İncelemesi — Sorunlar & Geliştirme Fikirleri

## Proje Özeti
Android (Compose) kripto likidasyon takip ve tahmin uygulaması. Binance, OKX, Bybit, Bitget, Hyperliquid, Gate Futures gibi borsalardan veri çekiyor. WebSocket + REST kullanıyor. Crash handler, banner sistemi, 4 sekmeli UI mevcut.

---

## Tespit Edilen Sorunlar

### 1. README Eksikliği
Repo'da README yok. Proje ne yapar, nasıl çalıştırılır bilinmiyor.

### 2. Sürüm Uyuşmazlığı (`build.gradle.kts`)
`versionCode = 1` ama `versionName = "2.0.0"`. Ayrıca namespace `com.glasspro.tracker` ama committe `rebrand` yazıyor; eski isim kalıntıları olabilir.

### 3. .env ve API Anahtarları
`.env.example` var ama `.env` repo'ya girerse risk. Secrets plugin kullanılıyor ama güvenli saklama (Android Keystore / EncryptedSharedPreferences) yok gibi görünüyor.

### 4. Crash Handler Eksik Detay
Crash log `crash.log` dosyasına yazılıyor ama kullanıcıya sadece metin gösteriliyor; gönderim butonu yok (mail/API'ye gönderme yok). Ayrıca `filesDir` sınırlı, log boyut sınırı yok.

### 5. Dependency Injection Elle Yapılmış
`ServiceLocator` manuel. Hilt veya Koin yok. Bu büyük projede sürdürülebilir değil, test edilebilirlik düşük.

### 6. Error Handling Zayıf
`MarketViewModel`'da `repository.start()` çağrılıyor ama hata durumları (feedHealth hariç) UI'a yansıtılmıyor. Kullanıcı bağlantı kesilince ne olduğunu göremeyebilir.

### 7. WebSocket Bağlantı Yönetimi
`WebSocketClient`'ta yeniden bağlanma (reconnect) mantığı görünmüyor. Bağlantı koparsa sessiz kalma riski.

### 8. Test Kapsamı Düşük / Görünür Değil
`testInstrumentationRunner` tanımlı ama test dosyaları repo'da görünmüyor veya çok az.

### 9. ProGuard Kuralları Minimal
`proguard-rules.pro` boş gibi görünüyor. Release'de minify açık (`isMinifyEnabled = true`) ama kurallar yetersizse app çökebilir.

### 10. UI / UX Sorunları
- Banner'da emoji (`🔴`) kullanılmış, font/render farkı olabilir.
- `MainScreen` çok uzun, tek dosyada hem UI hem navigation mantığı karışık.
- `CrashLogDialogIfPresent` `LocalContext.current` kullanıyor ama `ClipboardManager` erişimi modern Android'de izin gerektirebilir.

---

## Geliştirme Fikirleri (Öncelik Sırasıyla)

### Kısa Vadeli (Hızlı Kazanç)
1. **README.md ekle** — Proje açıklaması, kurulum, `.env.example` açıklaması.
2. **Sürüm tutarlılığı** — `versionCode = 2`, `versionName = "2.0.0"` veya semver kurallarına oturt.
3. **ProGuard kuralları** — Room, Retrofit, OkHttp, Compose için `-keep` kuralları ekle.
4. **Log sınırlama** — `crash.log` maksimum 50KB olsun, eski girişler silinsin.
5. **Error State UI** — `feedHealth` dışında bağlantı hatası gösteren bir banner/alert ekle.

### Orta Vadeli (Mimari & Kalite)
6. **Hilt'e Geçiş** — `ServiceLocator` yerine Hilt kullanarak test edilebilirliği artır.
7. **Repository Pattern Güçlendirme** — `MarketRepository` soyutlanmış ama adapterler doğrudan repository'ye bağlı; interface抽取 edilebilir.
8. **WebSocket Reconnect** — Exponential backoff ile yeniden bağlanma mantığı ekle.
9. **Unit Test Yaz** — `MarketAnalysisEngine`, adapter'ler için mock'lu testler.
10. **CI/CD** — GitHub Actions ile build, lint, test otomasyonu kur.

### Uzun Vadeli (Özellik & Ürün)
11. **Koyu/Açık Mod (Dynamic Theme)** — `Theme.GlassPro` statik gibi görünüyor; Material You desteği.
12. **Bildirimler (Notification)** — Önemli likidasyon olayları için foreground service + bildirim.
13. **Offline Cache** — Room DB zaten var; son analiz sonuçlarını offline göster.
14. **Ayarlar Sekmesi Geliştirme** — Yeni parametreler (zaman dilimi seçimi, sesli uyarı, widget desteği).
15. **Widget (App Widget)** — Ana ekrana küçük bir "HIT Rate" veya son likidasyon widget'i.
16. **Çoklu Dil (i18n)** — Şu an Türkçe sabit; `strings.xml` ile İngilizce desteği.
17. **Analytics / Telemetry** — Firebase Analytics veya Amplitude ile kullanıcı davranış takibi (opt-in).
18. **Güvenlik** — API anahtarlarını `EncryptedSharedPreferences` veya Android Keystore ile sakla; `.env` yerine `local.properties` kullan.

---

## Özet Tablo

| Alan | Durum | Öneri |
|---|---|---|
| Dokümantasyon | ❌ Eksik | README ekle |
| Build Config | ⚠️ Tutarsız | Sürüm/ProGuard düzelt |
| Mimari | ⚠️ Manuel DI | Hilt'e geç |
| Hata Yönetimi | ⚠️ Kısmi | Reconnect + UI hata durumu |
| Test | ❌ Az/Görünmez | Unit + UI test ekle |
| Güvenlik | ⚠️ Zayıf | Anahtar saklama, log sınırlama |
| UX | ✅ İyi başlangıç | Widget, bildirim, çoklu dil |

---

*İnceleyen: Assistant*  
*Proje: https://github.com/ahmetbysoy/glass*
