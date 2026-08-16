import React from 'react';

export default function SettingsPage() {
  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white tracking-tight">
          Settings
        </h2>
        <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
          Manage your account preferences, system appearance, and security policies.
        </p>
      </div>

      {/* Profile Section */}
      <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs space-y-4">
        <h3 className="text-base font-semibold text-slate-900 dark:text-white">
          Profile
        </h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase tracking-wider mb-1">
              Full Name
            </label>
            <input
              type="text"
              defaultValue="User"
              readOnly
              className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-slate-900 dark:text-slate-100"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase tracking-wider mb-1">
              Email Address
            </label>
            <input
              type="email"
              defaultValue="user@example.com"
              readOnly
              className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-slate-900 dark:text-slate-100"
            />
          </div>
        </div>
      </div>

      {/* Appearance Section */}
      <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs space-y-3">
        <h3 className="text-base font-semibold text-slate-900 dark:text-white">
          Appearance
        </h3>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          Switch between light and dark visual themes using the topbar toggle. Theme preferences are persisted automatically.
        </p>
      </div>

      {/* Security Section */}
      <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs space-y-3">
        <h3 className="text-base font-semibold text-slate-900 dark:text-white">
          Security
        </h3>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          Stateless JWT token authentication, BCrypt encryption, and refresh token rotation are enforced on the backend.
        </p>
      </div>
    </div>
  );
}
