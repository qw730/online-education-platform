import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: 'Sign in' }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { public: true, title: 'Create account' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: 'Home' } },
      { path: 'courses', name: 'courses', component: () => import('@/views/CourseListView.vue'), meta: { title: 'Courses' } },
      { path: 'courses/:id', name: 'course-detail', component: () => import('@/views/CourseDetailView.vue'), meta: { title: 'Course' } },
      { path: 'my/courses', name: 'my-courses', component: () => import('@/views/MyCoursesView.vue'), meta: { title: 'My Learning' } },
      { path: 'my/courses/:id', name: 'learning', component: () => import('@/views/LearningView.vue'), meta: { title: 'Learning' } },
      { path: 'my/plans', name: 'plans', component: () => import('@/views/StudyPlansView.vue'), meta: { title: 'Study Plans' } },
      { path: 'my/progress', name: 'progress', component: () => import('@/views/ProgressView.vue'), meta: { title: 'Progress' } },
      { path: 'my/attempts', name: 'attempts', component: () => import('@/views/AttemptsView.vue'), meta: { title: 'Quiz History' } },
      { path: 'quiz/:id', name: 'quiz', component: () => import('@/views/QuizView.vue'), meta: { title: 'Quiz' } },
      { path: 'quiz/result/:attemptId', name: 'quiz-result', component: () => import('@/views/QuizResultView.vue'), meta: { title: 'Quiz Result' } },
      { path: 'profile', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { title: 'Profile' } },

      /* --------------------------------------------------------- admin */
      { path: 'admin', name: 'admin-dashboard', component: () => import('@/views/admin/AdminDashboardView.vue'), meta: { admin: true, title: 'Admin' } },
      { path: 'admin/courses', name: 'admin-courses', component: () => import('@/views/admin/AdminCoursesView.vue'), meta: { admin: true, title: 'Manage courses' } },
      { path: 'admin/courses/new', name: 'admin-course-new', component: () => import('@/views/admin/AdminCourseEditView.vue'), meta: { admin: true, title: 'New course' } },
      { path: 'admin/courses/:id/edit', name: 'admin-course-edit', component: () => import('@/views/admin/AdminCourseEditView.vue'), meta: { admin: true, title: 'Edit course' } },
      { path: 'admin/courses/:id/lessons', name: 'admin-lessons', component: () => import('@/views/admin/AdminLessonsView.vue'), meta: { admin: true, title: 'Lessons' } },
      { path: 'admin/courses/:id/quizzes', name: 'admin-quizzes', component: () => import('@/views/admin/AdminQuizzesView.vue'), meta: { admin: true, title: 'Quizzes' } },
      { path: 'admin/quizzes/:id/questions', name: 'admin-questions', component: () => import('@/views/admin/AdminQuestionsView.vue'), meta: { admin: true, title: 'Questions' } },
      { path: 'admin/categories', name: 'admin-categories', component: () => import('@/views/admin/AdminCategoriesView.vue'), meta: { admin: true, title: 'Categories' } },
      { path: 'admin/users', name: 'admin-users', component: () => import('@/views/admin/AdminUsersView.vue'), meta: { admin: true, title: 'Users' } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue'), meta: { public: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  document.title = to.meta.title
    ? `${to.meta.title} · Online Personal Education Platform`
    : 'Online Personal Education Platform'

  if (to.meta.public) {
    return true
  }
  if (!userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.admin && !userStore.isAdmin) {
    return { path: '/' }
  }
  return true
})

export default router
