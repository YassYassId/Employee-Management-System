import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { isAuthenticated } from './services/auth';
import Navbar from './components/Navbar';
import Login from './components/Login';
import AuthCallback from './components/AuthCallback';
import DepartmentList from './components/DepartmentList';
import DepartmentForm from './components/DepartmentForm';
import EmployeeList from './components/EmployeeList';
import EmployeeForm from './components/EmployeeForm';
import './App.css';

// Protected Route Component
const ProtectedRoute = ({ children }) => {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  return children;
};

// Home Component
const Home = () => {
  return <Navigate to="/departments" replace />;
};

function App() {
  const [authState, setAuthState] = useState(isAuthenticated());

  // Listen for authentication state changes
  useEffect(() => {
    // Check auth state on mount and when storage changes
    const checkAuth = () => {
      setAuthState(isAuthenticated());
    };

    // Check immediately
    checkAuth();

    // Listen for custom auth state change events
    const handleAuthChange = () => {
      checkAuth();
    };
    window.addEventListener('authStateChange', handleAuthChange);

    // Listen for storage events (when localStorage changes in other tabs/windows)
    window.addEventListener('storage', checkAuth);

    // Poll for changes (fallback for same-tab localStorage changes)
    // This ensures we catch changes even if events don't fire
    const interval = setInterval(checkAuth, 100);

    return () => {
      window.removeEventListener('authStateChange', handleAuthChange);
      window.removeEventListener('storage', checkAuth);
      clearInterval(interval);
    };
  }, []);

  return (
    <Router>
      <div className="app">
        {authState && <Navbar />}
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/auth/callback" element={<AuthCallback />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Home />
              </ProtectedRoute>
            }
          />
          <Route
            path="/departments"
            element={
              <ProtectedRoute>
                <DepartmentList />
              </ProtectedRoute>
            }
          />
          <Route
            path="/departments/new"
            element={
              <ProtectedRoute>
                <DepartmentForm />
              </ProtectedRoute>
            }
          />
          <Route
            path="/departments/:id/edit"
            element={
              <ProtectedRoute>
                <DepartmentForm />
              </ProtectedRoute>
            }
          />
          <Route
            path="/employees"
            element={
              <ProtectedRoute>
                <EmployeeList />
              </ProtectedRoute>
            }
          />
          <Route
            path="/employees/new"
            element={
              <ProtectedRoute>
                <EmployeeForm />
              </ProtectedRoute>
            }
          />
          <Route
            path="/employees/:id/edit"
            element={
              <ProtectedRoute>
                <EmployeeForm />
              </ProtectedRoute>
            }
          />
        </Routes>
      </div>
    </Router>
  );
}

export default App;

