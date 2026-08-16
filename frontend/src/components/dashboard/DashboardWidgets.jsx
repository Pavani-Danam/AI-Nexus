import React from 'react';
import { Link } from 'react-router-dom';

export function StatCard({ title, value, label, icon: Icon, color = 'indigo' }) {
  const colorMap = {
    indigo: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20',
    cyan: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20',
    purple: 'bg-purple-500/10 text-purple-400 border-purple-500/20',
    emerald: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  };

  return (
    <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700/80 transition-all flex flex-col justify-between">
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold text-slate-400">{title}</span>
        {Icon && (
          <div className={`p-2 rounded-xl border ${colorMap[color] || colorMap.indigo}`}>
            <Icon className="w-4 h-4" />
          </div>
        )}
      </div>
      <div className="mt-4">
        <div className="text-2xl font-bold text-slate-100">{value}</div>
        <p className="text-xs text-slate-500 mt-0.5">{label}</p>
      </div>
    </div>
  );
}

export function QuickActionButton({ title, description, to, icon: Icon, onClick }) {
  const content = (
    <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 hover:bg-slate-800/60 hover:border-indigo-500/30 transition-all flex items-start gap-3.5 group text-left w-full cursor-pointer">
      {Icon && (
        <div className="p-2.5 rounded-lg bg-slate-800 border border-slate-700 text-slate-300 group-hover:text-indigo-400 group-hover:border-indigo-500/30 transition-all">
          <Icon className="w-4 h-4" />
        </div>
      )}
      <div className="flex-1">
        <div className="text-xs font-semibold text-slate-200 group-hover:text-white transition-colors">
          {title}
        </div>
        <p className="text-[11px] text-slate-400 mt-0.5">{description}</p>
      </div>
    </div>
  );

  if (to) {
    return <Link to={to} className="block">{content}</Link>;
  }

  return (
    <button type="button" onClick={onClick} className="block w-full">
      {content}
    </button>
  );
}

export function EmptyStateCard({ title, message, icon: Icon, action }) {
  return (
    <div className="p-8 rounded-2xl bg-slate-900/40 border border-slate-800/80 flex flex-col items-center justify-center text-center">
      {Icon && (
        <div className="w-10 h-10 rounded-xl bg-slate-800/80 border border-slate-700/60 flex items-center justify-center text-slate-400 mb-3">
          <Icon className="w-5 h-5" />
        </div>
      )}
      <h4 className="text-sm font-semibold text-slate-200">{title}</h4>
      <p className="text-xs text-slate-400 max-w-sm mt-1 mb-4">{message}</p>
      {action}
    </div>
  );
}

export function KnowledgeStatusItem({ label, count, color = 'emerald' }) {
  const colorMap = {
    emerald: 'bg-emerald-400',
    amber: 'bg-amber-400',
    rose: 'bg-rose-400',
    indigo: 'bg-indigo-400',
  };

  return (
    <div className="flex items-center justify-between py-2 border-b border-slate-800/60 last:border-0 text-xs">
      <div className="flex items-center gap-2">
        <span className={`w-2 h-2 rounded-full ${colorMap[color] || colorMap.indigo}`} />
        <span className="text-slate-300">{label}</span>
      </div>
      <span className="font-semibold text-slate-200">{count}</span>
    </div>
  );
}
