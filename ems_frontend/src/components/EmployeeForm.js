import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { employeeAPI, departmentAPI } from '../services/api';

const EmployeeForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;

  const [formData, setFormData] = useState({
    name: '',
    position: '',
    departmentId: '',
  });
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadDepartments();
    if (isEdit) {
      loadEmployee();
    }
  }, [id, isEdit]);

  const loadDepartments = async () => {
    try {
      const response = await departmentAPI.getAll();
      // Extract content from paginated response
      const deptData = response.data.content || response.data;
      setDepartments(Array.isArray(deptData) ? deptData : []);
    } catch (err) {
      console.error('Error loading departments:', err);
    }
  };

  const loadEmployee = async () => {
    try {
      setLoading(true);
      const response = await employeeAPI.getById(id);
      // For getById, response might include department details
      const employee = response.data;
      setFormData({
        name: employee.name || '',
        position: employee.position || '',
        departmentId: employee.departmentId?.toString() || '',
      });
    } catch (err) {
      setError('Failed to load employee');
      console.error('Error loading employee:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const employeeData = {
        name: formData.name,
        position: formData.position,
        departmentId: parseInt(formData.departmentId),
      };

      if (isEdit) {
        await employeeAPI.update(id, employeeData);
      } else {
        await employeeAPI.create(employeeData);
      }
      navigate('/employees');
    } catch (err) {
      setError(
          err.response?.data?.message ||
          `Failed to ${isEdit ? 'update' : 'create'} employee. Only ADMIN users can modify employees.`
      );
      console.error('Error saving employee:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading && isEdit) {
    return <div className="loading">Loading employee...</div>;
  }

  return (
      <div className="container">
        <div className="page-header">
          <h1 className="page-title">
            {isEdit ? 'Edit Employee' : 'Create New Employee'}
          </h1>
        </div>

        {error && <div className="error-message">{error}</div>}

        <div className="card">
          <form onSubmit={handleSubmit} className="form-container">
            <div className="form-group">
              <label htmlFor="name">Employee Name *</label>
              <input
                  type="text"
                  id="name"
                  name="name"
                  value={formData.name}
                  onChange={handleChange}
                  required
                  disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="position">Position *</label>
              <input
                  type="text"
                  id="position"
                  name="position"
                  value={formData.position}
                  onChange={handleChange}
                  required
                  disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="departmentId">Department *</label>
              <select
                  id="departmentId"
                  name="departmentId"
                  value={formData.departmentId}
                  onChange={handleChange}
                  required
                  disabled={loading}
              >
                <option value="">Select a department</option>
                {departments.map((dept) => (
                    <option key={dept.id} value={dept.id}>
                      {dept.name} - {dept.location}
                    </option>
                ))}
              </select>
              {departments.length === 0 && (
                  <p style={{ color: '#e74c3c', fontSize: '0.875rem', marginTop: '0.5rem' }}>
                    No departments available. Please create a department first.
                  </p>
              )}
            </div>

            <div className="form-actions">
              <button
                  type="button"
                  onClick={() => navigate('/employees')}
                  className="btn btn-secondary"
                  disabled={loading}
              >
                Cancel
              </button>
              <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={loading || departments.length === 0}
              >
                {loading ? 'Saving...' : isEdit ? 'Update' : 'Create'}
              </button>
            </div>
          </form>
        </div>
      </div>
  );
};

export default EmployeeForm;