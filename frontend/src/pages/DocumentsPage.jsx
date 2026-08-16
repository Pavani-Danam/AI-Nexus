import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { DocumentTable } from '../components/documents/DocumentTable';
import DocumentUploadModal from '../components/documents/DocumentUploadModal';
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

  // Debounce search query
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

  // Fetch documents using backend search
  const loadDocuments = useCallback(async () => {
    if (!workspaceId) return;
    try {
      setErrorMessage('');
      const docs = await documentService.getDocuments(workspaceId, debouncedSearch);
      setDocuments(docs);
    } catch (err) {
      console.error('Failed to load documents:', err);
      setErrorMessage('Unable to retrieve documents. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [workspaceId, debouncedSearch]);

  useEffect(() => {
    if (workspaceId) {
      loadDocuments();
    }
  }, [workspaceId, debouncedSearch, loadDocuments]);

  // Polling for processing status on active documents
  useEffect(() => {
    const hasUnfinished = documents.some(
      (doc) => doc.status === 'UPLOADED' || doc.status === 'PROCESSING'
    );
    if (!hasUnfinished || !workspaceId) return;

    const interval = setInterval(async () => {
      try {
        const docs = await documentService.getDocuments(workspaceId, debouncedSearch);
        setDocuments(docs);
      } catch (err) {
        console.error('Polling error:', err);
      }
    }, 3000);

    return () => clearInterval(interval);
  }, [documents, workspaceId, debouncedSearch]);

  // Handle document deletion
  const handleDeleteDocument = async (docId) => {
    try {
      setErrorMessage('');
      await documentService.deleteDocument(docId);
      setDocuments((prev) => prev.filter((d) => d.id !== docId));
    } catch (err) {
      console.error('Delete failed:', err);
      setErrorMessage('Failed to delete the document. Please try again.');
    }
  };

  // Extension/type filter
  const filteredDocuments = useMemo(() => {
    return documents.filter((doc) => {
      const type = (doc.fileType || doc.fileName || '').toUpperCase();
      if (selectedFilter === 'ALL') return true;
      if (selectedFilter === 'PDF') return type.includes('PDF');
      if (selectedFilter === 'DOCX') return type.includes('WORD') || type.includes('DOCX');
      if (selectedFilter === 'TXT') return type.includes('PLAIN') || type.includes('TXT');
      return true;
    });
  }, [documents, selectedFilter]);

  const filterOptions = ['ALL', 'PDF', 'DOCX', 'TXT'];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-100 tracking-tight">Documents</h1>
          <p className="text-xs text-slate-400 mt-1">
            Manage the knowledge sources used by your organization.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setIsUploadOpen(true)}
          className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-semibold shadow-md shadow-indigo-600/20 transition-all cursor-pointer"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
          </svg>
          Upload Document
        </button>
      </div>

      {/* Error Alert */}
      {errorMessage && (
        <div className="p-3 bg-rose-500/15 border border-rose-500/30 rounded-xl text-xs text-rose-300">
          {errorMessage}
        </div>
      )}

      {/* Search & Filter Controls */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        {/* Search Bar */}
        <div className="relative flex-1 max-w-md">
          <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </span>
          <input
            type="text"
            placeholder="Search documents by name..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-slate-900/90 border border-slate-800 rounded-xl text-xs text-slate-200 placeholder-slate-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-500 transition-all"
          />
        </div>

        {/* Filter Pills */}
        <div className="flex items-center gap-1 bg-slate-900/90 p-1 border border-slate-800 rounded-xl">
          {filterOptions.map((opt) => (
            <button
              key={opt}
              type="button"
              onClick={() => setSelectedFilter(opt)}
              className={`px-3 py-1 rounded-lg text-xs font-medium transition-all cursor-pointer ${
                selectedFilter === opt
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`}
            >
              {opt}
            </button>
          ))}
        </div>
      </div>

      {/* Document List / Empty State */}
      <DocumentTable
        documents={filteredDocuments}
        loading={loading}
        onOpenUpload={() => setIsUploadOpen(true)}
        onDeleteDocument={handleDeleteDocument}
      />

      {/* Upload Modal */}
      <DocumentUploadModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        workspaceId={workspaceId}
        onUploadSuccess={loadDocuments}
      />
    </div>
  );
}
