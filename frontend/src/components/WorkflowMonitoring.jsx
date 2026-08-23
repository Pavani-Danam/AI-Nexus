import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_BASE = '/api/v1';

export default function WorkflowMonitoring({ workspaceId }) {
  const [summary, setSummary] = useState(null);
  const [executions, setExecutions] = useState([]);
  const [selectedExecution, setSelectedExecution] = useState(null);
  const [auditTrail, setAuditTrail] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (workspaceId) {
      fetchSummary();
      fetchExecutions();
    }
  }, [workspaceId, statusFilter, page]);

  const fetchSummary = async () => {
    try {
      const res = await axios.get(`${API_BASE}/workspaces/${workspaceId}/workflow-monitoring/summary`);
      setSummary(res.data);
    } catch (err) {
      console.error('Failed to load monitoring summary', err);
    }
  };

  const fetchExecutions = async () => {
    setLoading(true);
    setError(null);
    try {
      const params = { page, size: 10 };
      if (statusFilter) params.status = statusFilter;
      const res = await axios.get(`${API_BASE}/workspaces/${workspaceId}/workflow-executions`, { params });
      setExecutions(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load executions');
    } finally {
      setLoading(false);
    }
  };

  const viewDetails = async (exec) => {
    setSelectedExecution(exec);
    try {
      const auditRes = await axios.get(`${API_BASE}/workflow-executions/${exec.id}/audit-trail`);
      setAuditTrail(auditRes.data || []);
    } catch (err) {
      console.error('Failed to fetch audit trail', err);
    }
  };

  const handleRecover = async (executionId) => {
    try {
      await axios.post(`${API_BASE}/workflow-executions/${executionId}/recover`);
      fetchSummary();
      fetchExecutions();
      setSelectedExecution(null);
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to trigger recovery');
    }
  };

  return (
    <div className="space-y-6 p-4">
      {/* Metric Cards */}
      {summary && (
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
          <div className="bg-slate-800 p-4 rounded-lg border border-slate-700">
            <div className="text-sm text-slate-400">Total Executions</div>
            <div className="text-2xl font-bold text-white">{summary.totalExecutions}</div>
          </div>
          <div className="bg-slate-800 p-4 rounded-lg border border-slate-700">
            <div className="text-sm text-emerald-400">Successful</div>
            <div className="text-2xl font-bold text-emerald-400">{summary.successfulExecutions}</div>
          </div>
          <div className="bg-slate-800 p-4 rounded-lg border border-slate-700">
            <div className="text-sm text-rose-400">Failed</div>
            <div className="text-2xl font-bold text-rose-400">{summary.failedExecutions}</div>
          </div>
          <div className="bg-slate-800 p-4 rounded-lg border border-slate-700">
            <div className="text-sm text-amber-400">Pending Approval</div>
            <div className="text-2xl font-bold text-amber-400">{summary.pendingApprovals}</div>
          </div>
          <div className="bg-slate-800 p-4 rounded-lg border border-slate-700">
            <div className="text-sm text-sky-400">Avg Duration</div>
            <div className="text-2xl font-bold text-sky-400">{(summary.avgDurationMs / 1000).toFixed(2)}s</div>
          </div>
        </div>
      )}

      {/* Filter Bar */}
      <div className="flex justify-between items-center bg-slate-900 p-3 rounded border border-slate-800">
        <div className="text-lg font-semibold text-white">Execution History</div>
        <div className="flex items-center gap-3">
          <select
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
            className="bg-slate-800 text-slate-200 border border-slate-700 rounded px-3 py-1.5 text-sm"
          >
            <option value="">All Statuses</option>
            <option value="COMPLETED">Completed</option>
            <option value="FAILED">Failed</option>
            <option value="RUNNING">Running</option>
            <option value="PENDING">Pending</option>
            <option value="WAITING_FOR_APPROVAL">Waiting Approval</option>
          </select>
          <button
            onClick={() => { fetchSummary(); fetchExecutions(); }}
            className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded text-sm transition"
          >
            Refresh
          </button>
        </div>
      </div>

      {error && <div className="text-rose-400 text-sm">{error}</div>}

      {/* Executions Table */}
      <div className="overflow-x-auto bg-slate-900 rounded border border-slate-800">
        <table className="w-full text-left text-sm text-slate-300">
          <thead className="bg-slate-800 text-slate-400 uppercase text-xs">
            <tr>
              <th className="px-4 py-3">Execution ID</th>
              <th className="px-4 py-3">Workflow</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Duration</th>
              <th className="px-4 py-3">Start Time</th>
              <th className="px-4 py-3">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800">
            {loading ? (
              <tr><td colSpan="6" className="text-center py-6 text-slate-500">Loading executions...</td></tr>
            ) : executions.length === 0 ? (
              <tr><td colSpan="6" className="text-center py-6 text-slate-500">No workflow executions found.</td></tr>
            ) : (
              executions.map((exec) => (
                <tr key={exec.id} className="hover:bg-slate-800/50">
                  <td className="px-4 py-3 font-mono text-xs text-indigo-400">#{exec.id}</td>
                  <td className="px-4 py-3 text-white font-medium">{exec.workflowName || `Workflow #${exec.workflowId}`}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded text-xs font-semibold ${
                      exec.status === 'COMPLETED' ? 'bg-emerald-950 text-emerald-400 border border-emerald-800' :
                      exec.status === 'FAILED' ? 'bg-rose-950 text-rose-400 border border-rose-800' :
                      exec.status === 'RUNNING' ? 'bg-sky-950 text-sky-400 border border-sky-800' :
                      'bg-amber-950 text-amber-400 border border-amber-800'
                    }`}>
                      {exec.status}
                    </span>
                  </td>
                  <td className="px-4 py-3">{exec.durationMs ? `${(exec.durationMs / 1000).toFixed(2)}s` : '-'}</td>
                  <td className="px-4 py-3 text-xs text-slate-400">{exec.startTime ? new Date(exec.startTime).toLocaleString() : '-'}</td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => viewDetails(exec)}
                      className="text-xs text-indigo-400 hover:text-indigo-300 underline mr-3"
                    >
                      Details
                    </button>
                    {exec.status === 'FAILED' && (
                      <button
                        onClick={() => handleRecover(exec.id)}
                        className="text-xs bg-rose-900/60 hover:bg-rose-800 text-rose-200 px-2 py-0.5 rounded"
                      >
                        Recover
                      </button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="flex justify-between items-center text-sm text-slate-400">
          <div>Page {page + 1} of {totalPages}</div>
          <div className="flex gap-2">
            <button
              disabled={page === 0}
              onClick={() => setPage(p => p - 1)}
              className="px-3 py-1 bg-slate-800 hover:bg-slate-700 disabled:opacity-40 rounded"
            >
              Previous
            </button>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage(p => p + 1)}
              className="px-3 py-1 bg-slate-800 hover:bg-slate-700 disabled:opacity-40 rounded"
            >
              Next
            </button>
          </div>
        </div>
      )}

      {/* Detail Drawer */}
      {selectedExecution && (
        <div className="bg-slate-900 border border-slate-700 rounded-lg p-5 space-y-4">
          <div className="flex justify-between items-center border-b border-slate-800 pb-3">
            <h3 className="text-lg font-bold text-white">Execution #{selectedExecution.id} Details</h3>
            <button onClick={() => setSelectedExecution(null)} className="text-slate-400 hover:text-white">&times;</button>
          </div>

          <div className="grid grid-cols-2 gap-4 text-sm">
            <div><span className="text-slate-400">Status:</span> <span className="text-white font-medium">{selectedExecution.status}</span></div>
            <div><span className="text-slate-400">Duration:</span> <span className="text-white">{selectedExecution.durationMs ? `${selectedExecution.durationMs}ms` : '-'}</span></div>
            {selectedExecution.errorMessage && (
              <div className="col-span-2 bg-rose-950/40 border border-rose-800 text-rose-300 p-3 rounded">
                <strong>Error:</strong> {selectedExecution.errorMessage}
              </div>
            )}
          </div>

          {/* Audit Trail */}
          <div className="mt-4">
            <h4 className="text-sm font-semibold text-slate-300 uppercase mb-2">Audit Trail</h4>
            <div className="space-y-2">
              {auditTrail.map((ev) => (
                <div key={ev.id} className="text-xs bg-slate-800/80 p-2.5 rounded border border-slate-700 flex justify-between">
                  <div>
                    <span className="font-semibold text-indigo-400 mr-2">[{ev.eventType}]</span>
                    <span className="text-slate-300">{ev.description}</span>
                  </div>
                  <span className="text-slate-500">{new Date(ev.timestamp).toLocaleTimeString()}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
