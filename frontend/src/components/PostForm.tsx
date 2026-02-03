'use client';

import { useState, useEffect } from 'react';
import { postApi, aiApi, getToken } from '@/lib/api';
import { Post, AnalyzeResult } from '@/lib/types';

interface PostFormProps {
  onPostCreated: (post: Post) => void;
}

export default function PostForm({ onPostCreated }: PostFormProps) {
  const [content, setContent] = useState('');
  const [author, setAuthor] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [analysis, setAnalysis] = useState<AnalyzeResult | null>(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  const MAX_LENGTH = 280;

  useEffect(() => {
    setIsLoggedIn(!!getToken());
  }, []);

  useEffect(() => {
    const debounce = setTimeout(() => {
      if (content.length > 10) {
        analyzeContent();
      } else {
        setAnalysis(null);
      }
    }, 500);

    return () => clearTimeout(debounce);
  }, [content]);

  const analyzeContent = async () => {
    try {
      const result = await aiApi.analyze(content);
      setAnalysis(result);
    } catch (error) {
      console.error('분석 실패:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim() || !author.trim()) return;

    setIsSubmitting(true);
    try {
      const response = await postApi.create({ content, author });
      onPostCreated(response.data);
      setContent('');
      setAnalysis(null);
    } catch (error) {
      console.error('작성 실패:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form className="post-form" onSubmit={handleSubmit}>
      <input
        type="text"
        placeholder="닉네임"
        value={author}
        onChange={(e) => setAuthor(e.target.value)}
        style={{
          width: '100%',
          padding: '8px 0',
          background: 'transparent',
          border: 'none',
          borderBottom: '1px solid var(--border)',
          color: 'var(--text)',
          fontSize: '14px',
          marginBottom: '12px',
        }}
      />
      <textarea
        placeholder="무슨 일이 일어나고 있나요?"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        maxLength={MAX_LENGTH + 50}
      />

      {analysis && (
        <div className="ai-result" style={{ marginTop: '12px' }}>
          <span className="post-sentiment">
            {analysis.sentiment.emoji} {analysis.sentiment.sentiment}
          </span>
          {analysis.hashtags.length > 0 && (
            <div className="post-hashtags">
              {analysis.hashtags.map((tag) => (
                <span
                  key={tag}
                  className="hashtag"
                  onClick={() => setContent(content + ` #${tag}`)}
                  style={{ cursor: 'pointer' }}
                >
                  #{tag}
                </span>
              ))}
            </div>
          )}
        </div>
      )}

      <div className="post-form-footer">
        <span className={`char-count ${content.length > MAX_LENGTH ? 'over' : ''}`}>
          {content.length}/{MAX_LENGTH}
        </span>
        <button
          type="submit"
          className="btn btn-primary"
          disabled={!content.trim() || !author.trim() || content.length > MAX_LENGTH || isSubmitting}
        >
          {isSubmitting ? '게시중...' : '게시하기'}
        </button>
      </div>
    </form>
  );
}
