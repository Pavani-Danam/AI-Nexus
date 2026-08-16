import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { DocumentTable } from '../components/documents/DocumentTable';
import DocumentUploadModal from '../components/documents/DocumentUploadModal';
import SemanticSearchSection from '../components/documents/SemanticSearchSection';
import { documentService } from '../services/documentService';
import api from '../services/api';

export default function DocumentsPage() {
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selectedFilter, setSelectedFilter] = useState('ALL');
  const [documents, setDocuments] = useState([]);
  const [workspaceId, setWorkspaceId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  // Debounce document table filter query
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchQuery);
    }, 300);
    return () => clearTimeout(handler);
  }, [searchQuery]);

  // Fetch active workspace ID
  useEffect(() => {
    const initWorkspace = async () => {
      try {
        const userRes = await api.get('/auth/me').catch(() => null);
        const userId = userRes?.data?.id;
        if (!userId) return;

        const wsRes = await api.get(`/workspaces?ownerId=${userId}`);
        const workspaces = wsRes.data;
        if (workspaces && workspaces.length > 0) {
          setWorkspaceId(workspaces[0].id);
        }
      } catch (err) {
        console.error('Failed to initialize workspace:', err);
      }
    };
    initWorkspace();
  }, []);

  // Fetch documents for document table
  const loadDocuments = useCallback(async () => {
    if (!workspaceId) return;
    try {
      setErrorMessage('');
      const docs = await documentService.getDocuments(workspaceId, debouncedSearch);
      setDocuments(docs);
    } catch (err) {
      console.error('Failed to load documents:', err);
      setErrorMessage('Failed to load documents for this workspace.');
    } finally {
      setLoading(false);
    }
  }, [workspaceId, debouncedSearch]);

  useEffect(() => {
    if (workspaceId) {
      setLoading(true);
      loadDocuments();
    }
  }, [workspaceId, loadDocuments]);

  // Delete handler
  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this document?')) {
      return;
    }
    try {
      await documentService.deleteDocument(id);
      loadDocuments();
    } catch (err) {
      console.error('Failed to delete document:', err);
      alert('Failed to delete document. Please try again.');
    }
  };

  // Status-filtered documents for the table
  const filteredDocuments = useMemo(() => {
    if (selectedFilter === 'ALL') return documents;
    return documents.filter((doc) => doc.status === selectedFilter);
  }, [documents, selectedFilter]);

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Documents</h1>
          <p className="text-sm text-slate-400 mt-1">
            Manage, index, and search through all documentation within your active workspace.
          </p>
        </div>
        <button
          onClick={() => setIsUploadOpen(true)}
          disabled={!workspaceId}
          className="inline-flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium px-4 py-2.5 rounded-xl shadow-lg shadow-indigo-600/30 transition-all text-sm self-start sm:self-auto"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
          </svg>
          Upload Document
        </button>
      </div>

      {/* Semantic Search Section */}
      <SemanticSearchSection workspaceId={workspaceId} />

      {/* Document Library Section */}
      <div className="bg-slate-900/60 backdrop-blur-md border border-slate-800 rounded-2xl p-6 shadow-xl">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
          <h2 className="text-lg font-bold text-white">Document Library</h2>

          {/* Table Filters */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            <div className="relative">
              <input
                type="text"
                placeholder="Filter by filename..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="bg-slate-800/80 border border-slate-700 text-white rounded-xl px-3.5 py-2 pl-9 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/50 w-full sm:w-56 placeholder:text-slate-500"
              />
              <svg className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>

            <div className="flex items-center gap-1 bg-slate-800/80 p-1 rounded-xl border border-slate-700 text-xs">
              {['ALL', 'INDEXED', 'PROCESSING', 'FAILED'].map((status) => (
                <button
                  key={status}
                  onClick={() => setSelectedFilter(status)}
                  className={`px-3 py-1 rounded-lg font-medium transition-all ${
                    selectedFilter === status
                      ? 'bg-indigo-600 text-white shadow'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {status}
                </button>
              ))}
            </div>
          </div>
        </div>

        {errorMessage && (
          <div className="mb-4 p-3 bg-red-500/10 border border-red-500/20 text-red-400 text-sm rounded-xl">
            {errorMessage}
          </div>
        )}

        <DocumentTable
          documents={filteredDocuments}
          loading={loading}
          onDelete={handleDelete}
        />
      </div>

      {/* Upload Modal */}
      {isUploadOpen && (
        <DocumentUploadModal
          isOpen={isUploadOpen}
          workspaceId={workspaceId}
          onClose={() => setIsUploadOpen(false)}
          onUploadSuccess={() => {
            setIsUploadOpen(false);
            loadDocuments();
          }}
        />
      )}
    </div>
  );
}
