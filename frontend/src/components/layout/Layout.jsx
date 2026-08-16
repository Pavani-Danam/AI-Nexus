import React from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';

export default function Layout() {
  const location = useLocation();

  const navLinks = [
    { name: 'Home', path: '/' },
    { name: 'Dashboard', path: '/dashboard' },
    { name: 'Documents', path: '/documents' },
    { name: 'Workspaces', path: '/workspaces' },
    { name: 'Chat', path: '/chat' },
  ];

  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 font-sans">
      <header className="border-b border-slate-800 bg-slate-900/70 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-indigo-500 to-cyan-400 flex items-center justify-center font-bold text-white shadow-lg shadow-indigo-500/25">
              AI
            </div>
            <Link to="/" className="text-xl font-bold tracking-tight text-white hover:text-indigo-400 transition-colors">
              AI-Nexus
            </Link>
          </div>

          <nav className="hidden md:flex items-center gap-1">
            {navLinks.map((link) => {
              const isActive = location.pathname === link.path;
              return (
                <Link
                  key={link.path}
                  to={link.path}
                  className={`px-3 py-2 rounded-lg text-sm font-medium transition-all ${
                    isActive
                      ? 'bg-indigo-600/20 text-indigo-400 border border-indigo-500/30'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                  }`}
                >
                  {link.name}
                </Link>
              );
            })}
          </nav>

          <div className="flex items-center gap-3">
            <Link
              to="/login"
              className="px-3.5 py-1.5 rounded-lg text-sm font-medium text-slate-300 hover:text-white hover:bg-slate-800/80 transition-colors border border-slate-700/50"
            >
              Sign In
            </Link>
            <Link
              to="/register"
              className="px-3.5 py-1.5 rounded-lg text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-500 transition-colors shadow-md shadow-indigo-600/20"
            >
              Get Started
            </Link>
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>

      <footer className="border-t border-slate-800/80 py-6 text-center text-xs text-slate-500">
        <p>&copy; 2026 AI-Nexus. Enterprise AI Knowledge Operating System. All rights reserved.</p>
      </footer>
    </div>
  );
}
