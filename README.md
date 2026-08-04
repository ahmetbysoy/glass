# GlassPro - Real-time Crypto Derivative Analytics & Liquidation Tracker

**GlassPro** is an advanced, high-frequency cryptocurrency derivative analytics and liquidation tracking app built with **Kotlin** and **Jetpack Compose**. It provides real-time multi-exchange WebSocket monitoring, order book imbalance detection, liquidation cascades analysis, short/long ratio analysis, and 1-minute direction prediction models across major crypto venues.

---

## 🚀 Key Features

- ⚡ **Multi-Exchange WebSocket Engine**: Real-time ticker, trade flow, depth, and liquidation data streams from **Binance Futures**, **OKX**, **Bybit**, **Bitget**, **Hyperliquid**, and **Gate Futures**.
- 📊 **Liquidation Cascade Tracker**: Tracks short & long liquidation events, cumulative dollar volumes, and volatility spikes.
- 🎯 **Predictive Analytics & Engine**: Calculates composite market direction, confidence score, ATR, CVD (Cumulative Volume Delta), Funding rate deviation, and order book imbalance.
- 🛡️ **Risk & Spoof Detection Engine**: Analyzes spoofing patterns, fake breakouts, volume mismatches, and manipulation indexes.
- 💾 **Offline Cache & Room Persistence**: Local database storing high-priority analysis snapshots and market statistics.
- 📱 **Modern Material 3 Jetpack Compose UI**: Clean dark theme, fast tabs (Live Feed, Analytics, AI Analysis, Settings), and responsive layouts.

---

## 🛠️ Tech Stack & Architecture

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM + Clean Architecture / Service Locator
- **Asynchronous / Flow**: Kotlin Coroutines & `StateFlow`
- **Networking**: OkHttp 4.x + WebSockets
- **Persistence**: Room Database (KSP)
- **Testing**: Robolectric + Roborazzi Screenshot Testing
- **CI/CD**: GitHub Actions (`.github/workflows/glass_build_workflow.yml`)

---

## 📂 Project Structure

```text
app/src/main/java/com/glasspro/tracker/
├── GlassProApplication.kt        # Application Entry & Crash Handler
├── MainActivity.kt               # Main Activity & Compose Root
├── core/
│   ├── config/Watchlist.kt       # Default & Custom Tracked Symbols
│   ├── di/ServiceLocator.kt      # Central Dependency Injection / Services
│   ├── math/                     # Technical Indicators & Statistics
│   ├── model/                    # Data Classes (Market, Analysis, Signals)
│   └── util/                     # Utilities & Deduplicators
├── data/
│   ├── db/                       # Room Database & DAOs
│   ├── engine/                   # OrderBook, Risk, Strategy, & Analytics Engines
│   ├── remote/                   # WebSocket Clients, REST Services, Exchange Adapters
│   └── repository/               # MarketRepository & Analysis Mappers
└── ui/
    ├── screens/                  # MainScreen, LiveFeedTab, AnalyticsTab, AnalysisTab, SettingsTab
    ├── theme/                    # Material 3 Theme, Color Palette, Typography
    └── viewmodel/                # MarketViewModel
```

---

## ⚙️ CI/CD & Building

### GitHub Actions Workflow
The project includes a GitHub Actions workflow `.github/workflows/glass_build_workflow.yml` that automatically:
1. Sets up JDK 17 & Gradle Caching.
2. Runs unit tests (`./gradlew testDebugUnitTest`).
3. Builds Debug and Release APKs (`./gradlew assembleDebug`, `./gradlew assembleRelease`).
4. Uploads APK artifacts to GitHub Releases/Actions.

### Local Building
To build locally with Gradle:

```bash
# Run Unit Tests
./gradlew testDebugUnitTest

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
