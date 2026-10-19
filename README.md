# ShopFlow

ShopFlow is a modern Android e-commerce application built with Jetpack Compose, Kotlin Coroutines & Flow, Ktor, and Room. It uses its **own custom Ktor REST API** (`:server` module) with a high-performance **Ktor Client** (`KtorShopFlowApi`) in place of third-party APIs to deliver a reactive, offline-first product catalog with search, category filtering, multi-criteria sorting, and shopping cart persistence.

---

## Table of Contents
1. [Screenshots](#screenshots)
2. [Offline Cart](#offline-cart)
3. [Ktor API Server](#ktor-api-server)
4. [Setup & Build Instructions](#setup--build-instructions)
5. [Architecture](#architecture)
6. [Libraries Used](#libraries-used)
7. [Local Storage Approach](#local-storage-approach)
8. [Important Design Decisions](#important-design-decisions)
9. [Production Scope](#production-scope)

---

## Screenshots

| Product Catalog | Product Details | Cart |
|---|---|---|
| <img src="docs/images/product_catalog.png" width="250" alt="Product Catalog" /> | <img src="docs/images/product_details.png" width="250" alt="Product Details" /> | <img src="docs/images/cart.png" width="250" alt="Cart" /> |

---

## Offline Cart

The cart is persisted locally using Room and remains fully functional
when the device is offline.

---

## Ktor API Server

ShopFlow includes its own Kotlin Ktor backend server in the `:server` module, replacing the external DummyJSON API.

### Running the Ktor Server
To start the Ktor API server locally on port 8080:
```bash
./gradlew :server:run
```
The server binds to all host interfaces on port `8080`. Verify it locally at `http://127.0.0.1:8080/`.

### Connecting a Physical Android Device

`127.0.0.1` and `localhost` on an Android device refer to the device itself, not the computer running Ktor. Debug builds use a configurable HTTP base URL and permit cleartext traffic only in the debug manifest.

1. Put the phone and development computer on the same Wi-Fi network and start the server:
   ```bash
   ./gradlew :server:run
   ```
2. Find the computer's LAN IPv4 address, then install the debug app with it:
   ```bash
   ./gradlew :app:installDebug -Pshopflow.serverUrl=http://YOUR_COMPUTER_LAN_IP:8080
   ```
   For example: `http://192.168.0.102:8080`.
3. If macOS or another host firewall prompts, allow incoming connections for the Java/Ktor process on the private network.

For a USB-connected device, an alternative is ADB reverse tunneling. Run this after connecting the device with USB debugging enabled, then install using the device-local address:
```bash
adb reverse tcp:8080 tcp:8080
./gradlew :app:installDebug -Pshopflow.serverUrl=http://127.0.0.1:8080
```

### Endpoints
- `GET /` - Server status & API health check
- `GET /products?limit={limit}&skip={skip}` - Paginated product catalog
- `GET /products/search?q={query}&limit={limit}&skip={skip}` - Search products by title, category, brand, and description
- `GET /products/{id}` - Single product details
- `GET /products/categories` - List of product categories
- `GET /products/category/{category}` - Filter products by category

---

## Setup & Build Instructions

### Prerequisites
- **Android Studio**: Android Studio Ladybug / Meerkat (or newer recommended)
- **JDK**: Java 11+ (configured via Gradle toolchain / Android Studio JBR)
- **Android SDK**:
  - `compileSdk`: **37**
  - `targetSdk`: **37**
  - `minSdk`: **24** (Android 7.0 Nougat and above)

### Cloning & Opening the Project
1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd shopFlow
   ```
2. Open **Android Studio** and select **Open**, then choose the `shopFlow` directory.
3. Allow Gradle to synchronize dependencies.

### Building via Command Line
To assemble the debug APK:
```bash
./gradlew assembleDebug
```
The APK will be generated under:
```
app/build/outputs/apk/debug/app-debug.apk
```

To run unit tests across both app and Ktor server, or run lint checks:
```bash
./gradlew testDebugUnitTest :server:test
./gradlew lintDebug
```

### Running on Device / Emulator
1. Connect a physical Android device with USB debugging enabled or launch an Android Virtual Device (AVD).
2. Start the local Ktor server (optional; app gracefully falls back to offline cache if server is offline):
   ```bash
   ./gradlew :server:run
   ```
3. Run the application from Android Studio by clicking **Run 'app'** (`Shift + F10`) or via the CLI:
   ```bash
   ./gradlew installDebug
   ```

---

## Architecture

ShopFlow adheres to official **Android Architecture Guidelines**, implementing a clean, reactive, single-activity **MVVM (Model-View-ViewModel)** pattern with Unidirectional Data Flow (UDF) and an **Offline-First Single Source of Truth (SSOT)**:

```
┌────────────────────────────────────────────────────────┐
│                   UI Layer (Compose)                   │
│   CatalogScreen  │  ProductDetailScreen  │  CartScreen │
│                  ▲                       ▲             │
│            StateFlow (UIState)      User Events        │
│                  ▼                       │             │
├────────────────────────────────────────────────────────┤
│                      ViewModel                         │
│                    ShopViewModel                       │
│    • Combines DB flows with in-memory search query     │
│    • Exposes catalogUiState & cartUiState              │
│    • Dispatches user actions to repository             │
├────────────────────────────────────────────────────────┤
│                     Data Layer                         │
│                  ProductRepository                     │
│                  ▲                 ▲                   │
│     (Remote Sync)│                 │(Reactive Flow)    │
│                  ▼                 ▼                   │
│          KtorShopFlowApi           Room Database       │
│                  ▲                 ├─ ProductDao       │
│                  │ (HTTP / JSON)   └─ CartDao          │
│                  ▼                                     │
│      ShopFlow Ktor Server (:server)                    │
└────────────────────────────────────────────────────────┘
```

- **UI Layer (Jetpack Compose & Navigation 3)**:
  - Declarative UI components observing immutable state (`CatalogUiState`, `CartUiState`) via `collectAsStateWithLifecycle()`.
  - Type-safe navigation powered by AndroidX `navigation3` with a backstack of sealed route representations (`CatalogRoute`, `ProductRoute`, `CartRoute`).
- **ViewModel Layer (`ShopViewModel`)**:
  - Manages UI state using Kotlin Coroutines `StateFlow` with `SharingStarted.WhileSubscribed(5_000)` to survive configuration changes and conserve resources when UI is inactive.
  - Combines repository flows with user search queries, category filters, and sorting options to provide fast, responsive filtering.
- **Data Layer (`ProductRepository`)**:
  - Acts as mediator between our custom Ktor API client and the local database.
  - Employs an offline-first caching policy: queries always stream from the Room database, while network updates refresh the local database cache.

---

## Libraries Used

| Library | Version / Component | Purpose |
| :--- | :--- | :--- |
| **Jetpack Compose** | Compose BOM (`2026.09.00`) | Declarative, reactive UI toolkit for Android. |
| **Material 3** | `androidx.compose.material3` | Modern Material You design components, typography, and styling. |
| **AndroidX Navigation 3** | `1.2.0` (`runtime` & `ui`) | Next-generation declarative navigation for Compose applications. |
| **Lifecycle & ViewModel** | `2.11.0` | Lifecycle-aware coroutine scopes and state observation in Compose. |
| **Room** | `2.8.5` | SQLite object mapping and persistence engine with KSP compiler. |
| **Ktor Client** | `3.1.1` (`core`, `okhttp`, `content-negotiation`) | High-performance asynchronous HTTP client for consuming the custom Ktor API. |
| **Ktor Server** | `3.1.1` (`core`, `netty`, `cors`, `content-negotiation`) | Custom lightweight backend server powering the product catalog API. |
| **Kotlinx Serialization** | `1.8.0` | Fast, type-safe JSON serialization/deserialization for API payloads. |
| **Coil 3** | `3.6.3` (`coil-compose`, `coil-network-okhttp`) | High-performance asynchronous image loading with memory and disk caching. |

---

## Local Storage Approach

ShopFlow uses **Room Database** (`ShopFlowDatabase`, version 1) backed by SQLite for robust, relational local persistence.

### Entities
1. **`products` (`ProductEntity`)**:
   - Stores cached product details (`id`, `title`, `description`, `price`, `rating`, `category`, `brand`, `stock`, `thumbnail`).
   - Serves as the local cache for offline reading and instant startup.
2. **`cart_items` (`CartItemEntity`)**:
   - Stores the user's shopping cart (`productId`, `title`, `price`, `thumbnail`, `quantity`).
   - Persists user selections permanently across app restarts and device reboots.

### Reactive Queries & Cache Invalidation
- **Continuous Flow Observation**: Both `ProductDao.observeAll()` and `CartDao.observeAll()` return `Flow<List<T>>`, automatically emitting updated lists whenever table contents change.
- **Atomic Cache Synchronization**: `ProductDao.replaceAll(products)` is marked with `@Transaction`, clearing old records and inserting fresh API entities in a single atomic database transaction.
- **Upsertion & Quantity Management**: The cart supports atomic item increment, decrement, and deletion through indexed `productId` operations.

---

## Important Design Decisions

1. **Single Source of Truth (SSOT) via Room**:
   - The UI does not directly display raw network responses. Instead, API responses populate Room, and the UI exclusively observes Room database streams. This guarantees data consistency, eliminates race conditions, and ensures full offline functionality.
2. **Graceful Offline Degradation & Error Handling**:
   - If network requests fail (e.g., no internet connectivity, socket timeouts), the app catches the `IOException` and presents a friendly inline error banner while retaining previously cached products and full cart usability.
3. **Optimized Cart Calculations in State**:
   - Cart subtotal and item count calculations are computed dynamically within `CartUiState` as pure functional transformations (`items.sumOf { it.price * it.quantity }`), avoiding synchronization inconsistencies.
4. **Coil 3 Multi-Tier Caching**:
   - Custom `ImageLoader` configured in `ShopFlowApplication` with a 25% memory cache and a 100MB disk cache to ensure fast rendering of product thumbnails without redundant network overhead.
5. **Fluid Navigation Stack**:
   - Implemented using AndroidX Navigation 3 with an explicit, mutable state backstack, making back navigation deterministic and decoupled from platform-specific fragments.
6. **Search, Category Filtering & Multi-Criteria Sorting**:
   - Dynamic category pills extracted from the offline-first catalog with support for instant local and debounced remote queries.
   - Comprehensive sorting (Featured, Price: Low to High, Price: High to Low, Top Rated, and Name: A to Z) integrated into state-driven reactive pipelines.
7. **Production Shopping Experience & Accessibility**:
   - Shimmer skeleton loading placeholders for smoother perceived performance.
   - Responsive Compose layouts adapting seamlessly between phones (< 600dp) and tablets/landscape (>= 600dp).
   - Full TalkBack accessibility semantics, 48dp touch targets, and high-contrast WCAG-compliant Light and Dark Material 3 color schemes.

---

## Production Scope

- **Incremental catalog loading**: The catalog now requests DummyJSON in 24-item pages using `limit` and `skip`, appending each page to Room while keeping cached pages available offline. For very large catalogs, migrate this behavior to Paging 3 with a Room `RemoteMediator`.
- **Cached-stock enforcement**: Cart additions and quantity increases are capped at the product stock recorded in the most recently synced catalog. The app cannot perform real-time stock reservation because DummyJSON does not expose a transactional inventory API.
- **Checkout / Payment Gateway**: Checkout and payment processing still require a merchant account, provider SDK, backend order service, and server-side payment verification. They cannot be implemented securely against DummyJSON alone.
- **Sync conflict resolution**: Products are a server-owned, read-only catalog cache, so there are no local product edits to conflict with refreshes. If products become editable or two-way synced, add server revisions, ETags, or timestamps and a defined merge policy.

[![Android CI](https://github.com/mshubhr/ShopFlow/actions/workflows/android-ci.yml/badge.svg?branch=main)](https://github.com/mshubhr/ShopFlow/actions/workflows/android-ci.yml)
