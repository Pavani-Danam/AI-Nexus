import React, { useState, useRef } from 'react';
import { FormButton } from '../ui/FormControls';

const ALLOWED_EXTENSIONS = ['.pdf', '.docx', '.txt'];
const MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024; // 25 MB

export default function DocumentUploadModal({ isOpen, onClose }) {
  const [selectedFile, setSelectedFile] = useState(null);
  const [dragActive, setDragActive] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const fileInputRef = useRef(null);

  if (!isOpen) return null;

  const validateAndSetFile = (file) => {
    setError('');
    setNotice('');

    if (!file) return;

    if (file.size === 0) {
      setError('Selected file is empty. Please select a valid file.');
      return;
    }

    if (file.size > MAX_FILE_SIZE_BYTES) {
      setError('File size exceeds the 25 MB limit.');
      return;
    }

    const fileNameLower = file.name.toLowerCase();
    const isAllowed = ALLOWED_EXTENSIONS.some((ext) => fileNameLower.endsWith(ext));

    if (!isAllowed) {
      setError('Unsupported file type. Allowed formats: PDF (.pdf), DOCX (.docx), TXT (.txt).');
      return;
    }

    setSelectedFile(file);
  };

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      validateAndSetFile(e.dataTransfer.files[0]);
    }
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      validateAndSetFile(e.target.files[0]);
    }
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  const getFileTypeLabel = (name) => {
    const ext = name.split('.').pop()?.toUpperCase() || 'FILE';
    return ext;
  };

  const handleRemoveFile = () => {
    setSelectedFile(null);
    setError('');
    setNotice('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleUploadSubmit = (e) => {
    e.preventDefault();
    if (!selectedFile) {
      setError('Please select a file to upload.');
      return;
    }

    // Modal UI ready for backend ingestion in upcoming steps
    setNotice('Upload UI validated. Multipart ingestion will connect to the backend in Step 23.');
    setTimeout(() => {
      handleRemoveFile();
      onClose();
    }, 1400);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-xs">
      <div className="w-full max-w-lg bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 space-y-5">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div>
            <h3 className="text-base font-semibold text-slate-100">Upload documents</h3>
            <p className="text-xs text-slate-400 mt-0.5">Ingest knowledge files into your workspace</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-slate-200 text-lg leading-none"
          >
            &times;
          </button>
        </div>

        {/* Notice Banner */}
        {notice && (
          <div className="p-3 bg-indigo-500/15 border border-indigo-500/30 rounded-xl text-xs text-indigo-300">
            {notice}
          </div>
        )}

        {/* Error Banner */}
        {error && (
          <div className="p-3 bg-rose-500/15 border border-rose-500/30 rounded-xl text-xs text-rose-300">
            {error}
          </div>
        )}

        <form onSubmit={handleUploadSubmit} className="space-y-4">
          {/* Drag & Drop Area */}
          {!selectedFile ? (
            <div
              onDragEnter={handleDrag}
              onDragLeave={handleDrag}
              onDragOver={handleDrag}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className={`p-8 border-2 border-dashed rounded-2xl flex flex-col items-center justify-center text-center cursor-pointer transition-all ${
                dragActive
                  ? 'border-indigo-500 bg-indigo-500/10'
                  : 'border-slate-700/80 hover:border-slate-600 bg-slate-900/40'
              }`}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,.docx,.txt"
                onChange={handleFileChange}
                className="hidden"
              />
              <div className="w-12 h-12 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-400 mb-3">
                <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
                </svg>
              </div>
              <p className="text-xs font-semibold text-slate-200">
                Drag and drop files here or <span className="text-indigo-400 hover:underline">browse</span>
              </p>
              <p className="text-[11px] text-slate-500 mt-1">
                Supported formats: PDF, DOCX, TXT (Max: 25 MB)
              </p>
            </div>
          ) : (
            /* Selected File Preview */
            <div className="p-4 rounded-xl bg-slate-800/60 border border-slate-700/80 flex items-center justify-between">
              <div className="flex items-center gap-3 overflow-hidden">
                <div className="px-2.5 py-1.5 rounded-lg bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 font-bold text-xs">
                  {getFileTypeLabel(selectedFile.name)}
                </div>
                <div className="truncate text-left">
                  <p className="text-xs font-semibold text-slate-200 truncate">{selectedFile.name}</p>
                  <p className="text-[11px] text-slate-400">{formatFileSize(selectedFile.size)}</p>
                </div>
              </div>

              <button
                type="button"
                onClick={handleRemoveFile}
                className="px-2.5 py-1 text-xs text-rose-400 hover:bg-rose-500/10 rounded-lg transition-colors cursor-pointer"
              >
                Remove
              </button>
            </div>
          )}

          {/* Action Footer */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={() => {
                handleRemoveFile();
                onClose();
              }}
              className="px-4 py-2 text-xs font-medium text-slate-300 hover:text-white rounded-xl hover:bg-slate-800 transition-all"
            >
              Cancel
            </button>
            <div className="w-36">
              <FormButton type="submit" disabled={!selectedFile}>
                Upload
              </FormButton>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
