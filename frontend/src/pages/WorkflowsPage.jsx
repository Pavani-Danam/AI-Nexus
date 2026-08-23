import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import workflowService from '../services/workflowService';

const STEP_TYPES = ['SEARCH', 'ANALYZE', 'KNOWLEDGE', 'SYNTHESIZE', 'NOTIFICATION'];
const STATUS_TYPES = ['DRAFT', 'ACTIVE', 'PAUSED', 'ARCHIVED'];

export default function WorkflowsPage() {
  const { currentWorkspace } = useContext(AuthContext);
  const workspaceId = currentWorkspace?.id || 1;

  const [workflows, setWorkflows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [executingId, setExecutingId] = useState(null);
  const [error, setError] = useState('');
  const [selectedWorkflow, setSelectedWorkflow] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  // Execution Result State
  const [executionResult, setExecutionResult] = useState(null);
  const [isResultOpen, setIsResultOpen] = useState(false);

  // Form State
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState('DRAFT');
  const [steps, setSteps] = useState([]);

  useEffect(() => {
    if (workspaceId) {
      loadWorkflows();
    }
  }, [workspaceId]);

  const loadWorkflows = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await workflowService.getWorkflows(workspaceId);
      setWorkflows(data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load workflows');
    } finally {
      setLoading(false);
    }
  };

  const openCreateModal = () => {
    setIsEditing(false);
    setSelectedWorkflow(null);
    setName('');
    setDescription('');
    setStatus('DRAFT');
    setSteps([
      { stepKey: 'step-1', name: 'Retrieve Documents', type: 'SEARCH', configuration: '{}', executionOrder: 1, dependencies: [], enabled: true }
    ]);
    setIsModalOpen(true);
  };

  const openEditModal = (wf) => {
    setIsEditing(true);
    setSelectedWorkflow(wf);
    setName(wf.name);
    setDescription(wf.description || '');
    setStatus(wf.status || 'DRAFT');
    setSteps(wf.steps?.map(s => ({
      stepKey: s.stepKey,
      name: s.name,
      type: s.type,
      configuration: s.configuration || '{}',
      executionOrder: s.executionOrder,
      dependencies: s.dependencies || [],
      enabled: s.enabled
    })) || []);
    setIsModalOpen(true);
  };

  const handleRunWorkflow = async (wf) => {
    try {
      setExecutingId(wf.id);
      setError('');
      const res = await workflowService.executeWorkflow(wf.id, { inputQuery: wf.name });
      setExecutionResult(res);
      setIsResultOpen(true);
    } catch (err) {
      alert(err.response?.data?.message || 'Workflow execution failed');
    } finally {
      setExecutingId(null);
    }
  };

  const addStep = () => {
    const nextIndex = steps.length + 1;
    setSteps([
      ...steps,
      { stepKey: `step-${nextIndex}`, name: `Step ${nextIndex}`, type: 'SEARCH', configuration: '{}', executionOrder: nextIndex, dependencies: [], enabled: true }
    ]);
  };

  const removeStep = (index) => {
    const updated = steps.filter((_, i) => i !== index);
    setSteps(updated);
  };

  const handleStepChange = (index, field, value) => {
    const updated = [...steps];
    updated[index][field] = value;
    setSteps(updated);
  };

  const handleDependenciesChange = (index, commaSeparated) => {
    const deps = commaSeparated.split(',').map(s => s.trim()).filter(Boolean);
    const updated = [...steps];
    updated[index].dependencies = deps;
    setSteps(updated);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      alert('Workflow name is required');
      return;
    }

    const payload = {
      name: name.trim(),
      description: description.trim(),
      workspaceId: Number(workspaceId),
      status,
      steps
    };

    try {
      if (isEditing && selectedWorkflow) {
        await workflowService.updateWorkflow(selectedWorkflow.id, payload);
      } else {
        await workflowService.createWorkflow(payload);
      }
      setIsModalOpen(false);
      loadWorkflows();
    } catch (err) {
      alert(err.response?.data?.message || 'Error saving workflow');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this workflow?')) {
      try {
        await workflowService.deleteWorkflow(id);
        loadWorkflows();
      } catch (err) {
        alert(err.response?.data?.message || 'Failed to delete workflow');
      }
    }
  };

  return (
    <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto', fontFamily: 'sans-serif' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: 'bold', margin: '0 0 6px 0', color: '#1e293b' }}>Workflows</h1>
          <p style={{ margin: 0, color: '#64748b' }}>Define, trigger, and inspect repeatable agent workflows.</p>
        </div>
        <button
          onClick={openCreateModal}
          style={{ padding: '10px 18px', backgroundColor: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 'bold' }}
        >
          + Create Workflow
        </button>
      </div>

      {error && <div style={{ padding: '12px', background: '#fee2e2', color: '#dc2626', borderRadius: '6px', marginBottom: '16px' }}>{error}</div>}

      {loading ? (
        <p style={{ color: '#64748b' }}>Loading workflows...</p>
      ) : workflows.length === 0 ? (
        <div style={{ padding: '48px', textAlign: 'center', background: '#f8fafc', borderRadius: '8px', border: '1px dashed #cbd5e1' }}>
          <p style={{ color: '#64748b', marginBottom: '16px' }}>No workflows configured in this workspace.</p>
          <button
            onClick={openCreateModal}
            style={{ padding: '8px 16px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer' }}
          >
            Create Your First Workflow
          </button>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: '20px' }}>
          {workflows.map((wf) => (
            <div key={wf.id} style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '18px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                  <h3 style={{ margin: 0, fontSize: '18px', color: '#0f172a' }}>{wf.name}</h3>
                  <span style={{ fontSize: '12px', padding: '2px 8px', borderRadius: '4px', fontWeight: 'bold', background: wf.status === 'ACTIVE' ? '#dcfce7' : '#f1f5f9', color: wf.status === 'ACTIVE' ? '#15803d' : '#475569' }}>
                    {wf.status} (v{wf.version})
                  </span>
                </div>
                <p style={{ color: '#64748b', fontSize: '14px', margin: '0 0 14px 0' }}>{wf.description || 'No description provided.'}</p>
                <div style={{ fontSize: '13px', color: '#334155', marginBottom: '12px' }}>
                  <strong>Steps ({wf.steps?.length || 0}):</strong>
                  <ul style={{ margin: '6px 0 0 0', paddingLeft: '20px' }}>
                    {wf.steps?.map((s) => (
                      <li key={s.id || s.stepKey} style={{ marginBottom: '3px' }}>
                        <span style={{ fontWeight: '500' }}>{s.stepKey}</span>: {s.name} ({s.type})
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
              <div>
                <button
                  onClick={() => handleRunWorkflow(wf)}
                  disabled={executingId === wf.id}
                  style={{ width: '100%', marginBottom: '8px', padding: '8px', background: '#16a34a', color: '#fff', border: 'none', borderRadius: '4px', cursor: executingId === wf.id ? 'not-allowed' : 'pointer', fontWeight: 'bold', fontSize: '13px' }}
                >
                  {executingId === wf.id ? '? Executing...' : '? Run Workflow'}
                </button>
                <div style={{ display: 'flex', gap: '8px', borderTop: '1px solid #f1f5f9', paddingTop: '10px' }}>
                  <button
                    onClick={() => openEditModal(wf)}
                    style={{ flex: 1, padding: '6px 12px', background: '#f8fafc', border: '1px solid #cbd5e1', borderRadius: '4px', cursor: 'pointer', fontSize: '13px' }}
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(wf.id)}
                    style={{ padding: '6px 12px', background: '#fee2e2', color: '#b91c1c', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '13px' }}
                  >
                    Delete
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Execution Result Modal */}
      {isResultOpen && executionResult && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1100 }}>
          <div style={{ background: '#fff', borderRadius: '8px', width: '750px', maxHeight: '90vh', overflowY: 'auto', padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 style={{ margin: 0, fontSize: '20px' }}>Workflow Execution Run #{executionResult.id}</h2>
              <span style={{ fontSize: '12px', padding: '3px 10px', borderRadius: '4px', fontWeight: 'bold', background: executionResult.status === 'COMPLETED' ? '#dcfce7' : '#fee2e2', color: executionResult.status === 'COMPLETED' ? '#15803d' : '#dc2626' }}>
                {executionResult.status} ({executionResult.durationMs || 0}ms)
              </span>
            </div>

            {executionResult.errorMessage && (
              <div style={{ padding: '10px', background: '#fee2e2', color: '#b91c1c', borderRadius: '4px', marginBottom: '14px', fontSize: '13px' }}>
                {executionResult.errorMessage}
              </div>
            )}

            <div style={{ marginBottom: '16px' }}>
              <h3 style={{ fontSize: '15px', marginBottom: '8px' }}>Final Output</h3>
              <div style={{ padding: '12px', background: '#f8fafc', borderRadius: '6px', border: '1px solid #e2e8f0', fontSize: '13px', whiteSpace: 'pre-wrap' }}>
                {executionResult.finalOutput || 'No output recorded.'}
              </div>
            </div>

            <div style={{ marginBottom: '16px' }}>
              <h3 style={{ fontSize: '15px', marginBottom: '8px' }}>Step Audit Trace</h3>
              {executionResult.stepExecutions?.map((step) => (
                <div key={step.id || step.stepKey} style={{ background: '#f1f5f9', padding: '10px', borderRadius: '6px', marginBottom: '8px', fontSize: '13px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold', marginBottom: '4px' }}>
                    <span>{step.stepKey}: {step.stepName} ({step.stepType})</span>
                    <span style={{ color: step.status === 'COMPLETED' ? '#15803d' : '#dc2626' }}>{step.status}</span>
                  </div>
                  {step.output && <div style={{ color: '#334155', marginTop: '4px' }}><strong>Output:</strong> {step.output}</div>}
                  {step.errorMessage && <div style={{ color: '#dc2626', marginTop: '4px' }}><strong>Error:</strong> {step.errorMessage}</div>}
                </div>
              ))}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button
                onClick={() => setIsResultOpen(false)}
                style={{ padding: '8px 16px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal for Create/Edit */}
      {isModalOpen && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ background: '#fff', borderRadius: '8px', width: '700px', maxHeight: '90vh', overflowY: 'auto', padding: '24px' }}>
            <h2 style={{ margin: '0 0 16px 0', fontSize: '20px' }}>{isEditing ? 'Edit Workflow' : 'Create New Workflow'}</h2>
            <form onSubmit={handleSubmit}>
              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }}>Workflow Name *</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }}
                  required
                />
              </div>

              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }}>Description</label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  rows="2"
                  style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }}
                />
              </div>

              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 'bold', marginBottom: '4px' }}>Status</label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                  style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #cbd5e1' }}
                >
                  {STATUS_TYPES.map(st => <option key={st} value={st}>{st}</option>)}
                </select>
              </div>

              <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: '16px', marginBottom: '16px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                  <h3 style={{ margin: 0, fontSize: '16px' }}>Workflow Steps</h3>
                  <button
                    type="button"
                    onClick={addStep}
                    style={{ padding: '4px 10px', background: '#e0f2fe', color: '#0369a1', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '12px', fontWeight: 'bold' }}
                  >
                    + Add Step
                  </button>
                </div>

                {steps.map((step, idx) => (
                  <div key={idx} style={{ background: '#f8fafc', padding: '12px', borderRadius: '6px', border: '1px solid #e2e8f0', marginBottom: '10px' }}>
                    <div style={{ display: 'flex', gap: '8px', marginBottom: '8px' }}>
                      <input
                        type="text"
                        placeholder="Key (e.g. s1)"
                        value={step.stepKey}
                        onChange={(e) => handleStepChange(idx, 'stepKey', e.target.value)}
                        style={{ width: '100px', padding: '6px', border: '1px solid #cbd5e1', borderRadius: '4px', fontSize: '12px' }}
                        required
                      />
                      <input
                        type="text"
                        placeholder="Step Name"
                        value={step.name}
                        onChange={(e) => handleStepChange(idx, 'name', e.target.value)}
                        style={{ flex: 1, padding: '6px', border: '1px solid #cbd5e1', borderRadius: '4px', fontSize: '12px' }}
                        required
                      />
                      <select
                        value={step.type}
                        onChange={(e) => handleStepChange(idx, 'type', e.target.value)}
                        style={{ width: '130px', padding: '6px', border: '1px solid #cbd5e1', borderRadius: '4px', fontSize: '12px' }}
                      >
                        {STEP_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                      </select>
                      <button
                        type="button"
                        onClick={() => removeStep(idx)}
                        style={{ background: '#fee2e2', color: '#b91c1c', border: 'none', borderRadius: '4px', padding: '0 8px', cursor: 'pointer' }}
                      >
                        ?
                      </button>
                    </div>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <input
                        type="text"
                        placeholder="Dependencies (comma-separated keys: e.g. s1, s2)"
                        value={step.dependencies?.join(', ') || ''}
                        onChange={(e) => handleDependenciesChange(idx, e.target.value)}
                        style={{ flex: 1, padding: '6px', border: '1px solid #cbd5e1', borderRadius: '4px', fontSize: '12px' }}
                      />
                    </div>
                  </div>
                ))}
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  style={{ padding: '8px 16px', background: '#f1f5f9', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  style={{ padding: '8px 16px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' }}
                >
                  {isEditing ? 'Save Changes' : 'Create Workflow'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
