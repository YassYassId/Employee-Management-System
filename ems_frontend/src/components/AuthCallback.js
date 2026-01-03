import React, { useEffect, useState, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { exchangeCodeForTokens, getUserInfo, storeTokens } from '../services/auth';

const AuthCallback = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const hasProcessed = useRef(false);

  useEffect(() => {
    if (hasProcessed.current) return;

    const code = searchParams.get('code');
    const state = searchParams.get('state');
    const storedState = localStorage.getItem('oauth_state');
    const processedCodeKey = `processed_code_${code}`;

    if (!code || state !== storedState) {
      setError('Invalid authorization code or state');
      return;
    }

    hasProcessed.current = true;
    localStorage.setItem(processedCodeKey, 'true');
    localStorage.setItem(processedCodeKey + '_time', Date.now().toString());

    const handleAuth = async () => {
      try {
        // Defensive: check PKCE code verifier
        const codeVerifier = sessionStorage.getItem('pkce_code_verifier');
        if (!codeVerifier) {
          setError('Missing PKCE code verifier. Please try logging in again.');
          localStorage.removeItem(processedCodeKey);
          localStorage.removeItem('oauth_state');
          return;
        }

        const tokenData = await exchangeCodeForTokens(code);
        storeTokens(tokenData);

        const userInfo = getUserInfo(tokenData.access_token);
        if (userInfo) localStorage.setItem('user_info', JSON.stringify(userInfo));

        localStorage.removeItem('oauth_state');
        localStorage.removeItem(processedCodeKey);
        sessionStorage.removeItem('pkce_code_verifier');

        window.dispatchEvent(new Event('authStateChange'));
        navigate('/', { replace: true });
      } catch (err) {
        console.error('Authentication error:', err);
        localStorage.removeItem(processedCodeKey);
        localStorage.removeItem('oauth_state');
        sessionStorage.removeItem('pkce_code_verifier');
        hasProcessed.current = false;
        setError(err.message || 'Failed to authenticate. Please try again.');
      }
    };

    handleAuth();
  }, [searchParams, navigate]);

  if (error) {
    return (
      <div className="container">
        <h2>Authentication Error</h2>
        <p>{error}</p>
        <button onClick={() => navigate('/login')}>Return to Login</button>
      </div>
    );
  }

  return <div className="container"><p>Completing authentication...</p></div>;
};

export default AuthCallback;
