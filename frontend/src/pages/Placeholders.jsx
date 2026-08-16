import React from 'react';
import { Link } from 'react-router-dom';

export function LoginPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 p-4">
      <div className="w-full max-w-md p-8 rounded-2xl bg-slate-900 border border-slate-800 text-center shadow-xl">
        <div className="w-10 h-10 mx-auto rounded-xl bg-indigo-600 flex items-center justify-center font-bold text-white mb-4">
          AI
        </div>
        <h2 className="text-2xl font-bold text-white mb-2">Sign in to AI-Nexus</h2>
        <p className="text-xs text-slate-400 mb-6">Authentication interface placeholder</p>
        <Link
          to="/dashboard"
          className="w-full block py-2.5 px-4 rounded-xl text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-500 transition-colors"
        >
          Enter Dashboard
        </Link>
        <p className="text-xs text-slate-500 mt-4">
          Don't have an account? <Link to="/register" className="text-indigo-400 hover:underline">Register</Link>
        </p>
      </div>
    </div>
  );
}

export function RegisterPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 p-4">
      <div className="w-full max-w-md p-8 rounded-2xl bg-slate-900 border border-slate-800 text-center shadow-xl">
        <div className="w-10 h-10 mx-auto rounded-xl bg-indigo-600 flex items-center justify-center font-bold text-white mb-4">
          AI
        </div>
        <h2 className="text-2xl font-bold text-white mb-2">Create an Account</h2>
        <p className="text-xs text-slate-400 mb-6">Registration interface placeholder</p>
        <Link
          to="/dashboard"
          className="w-full block py-2.5 px-4 rounded-xl text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-500 transition-colors"
        >
          Proceed to Dashboard
        </Link>
        <p className="text-xs text-slate-500 mt-4">
          Already have an account? <Link to="/login" className="text-indigo-400 hover:underline">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
