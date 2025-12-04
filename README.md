# Tombyts Monorepo

This monorepo contains all three components of the Tombyts project:
- Android TV application
- Frontend web application
- Backend server

## Repository Structure

```
tombyts-monorepo/
├── android/          # Android TV application (Kotlin/Jetpack Compose)
├── frontend/         # Frontend web app (Node.js)
├── backend/          # Backend server (Node.js)
├── .gitignore        # Root gitignore for all projects
└── README.md         # This file
```

## Getting Started

### Android Project

**Opening in Android Studio:**
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to and select the `android/` subdirectory (NOT the root directory)
4. This prevents Android Studio from indexing the Node.js node_modules directories

**Running the Android app:**
```bash
cd android
./gradlew build
```

### Frontend Project

**Setup:**
```bash
cd frontend
npm install
```

**Development:**
```bash
npm run dev
```

**Build:**
```bash
npm run build
```

### Backend Project

**Setup:**
```bash
cd backend
npm install
```

**Development:**
```bash
npm run dev
```

**Build:**
```bash
npm run build
```

## IDE Setup

### VSCode (for Frontend & Backend)
Simply open the root directory `tombyts-monorepo/` in VSCode. It handles monorepos well.

### Android Studio (for Android)
**Important:** Open the `android/` subdirectory, not the root directory. This prevents IDE performance issues caused by indexing the large `node_modules` directories.

The `.idea/tombyts-monorepo.iml` file is configured to exclude the frontend and backend directories when opening from the root, but it's still recommended to open from the `android/` subdirectory.

## Working with Git

All three projects are now in a single git repository. You can commit changes across all projects together:

```bash
git add .
git commit -m "Your commit message"
git push
```

## Environment Variables

Each project has its own `.env` file:
- `android/local.properties` - Android local configuration
- `frontend/.env` - Frontend environment variables
- `backend/.env` - Backend environment variables

Make sure to configure these before running the projects.

## Project Management

Having all projects in one repository makes it easier to:
- Track changes across the entire stack
- Create pull requests that span multiple components
- Maintain consistent versioning
- Share common configuration and documentation
