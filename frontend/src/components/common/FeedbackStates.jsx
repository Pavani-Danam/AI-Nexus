import React from 'react';

export function SkeletonCard({ count = 1 }) {
  return (
    <>
      {Array.from({ length: count }).map((_, index) => (
        <div
          key={index}
          className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 animate-pulse space-y-4"
        >
          <div className="flex justify-between items-center">
            <div className="h-3.5 bg-slate-800 rounded-md w-24" />
            <div className="w-8 h-8 bg-slate-800 rounded-xl" />
          </div>
          <div className="space-y-2">
            <div className="h-7 bg-slate-800 rounded-md w-16" />
            <div className="h-3 bg-slate-800/80 rounded-md w-32" />
          </div>
        </div>
      ))}
    </>
  );
}

export function ErrorStateCard({
  title = "Unable to load data",
  message = "A connectivity or server issue occurred while loading this section.",
  onRetry
}) {
  return (
    <div className="p-8 rounded-2xl bg-rose-500/5 border border-rose-500/20 text-center space-y-3">
      <div className="w-10 h-10 mx-auto rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      </div>
      <h4 className="text-sm font-semibold text-rose-300">{title}</h4>
      <p className="text-xs text-slate-400 max-w-sm mx-auto">{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-xl text-xs font-semibold transition-all"
        >
          Try Again
        </button>
      )}
    </div>
  );
}
