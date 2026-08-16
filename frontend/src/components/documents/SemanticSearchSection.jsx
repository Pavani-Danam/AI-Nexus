import React, { useState } from 'react';
import { searchService } from '../../services/searchService';

export default function SemanticSearchSection({ workspaceId }) {
  const [query, setQuery] = useState('');
  const [topK, setTopK] = useState(5);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [searchResponse, setSearchResponse] = useState(null);
  const [hasSearched, setHasSearched] = useState(false);

  const handleSearch = async (e) => {
    if (e) e.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) {
      setError('Please enter a valid search query.');
      return;
    }
    if (!workspaceId) {
      setError('No active workspace selected.');
      return;
    }

    try {
      setLoading(true);
      setError('');
      const data = await searchService.search(trimmed, workspaceId, topK);
      setSearchResponse(data);
      setHasSearched(true);
    } catch (err) {
      console.error('Semantic search failed:', err);
      const status = err.response?.status;
      if (status === 401) {
        setError('Your session has expired. Please log in again.');
      } else if (status === 403) {
        setError('You do not have permission to search this workspace.');
      } else if (status === 400) {
        setError(err.response?.data?.message || 'Please enter a valid search query.');
      } else {
        setError('Unable to search documents right now. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleClear = () => {
    setQuery('');
    setError('');
    setSearchResponse(null);
    setHasSearched(false);
  };

  return (
    <div className="bg-slate-900/60 backdrop-blur-md border border-slate-800 rounded-2xl p-6 shadow-xl mb-8">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <svg className="w-5 h-5 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            Semantic Document Search
          </h2>
          <p className="text-sm text-slate-400 mt-1">
            Search across all indexed document chunks in this workspace using natural language.
          </p>
        </div>
      </div>

      <form onSubmit={handleSearch} className="space-y-4">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="e.g. What are the key architecture guidelines or compliance rules?"
              className="w-full bg-slate-800/80 border border-slate-700 text-white rounded-xl px-4 py-3 pl-11 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 transition-all placeholder:text-slate-500"
              disabled={loading}
            />
            <svg className="w-5 h-5 text-slate-400 absolute left-3.5 top-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>

          <div className="flex items-center gap-2">
            <select
              value={topK}
              onChange={(e) => setTopK(Number(e.target.value))}
              disabled={loading}
              className="bg-slate-800/80 border border-slate-700 text-slate-200 text-sm rounded-xl px-3 py-3 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value={3}>Top 3</option>
              <option value={5}>Top 5</option>
              <option value={10}>Top 10</option>
            </select>

            <button
              type="submit"
              disabled={loading || !query.trim()}
              className="bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium px-5 py-3 rounded-xl text-sm transition-all flex items-center gap-2 shadow-lg shadow-indigo-600/30 shrink-0"
            >
              {loading ? (
                <>
                  <svg className="animate-spin h-4 w-4 text-white" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                  </svg>
                  Searching...
                </>
              ) : (
                <>
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  Search
                </>
              )}
            </button>

            {(query || searchResponse || error) && (
              <button
                type="button"
                onClick={handleClear}
                disabled={loading}
                className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-4 py-3 rounded-xl text-sm transition-all border border-slate-700"
              >
                Clear
              </button>
            )}
          </div>
        </div>
      </form>

      {error && (
        <div className="mt-4 p-3 bg-red-500/10 border border-red-500/20 text-red-400 text-sm rounded-xl flex items-center gap-2">
          <svg className="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0114 0z" />
          </svg>
          <span>{error}</span>
        </div>
      )}

      {hasSearched && searchResponse && (
        <div className="mt-6 border-t border-slate-800/80 pt-5">
          <div className="flex items-center justify-between mb-4">
            <span className="text-sm text-slate-400">
              Found <strong className="text-indigo-400">{searchResponse.totalResults}</strong> relevant chunk{searchResponse.totalResults === 1 ? '' : 's'}
            </span>
          </div>

          {searchResponse.totalResults === 0 ? (
            <div className="text-center py-8 bg-slate-800/40 rounded-xl border border-slate-800/80">
              <svg className="w-10 h-10 text-slate-500 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0114 0z" />
              </svg>
              <p className="text-slate-300 font-medium">No relevant documents found.</p>
              <p className="text-xs text-slate-500 mt-1">Try phrasing your query differently or upload more documents.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {searchResponse.results.map((item, idx) => (
                <div
                  key={item.vectorId || idx}
                  className="p-4 bg-slate-800/50 hover:bg-slate-800/80 border border-slate-700/60 rounded-xl transition-all"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2 mb-2">
                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 text-xs font-semibold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 rounded-md">
                        #{idx + 1}
                      </span>
                      <span className="text-sm font-semibold text-slate-200 truncate max-w-xs sm:max-w-md">
                        {item.filename || 'Document'}
                      </span>
                      {item.chunkIndex !== null && (
                        <span className="text-xs text-slate-400 bg-slate-700/50 px-2 py-0.5 rounded">
                          Chunk {item.chunkIndex + 1}
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-mono font-medium px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        Score: {item.score != null ? item.score.toFixed(3) : 'N/A'}
                      </span>
                    </div>
                  </div>
                  <p className="text-sm text-slate-300 whitespace-pre-wrap leading-relaxed bg-slate-900/60 p-3 rounded-lg border border-slate-800">
                    {item.content}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
