<template>
  <div class="layout">
    <header class="topbar">
      <div class="topbar-inner">
        <router-link to="/" class="brand">
          <div class="brand-mark">OP</div>
          <div class="brand-text">
            <strong>Online Personal Education</strong>
            <span>Learn at your own pace</span>
          </div>
        </router-link>

        <nav class="nav">
          <router-link to="/" class="nav-item" :class="{ active: isActive('/') }">Home</router-link>
          <router-link to="/courses" class="nav-item" :class="{ active: isActive('/courses') }">
            Courses
          </router-link>

          <el-dropdown trigger="click" @command="onLearningCommand">
            <span class="nav-item" :class="{ active: isLearningActive }">
              My Learning
              <el-icon class="nav-caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/my/courses">My courses</el-dropdown-item>
                <el-dropdown-item command="/my/plans">Study plans</el-dropdown-item>
                <el-dropdown-item command="/my/progress">Learning progress</el-dropdown-item>
                <el-dropdown-item command="/my/attempts">Quiz history</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown v-if="userStore.isAdmin" trigger="click" @command="onAdminCommand">
            <span class="nav-item" :class="{ active: isAdminActive }">
              Admin
              <el-icon class="nav-caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/admin">Overview</el-dropdown-item>
                <el-dropdown-item command="/admin/courses">Manage courses</el-dropdown-item>
                <el-dropdown-item command="/admin/categories">Categories</el-dropdown-item>
                <el-dropdown-item command="/admin/users">Users</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </nav>

        <div class="topbar-right">
          <el-dropdown trigger="click" @command="onUserCommand">
            <span class="user-chip">
              <span class="avatar">{{ userStore.initials }}</span>
              <span class="user-name">{{ userStore.displayName }}</span>
              <el-tag v-if="userStore.isAdmin" size="small" type="warning" effect="light" round>Admin</el-tag>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">My profile</el-dropdown-item>
                <el-dropdown-item command="logout" divided>Sign out</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <footer class="footer">
      <span>Online Personal Education Platform · Vue 3 + Spring Boot + MySQL</span>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isActive = (path) => (path === '/' ? route.path === '/' : route.path.startsWith(path))

const isLearningActive = computed(() => route.path.startsWith('/my') || route.path.startsWith('/quiz'))
const isAdminActive = computed(() => route.path.startsWith('/admin'))

function onLearningCommand(path) {
  router.push(path)
}

function onAdminCommand(path) {
  router.push(path)
}

async function onUserCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('Sign out of the platform?', 'Confirm', {
        confirmButtonText: 'Sign out',
        cancelButtonText: 'Cancel',
        type: 'warning'
      })
      userStore.logout()
      ElMessage.success('You have been signed out')
      router.push('/login')
    } catch (error) {
      /* cancelled */
    }
  }
}
</script>

<style scoped>
.layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
}

.topbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 62px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  flex-shrink: 0;
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 800;
  font-size: 14px;
  letter-spacing: 0.04em;
  background: linear-gradient(135deg, var(--brand-1), var(--brand-2));
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.brand-text strong {
  font-size: 14px;
  letter-spacing: -0.01em;
}

.brand-text span {
  font-size: 11px;
  color: var(--ink-500);
}

.nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.nav-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 7px 13px;
  border-radius: 9px;
  font-size: 14px;
  color: var(--ink-700);
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
  outline: none;
}

.nav-item:hover {
  background: var(--brand-soft);
  color: var(--brand-1);
}

.nav-item.active {
  background: var(--brand-soft);
  color: var(--brand-1);
  font-weight: 600;
}

.nav-caret {
  font-size: 11px;
}

.topbar-right {
  flex-shrink: 0;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px 5px 5px;
  border-radius: 999px;
  border: 1px solid var(--line);
  cursor: pointer;
  outline: none;
  transition: border-color 0.15s ease;
}

.user-chip:hover {
  border-color: var(--brand-1);
}

.avatar {
  width: 27px;
  height: 27px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--brand-1), var(--brand-2));
}

.user-name {
  font-size: 13px;
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content {
  flex: 1;
}

.footer {
  border-top: 1px solid var(--line);
  background: #fff;
  padding: 18px 24px;
  text-align: center;
  color: var(--ink-500);
  font-size: 12px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.16s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 900px) {
  .brand-text,
  .user-name {
    display: none;
  }

  .topbar-inner {
    gap: 14px;
    padding: 0 14px;
  }
}
</style>
