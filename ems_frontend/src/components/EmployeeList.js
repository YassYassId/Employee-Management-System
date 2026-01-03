import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { employeeAPI, departmentAPI } from '../services/api';
import { isAdmin, getStoredUserInfo } from '../services/auth';

const EmployeeList = () => {
  const [employees, setEmployees] = useState([]);
  const [departments, setDepartments] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [searchName, setSearchName] = useState('');
  const [filterDepartmentId, setFilterDepartmentId] = useState('');
  const userInfo = getStoredUserInfo();
  const admin = isAdmin(userInfo);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);

      // Load employees with optional filters
      const params = {};
      if (searchName) params.name = searchName;
      if (filterDepartmentId) params.departmentId = filterDepartmentId;

      const empResponse = await employeeAPI.getAll(params);
      // Extract content from paginated response
      const employeeData = empResponse.data.content || empResponse.data;
      setEmployees(Array.isArray(employeeData) ? employeeData : []);

      // Load departments for mapping
      const deptResponse = await departmentAPI.getAll();
      const deptData = deptResponse.data.content || deptResponse.data;
      const deptMap = {};
      (Array.isArray(deptData) ? deptData : []).forEach(dept => {
        deptMap[dept.id] = dept.name;
      });
      setDepartments(deptMap);

      setError(null);
    } catch (err) {
      setError('Failed to load employees. Make sure you are authenticated and the gateway is running.');
      console.error('Error loading employees:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    loadData();
  };

  const handleClearFilters = () => {
    setSearchName('');
    setFilterDepartmentId('');
    // Reload without filters
    setTimeout(() => loadData(), 0);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this employee?')) {
      return;
    }

    try {
      await employeeAPI.delete(id);
      setMessage('Employee deleted successfully');
      loadData();
      setTimeout(() => setMessage(null), 3000);
    } catch (err) {
      setError('Failed to delete employee. Only ADMIN users can delete.');
      console.error('Error deleting employee:', err);
    }
  };

  if (loading) {
    return <div className="loading">Loading employees...</div>;
  }

  return (
      <div className="container">
        <div className="page-header">
          <h1 className="page-title">Employees</h1>
          {admin && (
              <Link to="/employees/new" className="btn btn-primary">
                Add New Employee
              </Link>
          )}
        </div>

        {message && <div className="success-message">{message}</div>}
        {error && <div className="error-message">{error}</div>}

        {/* Search and Filter Section */}
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <form onSubmit={handleSearch} className="form-container">
            <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-end' }}>
              <div className="form-group" style={{ flex: 1, marginBottom: 0 }}>
                <label htmlFor="searchName">Search by Name</label>
                <input
                    type="text"
                    id="searchName"
                    value={searchName}
                    onChange={(e) => setSearchName(e.target.value)}
                    placeholder="Enter employee name"
                />
              </div>
              <div className="form-group" style={{ flex: 1, marginBottom: 0 }}>
                <label htmlFor="filterDepartment">Filter by Department</label>
                <select
                    id="filterDepartment"
                    value={filterDepartmentId}
                    onChange={(e) => setFilterDepartmentId(e.target.value)}
                >
                  <option value="">All Departments</option>
                  {Object.entries(departments).map(([id, name]) => (
                      <option key={id} value={id}>
                        {name}
                      </option>
                  ))}
                </select>
              </div>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button type="submit" className="btn btn-primary">
                  Search
                </button>
                <button
                    type="button"
                    onClick={handleClearFilters}
                    className="btn btn-secondary"
                >
                  Clear
                </button>
              </div>
            </div>
          </form>
        </div>

        {employees.length === 0 ? (
            <div className="empty-state">
              <h3>No employees found</h3>
              <p>Get started by creating your first employee.</p>
              {admin && (
                  <Link to="/employees/new" className="btn btn-primary" style={{ marginTop: '1rem' }}>
                    Create Employee
                  </Link>
              )}
            </div>
        ) : (
            <div className="table-container">
              <table>
                <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Position</th>
                  <th>Department</th>
                  {admin && <th>Actions</th>}
                </tr>
                </thead>
                <tbody>
                {employees.map((emp) => (
                    <tr key={emp.id}>
                      <td>{emp.id}</td>
                      <td>{emp.name}</td>
                      <td>{emp.position}</td>
                      <td>{departments[emp.departmentId] || `Dept ID: ${emp.departmentId}`}</td>
                      {admin && (
                          <td>
                            <div className="actions">
                              <Link
                                  to={`/employees/${emp.id}/edit`}
                                  className="btn btn-secondary btn-small"
                              >
                                Edit
                              </Link>
                              <button
                                  onClick={() => handleDelete(emp.id)}
                                  className="btn btn-danger btn-small"
                              >
                                Delete
                              </button>
                            </div>
                          </td>
                      )}
                    </tr>
                ))}
                </tbody>
              </table>
            </div>
        )}
      </div>
  );
};

export default EmployeeList;