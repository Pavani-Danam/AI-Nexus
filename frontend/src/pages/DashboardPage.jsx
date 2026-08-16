import React from 'react';
import { useAuth } from '../context/AuthContext';
import { StatCard, QuickActionButton, EmptyStateCard, KnowledgeStatusItem } from '../components/dashboard/DashboardWidgets';

// Simple lightweight SVG icons
const DocIcon = ({ className }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
  </svg>
);

const WorkspaceIcon = ({ className }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
  </svg>
);

const ChatIcon = ({ className }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
  </svg>
);

const DatabaseIcon = ({ className }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
  </svg>
);

const UploadIcon = ({ className }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
  </svg>
);

const ActivityIcon = ({ className }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
  </svg>
);

export default function DashboardPage() {
  const { user } = useAuth();
  const userName = user?.name || user?.fullName || 'User';

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-100 tracking-tight">
          Welcome back, {userName}
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          Manage your organization's knowledge base and interact with AI agents.
        </p>
      </div>

      {/* Quick Statistics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Documents"
          value="0"
          label="Total Documents"
          icon={DocIcon}
          color="indigo"
        />
        <StatCard
          title="Workspaces"
          value="0"
          label="Active Workspaces"
          icon={WorkspaceIcon}
          color="cyan"
        />
        <StatCard
          title="Conversations"
          value="0"
          label="AI Conversations"
          icon={ChatIcon}
          color="purple"
        />
        <StatCard
          title="Knowledge Sources"
          value="0"
          label="Knowledge Sources"
          icon={DatabaseIcon}
          color="emerald"
        />
      </div>

      {/* Quick Actions & Knowledge Overview */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Quick Actions (2 cols on large screen) */}
        <div className="lg:col-span-2 space-y-3">
          <h2 className="text-sm font-semibold text-slate-200">Quick Actions</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <QuickActionButton
              title="Upload Document"
              description="Ingest PDFs, Word files, or Markdown documents"
              to="/documents"
              icon={UploadIcon}
            />
            <QuickActionButton
              title="Open AI Chat"
              description="Ask questions grounded in indexed knowledge"
              to="/chat"
              icon={ChatIcon}
            />
            <QuickActionButton
              title="Create Workspace"
              description="Set up isolated departmental knowledge zones"
              to="/workspaces"
              icon={WorkspaceIcon}
            />
            <QuickActionButton
              title="View Documents"
              description="Browse and audit all uploaded repository assets"
              to="/documents"
              icon={DocIcon}
            />
          </div>
        </div>

        {/* Knowledge Overview (1 col) */}
        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
          <h2 className="text-sm font-semibold text-slate-200">Knowledge Overview</h2>
          <div className="divide-y divide-slate-800/60">
            <KnowledgeStatusItem label="Indexed Documents" count="0" color="emerald" />
            <KnowledgeStatusItem label="Queued for Processing" count="0" color="amber" />
            <KnowledgeStatusItem label="Failed Ingestions" count="0" color="rose" />
            <KnowledgeStatusItem label="Vector Embeddings" count="0" color="indigo" />
          </div>
        </div>
      </div>

      {/* Recent Activity Section */}
      <div className="space-y-3">
        <h2 className="text-sm font-semibold text-slate-200">Recent Activity</h2>
        <EmptyStateCard
          icon={ActivityIcon}
          title="No recent activity"
          message="Your document uploads, ingestion statuses, and AI chat sessions will appear here once active."
        />
      </div>
    </div>
  );
}
