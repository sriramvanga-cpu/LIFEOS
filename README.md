# LifeOS – All-in-One Personal Management Suite

LifeOS is a modern desktop application built with Java Swing and PostgreSQL that helps you manage your time, health, finances, and personal profile in one unified dashboard.

![Java](https://img.shields.io/badge/Java-17%2B-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15%2B-blue)
![License](https://img.shields.io/badge/license-MIT-green)

## ✨ Features

### 📅 Time Module
- Monthly calendar view with colour‑coded task categories
- Hourly day planner showing scheduled tasks
- Create, edit, and delete tasks with start/end times, categories, and notes
- Real‑time current time indicator on the daily timeline

### ❤️ Health Module
- **Overview Tab**: Track daily steps, sleep, weight, and BMI with interactive spinners
- Weekly step goal ring and inline bar charts for steps/sleep trends
- **Activity Journal**: Log workouts (walking, running, cycling, etc.) with duration and estimated calories burned
- Live activity timer with stopwatch functionality
- **Medications**: Manage prescriptions with dosage, interval reminders, and daily dose checkboxes

### 💰 Finance Module
- **Records Tab**: View all income, expenses, and transfers filtered by month
- Add transactions with a built‑in calculator (keyboard and mouse input supported)
- Balance validation prevents overspending; live account balance display
- **Accounts Tab**: Manage multiple accounts (Cash, Bank, Credit Card, Savings) with real‑time balances
- **Analysis Tab**:
   - Pie chart breakdown of monthly expenses by category
   - Yearly bar chart showing monthly expense trends with year selector
- Month navigation bar to switch between months

### 👤 Profile Module
- View and edit personal information (name, email, date of birth, height)
- Change password securely (SHA‑256 hashed)

### 🔐 Security
- User registration and login with SHA‑256 password hashing
- All data is user‑scoped – each user sees only their own information

## 🛠️ Tech Stack

| Layer        | Technology                          |
|--------------|-------------------------------------|
| Frontend     | Java Swing (Flat, modern UI)        |
| Backend      | Java JDBC                           |
| Database     | PostgreSQL (Neon Serverless / local)|
| Security     | SHA‑256 Hashing                     |
| Build        | Manual compilation (or any Java IDE)|

## 🚀 Getting Started

### Prerequisites
- **Java JDK 17** or later
- **PostgreSQL** database (local installation or cloud service like [Neon](https://neon.tech))
- PostgreSQL JDBC driver (`postgresql-42.x.x.jar`) – [Download here](https://jdbc.postgresql.org/download/)

### 1. Database Setup
1. Create a new PostgreSQL database (e.g., `lifeos`).
2. Execute the `schema.sql` script (provided separately) to create all tables and indexes.
   - The script should define tables: `USERS`, `TASKS`, `ACCOUNTS`, `TRANSACTIONS`, `HEALTH_RECORDS`, `TIME_LOGS`, `MEDICATIONS`, `MEDICATION_LOGS`.
3. Ensure the database user has full permissions on the schema.

### 2. Configure Database Connection
Edit `DBConnection.java` with your PostgreSQL credentials:

```java
private static final String URL = "jdbc:postgresql://your-host:5432/your-database?sslmode=require";
private static final String USER = "your-username";
private static final String PASSWORD = "your-password";
```
Note: The project currently uses a Neon cloud connection string. Replace it with your own database URL.

### 3.Add JDBC Driver
   Place the PostgreSQL JDBC driver JAR (e.g., postgresql-42.7.3.jar) inside a lib/ folder (create it if it doesn't exist).

### 4. Compile and Run
   Using terminal / command prompt:
#### Compile all Java files (include the JDBC driver in classpath)
```
javac -cp "lib/*" -d bin src/*.java
```
#### Run the application
```
java -cp "bin:lib/*" MainSwing
```
Using an IDE (IntelliJ / Eclipse / VS Code):
Add the PostgreSQL JDBC driver JAR to the project's build path.

Run MainSwing.java as a Java application.

### 5. First Use
   Launch the app – the login screen appears.

Click "New user? Sign Up" to create an account.

After registration, log in and explore the modules via the top navigation bar.

## 📄 License
This project is licensed under the MIT License – see the LICENSE file for details.

Happy organising with LifeOS!


