import React, { useState } from 'react';

export function FormInput({
  label,
  id,
  type = "text",
  value,
  onChange,
  placeholder,
  required = false,
  error = "",
  disabled = false,
  autoComplete
}) {
  return (
    <div className="space-y-1.5 text-left">
      <label htmlFor={id} className="block text-xs font-semibold text-slate-700 dark:text-slate-200">
        {label} {required && <span className="text-rose-500">*</span>}
      </label>
      <input
        id={id}
        name={id}
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        required={required}
        disabled={disabled}
        autoComplete={autoComplete}
        className={`w-full px-3.5 py-2.5 bg-slate-900/90 dark:bg-slate-800 border rounded-xl text-sm text-slate-100 placeholder-slate-500 transition-all focus:outline-hidden focus:ring-2 disabled:opacity-60 disabled:cursor-not-allowed ${
          error
            ? 'border-rose-500 focus:ring-rose-500/30 focus:border-rose-500'
            : 'border-slate-700 focus:ring-indigo-500/40 focus:border-indigo-400'
        }`}
      />
      {error && <p className="text-xs text-rose-400 font-medium">{error}</p>}
    </div>
  );
}

export function PasswordInput({
  label,
  id,
  value,
  onChange,
  placeholder,
  required = false,
  error = "",
  disabled = false,
  autoComplete
}) {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <div className="space-y-1.5 text-left">
      <label htmlFor={id} className="block text-xs font-semibold text-slate-700 dark:text-slate-200">
        {label} {required && <span className="text-rose-500">*</span>}
      </label>
      <div className="relative">
        <input
          id={id}
          name={id}
          type={showPassword ? "text" : "password"}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          required={required}
          disabled={disabled}
          autoComplete={autoComplete}
          className={`w-full pl-3.5 pr-11 py-2.5 bg-slate-900/90 dark:bg-slate-800 border rounded-xl text-sm text-slate-100 placeholder-slate-500 transition-all focus:outline-hidden focus:ring-2 disabled:opacity-60 disabled:cursor-not-allowed ${
            error
              ? 'border-rose-500 focus:ring-rose-500/30 focus:border-rose-500'
              : 'border-slate-700 focus:ring-indigo-500/40 focus:border-indigo-400'
          }`}
        />
        <button
          type="button"
          onClick={() => setShowPassword(!showPassword)}
          tabIndex={-1}
          className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-200"
          aria-label={showPassword ? "Hide password" : "Show password"}
        >
          {showPassword ? (
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
            </svg>
          ) : (
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          )}
        </button>
      </div>
      {error && <p className="text-xs text-rose-400 font-medium">{error}</p>}
    </div>
  );
}

export function FormButton({
  children,
  loading = false,
  type = "submit",
  disabled = false,
  className = ""
}) {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      className={`w-full py-2.5 px-4 rounded-xl text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 disabled:cursor-not-allowed transition-all shadow-md shadow-indigo-600/30 flex items-center justify-center gap-2 ${className}`}
    >
      {loading && (
        <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
        </svg>
      )}
      {children}
    </button>
  );
}

export function AlertBanner({ type = "error", message, onClose }) {
  if (!message) return null;

  const isError = type === "error";

  return (
    <div
      className={`p-3.5 rounded-xl text-xs sm:text-sm flex items-start justify-between gap-2 transition-all ${
        isError
          ? 'bg-rose-500/15 border border-rose-500/30 text-rose-300'
          : 'bg-emerald-500/15 border border-emerald-500/30 text-emerald-300'
      }`}
      role="alert"
    >
      <div className="flex items-center gap-2">
        <span className="font-semibold">{isError ? "Error:" : "Success:"}</span>
        <span>{message}</span>
      </div>
      {onClose && (
        <button
          type="button"
          onClick={onClose}
          className="text-slate-400 hover:text-slate-200 text-base leading-none"
          aria-label="Dismiss alert"
        >
          &times;
        </button>
      )}
    </div>
  );
}
