'use client';

import { useState, useEffect } from 'react';
import { postApi } from '@/lib/api';
import { Post } from '@/lib/types';
import PostCard from '@/components/PostCard';
import PostForm from '@/components/PostForm';

export default function Home() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  const fetchPosts = async (pageNum: number) => {
    try {
      const response = await postApi.getAll(pageNum);
      const newPosts = response.data.content;

      if (pageNum === 0) {
        setPosts(newPosts);
      } else {
        setPosts((prev) => [...prev, ...newPosts]);
      }

      setHasMore(response.data.number < response.data.totalPages - 1);
    } catch (err) {
      setError('게시글을 불러오는데 실패했습니다.');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchPosts(0);
  }, []);

  const handlePostCreated = (newPost: Post) => {
    setPosts((prev) => [newPost, ...prev]);
  };

  const handlePostDeleted = (id: number) => {
    setPosts((prev) => prev.filter((post) => post.id !== id));
  };

  const handlePostUpdated = (updatedPost: Post) => {
    setPosts((prev) =>
      prev.map((post) => (post.id === updatedPost.id ? updatedPost : post))
    );
  };

  const loadMore = () => {
    const nextPage = page + 1;
    setPage(nextPage);
    fetchPosts(nextPage);
  };

  return (
    <main>
      <PostForm onPostCreated={handlePostCreated} />

      {isLoading ? (
        <div className="loading">로딩 중...</div>
      ) : error ? (
        <div className="error-message" style={{ padding: '20px' }}>
          {error}
        </div>
      ) : posts.length === 0 ? (
        <div className="empty-state">
          <h2>아직 게시글이 없습니다</h2>
          <p>첫 번째 게시글을 작성해보세요!</p>
        </div>
      ) : (
        <>
          {posts.map((post) => (
            <PostCard
              key={post.id}
              post={post}
              onDelete={handlePostDeleted}
              onUpdate={handlePostUpdated}
            />
          ))}
          {hasMore && (
            <div style={{ padding: '20px', textAlign: 'center' }}>
              <button className="btn btn-outline" onClick={loadMore}>
                더 보기
              </button>
            </div>
          )}
        </>
      )}
    </main>
  );
}
