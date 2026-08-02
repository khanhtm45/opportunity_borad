import { Routes, Route } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import Splash from './components/Splash.jsx'
import Layout from './components/Layout.jsx'
import HomePage from './pages/HomePage.jsx'
import BoardPage from './pages/BoardPage.jsx'
import ExplorePage from './pages/ExplorePage.jsx'
import DetailPage from './pages/DetailPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import BookmarksPage from './pages/BookmarksPage.jsx'
import ProviderPage from './pages/ProviderPage.jsx'
import AdminPage from './pages/AdminPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import StudentProfilePage from './pages/StudentProfilePage.jsx'
import MyApplicationsPage from './pages/MyApplicationsPage.jsx'
import NotificationsPage from './pages/NotificationsPage.jsx'
import { Component } from 'react'

class ErrorBoundary extends Component {
  state = { err: null }
  static getDerivedStateFromError(err) { return { err } }
  render() {
    if (this.state.err) return <div className="p-8 text-red-600"><pre className="whitespace-pre-wrap text-sm">{String(this.state.err.stack || this.state.err)}</pre></div>
    return this.props.children
  }
}

export default function App() {
  const { loading } = useAuth()

  if (loading) return <Splash message="Đang khởi tạo…" />

  return (
    <ErrorBoundary>
    <Layout>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/home" element={<HomePage />} />
        <Route path="/board" element={<BoardPage />} />
        <Route path="/explore" element={<ExplorePage />} />
        <Route path="/opportunities/:slug" element={<DetailPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/me/bookmarks" element={<BookmarksPage />} />
        <Route path="/me/applications" element={<MyApplicationsPage />} />
        <Route path="/me/notifications" element={<NotificationsPage />} />
        <Route path="/me/profile" element={<StudentProfilePage />} />
        <Route path="/me" element={<DashboardPage />} />
        <Route path="/provider" element={<ProviderPage />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="*" element={<div className="py-16 text-center text-slate-400">404 — Không tìm thấy trang.</div>} />
      </Routes>
    </Layout>
    </ErrorBoundary>
  )
}
