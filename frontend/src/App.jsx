import React, { useState, useEffect } from 'react';
import { getToken, getUser, clearAuth } from './api';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import BoardPage from './pages/BoardPage';

export default function App() {
  const [currentPath, setCurrentPath] = useState(window.location.pathname);
  const [user, setUser] = useState(getUser());

  // Listen to browser back/forward buttons and pushState events
  useEffect(() => {
    const handleLocationChange = () => {
      setCurrentPath(window.location.pathname);
      setUser(getUser());
    };

    window.addEventListener('popstate', handleLocationChange);
    return () => window.removeEventListener('popstate', handleLocationChange);
  }, []);

  const navigate = (path) => {
    window.history.pushState({}, '', path);
    setCurrentPath(path);
    setUser(getUser());
  };

  const handleAuthSuccess = (authenticatedUser) => {
    setUser(authenticatedUser);
    navigate('/');
  };

  const handleLogout = () => {
    clearAuth();
    setUser(null);
    navigate('/login');
  };

  // Auth Guards
  const token = getToken();

  // If path is /login or /register and already authenticated, go to /
  if (token && (currentPath === '/login' || currentPath === '/register')) {
    navigate('/');
    return null;
  }

  // If unauthenticated and not on /register, show login
  if (!token) {
    if (currentPath === '/register') {
      return <RegisterPage onRegisterSuccess={handleAuthSuccess} onNavigate={navigate} />;
    }
    return <LoginPage onLoginSuccess={handleAuthSuccess} onNavigate={navigate} />;
  }

  // Authenticated: show Board
  return <BoardPage user={user} onLogout={handleLogout} />;
}
