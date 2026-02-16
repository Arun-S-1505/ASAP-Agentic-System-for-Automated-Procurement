# ASAP — Agentic System for Automated Procurement

An intelligent middleware system that automates procurement approval workflows by integrating with SAP ERP systems. The system uses risk-based scoring to auto-approve low-risk purchase requisitions while flagging high-risk ones for manual review.

## Architecture

```
User → Mobile App (Android) → Backend API (FastAPI) → MySQL Database
                                        ↕
                                   ERP System (SAP/Mock)
```

| Layer | Technology |
|-------|-----------|
| **Mobile App** | Kotlin, Jetpack Compose Multiplatform, Ktor, Navigation Compose |
| **Backend API** | Python, FastAPI, SQLAlchemy 2.0, APScheduler |
| **Database** | MySQL 8.0 |
| **Authentication** | JWT (HS256) + bcrypt password hashing |
| **ERP Integration** | SAP S/4HANA API / Mock Adapter (MySQL-backed) |

## Features

### Backend
- **Risk Scoring Engine** — Scores requisitions (0.0–1.0) based on amount, vendor history, and patterns
- **Auto-Approval Rules** — Low-risk items auto-approved; high-risk items held for manual review
- **Batch Operations** — Approve or reject multiple decisions in one call
- **Analytics API** — Aggregated stats: approval distribution, risk breakdown, daily trends
- **Background Worker** — APScheduler commits approved decisions to ERP after a grace period
- **JWT Authentication** — Secure login/register with bcrypt + HS256 tokens
- **ERP Adapter Pattern** — Pluggable adapters: Mock (MySQL-backed), SAP S/4HANA, Hybrid
- **Notification Service** — Logs all approval events for audit trail

### Mobile App
- **Login / Register** — JWT-based authentication
- **Dashboard** — Search, filter chips, KPI summary cards, decision list
- **Decision Detail** — Full requisition info with approve/reject/undo actions
- **Batch Approve/Reject** — Long-press to multi-select, batch action bar
- **Analytics** — Live charts (donut, bar, trend) powered by backend API
- **Notifications** — Activity feed of all approval events
- **Settings** — User profile from JWT, logout flow

## Project Structure

```
├── backend/
│   └── middleware/
│       ├── main.py                 # FastAPI app entry point
│       ├── config.py               # pydantic-settings configuration
│       ├── requirements.txt        # Python dependencies
│       ├── api/
│       │   ├── routes.py           # Core endpoints (detect, decisions, approve, reject)
│       │   ├── auth_routes.py      # Login, register, me
│       │   ├── analytics_routes.py # GET /analytics/summary
│       │   └── batch_routes.py     # POST /batch/approve, /batch/reject
│       ├── core/
│       │   ├── auth.py             # JWT + bcrypt utilities
│       │   ├── approval.py         # Approval logic helpers
│       │   └── risk_scoring.py     # Risk scoring engine
│       ├── adapters/
│       │   ├── base.py             # Abstract ERP adapter
│       │   ├── mock_adapter.py     # MySQL-backed mock ERP
│       │   └── sap_adapter.py      # SAP S/4HANA adapter
│       ├── db/
│       │   ├── models.py           # SQLAlchemy models
│       │   └── session.py          # Engine + session factory
│       ├── models/
│       │   ├── domain.py           # Domain models
│       │   └── schemas.py          # Pydantic schemas
│       ├── services/
│       │   └── orchestrator.py     # Approval orchestrator
│       └── scheduler/
│           └── worker.py           # Background commit worker
│
└── mobile/
    └── composeApp/
        └── src/commonMain/kotlin/com/arun/asap/
            ├── App.kt
            ├── core/network/       # ApiClient, TokenManager, HttpClientFactory
            ├── data/
            │   ├── model/          # DTOs (Decision, Auth, Analytics, Batch, etc.)
            │   └── repository/     # ApprovalRepository
            ├── domain/usecase/     # Clean architecture use cases
            ├── di/                 # AppModule (dependency injection)
            ├── navigation/         # AppNavHost, AppRoute
            └── presentation/
                ├── login/          # LoginScreen, LoginViewModel
                ├── splash/         # SplashScreen
                ├── main/           # MainScreen (bottom nav)
                ├── dashboard/      # DashboardScreen, DashboardViewModel
                ├── decisions/      # DecisionDetailScreen
                ├── analytics/      # AnalyticsScreen
                ├── notifications/  # NotificationsScreen
                ├── settings/       # SettingsScreen
                ├── components/     # DecisionCard, SummaryCard, Shimmer
                └── theme/          # Colors, Typography, Theme
```

## Getting Started

### Prerequisites
- Python 3.11+
- MySQL 8.0+
- Android Studio (for mobile development)
- JDK 17+

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/Arun-S-1505/ASAP_Agentic-System-for-Automated-Procurement.git
   cd ASAP_Agentic-System-for-Automated-Procurement/backend/middleware
   ```

2. **Create a virtual environment**
   ```bash
   python -m venv venv
   source venv/bin/activate    # Linux/Mac
   venv\Scripts\activate       # Windows
   ```

3. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

4. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env with your MySQL credentials and JWT secret
   ```

5. **Create the MySQL database**
   ```sql
   CREATE DATABASE erp_middleware CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

6. **Run the server**
   ```bash
   uvicorn main:app --reload --host 0.0.0.0 --port 8000
   ```

7. **Open API docs** at [http://localhost:8000/docs](http://localhost:8000/docs)

### Mobile Setup

1. **Open the `mobile/` folder** in Android Studio

2. **Update the backend URL** in `composeApp/src/commonMain/.../di/AppModule.kt`:
   ```kotlin
   private const val BASE_URL = "http://<your-ip>:8000/api/v1"
   ```

3. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```
   APK output: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/auth/login` | Login with username/password |
| `POST` | `/api/v1/auth/register` | Register a new user |
| `GET` | `/api/v1/auth/me` | Get current user info |
| `POST` | `/api/v1/detect` | Detect staged requisitions from ERP |
| `GET` | `/api/v1/decisions` | List all approval decisions |
| `POST` | `/api/v1/approve/{id}` | Approve a decision |
| `POST` | `/api/v1/reject/{id}` | Reject a decision |
| `POST` | `/api/v1/undo/{id}` | Undo a pending decision |
| `GET` | `/api/v1/analytics/summary` | Aggregated analytics |
| `POST` | `/api/v1/batch/approve` | Batch approve decisions |
| `POST` | `/api/v1/batch/reject` | Batch reject decisions |
| `GET` | `/api/v1/notifications` | List notification logs |

All endpoints except `/auth/login` and `/auth/register` require a `Bearer` token.

## Deployment (Render)

The backend is configured for deployment on [Render](https://render.com):

1. Push this repo to GitHub
2. Create a **New Web Service** on Render
3. Connect your GitHub repo
4. Set:
   - **Root Directory**: `backend/middleware`
   - **Build Command**: `pip install -r requirements.txt`
   - **Start Command**: `gunicorn main:app -w 2 -k uvicorn.workers.UvicornWorker --bind 0.0.0.0:$PORT`
5. Add environment variables (Settings → Environment):
   - `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME` — your MySQL host
   - `JWT_SECRET_KEY` — a strong random string
   - `SAP_API_KEY` — your SAP API key (required for `sap` or `hybrid` mode)
   - `APP_ENV` — `production`
   - `ERP_MODE` — `mock` (or `sap` / `hybrid` for production)
6. Deploy!

For MySQL, use a free managed MySQL provider like [Aiven](https://aiven.io), [TiDB Serverless](https://tidbcloud.com), or [PlanetScale](https://planetscale.com).

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_ENV` | `development` | `development` / `production` |
| `DEBUG` | `true` | Enable debug logging |
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_USER` | `root` | MySQL username |
| `DB_PASSWORD` | `changeme` | MySQL password |
| `DB_NAME` | `erp_middleware` | MySQL database name |
| `ERP_MODE` | `mock` | `mock` / `sap` / `hybrid` |
| `SAP_API_KEY` | (none) | SAP API key (required for `sap` or `hybrid` mode) |
| `JWT_SECRET_KEY` | (default) | Secret for signing JWT tokens |
| `JWT_EXPIRY_MINUTES` | `480` | Token expiry (8 hours) |
| `GRACE_PERIOD_MINUTES` | `5` | Time before auto-commit to ERP |
| `SCHEDULER_COMMIT_INTERVAL_SECONDS` | `60` | Background worker interval |

## Tech Stack

**Backend:** Python 3.11 · FastAPI 0.109 · SQLAlchemy 2.0 · APScheduler 3.10 · python-jose · passlib · bcrypt · Gunicorn + Uvicorn

**Mobile:** Kotlin 2.3 · Compose Multiplatform 1.10 · Ktor 2.3 · Navigation Compose · Material 3 · AndroidX ViewModel · StateFlow

**Database:** MySQL 8.0 · PyMySQL driver

## License

This project is developed as a research prototype for academic purposes.
