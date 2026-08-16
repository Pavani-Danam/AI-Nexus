import React from 'react';

export function DocumentStatusBadge({ status }) {
  const statusStyles = {
    UPLOADING: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20',
    PROCESSING: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    INDEXING: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20',
    INDEXED: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    FAILED: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
  };

  const currentStyle = statusStyles[status?.toUpperCase()] || 'bg-slate-800 text-slate-400 border-slate-700';

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold border ${currentStyle}`}>
      {status || 'UNKNOWN'}
    </span>
  );
}

export function DocumentTable({ documents = [], onOpenUpload }) {
  if (!documents || documents.length === 0) {
    return (
      <div className="p-12 rounded-2xl bg-slate-900/40 border border-slate-800 flex flex-col items-center justify-center text-center">
        <div className="w-12 h-12 rounded-2xl bg-slate-800/80 border border-slate-700/60 flex items-center justify-center text-slate-400 mb-3.5">
          <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 13h6m-3-3v6m5 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        </div>
        <h3 className="text-sm font-semibold text-slate-200">No documents yet</h3>
        <p className="text-xs text-slate-400 max-w-sm mt-1 mb-4">
          Your organization's knowledge base is empty. Upload a PDF, DOCX, or TXT file to get started.
        </p>
        {onOpenUpload && (
          <button
            type="button"
            onClick={onOpenUpload}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-semibold shadow-md shadow-indigo-600/20 transition-all cursor-pointer"
          >
            Upload Document
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-2xl border border-slate-800 bg-slate-900/60">
      <table className="w-full text-left border-collapse">
        <thead>
          <tr className="border-b border-slate-800 text-[11px] font-semibold text-slate-400 uppercase tracking-wider bg-slate-900/80">
            <th className="py-3 px-4">Name</th>
            <th className="py-3 px-4">Type</th>
            <th className="py-3 px-4">Size</th>
            <th className="py-3 px-4">Status</th>
            <th className="py-3 px-4">Uploaded</th>
            <th className="py-3 px-4 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-800/60 text-xs">
          {documents.map((doc) => (
            <tr key={doc.id || doc.name} className="hover:bg-slate-800/40 transition-colors">
              <td className="py-3.5 px-4 font-medium text-slate-200 flex items-center gap-2.5">
                <div className="p-1.5 rounded-lg bg-slate-800 text-slate-400 border border-slate-700">
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                  </svg>
                </div>
                <span className="truncate max-w-xs">{doc.name}</span>
              </td>
              <td className="py-3.5 px-4 text-slate-400 uppercase">{doc.type || 'N/A'}</td>
              <td className="py-3.5 px-4 text-slate-400">{doc.size || 'N/A'}</td>
              <td className="py-3.5 px-4">
                <DocumentStatusBadge status={doc.status} />
              </td>
              <td className="py-3.5 px-4 text-slate-400">{doc.uploadedAt || 'N/A'}</td>
              <td className="py-3.5 px-4 text-right">
                <div className="flex items-center justify-end gap-2 text-slate-400">
                  <button
                    type="button"
                    title="View unavailable until backend integration"
                    className="p-1.5 hover:text-slate-200 hover:bg-slate-800 rounded-lg cursor-not-allowed opacity-50"
                    disabled
                  >
                    View
                  </button>
                  <button
                    type="button"
                    title="Download unavailable until backend integration"
                    className="p-1.5 hover:text-slate-200 hover:bg-slate-800 rounded-lg cursor-not-allowed opacity-50"
                    disabled
                  >
                    Download
                  </button>
                  <button
                    type="button"
                    title="Delete unavailable until backend integration"
                    className="p-1.5 hover:text-rose-400 hover:bg-slate-800 rounded-lg cursor-not-allowed opacity-50"
                    disabled
                  >
                    Delete
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
