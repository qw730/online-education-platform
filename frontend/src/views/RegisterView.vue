<template>
  <div class="auth">
    <aside class="auth-aside">
      <div class="aside-inner">
        <div class="brand-mark">OP</div>
        <h1>Start your learning journey</h1>
        <p>
          Creating an account takes a few seconds. You will be able to enrol in any published
          course right away.
        </p>
        <ul class="features">
          <li><el-icon><Check /></el-icon> Free student account</li>
          <li><el-icon><Check /></el-icon> Track progress chapter by chapter</li>
          <li><el-icon><Check /></el-icon> Take quizzes and see your score instantly</li>
        </ul>
      </div>
    </aside>

    <main class="auth-main">
      <div class="auth-card">
        <h2>Create your account</h2>
        <p class="muted">All new accounts are student accounts.</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="Username" prop="username">
            <el-input v-model="form.username" size="large" placeholder="At least 3 characters"
                      :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item label="Full name" prop="realName">
            <el-input v-model="form.realName" size="large" placeholder="How should we call you?"
                      :prefix-icon="Postcard" clearable />
          </el-form-item>
          <el-form-item label="Email" prop="email">
            <el-input v-model="form.email" size="large" placeholder="optional@example.com"
                      :prefix-icon="Message" clearable />
          </el-form-item>
          <el-form-item label="Password" prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password
                      placeholder="At least 6 characters" :prefix-icon="Lock" />
          </el-form-item>
          <el-form-item label="Confirm password" prop="confirm">
            <el-input v-model="form.confirm" size="large" type="password" show-password
                      placeholder="Repeat the password" :prefix-icon="Lock" @keyup.enter="submit" />
          </el-form-item>

          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
            Create account
          </el-button>
        </el-form>

        <div class="auth-foot">
          <span class="muted">Already registered?</span>
          <router-link to="/login" class="link">Sign in</router-link>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, Postcard, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', realName: '', email: '', password: '', confirm: '' })

const rules = {
  username: [
    { required: true, message: 'Please choose a username', trigger: 'blur' },
    { min: 3, max: 30, message: 'Username must be 3-30 characters', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_]+$/,
      message: 'Only letters, digits and underscore are allowed',
      trigger: 'blur'
    }
  ],
  email: [{ type: 'email', message: 'Please enter a valid email or leave it empty', trigger: 'blur' }],
  password: [
    { required: true, message: 'Please choose a password', trigger: 'blur' },
    { min: 6, max: 30, message: 'Password must be 6-30 characters', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: 'Please repeat the password', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('The two passwords do not match'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      realName: form.realName,
      email: form.email || null
    })
    ElMessage.success('Account created, signing you in ...')
    await userStore.login({ username: form.username, password: form.password })
    router.push('/courses')
  } catch (error) {
    /* handled by the interceptor */
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
  background: linear-gradient(150deg, #0f766e 0%, #4f46e5 60%, #7c3aed 100%);
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
  font-size: 32px;
  line-height: 1.24;
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
  max-width: 420px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: var(--shadow-md);
  padding: 32px;
}

.auth-card h2 {
  margin: 0 0 4px;
  font-size: 22px;
  letter-spacing: -0.02em;
}

.auth-card > p {
  margin: 0 0 20px;
}

.submit {
  width: 100%;
}

.auth-foot {
  margin-top: 16px;
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

@media (max-width: 900px) {
  .auth {
    grid-template-columns: 1fr;
  }

  .auth-aside {
    display: none;
  }
}
</style>
