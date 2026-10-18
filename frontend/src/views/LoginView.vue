<template>
  <div class="auth">
    <aside class="auth-aside">
      <div class="aside-inner">
        <div class="brand-mark">OP</div>
        <h1>Online Personal<br />Education Platform</h1>
        <p>
          Browse courses, build a study plan, track your progress and test yourself with
          auto-graded quizzes.
        </p>
        <ul class="features">
          <li><el-icon><Check /></el-icon> Enrol in courses and follow your own schedule</li>
          <li><el-icon><Check /></el-icon> Personal study plans with daily tasks</li>
          <li><el-icon><Check /></el-icon> Instant scoring on every quiz</li>
          <li><el-icon><Check /></el-icon> Course suggestions based on your history</li>
        </ul>
      </div>
    </aside>

    <main class="auth-main">
      <div class="auth-card">
        <h2>Welcome back</h2>
        <p class="muted">Sign in to continue learning.</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
          <el-form-item label="Username" prop="username">
            <el-input v-model="form.username" size="large" placeholder="Your username" clearable
                      :prefix-icon="User" @keyup.enter="submit" />
          </el-form-item>
          <el-form-item label="Password" prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password
                      placeholder="Your password" :prefix-icon="Lock" @keyup.enter="submit" />
          </el-form-item>

          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
            Sign in
          </el-button>
        </el-form>

        <div class="auth-foot">
          <span class="muted">No account yet?</span>
          <router-link to="/register" class="link">Create one</router-link>
        </div>

        <el-divider>Demo accounts</el-divider>
        <div class="demo-grid">
          <button class="demo" type="button" @click="fill('student')">
            <strong>Student</strong>
            <span class="mono">student / student123</span>
          </button>
          <button class="demo" type="button" @click="fill('admin')">
            <strong>Administrator</strong>
            <span class="mono">admin / admin123</span>
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: 'Please enter your username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please enter your password', trigger: 'blur' }]
}

function fill(kind) {
  if (kind === 'admin') {
    form.username = 'admin'
    form.password = 'admin123'
  } else {
    form.username = 'student'
    form.password = 'student123'
  }
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const user = await userStore.login({ ...form })
    ElMessage.success(`Welcome back, ${user.realName || user.username}!`)
    router.push(route.query.redirect || (user.role === 'ADMIN' ? '/admin' : '/'))
  } catch (error) {
    /* the interceptor already showed the message */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.05fr 1fr;
}

.auth-aside {
  background: linear-gradient(150deg, #4338ca 0%, #6d28d9 55%, #a855f7 100%);
  color: #fff;
  display: flex;
  align-items: center;
  padding: 56px;
}

.aside-inner {
  max-width: 460px;
}

.brand-mark {
  width: 46px;
  height: 46px;
  border-radius: 13px;
  display: grid;
  place-items: center;
  font-weight: 800;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.24);
  margin-bottom: 28px;
}

.auth-aside h1 {
  font-size: 34px;
  line-height: 1.22;
  margin: 0 0 16px;
  letter-spacing: -0.025em;
}

.auth-aside p {
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.7;
  margin: 0 0 28px;
}

.features {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.features li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.94);
}

.auth-main {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background: var(--page);
}

.auth-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-md);
  padding: 34px 32px;
}

.auth-card h2 {
  margin: 0 0 4px;
  font-size: 23px;
  letter-spacing: -0.02em;
}

.auth-card > p {
  margin: 0 0 24px;
}

.submit {
  width: 100%;
  margin-top: 4px;
}

.auth-foot {
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
  display: flex;
  gap: 6px;
  justify-content: center;
}

.link {
  color: var(--brand-1);
  font-weight: 600;
}

.demo-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.demo {
  display: flex;
  flex-direction: column;
  gap: 3px;
  text-align: left;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px dashed var(--line);
  background: #fafafa;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.demo:hover {
  border-color: var(--brand-1);
  background: var(--brand-soft);
}

.demo strong {
  font-size: 13px;
}

.demo span {
  font-size: 11px;
  color: var(--ink-500);
}

@media (max-width: 900px) {
  .auth {
    grid-template-columns: 1fr;
  }

  .auth-aside {
    display: none;
  }
}
</style>
