import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_BASE = '/api/v1';

const ACTION_TYPES = [
  'ALL',
  'LOGIN_SUCCESS',
  'LOGIN_FAILED',
  'LOGOUT',
  'AUTHORIZATION_DENIED',
  'DOCUMENT_UPLOAD',
  'DOCUMENT_DELETE',
  'DOCUMENT_ACCESS',
  'WORKSPACE_CREATED',
  'WORKSPACE_UPDATED',
  'WORKSPACE_MEMBER_ADDED',
  'WORKSPACE_MEMBER_REMOVED',
  'USER_CREATED',
  'USER_UPDATED',
  'ROLE_CHANGED',
  'STATUS_CHANGED',
  'WORKFLOW_EXECUTED',
  'WORKFLOW_APPROVAL',
  'SCHEDULE_TRIGGERED',
  'AI_INFERENCE_REQUESTED',
  'AI_QUOTA_EXCEEDED',
  'ADMIN_CONFIG_CHANGED',
  'SECURITY_ALERT'
];

export default function EnterpriseAuditLogs() {
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Filters
  const [selectedAction, setSelectedAction] = useState('ALL');
  const [actorUsername, setActorUsername] = useState('');
  const [workspaceId, setWorkspaceId] = useState('');
  const [selectedDetailLog, setSelectedDetailLog] = useState(null);

  useEffect(() => {
    fetchLogs();
  }, [page]);

  const fetchLogs = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axios.get(`${API_BASE}/audit-logs`, {
        params: {
          actionType: selectedAction !== 'ALL' ? selectedAction : undefined,
          actorUsername: actorUsername || undefined,
          workspaceId: workspaceId || undefined,
          page,
          size: 15
        }
      });
      setLogs(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch audit log telemetry');
    } finally {
      setLoading(false);
    }
  };

  const handleFilterSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchLogs();
  };

  const getResultBadge = (result) => {
    switch (result) {
      case 'SUCCESS':
        return <span className="px-2 py-0.5 rounded text-[10px] bg-emerald-950/60 text-emerald-300 border border-emerald-800">SUCCESS</span>;
      case 'DENIED':
        return <span className="px-2 py-0.5 rounded text-[10px] bg-amber-950/60 text-amber-300 border border-amber-800">DENIED</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[10px] bg-rose-950/60 text-rose-300 border border-rose-800">{result}</span>;
    }
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center border-b border-slate-800 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Enterprise Audit & Compliance</h1>
          <p className="text-sm text-slate-400">Immutable record of security events, administrative changes, and user operations</p>
        </div>
        <button
          onClick={fetchLogs}
          className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm rounded transition"
        >
          ? Refresh
        </button>
      </div>

      {/* Filter Bar */}
      <form onSubmit={handleFilterSubmit} className="bg-slate-900 border border-slate-800 p-4 rounded-lg flex flex-wrap gap-3 items-center">
        <select
          value={selectedAction}
          onChange={(e) => setSelectedAction(e.target.value)}
          className="bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-xs"
        >
          {ACTION_TYPES.map((act) => (
            <option key={act} value={act}>{act}</option>
          ))}
        </select>

        <input
          type="text"
          placeholder="Actor username..."
          value={actorUsername}
          onChange={(e) => setActorUsername(e.target.value)}
          className="bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-xs w-36 focus:outline-none"
        />

        <input
          type="number"
          placeholder="Workspace ID"
          value={workspaceId}
          onChange={(e) => setWorkspaceId(e.target.value)}
          className="bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-xs w-32 focus:outline-none"
        />

        <button
          type="submit"
          className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded text-xs font-semibold transition"
        >
          Filter Logs
        </button>
      </form>

      {error && <div className="p-3 bg-rose-950/40 border border-rose-800 text-rose-300 rounded text-sm">{error}</div>}

      {/* Audit Log Table */}
      <div className="overflow-x-auto bg-slate-900 rounded border border-slate-800">
        <table className="w-full text-left text-sm text-slate-300">
          <thead className="bg-slate-800 text-slate-400 uppercase text-xs">
            <tr>
              <th className="px-4 py-3">Timestamp</th>
              <th className="px-4 py-3">Action Type</th>
              <th className="px-4 py-3">Actor</th>
              <th className="px-4 py-3">Workspace</th>
              <th className="px-4 py-3">Resource</th>
              <th className="px-4 py-3">Result</th>
              <th className="px-4 py-3">Safe Details</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800 text-xs font-mono">
            {loading ? (
              <tr><td colSpan="7" className="text-center py-6 text-slate-500 font-sans">Loading audit trail...</td></tr>
            ) : logs.length === 0 ? (
              <tr><td colSpan="7" className="text-center py-6 text-slate-500 font-sans">No audit events match criteria.</td></tr>
            ) : (
              logs.map((log) => (
                <tr
                  key={log.id}
                  onClick={() => setSelectedDetailLog(log)}
                  className="hover:bg-slate-800/60 cursor-pointer transition"
                >
                  <td className="px-4 py-2.5 text-slate-400 whitespace-nowrap">
                    {new Date(log.createdAt).toLocaleString()}
                  </td>
                  <td className="px-4 py-2.5 font-semibold text-indigo-400">{log.actionType}</td>
                  <td className="px-4 py-2.5 text-slate-200">@{log.actorUsername}</td>
                  <td className="px-4 py-2.5 text-slate-400">{log.workspaceId ? `#${log.workspaceId}` : 'GLOBAL'}</td>
                  <td className="px-4 py-2.5 text-slate-300">{log.resourceType ? `${log.resourceType}:${log.resourceId || ''}` : '-'}</td>
                  <td className="px-4 py-2.5">{getResultBadge(log.result)}</td>
                  <td className="px-4 py-2.5 text-slate-400 max-w-xs truncate font-sans">{log.safeDetails}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-between items-center text-sm text-slate-400">
          <div>Page {page + 1} of {totalPages}</div>
          <div className="flex gap-2">
            <button
              disabled={page === 0}
              onClick={() => setPage(p => p - 1)}
              className="px-3 py-1 bg-slate-800 hover:bg-slate-700 disabled:opacity-40 rounded text-xs"
            >
              Previous
            </button>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage(p => p + 1)}
              className="px-3 py-1 bg-slate-800 hover:bg-slate-700 disabled:opacity-40 rounded text-xs"
            >
              Next
            </button>
          </div>
        </div>
      )}

      {/* Audit Detail Modal */}
      {selectedDetailLog && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-700 p-6 rounded-lg max-w-lg w-full space-y-4 font-sans">
            <div className="flex justify-between items-center border-b border-slate-800 pb-3">
              <h3 className="text-lg font-bold text-white">Audit Event #{selectedDetailLog.id}</h3>
              <button onClick={() => setSelectedDetailLog(null)} className="text-slate-400 hover:text-white">&times;</button>
            </div>
            <div className="space-y-2 text-xs">
              <div><span className="text-slate-400">Action:</span> <span className="text-indigo-400 font-mono font-bold">{selectedDetailLog.actionType}</span></div>
              <div><span className="text-slate-400">Actor:</span> <span className="text-white font-mono">@{selectedDetailLog.actorUsername}</span></div>
              <div><span className="text-slate-400">Workspace ID:</span> <span className="text-white font-mono">{selectedDetailLog.workspaceId || 'SYSTEM'}</span></div>
              <div><span className="text-slate-400">Resource:</span> <span className="text-white font-mono">{selectedDetailLog.resourceType} ({selectedDetailLog.resourceId})</span></div>
              <div><span className="text-slate-400">Result:</span> {getResultBadge(selectedDetailLog.result)}</div>
              <div><span className="text-slate-400">Timestamp:</span> <span className="text-white">{new Date(selectedDetailLog.createdAt).toLocaleString()}</span></div>
              <div className="pt-2">
                <label className="block text-slate-400 mb-1">Sanitized Event Details:</label>
                <div className="bg-slate-950 p-3 rounded border border-slate-800 text-slate-300 font-mono text-[11px] whitespace-pre-wrap">
                  {selectedDetailLog.safeDetails}
                </div>
              </div>
            </div>
            <div className="flex justify-end pt-2">
              <button
                onClick={() => setSelectedDetailLog(null)}
                className="px-4 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs rounded transition"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
