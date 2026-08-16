import React, { useState } from 'react';
import { FormInput, FormButton } from '../ui/FormControls';

export default function CreateWorkspaceModal({ isOpen, onClose }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    setError('');
    setNotice('');

    if (!name.trim()) {
      setError('Workspace name is required.');
      return;
    }

    // Modal UI ready for backend API integration in upcoming steps
    setNotice('Workspace creation UI is ready for backend integration in Step 22.');
    setTimeout(() => {
      setName('');
      setDescription('');
      setNotice('');
      onClose();
    }, 1200);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-xs">
      <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 space-y-5">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <h3 className="text-base font-semibold text-slate-100">Create Workspace</h3>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-slate-200 text-lg leading-none"
          >
            &times;
          </button>
        </div>

        {notice && (
          <div className="p-3 bg-indigo-500/15 border border-indigo-500/30 rounded-xl text-xs text-indigo-300">
            {notice}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <FormInput
            label="Workspace Name"
            id="workspaceName"
            placeholder="e.g. Legal Research, Engineering"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              if (error) setError('');
            }}
            error={error}
            required
          />

          <div className="space-y-1.5 text-left">
            <label htmlFor="description" className="block text-xs font-semibold text-slate-200">
              Description <span className="text-slate-500 font-normal">(Optional)</span>
            </label>
            <textarea
              id="description"
              rows={3}
              placeholder="Brief description of the workspace purpose..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-3.5 py-2.5 bg-slate-900/90 border border-slate-700 rounded-xl text-sm text-slate-100 placeholder-slate-500 transition-all focus:outline-hidden focus:ring-2 focus:ring-indigo-500/40 focus:border-indigo-400"
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-medium text-slate-300 hover:text-white rounded-xl hover:bg-slate-800 transition-all"
            >
              Cancel
            </button>
            <div className="w-40">
              <FormButton type="submit">
                Create Workspace
              </FormButton>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
