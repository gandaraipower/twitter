'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { getToken, removeToken } from '@/lib/api';

export default function Header() {
  const router = useRouter();
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  useEffect(() => {
    setIsLoggedIn(!!getToken());
  }, []);

  const handleLogout = () => {
    removeToken();
    setIsLoggedIn(false);
    router.refresh();
  };

  return (
    <header className="header">
      <Link href="/">
        <h1>Twitter Clone</h1>
      </Link>
      <div className="header-actions">
        {isLoggedIn ? (
          <button className="btn btn-outline btn-small" onClick={handleLogout}>
            로그아웃
          </button>
        ) : (
          <>
            <Link href="/login">
              <button className="btn btn-outline btn-small">로그인</button>
            </Link>
            <Link href="/signup">
              <button className="btn btn-primary btn-small">회원가입</button>
            </Link>
          </>
        )}
      </div>
    </header>
  );
}
