import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_BASE = '/api/v1';

export default function AdminQuotaManagement() {
  const [quotas, setQuotas] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedQuota, setSelectedQuota] = useState(null);

  // Edit limits state
  const [editMaxAi, setEditMaxAi] = useState('');
  const [editMaxTokens, setEditMaxTokens] = useState('');
  const [editMaxDocs, setEditMaxDocs] = useState('');
  const [editMaxEmbeddings, setEditMaxEmbeddings] = useState('');
  const [editMaxVectors, setEditMaxVectors] = useState('');
  const [editMaxWorkflows, setEditMaxWorkflows] = useState('');
  const [editMaxAgents, setEditMaxAgents] = useState('');

  useEffect(() => {
    fetchQuotas();
  }, []);

  const fetchQuotas = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axios.get(`${API_BASE}/admin/quotas`);
      setQuotas(res.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch resource quotas');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenEdit = (q) => {
    setSelectedQuota(q);
    setEditMaxAi(q.maxAiRequests);
    setEditMaxTokens(q.maxTokens);
    setEditMaxDocs(q.maxDocumentProcessing);
    setEditMaxEmbeddings(q.maxEmbeddings);
    setEditMaxVectors(q.maxVectorOperations);
    setEditMaxWorkflows(q.maxWorkflowExecutions);
    setEditMaxAgents(q.maxAgentExecutions);
  };

  const handleSaveLimits = async (e) => {
    e.preventDefault();
    if (!selectedQuota) return;

    try {
      const res = await axios.put(`${API_BASE}/admin/quotas/${selectedQuota.workspaceId}`, {
        maxAiRequests: Number(editMaxAi),
        maxTokens: Number(editMaxTokens),
        maxDocumentProcessing: Number(editMaxDocs),
        maxEmbeddings: Number(editMaxEmbeddings),
        maxVectorOperations: Number(editMaxVectors),
        maxWorkflowExecutions: Number(editMaxWorkflows),
        maxAgentExecutions: Number(editMaxAgents)
      });
      setQuotas(quotas.map((q) => (q.workspaceId === selectedQuota.workspaceId ? res.data : q)));
      setSelectedQuota(null);
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update quota limits');
    }
  };

  const handleResetUsage = async (workspaceId) => {
    if (!window.confirm('Are you sure you want to reset all tracked usage counters for this workspace?')) return;

    try {
      const res = await axios.post(`${API_BASE}/admin/quotas/${workspaceId}/reset`);
      setQuotas(quotas.map((q) => (q.workspaceId === workspaceId ? res.data : q)));
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to reset usage');
    }
  };

  const renderProgressBar = (used, max, label) => {
    const percent = Math.min(100, Math.round((used / (max || 1)) * 100));
    let color = 'bg-indigo-500';
    if (percent >= 90) color = 'bg-rose-500';
    else if (percent >= 75) color = 'bg-amber-500';

    return (
      <div className="space-y-1">
        <div className="flex justify-between text-[11px] text-slate-400">
          <span>{label}</span>
          <span>
            {used.toLocaleString()} / {max.toLocaleString()} ({percent}%)
          </span>
        </div>
        <div className="w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
          <div className={`${color} h-1.5 rounded-full transition-all duration-300`} style={{ width: `${percent}%` }}></div>
        </div>
      </div>
    );
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center border-b border-slate-800 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Usage & Quota Management</h1>
          <p className="text-sm text-slate-400">Monitor multi-tenant consumption, enforce limits, and prevent resource exhaustion</p>
        </div>
        <button
          onClick={fetchQuotas}
          className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm rounded transition"
        >
          ? Refresh
        </button>
      </div>

      {error && <div className="p-3 bg-rose-950/40 border border-rose-800 text-rose-300 rounded text-sm">{error}</div>}

      {/* Quotas Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {loading ? (
          <div className="col-span-2 text-center py-10 text-slate-500">Loading quota telemetry...</div>
        ) : quotas.length === 0 ? (
          <div className="col-span-2 text-center py-10 text-slate-500">No workspace quota records found.</div>
        ) : (
          quotas.map((q) => (
            <div key={q.workspaceId} className="bg-slate-900 border border-slate-800 rounded-lg p-5 space-y-4">
              <div className="flex justify-between items-start border-b border-slate-800 pb-3">
                <div>
                  <h3 className="font-bold text-white text-base">{q.workspaceName}</h3>
                  <p className="text-xs text-indigo-400 font-mono">Workspace #{q.workspaceId}</p>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => handleOpenEdit(q)}
                    className="px-2.5 py-1 bg-indigo-600/30 hover:bg-indigo-600/50 text-indigo-300 border border-indigo-500/40 rounded text-xs transition"
                  >
                    Adjust Limits
                  </button>
                  <button
                    onClick={() => handleResetUsage(q.workspaceId)}
                    className="px-2.5 py-1 bg-rose-950/40 hover:bg-rose-900/50 text-rose-300 border border-rose-800/40 rounded text-xs transition"
                  >
                    Reset Usage
                  </button>
                </div>
              </div>

              {/* Resource Bars */}
              <div className="space-y-3">
                {renderProgressBar(q.usedAiRequests, q.maxAiRequests, 'AI Inferences & Chat')}
                {renderProgressBar(q.usedTokens, q.maxTokens, 'Tokens Consumed')}
                {renderProgressBar(q.usedDocumentProcessing, q.maxDocumentProcessing, 'Document Ingestions')}
                {renderProgressBar(q.usedEmbeddings, q.maxEmbeddings, 'Embedding Generations')}
                {renderProgressBar(q.usedVectorOperations, q.maxVectorOperations, 'Vector DB Operations')}
                {renderProgressBar(q.usedWorkflowExecutions, q.maxWorkflowExecutions, 'Workflow Executions')}
                {renderProgressBar(q.usedAgentExecutions, q.maxAgentExecutions, 'Autonomous Agent Runs')}
              </div>
            </div>
          ))
        )}
      </div>

      {/* Edit Limits Modal */}
      {selectedQuota && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-700 p-6 rounded-lg max-w-lg w-full space-y-4">
            <div className="flex justify-between items-center border-b border-slate-800 pb-3">
              <h3 className="text-lg font-bold text-white">Adjust Quota Limits: {selectedQuota.workspaceName}</h3>
              <button onClick={() => setSelectedQuota(null)} className="text-slate-400 hover:text-white">&times;</button>
            </div>
            <form onSubmit={handleSaveLimits} className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Max AI Requests</label>
                  <input
                    type="number"
                    value={editMaxAi}
                    onChange={(e) => setEditMaxAi(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-xs focus:outline-none"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Max Tokens</label>
                  <input
                    type="number"
                    value={editMaxTokens}
                    onChange={(e) => setEditMaxTokens(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-xs focus:outline-none"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Max Document Ingestion</label>
                  <input
                    type="number"
                    value={editMaxDocs}
                    onChange={(e) => setEditMaxDocs(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-xs focus:outline-none"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Max Embeddings</label>
                  <input
                    type="number"
                    value={editMaxEmbeddings}
                    onChange={(e) => setEditMaxEmbeddings(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-xs focus:outline-none"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Max Vector Operations</label>
                  <input
                    type="number"
                    value={editMaxVectors}
                    onChange={(e) => setEditMaxVectors(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-xs focus:outline-none"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Max Workflow Runs</label>
                  <input
                    type="number"
                    value={editMaxWorkflows}
                    onChange={(e) => setEditMaxWorkflows(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-xs focus:outline-none"
                    required
                  />
                </div>
                <div className="col-span-2">
                  <label className="block text-xs text-slate-400 mb-1">Max Agent Executions</label>
                  <input
                    type="number"
                    value={editMaxAgents}
                    onChange={(e) => setEditMaxAgents(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-xs focus:outline-none"
                    required
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setSelectedQuota(null)}
                  className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded text-xs font-semibold"
                >
                  Save Limits
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
