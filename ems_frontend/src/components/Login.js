import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAuthUrl, isAuthenticated } from '../services/auth';
import './Login.css';

const Login = () => {
  const navigate = useNavigate();
  const [error, setError] = useState(null);

  useEffect(() => {
    if (isAuthenticated()) navigate('/');
  }, [navigate]);

  const handleLogin = async (forceLogin = false) => {
    try {
      const authUrl = await getAuthUrl(forceLogin);
      window.location.href = authUrl;
    } catch (err) {
      setError(err.message || 'Login error');
      console.error('Login error:', err);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h1>Employee Management System</h1>
        <p>Please login to access the system</p>
        <button onClick={() => handleLogin(false)} className="login-button">Login with Keycloak</button>
        <button onClick={() => handleLogin(true)} className="login-button" style={{marginTop: 8}}>Force Login (Troubleshoot)</button>
        {error && <div style={{color: 'red', marginTop: 10}}>{error}</div>}
      </div>
    </div>
  );
};

export default Login;
