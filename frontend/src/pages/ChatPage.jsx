import React from 'react';
import { EmptyStateCard } from '../components/dashboard/DashboardWidgets';

const ChatIcon = ({ className }) => (
  <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
  </svg>
);

export default function ChatPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-100 tracking-tight">AI Chat</h1>
        <p className="text-xs text-slate-400 mt-1">
          Interact with AI models grounded on your organization's verified knowledge assets.
        </p>
      </div>

      <div className="min-h-[400px] flex items-center justify-center">
        <EmptyStateCard
          icon={ChatIcon}
          title="AI Chat Engine"
          message="Grounded RAG chat with Gemini AI and Pinecone vector search will be activated in Phase 6."
        />
      </div>
    </div>
  );
}
