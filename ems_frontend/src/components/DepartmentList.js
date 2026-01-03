import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { departmentAPI } from '../services/api';
import { isAdmin, getStoredUserInfo } from '../services/auth';

const DepartmentList = () => {
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteDeptId, setDeleteDeptId] = useState(null);
  const userInfo = getStoredUserInfo();
  const admin = isAdmin(userInfo);

  useEffect(() => {
    loadDepartments();
  }, []);

  const loadDepartments = async () => {
    try {
      setLoading(true);
      const response = await departmentAPI.getAll();
      // Extract content from paginated response
      const departmentData = response.data.content || response.data;
      setDepartments(Array.isArray(departmentData) ? departmentData : []);
      setError(null);
    } catch (err) {
      setError('Failed to load departments. Make sure you are authenticated and the gateway is running.');
      console.error('Error loading departments:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    try {
      await departmentAPI.delete(id);
      setMessage('Department deleted successfully');
      loadDepartments();
      setTimeout(() => setMessage(null), 3000);
    } catch (err) {
      setError('Failed to delete department. Only ADMIN users can delete.');
      console.error('Error deleting department:', err);
    } finally {
      setShowDeleteModal(false);
      setDeleteDeptId(null);
    }
  };

  const openDeleteModal = (id) => {
    setDeleteDeptId(id);
    setShowDeleteModal(true);
  };

  const closeDeleteModal = () => {
    setShowDeleteModal(false);
    setDeleteDeptId(null);
  };

  if (loading) {
    return <div className="loading">Loading departments...</div>;
  }

  return (
      <div className="container">
        <div className="page-header">
          <h1 className="page-title">Departments</h1>
          {admin && (
              <Link to="/departments/new" className="btn btn-primary">
                Add New Department
              </Link>
          )}
        </div>

        {message && <div className="success-message">{message}</div>}
        {error && <div className="error-message">{error}</div>}

        {departments.length === 0 ? (
            <div className="empty-state">
              <h3>No departments found</h3>
              <p>Get started by creating your first department.</p>
              {admin && (
                  <Link to="/departments/new" className="btn btn-primary" style={{ marginTop: '1rem' }}>
                    Create Department
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
                  <th>Location</th>
                  {admin && <th>Actions</th>}
                </tr>
                </thead>
                <tbody>
                {departments.map((dept) => (
                    <tr key={dept.id}>
                      <td>{dept.id}</td>
                      <td>{dept.name}</td>
                      <td>{dept.location}</td>
                      {admin && (
                          <td>
                            <div className="actions">
                              <Link
                                  to={`/departments/${dept.id}/edit`}
                                  className="btn btn-secondary btn-small"
                              >
                                Edit
                              </Link>
                              <button
                                  onClick={() => openDeleteModal(dept.id)}
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

        {/* Delete Confirmation Modal */}
        {showDeleteModal && (
            <div className="modal-overlay">
              <div className="modal">
                <h3>Confirm Deletion</h3>
                <p>Are you sure you want to delete this department?</p>
                <div className="modal-actions">
                  <button className="btn btn-danger" onClick={() => handleDelete(deleteDeptId)}>
                    Yes, Delete
                  </button>
                  <button className="btn btn-secondary" onClick={closeDeleteModal}>
                    Cancel
                  </button>
                </div>
              </div>
            </div>
        )}
      </div>
  );
};

export default DepartmentList;

// Add minimal modal styles (can be moved to CSS file)
const modalStyles = document.createElement('style');
modalStyles.innerHTML = `
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal {
  background: #fff;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 16px rgba(0,0,0,0.2);
  min-width: 320px;
  max-width: 90vw;
}
.modal-actions {
  display: flex;
  gap: 1rem;
  margin-top: 1.5rem;
  justify-content: flex-end;
}
`;
if (typeof window !== 'undefined' && !document.getElementById('modal-styles')) {
  modalStyles.id = 'modal-styles';
  document.head.appendChild(modalStyles);
}