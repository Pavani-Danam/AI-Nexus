import React, { useState, useMemo } from 'react';
import { DocumentTable } from '../components/documents/DocumentTable';
import DocumentUploadModal from '../components/documents/DocumentUploadModal';

export default function DocumentsPage() {
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFilter, setSelectedFilter] = useState('ALL');

  // Prepared for API data integration in Step 23
  const [documents] = useState([]);

  // Client-side search and filtering logic
  const filteredDocuments = useMemo(() => {
    return documents.filter((doc) => {
      const matchesSearch = doc.name?.toLowerCase().includes(searchQuery.toLowerCase());
      const matchesFilter = selectedFilter === 'ALL' || doc.type?.toUpperCase() === selectedFilter;
      return matchesSearch && matchesFilter;
    });
  }, [documents, searchQuery, selectedFilter]);

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
              className={`px-3 py-1 rounded-lg text-xs font-medium transition-all ${
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
        onOpenUpload={() => setIsUploadOpen(true)}
      />

      {/* Upload Modal */}
      <DocumentUploadModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
      />
    </div>
  );
}
