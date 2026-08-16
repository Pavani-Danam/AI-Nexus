import React from 'react';
import { Link } from 'react-router-dom';

export default function HomePage() {
  return (
    <div className="flex flex-col items-center justify-center text-center py-16 sm:py-24">
      <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 mb-6">
        <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
        Backend Connected • Spring Boot Security Active
      </div>

      <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight text-white max-w-4xl leading-tight">
        AI-Nexus
      </h1>
      <p className="mt-3 text-xl sm:text-2xl font-medium text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 via-cyan-400 to-emerald-400">
        Enterprise AI Knowledge Operating System
      </p>

      <p className="mt-6 text-slate-400 max-w-2xl text-base sm:text-lg leading-relaxed">
        Unified intelligence combining Retrieval-Augmented Generation, vector embeddings, multi-agent orchestration, and secure role-based collaboration.
      </p>

      <div className="mt-10 flex flex-wrap items-center justify-center gap-4">
        <Link
          to="/register"
          className="px-6 py-3 rounded-xl font-medium text-white bg-indigo-600 hover:bg-indigo-500 transition-all shadow-lg shadow-indigo-600/30 flex items-center gap-2"
        >
          Create Free Account
        </Link>
        <Link
          to="/dashboard"
          className="px-6 py-3 rounded-xl font-medium text-slate-300 bg-slate-900 border border-slate-700/60 hover:bg-slate-800 hover:text-white transition-all"
        >
          Explore Workspaces
        </Link>
      </div>

      <div className="mt-16 grid grid-cols-1 sm:grid-cols-3 gap-6 max-w-4xl w-full text-left">
        <div className="p-5 rounded-xl bg-slate-900/50 border border-slate-800/80">
          <h3 className="text-indigo-400 font-semibold mb-1">RAG & Vector Search</h3>
          <p className="text-xs text-slate-400">Vectorized document ingestion with chunking and semantic relevance scoring.</p>
        </div>
        <div className="p-5 rounded-xl bg-slate-900/50 border border-slate-800/80">
          <h3 className="text-cyan-400 font-semibold mb-1">Agent Orchestration</h3>
          <p className="text-xs text-slate-400">Multi-agent execution with tools, memory retention, and contextual synthesis.</p>
        </div>
        <div className="p-5 rounded-xl bg-slate-900/50 border border-slate-800/80">
          <h3 className="text-emerald-400 font-semibold mb-1">Enterprise RBAC</h3>
          <p className="text-xs text-slate-400">Stateless JWT security, refresh rotation, and fine-grained permissions.</p>
        </div>
      </div>
    </div>
  );
}
