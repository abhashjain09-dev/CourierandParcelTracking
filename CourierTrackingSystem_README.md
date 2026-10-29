# Courier & Parcel Tracking System

> Java console application with SQLite database integration

---

## How to Run

```batch
# Option 1 – double-click
run.bat

# Option 2 – command line
java -cp "CourierTrackingSystem.jar;lib\sqlite-jdbc.jar" com.courier.Main

# To rebuild after code changes
build.bat
```

> [!IMPORTANT]
> Java 11+ must be on your PATH. The file `lib\sqlite-jdbc.jar` must remain alongside the JAR.
> The SQLite database file `courier_tracking.db` is auto-created in the working directory on first run.

---

## Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | **Dispatch Shipment** | Register a new parcel — captures sender/receiver info, generates a unique tracking number |
| 2 | **Track Shipment** | Full detail view by tracking number — status, location, dates, parcel & contact info |
| 3 | **Update Status** | Transition status with location & remarks; fully audited |
| 4 | **View All Shipments** | Colour-coded table of all shipments, sorted by dispatch date |
| 5 | **Filter by Status** | List shipments matching a chosen status |
| 6 | **Search** | Partial-name search by sender or receiver |
| 7 | **Tracking History** | Full chronological audit log for any tracking number |
| 8 | **Dashboard** | Live counts by status, delivery success rate, and recent activity |
| 9 | **Delete Shipment** | Removes shipment and its entire history (with confirmation) |

### Shipment Statuses
```
DISPATCHED → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
                                           ↘ FAILED_DELIVERY → RETURNED
                                           ↘ ON_HOLD
```

### Service Types & ETA
| Service | Estimated Delivery |
|---------|--------------------|
| Standard | 5 days |
| Express | 2 days |
| Same-Day | 8 hours |

### Tracking Number Format
```
CTS-YYYYMMDD-XXXXXX
e.g. CTS-20261029-A3Z7K1
```

---

## Project Architecture

```
Java oops/
├── src/main/java/com/courier/
│   ├── Main.java                      ← Entry point
│   ├── model/
│   │   ├── ShipmentStatus.java        ← Enum: all possible statuses
│   │   ├── Person.java                ← Sender / Receiver entity
│   │   ├── Shipment.java              ← Core domain object
│   │   └── StatusLog.java             ← Single audit-trail entry
│   ├── database/
│   │   ├── DatabaseManager.java       ← Singleton SQLite connection + schema
│   │   ├── ShipmentDAO.java           ← CRUD for shipments table
│   │   └── StatusLogDAO.java          ← CRUD for status_logs table
│   ├── service/
│   │   └── ShipmentService.java       ← Business logic layer
│   ├── ui/
│   │   └── CourierUI.java             ← Full console menu UI
│   └── util/
│       ├── TrackingNumberGenerator.java
│       └── ConsoleHelper.java         ← ANSI colour, I/O helpers
├── lib/
│   └── sqlite-jdbc.jar                ← JDBC driver (v3.45.1)
├── CourierTrackingSystem.jar          ← Runnable fat JAR
├── build.bat                          ← Recompile & repackage
└── run.bat                            ← Launch the app
```

---

## Database Schema (SQLite)

### `shipments` table
| Column | Type | Notes |
|--------|------|-------|
| `id` | INTEGER PK | Auto-increment |
| `tracking_number` | TEXT UNIQUE | e.g. `CTS-20261029-A3Z7K1` |
| `status` | TEXT | Enum name (DISPATCHED, IN_TRANSIT …) |
| `service_type` | TEXT | Standard / Express / Same-Day |
| `parcel_description` | TEXT | Free text |
| `parcel_category` | TEXT | Electronics, Documents … |
| `weight_kg` | REAL | |
| `declared_value` | REAL | In rupees |
| `is_fragile` | INTEGER | 0 / 1 |
| `current_location` | TEXT | |
| `dispatch_date` | TEXT | ISO-8601 datetime |
| `estimated_delivery` | TEXT | ISO-8601 datetime |
| `last_updated` | TEXT | ISO-8601 datetime |
| `sender_*` | TEXT | name, address, city, state, pincode, phone, email |
| `receiver_*` | TEXT | same fields as sender |

### `status_logs` table
| Column | Type | Notes |
|--------|------|-------|
| `id` | INTEGER PK | Auto-increment |
| `tracking_number` | TEXT FK | References `shipments` |
| `status` | TEXT | Status at time of event |
| `location` | TEXT | Where the event occurred |
| `remarks` | TEXT | Notes by the operator |
| `updated_by` | TEXT | Operator name |
| `timestamp` | TEXT | ISO-8601 datetime |

> [!NOTE]
> Indexes exist on `shipments(tracking_number)`, `shipments(status)`, and `status_logs(tracking_number)` for fast lookups.

---

## OOP Design Patterns Used

| Pattern | Where |
|---------|-------|
| **Singleton** | `DatabaseManager` — one shared DB connection |
| **DAO (Data Access Object)** | `ShipmentDAO`, `StatusLogDAO` — separates SQL from logic |
| **Service Layer** | `ShipmentService` — business rules isolated from UI |
| **Model / Entity** | `Shipment`, `Person`, `StatusLog` |
| **Enum** | `ShipmentStatus` — type-safe status representation |
| **Static Factory** | `TrackingNumberGenerator.generate()` |
