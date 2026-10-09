# Service Marketplace

A small home-service marketplace. The frontend uses React; the backend uses Java and Spring Boot; MySQL stores the data; Maven builds the backend. The backend keeps the same API paths used by the frontend.

## Requirements

- JDK 8 through 21 (a JDK includes `javac`)
- MySQL running locally
- Node.js and npm for the React development server
- No global Maven install is needed; `backend/mvnw.cmd` downloads and runs Maven

Spring Boot is set to version 2.7.18 so the backend can run with the Java 8 JDK currently installed on this computer. Spring Boot's official 2.7.18 guide lists Java 8 as its minimum requirement. [Spring Boot 2.7.18 requirements](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/getting-started.html#getting-started.system-requirements)

## Configure MySQL

Open PowerShell in the project folder and create the backend settings file if it does not exist:

```powershell
if (!(Test-Path backend/.env)) { Copy-Item backend/.env.example backend/.env }
notepad backend/.env
```

Set `MYSQL_USER`, `MYSQL_PASSWORD`, and `JWT_SECRET` in `backend/.env` to your local MySQL account and a private secret of at least 32 characters. Spring Boot reads this file automatically. The database and tables are created when the backend starts. Do not share `backend/.env`.

## Start the backend

In one VS Code terminal:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

If the previous Node backend is still running on port 5000, stop it with `Ctrl+C` in its terminal before starting Spring Boot.

The Maven Wrapper downloads Maven the first time it runs. The API starts at `http://localhost:5000`; check it at `http://localhost:5000/api/health`.
Keep this terminal open while using the site. The browser API calls go through the Vite development proxy to this backend.

## Start the frontend

In a second VS Code terminal:

```powershell
cd frontend
npm install
npm run dev -- --host 127.0.0.1
```

Open `http://localhost:5173`.

Keep this in a second VS Code terminal, separate from the backend terminal.

## Admin accounts

The registration page offers an **Initial admin** role only when no admin account exists. After the first admin is created, customers and workers cannot register as admins. A signed-in admin can add another admin from **Admin dashboard → View users → Add an admin**. The API also checks the signed-in role, so hiding the option in the interface is not the only protection.

## Build and validate

From `backend/` run the API flow test and package build:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

From `frontend/` build the React app:

```powershell
npm run build
```

## API routes

- `POST /api/auth/register`, `POST /api/auth/login`
- `GET /api/auth/admin-setup` (checks whether the initial admin can be created)
- `GET /api/services`, `POST /api/services` (ADMIN)
- `POST /api/requests`, `GET /api/requests`, `GET /api/requests/:id`
- `GET /api/requests/my-jobs` (WORKER)
- `POST /api/bids`, `GET /api/bids/request/:requestId`, `PUT /api/bids/:id/select`
- `POST /api/reviews`, `GET /api/reviews/worker/:workerId`
- `GET /api/users`, `POST /api/users/admins` (ADMIN)

For protected routes, send `Authorization: Bearer <token>`. Available roles are `CUSTOMER`, `WORKER`, and `ADMIN`.

## Deploy to GitHub, Railway, and Vercel

This is a two-part deployment: Vercel hosts the React/Vite frontend, while Railway hosts the Spring Boot API and MySQL database. The frontend's local Vite proxy is only for local development; set `VITE_API_URL` on Vercel to the public backend URL ending in `/api`.

### Push the project to GitHub

Run these commands from the project root in PowerShell. Create an empty GitHub repository first, without adding a README or license:

```powershell
git init -b main
git add .
git status --short
git commit -m "Initial ServiceHub marketplace"
git remote add origin https://github.com/YOUR_GITHUB_NAME/YOUR_REPOSITORY.git
git push -u origin main
```

Before committing, confirm `backend/.env` is not listed by `git status`. It is ignored by `.gitignore`; never push real passwords or JWT secrets.

### Deploy the backend and MySQL to Railway

1. Create a Railway project and add a **MySQL** database.
2. Add a service from the GitHub repository. Set its root directory to `/backend`; Railway can detect the Java/Maven Spring Boot app.
3. In the backend service's Variables, reference the MySQL service variables (change `MySQL` if you named that service differently):

   ```text
   MYSQL_HOST=${{MySQL.MYSQLHOST}}
   MYSQL_PORT=${{MySQL.MYSQLPORT}}
   MYSQL_USER=${{MySQL.MYSQLUSER}}
   MYSQL_PASSWORD=${{MySQL.MYSQLPASSWORD}}
   MYSQL_DATABASE=${{MySQL.MYSQLDATABASE}}
   JWT_SECRET=<a private random value of at least 32 characters>
   FRONTEND_ORIGIN=https://<your-vercel-project>.vercel.app
   ```

   Railway supplies the `PORT` value; Spring Boot already reads it. Do not set it to a fixed local-only port.
4. Deploy the backend, then generate a public domain in its Railway Networking settings. Check `https://<railway-domain>/api/health` and confirm it returns `{"status":"ok"}`.

### Deploy the frontend to Vercel

1. Import the same GitHub repository into Vercel.
2. Set **Root Directory** to `frontend`. Use the Vite defaults: build command `npm run build`, output directory `dist`.
3. Add the environment variable `VITE_API_URL` with the value `https://<railway-domain>/api` (no trailing slash). Apply it to Production, then deploy.
4. If you change the Vercel domain, update Railway's `FRONTEND_ORIGIN` to the exact production origin and redeploy the backend.

Once GitHub is connected, pushes to the production branch can trigger new deployments. Deploy the API and database first, then the frontend so its API URL and the backend's allowed frontend origin are set correctly.
