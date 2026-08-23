import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_BASE = '/api/v1';

export default function AdminWorkspaceManagement() {
  const [workspaces, setWorkspaces] = useState([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [selectedWorkspace, setSelectedWorkspace] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newWsName, setNewWsName] = useState('');
  const [newWsDesc, setNewWsDesc] = useState('');
  const [newWsOwnerId, setNewWsOwnerId] = useState('');

  const [addMemberUserId, setAddMemberUserId] = useState('');
  const [addMemberRole, setAddMemberRole] = useState('EDITOR');

  useEffect(() => {
    fetchWorkspaces();
  }, [page]);

  const fetchWorkspaces = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axios.get(`${API_BASE}/admin/workspaces`, {
        params: { search: search || undefined, page, size: 10 }
      });
      setWorkspaces(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load enterprise workspaces');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    fetchWorkspaces();
  };

  const handleCreateWorkspace = async (e) => {
    e.preventDefault();
    try {
      await axios.post(`${API_BASE}/admin/workspaces`, {
        name: newWsName,
        description: newWsDesc,
        ownerId: Number(newWsOwnerId)
      });
      setShowCreateModal(false);
      setNewWsName('');
      setNewWsDesc('');
      setNewWsOwnerId('');
      fetchWorkspaces();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to create workspace');
    }
  };

  const handleAddMember = async (e) => {
    e.preventDefault();
    if (!selectedWorkspace || !addMemberUserId) return;
    try {
      const res = await axios.post(
        `${API_BASE}/admin/workspaces/${selectedWorkspace.id}/members/${addMemberUserId}`,
        { workspaceId: selectedWorkspace.id, role: addMemberRole }
      );
      setSelectedWorkspace(res.data);
      setWorkspaces(workspaces.map((w) => (w.id === selectedWorkspace.id ? res.data : w)));
      setAddMemberUserId('');
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to add workspace member');
    }
  };

  const handleRemoveMember = async (userId) => {
    if (!selectedWorkspace) return;
    try {
      const res = await axios.delete(
        `${API_BASE}/admin/workspaces/${selectedWorkspace.id}/members/${userId}`
      );
      setSelectedWorkspace(res.data);
      setWorkspaces(workspaces.map((w) => (w.id === selectedWorkspace.id ? res.data : w)));
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to remove member');
    }
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header & Controls */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-slate-800 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Workspace Administration</h1>
          <p className="text-sm text-slate-400">Manage multi-tenant workspaces, member access policies, and quotas</p>
        </div>
        <div className="flex items-center gap-3">
          <form onSubmit={handleSearch} className="flex gap-2">
            <input
              type="text"
              placeholder="Search workspaces..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-sm w-60 focus:outline-none focus:border-indigo-500"
            />
            <button
              type="submit"
              className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded text-sm transition"
            >
              Search
            </button>
          </form>
          <button
            onClick={() => setShowCreateModal(true)}
            className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded text-sm font-semibold transition"
          >
            + Create Workspace
          </button>
        </div>
      </div>

      {error && <div className="p-3 bg-rose-950/40 border border-rose-800 text-rose-300 rounded text-sm">{error}</div>}

      {/* Workspace List Table */}
      <div className="overflow-x-auto bg-slate-900 rounded border border-slate-800">
        <table className="w-full text-left text-sm text-slate-300">
          <thead className="bg-slate-800 text-slate-400 uppercase text-xs">
            <tr>
              <th className="px-4 py-3">ID</th>
              <th className="px-4 py-3">Workspace Name</th>
              <th className="px-4 py-3">Owner</th>
              <th className="px-4 py-3">Members</th>
              <th className="px-4 py-3">Docs</th>
              <th className="px-4 py-3">Workflows</th>
              <th className="px-4 py-3">Created</th>
              <th className="px-4 py-3">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800">
            {loading ? (
              <tr><td colSpan="8" className="text-center py-6 text-slate-500">Loading workspaces...</td></tr>
            ) : workspaces.length === 0 ? (
              <tr><td colSpan="8" className="text-center py-6 text-slate-500">No workspaces found.</td></tr>
            ) : (
              workspaces.map((ws) => (
                <tr key={ws.id} className="hover:bg-slate-800/50">
                  <td className="px-4 py-3 font-mono text-xs text-indigo-400">#{ws.id}</td>
                  <td className="px-4 py-3">
                    <div className="font-semibold text-white">{ws.name}</div>
                    <div className="text-xs text-slate-400 truncate max-w-xs">{ws.description || 'No description'}</div>
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-300">@{ws.ownerUsername}</td>
                  <td className="px-4 py-3 text-xs font-semibold text-slate-200">{ws.memberCount}</td>
                  <td className="px-4 py-3 text-xs text-sky-400">{ws.documentCount}</td>
                  <td className="px-4 py-3 text-xs text-emerald-400">{ws.workflowCount}</td>
                  <td className="px-4 py-3 text-xs text-slate-400">
                    {ws.createdAt ? new Date(ws.createdAt).toLocaleDateString() : '-'}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => setSelectedWorkspace(ws)}
                      className="text-xs text-indigo-400 hover:text-indigo-300 underline"
                    >
                      Configure & Members
                    </button>
                  </td>
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

      {/* Workspace Detail & Members Modal/Drawer */}
      {selectedWorkspace && (
        <div className="bg-slate-900 border border-slate-700 rounded-lg p-5 space-y-4">
          <div className="flex justify-between items-center border-b border-slate-800 pb-3">
            <div>
              <h3 className="text-lg font-bold text-white">{selectedWorkspace.name}</h3>
              <p className="text-xs text-slate-400">Owner: @{selectedWorkspace.ownerUsername} • ID #{selectedWorkspace.id}</p>
            </div>
            <button onClick={() => setSelectedWorkspace(null)} className="text-slate-400 hover:text-white">&times;</button>
          </div>

          <div>
            <h4 className="text-sm font-semibold text-slate-300 uppercase mb-2">Workspace Members ({selectedWorkspace.members?.length || 0})</h4>
            <div className="space-y-2">
              {selectedWorkspace.members?.map((m) => (
                <div key={m.userId} className="flex justify-between items-center bg-slate-800 p-2.5 rounded border border-slate-700 text-xs">
                  <div>
                    <span className="font-semibold text-white">{m.name || m.username}</span>
                    <span className="text-slate-400 ml-2">(@{m.username})</span>
                    <span className="ml-3 text-indigo-400 font-mono">[{m.role}]</span>
                  </div>
                  {m.userId !== selectedWorkspace.ownerId && (
                    <button
                      onClick={() => handleRemoveMember(m.userId)}
                      className="text-rose-400 hover:text-rose-300 text-xs"
                    >
                      Remove
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Add Member Form */}
          <form onSubmit={handleAddMember} className="flex gap-2 pt-2 border-t border-slate-800">
            <input
              type="number"
              placeholder="User ID"
              value={addMemberUserId}
              onChange={(e) => setAddMemberUserId(e.target.value)}
              className="bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1 rounded text-xs w-32 focus:outline-none"
              required
            />
            <select
              value={addMemberRole}
              onChange={(e) => setAddMemberRole(e.target.value)}
              className="bg-slate-800 border border-slate-700 text-slate-200 px-2 py-1 rounded text-xs"
            >
              <option value="VIEWER">VIEWER</option>
              <option value="EDITOR">EDITOR</option>
              <option value="ADMIN">ADMIN</option>
            </select>
            <button
              type="submit"
              className="px-3 py-1 bg-indigo-600 hover:bg-indigo-500 text-white rounded text-xs transition"
            >
              Add Member
            </button>
          </form>
        </div>
      )}

      {/* Create Workspace Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-700 p-6 rounded-lg max-w-md w-full space-y-4">
            <div className="flex justify-between items-center border-b border-slate-800 pb-3">
              <h3 className="text-lg font-bold text-white">Create New Workspace</h3>
              <button onClick={() => setShowCreateModal(false)} className="text-slate-400 hover:text-white">&times;</button>
            </div>
            <form onSubmit={handleCreateWorkspace} className="space-y-3">
              <div>
                <label className="block text-xs text-slate-400 mb-1">Workspace Name</label>
                <input
                  type="text"
                  value={newWsName}
                  onChange={(e) => setNewWsName(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-sm focus:outline-none"
                  required
                />
              </div>
              <div>
                <label className="block text-xs text-slate-400 mb-1">Description</label>
                <textarea
                  value={newWsDesc}
                  onChange={(e) => setNewWsDesc(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-sm focus:outline-none"
                  rows={2}
                />
              </div>
              <div>
                <label className="block text-xs text-slate-400 mb-1">Owner User ID</label>
                <input
                  type="number"
                  value={newWsOwnerId}
                  onChange={(e) => setNewWsOwnerId(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-sm focus:outline-none"
                  required
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded text-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded text-sm font-semibold"
                >
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
