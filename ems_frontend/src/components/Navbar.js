import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { logout, getStoredUserInfo, isAdmin } from '../services/auth';
import './Navbar.css';

const Navbar = () => {
  const location = useLocation();
  const userInfo = getStoredUserInfo();
  const admin = isAdmin(userInfo);

  const handleLogout = () => {
    logout();
  };

  return (
    <nav className="navbar">
      <div className="navbar-content">
        <div className="navbar-brand">Employee Management System</div>
        <div className="navbar-links">
          <Link
            to="/departments"
            className={location.pathname.startsWith('/departments') ? 'active' : ''}
          >
            Departments
          </Link>
          <Link
            to="/employees"
            className={location.pathname.startsWith('/employees') ? 'active' : ''}
          >
            Employees
          </Link>
          <div className="auth-section">
            {userInfo && (
              <>
                <span className="user-info">
                  {userInfo.username}
                  {admin && <span className="role-badge admin">ADMIN</span>}
                  {!admin && <span className="role-badge user">USER</span>}
                </span>
                <button onClick={handleLogout} className="auth-button logout">
                  Logout
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;

