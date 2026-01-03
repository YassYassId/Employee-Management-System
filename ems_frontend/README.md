# Employee Management System - React Frontend

A modern React.js application for managing employees and departments, integrated with a Spring Boot microservices backend.

## Features

- **Authentication**: Keycloak OAuth2 integration
- **Department Management**: CRUD operations for departments
- **Employee Management**: CRUD operations for employees
- **Role-Based Access Control**: Different permissions for ADMIN and USER roles
- **Modern UI**: Clean, responsive design
- **Error Handling**: Comprehensive error messages and loading states

## Prerequisites

Before running the frontend, ensure the following services are running:

1. **Keycloak** on `http://localhost:8080`
   - Realm: `employee-realm`
   - Client ID: `ems-app`
   - Client Secret: `rMqy1ICMSNLuc2MzLbIOqrEISVhaPt6S`

2. **Discovery Service (Eureka)** on `http://localhost:8761`

3. **Gateway Service** on `http://localhost:8060`

4. **Department Service** on `http://localhost:8040`

5. **Employee Service** on `http://localhost:8020`

## Installation

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

## Running the Application

Start the development server:
```bash
npm start
```

The application will open at `http://localhost:3000` in your browser.

## Usage

### Login

1. Click "Login with Keycloak" on the login page
2. You will be redirected to Keycloak for authentication
3. After successful login, you'll be redirected back to the application

### User Roles

- **ADMIN**: Can create, read, update, and delete departments and employees
- **USER**: Can only read departments and employees

### Managing Departments

- View all departments on the Departments page
- ADMIN users can create new departments
- ADMIN users can edit existing departments
- ADMIN users can delete departments

### Managing Employees

- View all employees on the Employees page
- ADMIN users can create new employees
- ADMIN users can edit existing employees
- ADMIN users can delete employees
- When creating/editing employees, you must select a valid department

## API Endpoints

The frontend communicates with the backend through the Gateway Service:

- **Departments**: `http://localhost:8060/department-service/departments`
- **Employees**: `http://localhost:8060/employee-service/employees`

All requests require authentication via JWT tokens obtained from Keycloak.

## Project Structure

```
frontend/
├── public/
│   └── index.html
├── src/
│   ├── components/
│   │   ├── AuthCallback.js      # Handles OAuth callback
│   │   ├── DepartmentForm.js    # Create/Edit department form
│   │   ├── DepartmentList.js     # List all departments
│   │   ├── EmployeeForm.js       # Create/Edit employee form
│   │   ├── EmployeeList.js       # List all employees
│   │   ├── Login.js              # Login page
│   │   └── Navbar.js             # Navigation bar
│   ├── services/
│   │   ├── api.js                # API client with axios
│   │   └── auth.js                # Keycloak authentication utilities
│   ├── App.js                     # Main app component with routing
│   ├── App.css                    # Application styles
│   ├── index.js                   # Entry point
│   └── index.css                  # Global styles
├── package.json
└── README.md
```

## Testing

The application is ready for testing. You can:

1. Test authentication flow with Keycloak
2. Test CRUD operations for departments
3. Test CRUD operations for employees
4. Test role-based access control (try with different user roles)
5. Test error handling (e.g., invalid department ID when creating employee)

## Troubleshooting

### CORS Issues

If you encounter CORS errors, ensure:
- The Gateway Service has CORS enabled (it should be configured)
- You're accessing the app from `http://localhost:3000`

### Authentication Issues

- Verify Keycloak is running on `http://localhost:8080`
- Check that the realm and client configuration match
- Ensure the redirect URI is correctly configured in Keycloak

### API Connection Issues

- Verify all microservices are running
- Check that the Gateway Service is accessible at `http://localhost:8060`
- Ensure you're authenticated (check localStorage for `access_token`)

## Development

### Available Scripts

- `npm start`: Runs the app in development mode
- `npm build`: Builds the app for production
- `npm test`: Launches the test runner

### Environment Variables

You can create a `.env` file to customize:

```
REACT_APP_API_BASE_URL=http://localhost:8060
REACT_APP_KEYCLOAK_URL=http://localhost:8080
REACT_APP_REALM=employee-realm
REACT_APP_CLIENT_ID=ems-app
```

Then update `src/services/api.js` and `src/services/auth.js` to use these variables.

## Notes

- The application uses localStorage to store authentication tokens
- Tokens are automatically included in API requests via axios interceptors
- The app redirects to login if authentication fails
- Role-based UI elements are shown/hidden based on user roles

