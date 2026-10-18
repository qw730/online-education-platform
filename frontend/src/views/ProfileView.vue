<template>
  <div class="page narrow">
    <h1 class="page-title">My profile</h1>
    <p class="page-subtitle">Update your details and your password.</p>

    <div class="card card-pad">
      <div class="identity">
        <div class="avatar-big">{{ userStore.initials }}</div>
        <div>
          <div class="name">{{ userStore.displayName }}</div>
          <div class="muted tiny">@{{ userStore.user?.username }}</div>
          <el-tag size="small" :type="userStore.isAdmin ? 'warning' : 'primary'" effect="light" round>
            {{ userStore.isAdmin ? 'Administrator' : 'Student' }}
          </el-tag>
        </div>
        <span class="spacer" />
        <div class="muted tiny">Member since {{ formatDate(userStore.user?.createdAt) }}</div>
      </div>

      <el-divider />

      <el-form :model="form" label-position="top" style="max-width: 520px">
        <el-form-item label="Full name">
          <el-input v-model="form.realName" placeholder="Your display name" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="form.email" placeholder="you@example.com" />
        </el-form-item>
        <el-form-item label="Short bio">
          <el-input v-model="form.bio" type="textarea" :rows="2"
                    placeholder="Tell us what you are learning" />
        </el-form-item>
        <el-button type="primary" :loading="saving" @click="save">Save changes</el-button>
      </el-form>
    </div>

    <div class="card card-pad" style="margin-top: 20px">
      <h2 class="section-title">Change password</h2>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules"
               label-position="top" style="max-width: 520px; margin-top: 12px">
        <el-form-item label="Current password" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="New password" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="Repeat new password" prop="confirm">
          <el-input v-model="passwordForm.confirm" type="password" show-password />
        </el-form-item>
        <el-button :loading="changing" @click="changePassword">Update password</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/format'

const userStore = useUserStore()

const saving = ref(false)
const changing = ref(false)
const passwordFormRef = ref()

const form = reactive({ realName: '', email: '', bio: '', avatar: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const passwordRules = {
  oldPassword: [{ required: true, message: 'Please enter your current password', trigger: 'blur' }],
  newPassword: [
    { required: true, message: 'Please enter a new password', trigger: 'blur' },
    { min: 6, max: 30, message: 'Password must be 6-30 characters', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: 'Please repeat the new password', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('The two passwords do not match'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function save() {
  saving.value = true
  try {
    await userStore.updateProfile({ ...form })
    ElMessage.success('Profile updated')
  } catch (error) {
    /* handled globally */
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  changing.value = true
  try {
    await authApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('Password updated, please use it next time you sign in')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirm = ''
  } catch (error) {
    /* handled globally */
  } finally {
    changing.value = false
  }
}

onMounted(async () => {
  try {
    const user = await userStore.fetchProfile()
    form.realName = user.realName || ''
    form.email = user.email || ''
    form.bio = user.bio || ''
    form.avatar = user.avatar || ''
  } catch (error) {
    /* handled globally */
  }
})
</script>

<style scoped>
.narrow {
  max-width: 820px;
}

.identity {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-big {
  width: 62px;
  height: 62px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 20px;
  font-weight: 750;
  color: #fff;
  background: linear-gradient(135deg, var(--brand-1), var(--brand-2));
}

.name {
  font-size: 18px;
  font-weight: 700;
}
</style>
