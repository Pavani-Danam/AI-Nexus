import React from 'react';
import { WorkspaceIcon } from '../components/ui/Icons';

export default function WorkspacesPage() {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 dark:text-white tracking-tight">
            Workspaces
          </h2>
          <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
            Organize knowledge and collaboration by workspace.
          </p>
        </div>
        <button
          type="button"
          className="self-start sm:self-auto px-4 py-2 rounded-xl text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-500 transition-colors shadow-xs"
        >
          Create Workspace
        </button>
      </div>

      {/* Empty State */}
      <div className="p-12 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-center">
        <div className="w-12 h-12 mx-auto rounded-2xl bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 flex items-center justify-center mb-4">
          <WorkspaceIcon className="w-6 h-6" />
        </div>
        <h3 className="text-base font-semibold text-slate-900 dark:text-white mb-1">
          No workspaces yet
        </h3>
        <p className="text-sm text-slate-500 dark:text-slate-400 max-w-sm mx-auto mb-6">
          Create a workspace to organize your knowledge and isolate access control for your team.
        </p>
        <button
          type="button"
          className="px-4 py-2 rounded-xl text-sm font-medium text-slate-700 dark:text-slate-200 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors"
        >
          New Workspace
        </button>
      </div>
    </div>
  );
}
