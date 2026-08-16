import React, { useState } from 'react';
import CreateWorkspaceModal from '../components/workspaces/CreateWorkspaceModal';
import WorkspaceCard from '../components/workspaces/WorkspaceCard';
import { EmptyStateCard } from '../components/dashboard/DashboardWidgets';

const WorkspaceIcon = ({ className }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
  </svg>
);

export default function WorkspacesPage() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  // Default empty state ready for future API data
  const [workspaces] = useState([]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-100 tracking-tight">Workspaces</h1>
          <p className="text-xs text-slate-400 mt-1">
            Organize your organization's knowledge by department, project, or domain.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setIsModalOpen(true)}
          className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-semibold shadow-md shadow-indigo-600/20 transition-all cursor-pointer"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
          </svg>
          Create Workspace
        </button>
      </div>

      {/* Workspaces List or Empty State */}
      {workspaces.length === 0 ? (
        <EmptyStateCard
          icon={WorkspaceIcon}
          title="No workspaces yet"
          message="Create a workspace to organize documents, conversations, and departmental knowledge assets."
          action={
            <button
              type="button"
              onClick={() => setIsModalOpen(true)}
              className="mt-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-xl text-xs font-semibold transition-all cursor-pointer"
            >
              Create Workspace
            </button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {workspaces.map((ws) => (
            <WorkspaceCard key={ws.id} workspace={ws} />
          ))}
        </div>
      )}

      {/* Modal Dialog */}
      <CreateWorkspaceModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
      />
    </div>
  );
}
