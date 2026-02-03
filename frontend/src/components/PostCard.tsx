'use client';

import { useState } from 'react';
import { Post, AnalyzeResult } from '@/lib/types';
import { postApi, aiApi } from '@/lib/api';

interface PostCardProps {
  post: Post;
  onDelete?: (id: number) => void;
  onUpdate?: (post: Post) => void;
}

export default function PostCard({ post, onDelete, onUpdate }: PostCardProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(post.content);
  const [analysis, setAnalysis] = useState<AnalyzeResult | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString('ko-KR', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const handleAnalyze = async () => {
    setIsAnalyzing(true);
    try {
      const result = await aiApi.analyze(post.content);
      setAnalysis(result);
    } catch (error) {
      console.error('분석 실패:', error);
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleUpdate = async () => {
    try {
      const response = await postApi.update(post.id, {
        content: editContent,
        author: post.author,
      });
      onUpdate?.(response.data);
      setIsEditing(false);
    } catch (error) {
      console.error('수정 실패:', error);
    }
  };

  const handleDelete = async () => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    try {
      await postApi.delete(post.id);
      onDelete?.(post.id);
    } catch (error) {
      console.error('삭제 실패:', error);
    }
  };

  return (
    <div className="post-card">
      <div className="post-header">
        <div>
          <span className="post-author">{post.author}</span>
          <span className="post-time"> · {formatDate(post.createdAt)}</span>
        </div>
      </div>

      {isEditing ? (
        <div>
          <textarea
            value={editContent}
            onChange={(e) => setEditContent(e.target.value)}
            style={{ width: '100%', minHeight: '80px', marginBottom: '8px' }}
          />
          <div className="post-actions">
            <button className="btn btn-primary btn-small" onClick={handleUpdate}>
              저장
            </button>
            <button className="btn btn-outline btn-small" onClick={() => setIsEditing(false)}>
              취소
            </button>
          </div>
        </div>
      ) : (
        <>
          <p className="post-content">{post.content}</p>

          {analysis && (
            <div style={{ marginTop: '12px' }}>
              <span className="post-sentiment">
                {analysis.sentiment.emoji} {analysis.sentiment.sentiment}
              </span>
              {analysis.hashtags.length > 0 && (
                <div className="post-hashtags">
                  {analysis.hashtags.map((tag) => (
                    <span key={tag} className="hashtag">
                      #{tag}
                    </span>
                  ))}
                </div>
              )}
            </div>
          )}

          <div className="post-actions">
            <button
              className="btn btn-outline btn-small"
              onClick={handleAnalyze}
              disabled={isAnalyzing}
            >
              {isAnalyzing ? '분석중...' : 'AI 분석'}
            </button>
            <button className="btn btn-outline btn-small" onClick={() => setIsEditing(true)}>
              수정
            </button>
            <button className="btn btn-danger btn-small" onClick={handleDelete}>
              삭제
            </button>
          </div>
        </>
      )}
    </div>
  );
}
