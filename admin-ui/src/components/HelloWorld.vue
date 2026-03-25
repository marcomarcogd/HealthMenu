<script setup>
const quickLinks = [
  { label: '查看项目文档', href: 'https://github.com/marcomarcogd/HealthMenu' },
  { label: '了解 Vue 3', href: 'https://cn.vuejs.org/' },
  { label: '了解 Vite', href: 'https://cn.vite.dev/' },
]
</script>

<template>
  <section class="intro-card">
    <h1>后台管理端开发说明</h1>
    <p class="intro-text">如果你正在本地调试这个前端，可以优先关注接口代理、权限菜单和页面构建是否正常。</p>

    <div class="tips">
      <div class="tip-item">
        <h2>建议检查</h2>
        <p>确认后端服务已启动，`/api/admin` 代理可访问。</p>
      </div>
      <div class="tip-item">
        <h2>常用命令</h2>
        <p>`npm run dev`、`npm run build`、`npm test`</p>
      </div>
      <div class="tip-item">
        <h2>相关资料</h2>
        <ul>
          <li v-for="item in quickLinks" :key="item.href">
            <a :href="item.href" target="_blank" rel="noreferrer">{{ item.label }}</a>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<style scoped>
.intro-card {
  padding: 24px;
  border: 1px solid #d8e0eb;
  border-radius: 16px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
  color: #23324d;
}

.intro-text {
  margin-top: 12px;
  color: #5f6f8c;
  line-height: 1.7;
}

.tips {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.tip-item {
  padding: 16px;
  border-radius: 12px;
  background: #f3f7fc;
}

.tip-item h2 {
  margin: 0 0 8px;
  font-size: 16px;
}

.tip-item p,
.tip-item ul {
  margin: 0;
  color: #5f6f8c;
  line-height: 1.7;
}

.tip-item ul {
  padding-left: 18px;
}

.tip-item a {
  color: #2d6cdf;
  text-decoration: none;
}
</style>
