import React, { createContext, useContext, useState, useEffect } from 'react';
import authService from '../services/authService';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Check initial authentication state from stored token & user data
    const token = authService.getAccessToken();
    const storedUser = authService.getCurrentUser();

    if (token && storedUser) {
      setUser(storedUser);
    } else {
      setUser(null);
    }
    setIsLoading(false);
  }, []);

  const login = async (credentials) => {
    const data = await authService.login(credentials);
    if (data.user) {
      setUser(data.user);
    } else {
      // Fallback if backend does not embed full user in root response
      const currentUser = authService.getCurrentUser();
      setUser(currentUser);
    }
    return data;
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
  };

  const value = {
    user,
    isAuthenticated: !!user,
    isLoading,
    login,
    logout
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
