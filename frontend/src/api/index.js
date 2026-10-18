import request from './request'

/* ------------------------------------------------------------------- auth */
export const authApi = {
  register: (payload) => request.post('/auth/register', payload),
  login: (payload) => request.post('/auth/login', payload),
  me: () => request.get('/auth/me'),
  updateProfile: (payload) => request.put('/auth/profile', payload),
  changePassword: (payload) => request.put('/auth/password', payload)
}

/* ------------------------------------------------------------- catalogue */
export const courseApi = {
  categories: () => request.get('/categories'),
  list: (params) => request.get('/courses', { params }),
  detail: (courseId) => request.get(`/courses/${courseId}`)
}

/* ----------------------------------------------- enrolment and progress */
export const learningApi = {
  enroll: (courseId) => request.post('/enrollments', { courseId }),
  drop: (courseId) => request.delete(`/enrollments/${courseId}`),
  myCourses: (params) => request.get('/enrollments/my', { params }),
  myActiveCourses: () => request.get('/enrollments/my/all'),
  courseProgress: (courseId) => request.get(`/progress/course/${courseId}`),
  toggleLesson: (lessonId, payload) => request.post(`/progress/lesson/${lessonId}`, payload)
}

/* -------------------------------------------------------------- study plan */
export const planApi = {
  list: () => request.get('/plans'),
  detail: (planId) => request.get(`/plans/${planId}`),
  create: (payload) => request.post('/plans', payload),
  update: (planId, payload) => request.put(`/plans/${planId}`, payload),
  remove: (planId) => request.delete(`/plans/${planId}`),
  addItem: (planId, payload) => request.post(`/plans/${planId}/items`, payload),
  toggleItem: (planId, itemId) => request.patch(`/plans/${planId}/items/${itemId}`),
  removeItem: (planId, itemId) => request.delete(`/plans/${planId}/items/${itemId}`)
}

/* ------------------------------------------------------------------ quiz */
export const quizApi = {
  byCourse: (courseId) => request.get(`/quizzes/course/${courseId}`),
  detail: (quizId) => request.get(`/quizzes/${quizId}`),
  submit: (quizId, payload) => request.post(`/quizzes/${quizId}/submit`, payload),
  attemptsOfQuiz: (quizId) => request.get(`/quizzes/${quizId}/attempts`),
  myAttempts: (params) => request.get('/attempts/my', { params }),
  attemptDetail: (attemptId) => request.get(`/attempts/${attemptId}`)
}

/* ------------------------------------------------------------- dashboard */
export const dashboardApi = {
  student: () => request.get('/dashboard/student'),
  admin: () => request.get('/dashboard/admin'),
  recommendations: (limit = 6) => request.get('/recommendations', { params: { limit } })
}

/* ----------------------------------------------------------------- admin */
export const adminApi = {
  courses: (params) => request.get('/admin/courses', { params }),
  courseDetail: (courseId) => request.get(`/admin/courses/${courseId}`),
  createCourse: (payload) => request.post('/admin/courses', payload),
  updateCourse: (courseId, payload) => request.put(`/admin/courses/${courseId}`, payload),
  deleteCourse: (courseId) => request.delete(`/admin/courses/${courseId}`),

  lessons: (courseId) => request.get(`/admin/courses/${courseId}/lessons`),
  createLesson: (courseId, payload) => request.post(`/admin/courses/${courseId}/lessons`, payload),
  updateLesson: (lessonId, payload) => request.put(`/admin/lessons/${lessonId}`, payload),
  deleteLesson: (lessonId) => request.delete(`/admin/lessons/${lessonId}`),

  createCategory: (payload) => request.post('/admin/categories', payload),
  updateCategory: (categoryId, payload) => request.put(`/admin/categories/${categoryId}`, payload),

  quizzes: (courseId) => request.get(`/admin/quizzes/course/${courseId}`),
  createQuiz: (payload) => request.post('/admin/quizzes', payload),
  updateQuiz: (quizId, payload) => request.put(`/admin/quizzes/${quizId}`, payload),
  deleteQuiz: (quizId) => request.delete(`/admin/quizzes/${quizId}`),
  questions: (quizId) => request.get(`/admin/quizzes/${quizId}/questions`),
  createQuestion: (quizId, payload) => request.post(`/admin/quizzes/${quizId}/questions`, payload),
  updateQuestion: (questionId, payload) => request.put(`/admin/questions/${questionId}`, payload),
  deleteQuestion: (questionId) => request.delete(`/admin/questions/${questionId}`),

  users: (params) => request.get('/admin/users', { params }),
  toggleUserStatus: (userId) => request.patch(`/admin/users/${userId}/status`)
}
