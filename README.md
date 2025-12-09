# Warehouse Management System

Multi-warehouse management system for managing 15+ warehouses with inventory, sales, purchases, and financial tracking.

## Project Information
- **Version:** 1.0.0
- **Java:** 21
- **Spring Boot:** 3.5.5
- **Database:** PostgreSQL 16
- **Build Tool:** Gradle

## Technology Stack
- **Backend:** Spring Boot, Spring Data JPA, Spring Security
- **Frontend:** Thymeleaf, Bootstrap 5.3, HTMX
- **Database:** PostgreSQL 16 with Flyway migrations
- **Reporting:** Apache POI (Excel), iText (PDF)

## Getting Started

### Prerequisites
- Java 21 JDK installed and configured
- PostgreSQL 16 installed and running
- Gradle 8.x (or use Gradle Wrapper - included)
- Git (optional)

### Database Setup
1. Install PostgreSQL 16 if not already installed
2. Create PostgreSQL database:
```sql
CREATE DATABASE warehouse_db;
CREATE USER warehouse_user WITH PASSWORD 'warehouse_password';
GRANT ALL PRIVILEGES ON DATABASE warehouse_db TO warehouse_user;
```

3. Update database credentials in `src/main/resources/application.yml` if different:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/warehouse_db
    username: postgres
    password: your_password
```

### Initial Setup
1. Navigate to project directory:
```bash
cd D:\Naeem\Personal\projects\warehouse-management-system
```

2. Build the project:
```bash
gradlew build
```

3. Run database migrations (automatic on first run):
```bash
gradlew bootRun
```

### Running the Application
```bash
gradlew bootRun
```

Or on Windows Command Prompt:
```cmd
gradlew.bat bootRun
```

The application will start on port 8080 and automatically:
- Create database tables using Flyway migrations
- Insert default admin user
- Set up all necessary configurations

### Accessing the Application
- **URL:** http://localhost:8080
- **Login Page:** http://localhost:8080/login
- **Default Admin Credentials:**
  - Email: `admin@warehouse.com`
  - Password: `admin123`

**IMPORTANT:** Change the default admin password after first login!

### First Time Setup Steps
1. Access http://localhost:8080
2. Login with default admin credentials
3. Navigate to User Management and change admin password
4. Create the 3 pilot warehouses:
   - Code: `ENGRO-FOOD`, Name: Engro Food Warehouse
   - Code: `SAHIWAL-1`, Name: Sahiwal Warehouse 1
   - Code: `SAHIWAL-2`, Name: Sahiwal Warehouse 2
5. Create warehouse managers and staff users
6. Assign users to their respective warehouses

## Project Structure
```
warehouse-management-system/
├── src/
│   ├── main/
│   │   ├── java/com/warehouse/wms/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # Controllers
│   │   │   ├── entity/          # JPA Entities
│   │   │   ├── repository/      # Repositories
│   │   │   ├── service/         # Business logic
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   └── util/            # Utilities
│   │   └── resources/
│   │       ├── db/migration/    # Flyway migrations
│   │       ├── templates/       # Thymeleaf templates
│   │       ├── static/          # CSS, JS, images
│   │       └── application.yml  # Configuration
│   └── test/                    # Tests
└── build.gradle                 # Build configuration
```

## Development Phases

### Phase 1: Core Foundation (Week 1-8) - In Progress
- ✅ **Week 1-2:** Project Setup, Authentication, User Management, Warehouse Management
- ⏳ **Week 3-4:** Product Management, Categories, Units
- ⏳ **Week 5-6:** Purchase Management, Purchase Orders
- ⏳ **Week 7-8:** Sales Management, Invoices, Stock Updates

### Phase 2: Financial Management (Week 9-14)
- ⏳ **Week 9-10:** Supplier/Customer Ledgers, Payments
- ⏳ **Week 11-12:** Stock Transfers Between Warehouses
- ⏳ **Week 13-14:** Financial Reports (P&L, Balance Sheet)

### Phase 3: Operations & Analytics (Week 15-18)
- ⏳ **Week 15-16:** Daily Expenses, Expense Categories
- ⏳ **Week 17-18:** Analytics Dashboard, Sales Analytics

### Phase 4: Polish & Optimization (Week 19-22)
- ⏳ **Week 19-20:** UI/UX Improvements, Performance Optimization
- ⏳ **Week 21:** Testing, Bug Fixes, Documentation
- ⏳ **Week 22:** Deployment, Training, Handover

## Current Features (Phase 1 - Week 1-2 Completed)
✅ **Authentication & Authorization**
  - Spring Security with BCrypt password encryption
  - Login/Logout functionality
  - Session management
  - Role-based access control (ADMIN, WAREHOUSE_MANAGER, STAFF)

✅ **User Management** (Admin Only)
  - Create, Read, Update, Delete users
  - Assign roles and manage status
  - User profile management
  - Pagination and search

✅ **Warehouse Management** (Admin Only)
  - Create, Read, Update, Delete warehouses
  - Unique warehouse codes
  - Location and contact information
  - Status management
  - Pagination and search

✅ **Dashboard**
  - Role-based dashboard views
  - Statistics overview
  - Quick actions
  - System information

✅ **Responsive Design**
  - Bootstrap 5.3 UI framework
  - Mobile and tablet responsive
  - Modern, clean interface
  - Icons with Bootstrap Icons

✅ **Audit Logging**
  - Automatic tracking of all changes
  - JSON-based old/new value storage
  - User activity tracking
  - PostgreSQL JSONB support

## Upcoming Features
⏳ Product Management
⏳ Purchase Management
⏳ Sales Management
⏳ Stock Tracking
⏳ Supplier/Customer Ledgers
⏳ Payment Tracking
⏳ Stock Transfers
⏳ Financial Reports
⏳ Daily Expenses

## License
Proprietary - All rights reserved

## Contact
Project Manager: [To be assigned]