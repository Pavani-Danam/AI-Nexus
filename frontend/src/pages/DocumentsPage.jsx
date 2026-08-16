import React from 'react';
import { DocumentIcon } from '../components/ui/Icons';

export default function DocumentsPage() {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white tracking-tight">
          Documents
        </h2>
        <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
          Manage and search your organization's knowledge.
        </p>
      </div>

      {/* Upload Dropzone Placeholder */}
      <div className="p-8 sm:p-12 border-2 border-dashed border-slate-300 dark:border-slate-700 hover:border-indigo-500/50 rounded-2xl bg-white dark:bg-slate-900/50 text-center transition-all">
        <div className="w-12 h-12 mx-auto rounded-2xl bg-indigo-50 dark:bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center mb-4">
          <DocumentIcon className="w-6 h-6" />
        </div>
        <h3 className="text-base font-semibold text-slate-900 dark:text-white mb-1">
          Upload documents
        </h3>
        <p className="text-xs text-slate-500 dark:text-slate-400 mb-4">
          Drag and drop your knowledge files here or browse from your computer
        </p>
        <p className="text-xs font-semibold text-slate-400 dark:text-slate-500 mb-6 uppercase tracking-wider">
          Supported formats: PDF, DOCX, TXT
        </p>
        <button
          type="button"
          className="px-4 py-2 rounded-xl text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-500 transition-colors shadow-xs"
        >
          Select Files
        </button>
      </div>

      {/* Empty State / Documents Table Placeholder */}
      <div className="p-8 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-center">
        <p className="text-sm font-medium text-slate-900 dark:text-slate-100">
          No documents uploaded yet
        </p>
        <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
          Ingested documents will appear here with chunk counts and vectorization status.
        </p>
      </div>
    </div>
  );
}
