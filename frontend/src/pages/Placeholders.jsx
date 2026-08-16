import React from 'react';

function PlaceholderCard({ title, description, badge }) {
  return (
    <div className="p-8 rounded-2xl bg-slate-900/60 border border-slate-800 max-w-2xl mx-auto text-center mt-12">
      {badge && (
        <span className="inline-block px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 mb-4">
          {badge}
        </span>
      )}
      <h2 className="text-2xl font-bold text-white mb-2">{title}</h2>
      <p className="text-slate-400 text-sm mb-6">{description}</p>
      <div className="text-xs font-mono text-slate-500 bg-slate-950 p-3 rounded-lg border border-slate-800/60 inline-block">
        Component foundation established • Ready for Phase 4 implementation
      </div>
    </div>
  );
}

export function LoginPage() {
  return (
    <PlaceholderCard
      badge="Authentication"
      title="Sign In to AI-Nexus"
      description="Secure authentication interface with JWT access and refresh token rotation."
    />
  );
}

export function RegisterPage() {
  return (
    <PlaceholderCard
      badge="Onboarding"
      title="Create Your AI-Nexus Account"
      description="User registration with input validation and BCrypt password encryption."
    />
  );
}

export function DashboardPage() {
  return (
    <PlaceholderCard
      badge="Analytics & Workspace"
      title="Workspace Dashboard"
      description="Overview of active knowledge bases, processing jobs, and agent sessions."
    />
  );
}

export function DocumentsPage() {
  return (
    <PlaceholderCard
      badge="Knowledge Base"
      title="Document Ingestion & Management"
      description="Upload PDFs, markdown, and text documents for vector embedding extraction."
    />
  );
}

export function WorkspacesPage() {
  return (
    <PlaceholderCard
      badge="Collaboration"
      title="Team Workspaces"
      description="Isolated workspace environments with fine-grained access control."
    />
  );
}

export function ChatPage() {
  return (
    <PlaceholderCard
      badge="AI Conversation"
      title="Interactive AI Chat & Agents"
      description="Real-time conversational streaming with context retrieval and tool calling."
    />
  );
}
