import React from 'react';
import { NavLink, Link } from 'react-router-dom';
import {
  DashboardIcon,
  DocumentIcon,
  WorkspaceIcon,
  ChatIcon,
  SettingsIcon,
} from '../ui/Icons';

export default function Sidebar({ onItemClick }) {
  const mainNav = [
    { name: 'Dashboard', path: '/dashboard', icon: DashboardIcon },
    { name: 'Documents', path: '/documents', icon: DocumentIcon },
    { name: 'Workspaces', path: '/workspaces', icon: WorkspaceIcon },
    { name: 'Workflows', path: '/workflows', icon: WorkspaceIcon },
    { name: 'AI Chat', path: '/chat', icon: ChatIcon },
  ];

  const systemNav = [
    { name: 'Settings', path: '/settings', icon: SettingsIcon },
  ];

  return (
    <aside className="w-64 h-full flex flex-col bg-white dark:bg-slate-900 border-r border-slate-200 dark:border-slate-800 transition-colors duration-200">
      {/* Brand Header */}
      <div className="h-16 px-6 flex items-center gap-3 border-b border-slate-200 dark:border-slate-800">
        <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-indigo-600 to-cyan-500 flex items-center justify-center font-bold text-white shadow-md shadow-indigo-500/20">
          AI
        </div>
        <div className="flex flex-col">
          <Link to="/" className="text-base font-bold tracking-tight text-slate-900 dark:text-white hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors">
            AI-Nexus
          </Link>
          <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500">
            Enterprise OS
          </span>
        </div>
      </div>

      {/* Navigation Sections */}
      <div className="flex-1 overflow-y-auto px-4 py-5 space-y-6">
        <div>
          <p className="px-3 text-[11px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-2">
            Main
          </p>
          <nav className="space-y-1">
            {mainNav.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  onClick={onItemClick}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all ${
                      isActive
                        ? 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950/50 dark:text-indigo-400'
                        : 'text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800/50 hover:text-slate-900 dark:hover:text-slate-200'
                    }`
                  }
                >
                  <Icon className="w-5 h-5" />
                  {item.name}
                </NavLink>
              );
            })}
          </nav>
        </div>

        <div>
          <p className="px-3 text-[11px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-2">
            System
          </p>
          <nav className="space-y-1">
            {systemNav.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  onClick={onItemClick}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all ${
                      isActive
                        ? 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950/50 dark:text-indigo-400'
                        : 'text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800/50 hover:text-slate-900 dark:hover:text-slate-200'
                    }`
                  }
                >
                  <Icon className="w-5 h-5" />
                  {item.name}
                </NavLink>
              );
            })}
          </nav>
        </div>
      </div>
    </aside>
  );
}
