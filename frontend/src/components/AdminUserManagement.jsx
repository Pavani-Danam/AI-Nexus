import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_BASE = '/api/v1';

export default function AdminUserManagement() {
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedUser, setSelectedUser] = useState(null);
  const [newWorkspaceId, setNewWorkspaceId] = useState('');
  const [newWorkspaceRole, setNewWorkspaceRole] = useState('MEMBER');

  useEffect(() => {
    fetchUsers();
  }, [page]);

  const fetchUsers = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axios.get(`${API_BASE}/admin/users`, {
        params: { search: search || undefined, page, size: 10 }
      });
      setUsers(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load enterprise users');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    fetchUsers();
  };

  const handleToggleStatus = async (user) => {
    try {
      const res = await axios.patch(`${API_BASE}/admin/users/${user.id}/status`, {
        enabled: !user.enabled
      });
      setUsers(users.map((u) => (u.id === user.id ? res.data : u)));
      if (selectedUser?.id === user.id) setSelectedUser(res.data);
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update user status');
    }
  };

  const handleRoleChange = async (userId, newRole) => {
    try {
      const res = await axios.patch(`${API_BASE}/admin/users/${userId}/role`, {
        role: newRole
      });
      setUsers(users.map((u) => (u.id === userId ? res.data : u)));
      if (selectedUser?.id === userId) setSelectedUser(res.data);
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update user role');
    }
  };

  const handleAssignWorkspace = async (e) => {
    e.preventDefault();
    if (!selectedUser || !newWorkspaceId) return;

    try {
      const res = await axios.post(`${API_BASE}/admin/users/${selectedUser.id}/workspaces`, {
        workspaceId: Number(newWorkspaceId),
        role: newWorkspaceRole
      });
      setSelectedUser(res.data);
      setUsers(users.map((u) => (u.id === selectedUser.id ? res.data : u)));
      setNewWorkspaceId('');
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to assign workspace membership');
    }
  };

  const handleRemoveWorkspace = async (workspaceId) => {
    if (!selectedUser) return;
    try {
      const res = await axios.delete(`${API_BASE}/admin/users/${selectedUser.id}/workspaces/${workspaceId}`);
      setSelectedUser(res.data);
      setUsers(users.map((u) => (u.id === selectedUser.id ? res.data : u)));
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to remove workspace membership');
    }
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header & Search */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-slate-800 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-white">User & Role Management</h1>
          <p className="text-sm text-slate-400">Control platform accounts, system-level roles, and workspace access</p>
        </div>
        <form onSubmit={handleSearch} className="flex gap-2">
          <input
            type="text"
            placeholder="Search by username, email, or name..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1.5 rounded text-sm w-72 focus:outline-none focus:border-indigo-500"
          />
          <button
            type="submit"
            className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded text-sm transition"
          >
            Search
          </button>
        </form>
      </div>

      {error && <div className="p-3 bg-rose-950/40 border border-rose-800 text-rose-300 rounded text-sm">{error}</div>}

      {/* Users Table */}
      <div className="overflow-x-auto bg-slate-900 rounded border border-slate-800">
        <table className="w-full text-left text-sm text-slate-300">
          <thead className="bg-slate-800 text-slate-400 uppercase text-xs">
            <tr>
              <th className="px-4 py-3">ID</th>
              <th className="px-4 py-3">User</th>
              <th className="px-4 py-3">Email</th>
              <th className="px-4 py-3">System Role</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Workspaces</th>
              <th className="px-4 py-3">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800">
            {loading ? (
              <tr><td colSpan="7" className="text-center py-6 text-slate-500">Loading accounts...</td></tr>
            ) : users.length === 0 ? (
              <tr><td colSpan="7" className="text-center py-6 text-slate-500">No users found matching query.</td></tr>
            ) : (
              users.map((u) => (
                <tr key={u.id} className="hover:bg-slate-800/50">
                  <td className="px-4 py-3 font-mono text-xs text-indigo-400">#{u.id}</td>
                  <td className="px-4 py-3">
                    <div className="font-semibold text-white">{u.name || u.username}</div>
                    <div className="text-xs text-slate-400">@{u.username}</div>
                  </td>
                  <td className="px-4 py-3 text-slate-300">{u.email}</td>
                  <td className="px-4 py-3">
                    <select
                      value={u.role}
                      onChange={(e) => handleRoleChange(u.id, e.target.value)}
                      className="bg-slate-800 text-slate-200 border border-slate-700 text-xs rounded px-2 py-1"
                    >
                      <option value="ROLE_USER">ROLE_USER</option>
                      <option value="ROLE_ADMIN">ROLE_ADMIN</option>
                    </select>
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => handleToggleStatus(u)}
                      className={`px-2 py-0.5 rounded text-xs font-semibold ${
                        u.enabled
                          ? 'bg-emerald-950 text-emerald-400 border border-emerald-800 hover:bg-emerald-900'
                          : 'bg-rose-950 text-rose-400 border border-rose-800 hover:bg-rose-900'
                      }`}
                    >
                      {u.enabled ? 'Active' : 'Disabled'}
                    </button>
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-400">
                    {u.workspaceMemberships?.length || 0} workspaces
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => setSelectedUser(u)}
                      className="text-xs text-indigo-400 hover:text-indigo-300 underline"
                    >
                      Manage Access
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

      {/* Workspace Memberships Drawer */}
      {selectedUser && (
        <div className="bg-slate-900 border border-slate-700 rounded-lg p-5 space-y-4">
          <div className="flex justify-between items-center border-b border-slate-800 pb-3">
            <div>
              <h3 className="text-lg font-bold text-white">Workspace Memberships for {selectedUser.username}</h3>
              <p className="text-xs text-slate-400">Configure tenant workspaces and granular member roles</p>
            </div>
            <button onClick={() => setSelectedUser(null)} className="text-slate-400 hover:text-white">&times;</button>
          </div>

          <div className="space-y-2">
            {selectedUser.workspaceMemberships?.length === 0 ? (
              <div className="text-xs text-slate-500 italic py-2">No workspace memberships assigned.</div>
            ) : (
              selectedUser.workspaceMemberships.map((wm) => (
                <div key={wm.workspaceId} className="flex justify-between items-center bg-slate-800 p-2.5 rounded border border-slate-700 text-xs">
                  <div>
                    <span className="font-semibold text-white">{wm.workspaceName}</span>
                    <span className="ml-2 text-indigo-400">[{wm.memberRole}]</span>
                  </div>
                  <button
                    onClick={() => handleRemoveWorkspace(wm.workspaceId)}
                    className="text-rose-400 hover:text-rose-300"
                  >
                    Remove
                  </button>
                </div>
              ))
            )}
          </div>

          {/* Add Workspace Membership Form */}
          <form onSubmit={handleAssignWorkspace} className="flex gap-2 pt-2 border-t border-slate-800">
            <input
              type="number"
              placeholder="Workspace ID"
              value={newWorkspaceId}
              onChange={(e) => setNewWorkspaceId(e.target.value)}
              className="bg-slate-800 border border-slate-700 text-slate-200 px-3 py-1 rounded text-xs w-36 focus:outline-none"
              required
            />
            <select
              value={newWorkspaceRole}
              onChange={(e) => setNewWorkspaceRole(e.target.value)}
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
              Assign Membership
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
