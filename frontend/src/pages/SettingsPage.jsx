import React from 'react';
import { useAuth } from '../context/AuthContext';

export default function SettingsPage() {
  const { user } = useAuth();

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-2xl font-bold text-slate-100 tracking-tight">Settings</h1>
        <p className="text-xs text-slate-400 mt-1">
          Manage your account profile and workspace preferences.
        </p>
      </div>

      <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-5">
        <h2 className="text-sm font-semibold text-slate-200 border-b border-slate-800 pb-3">
          User Profile
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div>
            <span className="text-slate-500 font-medium">Full Name</span>
            <p className="text-slate-200 font-semibold mt-0.5">{user?.name || user?.fullName || 'User'}</p>
          </div>
          <div>
            <span className="text-slate-500 font-medium">Email Address</span>
            <p className="text-slate-200 font-semibold mt-0.5">{user?.email || 'N/A'}</p>
          </div>
          <div>
            <span className="text-slate-500 font-medium">Role</span>
            <p className="text-slate-200 font-semibold mt-0.5">
              <span className="inline-block px-2 py-0.5 rounded text-[10px] font-medium bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                {user?.role || 'USER'}
              </span>
            </p>
          </div>
          <div>
            <span className="text-slate-500 font-medium">Session Status</span>
            <p className="text-emerald-400 font-semibold mt-0.5 flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              Authenticated
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
