import React from 'react';
import { Routes, Route } from 'react-router-dom';
import Layout from './components/layout/Layout';
import HomePage from './pages/HomePage';
import {
  LoginPage,
  RegisterPage,
  DashboardPage,
  DocumentsPage,
  WorkspacesPage,
  ChatPage
} from './pages/Placeholders';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="documents" element={<DocumentsPage />} />
        <Route path="workspaces" element={<WorkspacesPage />} />
        <Route path="chat" element={<ChatPage />} />
      </Route>
    </Routes>
  );
}
