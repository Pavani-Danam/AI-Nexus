# AI-Nexus: Enterprise Autonomous AI & RAG Platform

AI-Nexus is an enterprise-grade Autonomous Agent, Workflow Orchestration, and Retrieval-Augmented Generation (RAG) platform with multi-tenant isolation, real-time observability, and production deployment automation.

## System Architecture

1. Frontend (SPA): React 18, Vite, Tailwind CSS, Lucide Icons, and NGINX reverse proxy.
2. Backend Engine: Spring Boot 3.2.5 (Java 21), Spring Security with stateless JWT, JPA / Hibernate, and HikariCP connection pooling.
3. AI & Vector Microservices: Google Gemini 1.5 Flash / Text-Embedding-004 and Pinecone vector store integration.
4. Database & Storage: PostgreSQL 16 Alpine with schema isolation and local persistent volume storage.

## Key Features

- Multi-Tenant Security & Isolation: Cryptographic JWT authentication, role-based workspace boundaries, and document-level permission gates.
- Document Ingestion & Vector Pipeline: Automated text extraction (PDF, DOCX, TXT), semantic chunking, embedding generation, and Pinecone vector indexing.
- Context-Aware Conversational RAG: Semantic caching, multi-query expansion, hybrid vector search, context budget management, and conversation memory tracking.
- Autonomous Agent Planner: Multi-step heuristic and LLM task decomposition, parallel task execution, DAG dependency resolution, transient failure retry, and self-correcting replanning.
- Workflow Automation Engine: Visual workflow definitions, human-in-the-loop approval gates, scheduled executions, automated timeout recovery, and real-time execution audit logs.
- Enterprise Administration & Governance: User & role administration, workspace access controls, usage quota enforcement, and tamper-evident audit trails.
- Production Observability & Monitoring: Correlation ID propagation (X-Correlation-ID), MDC logging, telemetry metrics registry, and Kubernetes liveness/readiness probes.

## Automated Testing

Execute the complete regression suite:
mvn -f backend/pom.xml test
