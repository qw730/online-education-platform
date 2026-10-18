<template>
  <div class="nf">
    <div class="nf-inner">
      <div class="code">404</div>
      <h1>Page not found</h1>
      <p class="muted">The page you are looking for does not exist or has been moved.</p>
      <el-button type="primary" size="large" @click="$router.push('/')">Back to home</el-button>
    </div>
  </div>
</template>

<script setup>
// Simple 404 view.
</script>

<style scoped>
.nf {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: var(--page);
}

.nf-inner {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.code {
  font-size: 76px;
  font-weight: 850;
  letter-spacing: -0.04em;
  background: linear-gradient(135deg, var(--brand-1), var(--brand-2));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.nf-inner h1 {
  margin: 0;
  font-size: 22px;
}

.nf-inner p {
  margin: 0 0 12px;
}
</style>
