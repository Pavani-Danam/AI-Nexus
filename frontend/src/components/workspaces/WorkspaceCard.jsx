import React from 'react';

export default function WorkspaceCard({ workspace }) {
  if (!workspace) return null;

  return (
    <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-all text-left flex flex-col justify-between">
      <div>
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-slate-100 truncate">{workspace.name}</h3>
          <span className="text-[10px] font-medium px-2 py-0.5 rounded-full bg-slate-800 text-slate-400 border border-slate-700">
            Active
          </span>
        </div>
        <p className="text-xs text-slate-400 mt-1.5 line-clamp-2">
          {workspace.description || 'No description provided.'}
        </p>
      </div>

      <div className="mt-5 pt-3 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-500">
        <span>{workspace.documentCount || 0} documents</span>
        <span>{workspace.memberCount || 1} member</span>
      </div>
    </div>
  );
}
