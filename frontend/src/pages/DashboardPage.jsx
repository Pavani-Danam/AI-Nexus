import React from 'react';
import { Link } from 'react-router-dom';
import { DocumentIcon, WorkspaceIcon, ChatIcon } from '../components/ui/Icons';

export default function DashboardPage() {
  const stats = [
    { title: 'Documents', value: '0', description: 'Indexed in knowledge base', icon: DocumentIcon, link: '/documents' },
    { title: 'Workspaces', value: '0', description: 'Active team environments', icon: WorkspaceIcon, link: '/workspaces' },
    { title: 'AI Conversations', value: '0', description: 'Active reasoning sessions', icon: ChatIcon, link: '/chat' },
    { title: 'Knowledge Sources', value: '0', description: 'Vector embeddings ready', icon: DocumentIcon, link: '/documents' },
  ];

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="p-6 sm:p-8 rounded-2xl bg-gradient-to-r from-indigo-900/40 via-slate-900 to-slate-900 border border-indigo-500/20 shadow-xs">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 mb-3">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          System Online
        </div>
        <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 dark:text-white tracking-tight">
          Welcome to AI-Nexus
        </h2>
        <p className="text-slate-600 dark:text-slate-400 mt-1 text-sm sm:text-base max-w-2xl">
          Enterprise AI Knowledge Operating System — Manage vectorized knowledge bases, configure multi-agent orchestration, and explore contextual synthesis.
        </p>
      </div>

      {/* Metric Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {stats.map((stat) => (
          <Link
            key={stat.title}
            to={stat.link}
            className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 hover:border-indigo-500/40 dark:hover:border-indigo-500/40 transition-all shadow-xs group"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                {stat.title}
              </span>
              <div className="p-2 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 group-hover:bg-indigo-50 dark:group-hover:bg-indigo-500/20 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                <stat.icon className="w-5 h-5" />
              </div>
            </div>
            <div className="mt-4 flex items-baseline gap-2">
              <span className="text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight">
                {stat.value}
              </span>
            </div>
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
              {stat.description}
            </p>
          </Link>
        ))}
      </div>

      {/* Quick Access Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
          <h3 className="text-base font-semibold text-slate-900 dark:text-white mb-2">
            Knowledge Ingestion
          </h3>
          <p className="text-sm text-slate-600 dark:text-slate-400 mb-4">
            Upload PDF, DOCX, and TXT files to generate embeddings and vectorize knowledge for agent retrieval.
          </p>
          <Link
            to="/documents"
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-500 transition-colors shadow-xs"
          >
            Upload Document
          </Link>
        </div>

        <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
          <h3 className="text-base font-semibold text-slate-900 dark:text-white mb-2">
            AI Knowledge Assistant
          </h3>
          <p className="text-sm text-slate-600 dark:text-slate-400 mb-4">
            Query your ingested knowledge base with contextual RAG and multi-step reasoning capabilities.
          </p>
          <Link
            to="/chat"
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium text-slate-700 dark:text-slate-200 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors"
          >
            Start Conversation
          </Link>
        </div>
      </div>
    </div>
  );
}
