import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_BASE = '/api/v1';

export default function AdminDashboard() {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchAdminSummary();
  }, []);

  const fetchAdminSummary = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axios.get(`${API_BASE}/admin/dashboard/summary`);
      setSummary(res.data);
    } catch (err) {
      if (err.response?.status === 403) {
        setError('Access Denied: Administrator privileges are required to view this dashboard.');
      } else {
        setError(err.response?.data?.message || 'Failed to load admin summary');
      }
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <div className="text-slate-400 font-medium animate-pulse">Loading enterprise administration metrics...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6 max-w-4xl mx-auto">
        <div className="bg-rose-950/40 border border-rose-800 text-rose-300 p-5 rounded-lg text-center space-y-3">
          <div className="text-lg font-bold">Unauthorized / Error</div>
          <p className="text-sm">{error}</p>
          <button
            onClick={fetchAdminSummary}
            className="px-4 py-1.5 bg-rose-900 hover:bg-rose-800 text-white rounded text-sm transition"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  const formatBytes = (bytes) => {
    if (!bytes) return '0 MB';
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-8">
      {/* Header */}
      <div className="flex justify-between items-center border-b border-slate-800 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Enterprise Admin Dashboard</h1>
          <p className="text-sm text-slate-400">System overview, platform health, and enterprise telemetry</p>
        </div>
        <div className="flex items-center gap-3">
          <span className="flex items-center gap-2 px-3 py-1 bg-emerald-950/80 border border-emerald-800 text-emerald-300 text-xs font-semibold rounded-full">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
            System: {summary?.systemHealthStatus || 'HEALTHY'}
          </span>
          <button
            onClick={fetchAdminSummary}
            className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded text-sm transition"
          >
            Refresh
          </button>
        </div>
      </div>

      {/* Primary KPI Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <div className="bg-slate-800/90 border border-slate-700 p-4 rounded-lg">
          <div className="text-xs text-slate-400 uppercase font-medium">Total Users</div>
          <div className="text-2xl font-bold text-white mt-1">{summary?.totalUsers || 0}</div>
          <div className="text-xs text-emerald-400 mt-1">{summary?.activeUsers || 0} Active</div>
        </div>

        <div className="bg-slate-800/90 border border-slate-700 p-4 rounded-lg">
          <div className="text-xs text-slate-400 uppercase font-medium">Workspaces</div>
          <div className="text-2xl font-bold text-white mt-1">{summary?.totalWorkspaces || 0}</div>
          <div className="text-xs text-slate-400 mt-1">Tenant isolations</div>
        </div>

        <div className="bg-slate-800/90 border border-slate-700 p-4 rounded-lg">
          <div className="text-xs text-slate-400 uppercase font-medium">Knowledge Docs</div>
          <div className="text-2xl font-bold text-white mt-1">{summary?.totalDocuments || 0}</div>
          <div className="text-xs text-slate-400 mt-1">RAG Indexed</div>
        </div>

        <div className="bg-slate-800/90 border border-slate-700 p-4 rounded-lg">
          <div className="text-xs text-slate-400 uppercase font-medium">Workflows</div>
          <div className="text-2xl font-bold text-white mt-1">{summary?.totalWorkflows || 0}</div>
          <div className="text-xs text-slate-400 mt-1">Automations</div>
        </div>

        <div className="bg-slate-800/90 border border-slate-700 p-4 rounded-lg">
          <div className="text-xs text-slate-400 uppercase font-medium">Executions</div>
          <div className="text-2xl font-bold text-white mt-1">{summary?.totalExecutions || 0}</div>
          <div className="text-xs text-emerald-400 mt-1">{summary?.successfulExecutions || 0} Succeeded</div>
        </div>

        <div className="bg-slate-800/90 border border-slate-700 p-4 rounded-lg">
          <div className="text-xs text-slate-400 uppercase font-medium">AI Tokens Used</div>
          <div className="text-2xl font-bold text-indigo-400 mt-1">
            {summary?.totalAiTokensUsed ? (summary.totalAiTokensUsed / 1000).toFixed(1) + 'k' : '0'}
          </div>
          <div className="text-xs text-slate-400 mt-1">Cumulative</div>
        </div>
      </div>

      {/* Execution Health & Runtime Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Execution Status Breakdown */}
        <div className="bg-slate-900 border border-slate-800 rounded-lg p-5">
          <h2 className="text-lg font-semibold text-white mb-4">Workflow Execution Reliability</h2>
          <div className="space-y-4">
            <div>
              <div className="flex justify-between text-sm mb-1">
                <span className="text-slate-300">Successful Executions</span>
                <span className="text-emerald-400 font-semibold">{summary?.successfulExecutions || 0}</span>
              </div>
              <div className="w-full bg-slate-800 rounded-full h-2">
                <div
                  className="bg-emerald-500 h-2 rounded-full"
                  style={{
                    width: `${summary?.totalExecutions > 0 ? (summary.successfulExecutions / summary.totalExecutions) * 100 : 0}%`
                  }}
                ></div>
              </div>
            </div>

            <div>
              <div className="flex justify-between text-sm mb-1">
                <span className="text-slate-300">Failed Executions</span>
                <span className="text-rose-400 font-semibold">{summary?.failedExecutions || 0}</span>
              </div>
              <div className="w-full bg-slate-800 rounded-full h-2">
                <div
                  className="bg-rose-500 h-2 rounded-full"
                  style={{
                    width: `${summary?.totalExecutions > 0 ? (summary.failedExecutions / summary.totalExecutions) * 100 : 0}%`
                  }}
                ></div>
              </div>
            </div>
          </div>
        </div>

        {/* System & JVM Runtime Metrics */}
        <div className="bg-slate-900 border border-slate-800 rounded-lg p-5">
          <h2 className="text-lg font-semibold text-white mb-4">JVM Runtime Telemetry</h2>
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div className="bg-slate-800/60 p-3 rounded border border-slate-700">
              <div className="text-xs text-slate-400">Total Heap Memory</div>
              <div className="text-base font-semibold text-slate-200 mt-0.5">
                {formatBytes(summary?.systemMetrics?.jvmTotalMemoryBytes)}
              </div>
            </div>
            <div className="bg-slate-800/60 p-3 rounded border border-slate-700">
              <div className="text-xs text-slate-400">Free Heap Memory</div>
              <div className="text-base font-semibold text-emerald-400 mt-0.5">
                {formatBytes(summary?.systemMetrics?.jvmFreeMemoryBytes)}
              </div>
            </div>
            <div className="bg-slate-800/60 p-3 rounded border border-slate-700">
              <div className="text-xs text-slate-400">Max Heap Limit</div>
              <div className="text-base font-semibold text-slate-200 mt-0.5">
                {formatBytes(summary?.systemMetrics?.jvmMaxMemoryBytes)}
              </div>
            </div>
            <div className="bg-slate-800/60 p-3 rounded border border-slate-700">
              <div className="text-xs text-slate-400">CPU Processors</div>
              <div className="text-base font-semibold text-sky-400 mt-0.5">
                {summary?.systemMetrics?.availableProcessors || 0} Cores
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
