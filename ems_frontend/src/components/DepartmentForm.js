import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { departmentAPI } from '../services/api';

const DepartmentForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;
  
  const [formData, setFormData] = useState({
    name: '',
    location: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (isEdit) {
      loadDepartment();
    }
  }, [id, isEdit]);

  const loadDepartment = async () => {
    try {
      setLoading(true);
      const response = await departmentAPI.getById(id);
      setFormData({
        name: response.data.name || '',
        location: response.data.location || '',
      });
    } catch (err) {
      setError('Failed to load department');
      console.error('Error loading department:', err);
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
      if (isEdit) {
        await departmentAPI.update(id, formData);
      } else {
        await departmentAPI.create(formData);
      }
      navigate('/departments');
    } catch (err) {
      setError(
        err.response?.data?.message || 
        `Failed to ${isEdit ? 'update' : 'create'} department. Only ADMIN users can modify departments.`
      );
      console.error('Error saving department:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading && isEdit) {
    return <div className="loading">Loading department...</div>;
  }

  return (
    <div className="container">
      <div className="page-header">
        <h1 className="page-title">
          {isEdit ? 'Edit Department' : 'Create New Department'}
        </h1>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="card">
        <form onSubmit={handleSubmit} className="form-container">
          <div className="form-group">
            <label htmlFor="name">Department Name *</label>
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
            <label htmlFor="location">Location *</label>
            <input
              type="text"
              id="location"
              name="location"
              value={formData.location}
              onChange={handleChange}
              required
              disabled={loading}
            />
          </div>

          <div className="form-actions">
            <button
              type="button"
              onClick={() => navigate('/departments')}
              className="btn btn-secondary"
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              {loading ? 'Saving...' : isEdit ? 'Update' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default DepartmentForm;

