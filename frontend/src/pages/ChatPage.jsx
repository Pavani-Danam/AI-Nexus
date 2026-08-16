import React from 'react';
import { ChatIcon } from '../components/ui/Icons';

export default function ChatPage() {
  return (
    <div className="h-[calc(100vh-10rem)] flex flex-col rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs overflow-hidden">
      {/* Chat Header */}
      <div className="px-6 py-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-xl bg-indigo-50 dark:bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
            <ChatIcon className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-semibold text-slate-900 dark:text-white">
              AI Knowledge Assistant
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Contextual synthesis via RAG pipeline
            </p>
          </div>
        </div>
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
          Ready
        </span>
      </div>

      {/* Message Area / Empty Chat State */}
      <div className="flex-1 overflow-y-auto p-6 flex flex-col items-center justify-center text-center">
        <div className="w-12 h-12 rounded-2xl bg-slate-100 dark:bg-slate-800 text-slate-400 flex items-center justify-center mb-3">
          <ChatIcon className="w-6 h-6" />
        </div>
        <h3 className="text-base font-medium text-slate-900 dark:text-white mb-1">
          How can I help you today?
        </h3>
        <p className="text-xs text-slate-500 dark:text-slate-400 max-w-sm">
          Ask questions across your ingested enterprise documents, research summaries, and workspace notes.
        </p>
      </div>

      {/* Prompt Input Area */}
      <div className="p-4 border-t border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
        <form onSubmit={(e) => e.preventDefault()} className="flex items-center gap-3">
          <input
            type="text"
            placeholder="Ask AI-Nexus anything about your documents..."
            className="flex-1 px-4 py-2.5 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700/80 rounded-xl text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/30 focus:border-indigo-500 transition-all"
          />
          <button
            type="submit"
            className="px-5 py-2.5 rounded-xl text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-500 transition-colors shadow-xs flex-shrink-0"
          >
            Send
          </button>
        </form>
      </div>
    </div>
  );
}
