import axios from 'axios';
import { clearAuthData } from './auth';

const API_BASE_URL = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8888';

// Create axios instance with default config
const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor to add JWT token from localStorage
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('access_token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor to handle errors
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            // JWT token expired or invalid - clear all auth data from localStorage
            clearAuthData();
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// Department API - updated to match OpenAPI spec
export const departmentAPI = {
    getAll: () => api.get('/department-service/departments'),
    getById: (id) => api.get(`/department-service/departments/${id}`),
    create: (department) => api.post('/department-service/departments', department),
    update: (id, department) => api.put(`/department-service/departments/${id}`, department),
    delete: (id) => api.delete(`/department-service/departments/${id}`),
};

// Employee API - updated to match OpenAPI spec
export const employeeAPI = {
    getAll: (params) => api.get('/employee-service/employees', { params }),
    getById: (id) => api.get(`/employee-service/employees/${id}`),
    create: (employee) => api.post('/employee-service/employees', employee),
    update: (id, employee) => api.put(`/employee-service/employees/${id}`, employee),
    delete: (id) => api.delete(`/employee-service/employees/${id}`),
};

export default api;