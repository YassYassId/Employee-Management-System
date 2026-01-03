// src/services/auth.js

const KEYCLOAK_URL = 'http://localhost:8080';
const REALM = 'employee-realm';
const CLIENT_ID = 'ems-app';

const KEYCLOAK_TOKEN_URL = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;
const KEYCLOAK_AUTH_URL = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/auth`;
const KEYCLOAK_LOGOUT_URL = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/logout`;

// Generate a random code verifier (43–128 characters, URL-safe)
export const generateCodeVerifier = () => {
  const array = new Uint8Array(56);
  window.crypto.getRandomValues(array);
  // Convert to URL-safe Base64
  return btoa(String.fromCharCode(...array))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
};

// Generate code challenge (SHA256 -> Base64 URL)
export const generateCodeChallenge = async (verifier) => {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const digest = await crypto.subtle.digest('SHA-256', data);
  const base64 = btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
  return base64;
};

// Always use window.location.origin for redirectUri to support Dockerized environments
const getRedirectUri = () => `${window.location.origin}/auth/callback`;

// ---------------- Auth URL ----------------
export const getAuthUrl = async (forceLogin = true) => {
  const redirectUri = getRedirectUri();

  const state = Math.random().toString(36).substring(7);
  localStorage.setItem('oauth_state', state);

  const codeVerifier = generateCodeVerifier();
  sessionStorage.setItem('pkce_code_verifier', codeVerifier);
  const codeChallenge = await generateCodeChallenge(codeVerifier);

  const params = new URLSearchParams({
    client_id: CLIENT_ID,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope: 'openid profile email',
    state: state,
    code_challenge: codeChallenge,
    code_challenge_method: 'S256',
  });

  if (forceLogin) {
    params.append('prompt', 'login');
    params.append('max_age', '0');
  }

  return `${KEYCLOAK_AUTH_URL}?${params.toString()}`;
};

// ---------------- Token Exchange ----------------
export const exchangeCodeForTokens = async (code) => {
  const redirectUri = getRedirectUri();
  const codeVerifier = sessionStorage.getItem('pkce_code_verifier');
  if (!codeVerifier) throw new Error('PKCE code verifier missing');

  const params = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: CLIENT_ID,
    code,
    redirect_uri: redirectUri,
    code_verifier: codeVerifier,
  });

  const response = await fetch(KEYCLOAK_TOKEN_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: params.toString(),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    const errorMessage = errorData.error_description || errorData.error || `HTTP ${response.status}`;
    // Log for debugging
    console.error('Token exchange failed:', errorMessage, errorData);
    throw new Error(errorMessage);
  }

  const data = await response.json();
  storeTokens(data);
  return data;
};

// ---------------- Token & User Storage ----------------
export const storeTokens = (tokenData) => {
  if (tokenData.access_token) localStorage.setItem('access_token', tokenData.access_token);
  if (tokenData.refresh_token) localStorage.setItem('refresh_token', tokenData.refresh_token);
  window.dispatchEvent(new Event('authStateChange'));
};

export const getAccessToken = () => localStorage.getItem('access_token');
export const getRefreshToken = () => localStorage.getItem('refresh_token');

export const clearAuthData = () => {
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
  localStorage.removeItem('user_info');
  localStorage.removeItem('oauth_state');
  Object.keys(localStorage).forEach((key) => {
    if (key.startsWith('processed_code_')) {
      localStorage.removeItem(key);
      localStorage.removeItem(key + '_time');
    }
  });
  window.dispatchEvent(new Event('authStateChange'));
};

// ---------------- Logout ----------------
export const logout = () => {
  clearAuthData();
  const redirectUri = window.location.origin + '/login';
  const params = new URLSearchParams({ client_id: CLIENT_ID, redirect_uri: redirectUri });
  window.location.href = `${KEYCLOAK_LOGOUT_URL}?${params.toString()}`;
};

// ---------------- Authentication Check ----------------
export const isAuthenticated = () => {
  const token = getAccessToken();
  if (!token) return false;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    // Log token expiration for debugging
    // console.log('Token exp:', payload.exp, 'Now:', Math.floor(Date.now() / 1000));
    return payload.exp * 1000 > Date.now();
  } catch (e) {
    // Log for debugging
    // console.error('Token parse error:', e);
    return false;
  }
};

// ---------------- User Info ----------------
export const getUserInfo = (token) => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return {
      username: payload.preferred_username || payload.sub,
      email: payload.email,
      roles: extractRoles(payload),
    };
  } catch {
    return null;
  }
};

const extractRoles = (payload) => {
  const roles = [];
  if (payload.realm_access?.roles) roles.push(...payload.realm_access.roles);
  if (payload.resource_access?.[CLIENT_ID]?.roles) roles.push(...payload.resource_access[CLIENT_ID].roles);
  return roles;
};

export const isAdmin = (userInfo) => userInfo?.roles?.some((r) => r.toUpperCase() === 'ADMIN');

export const cleanupProcessedCodes = () => {
  Object.keys(localStorage).forEach((key) => {
    if (key.startsWith('processed_code_')) {
      const timestamp = localStorage.getItem(key + '_time');
      if (timestamp && Date.now() - parseInt(timestamp) > 5 * 60 * 1000) {
        localStorage.removeItem(key);
        localStorage.removeItem(key + '_time');
      }
    }
  });
};

export const getStoredUserInfo = () => {
  const str = localStorage.getItem('user_info');
  if (!str) return null;
  try { return JSON.parse(str); } catch { return null; }
};
