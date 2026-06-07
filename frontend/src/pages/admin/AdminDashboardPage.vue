<template>
  <div class="admin-dashboard">
    <AdminPageHeader title="관리자 대시보드" desc="Corum 시스템 현황" />
    <div class="stat-grid">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-icon" :style="{ background: s.bg, color: s.color }">
          <i :class="'ti ' + s.icon"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>
    <div class="quick-links">
      <div class="ql-title">빠른 이동</div>
      <div class="ql-grid">
        <router-link to="/admin/members"   class="ql-item"><i class="ti ti-users"></i><span>회원 관리</span></router-link>
        <router-link to="/admin/boards"    class="ql-item"><i class="ti ti-layout-list"></i><span>게시판 관리</span></router-link>
        <router-link to="/admin/menus"     class="ql-item"><i class="ti ti-menu-2"></i><span>메뉴 관리</span></router-link>
        <router-link to="/admin/inquiries" class="ql-item"><i class="ti ti-mail"></i><span>문의 관리</span></router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const stats = ref([
  { label: '전체 회원', value: '-', icon: 'ti-users',      bg: '#EFF6FF', color: '#2563EB' },
  { label: '게시판 수', value: '-', icon: 'ti-layout-list', bg: '#ECFDF5', color: '#10B981' },
  { label: '미처리 문의', value: '-', icon: 'ti-mail',     bg: '#FEF2F2', color: '#EF4444' },
  { label: '오늘 방문자', value: '-', icon: 'ti-chart-bar', bg: '#FAEEDA', color: '#F59E0B' },
])

onMounted(async () => {
  try {
    const [members, boards, inquiries] = await Promise.all([
      api.get('/admin/members?size=1'),
      api.get('/boards'),
      api.get('/inquiries?status=RECEIVED&size=1'),
    ])
    stats.value[0].value = members.data.data?.totalElements ?? '-'
    stats.value[1].value = boards.data.data?.length ?? '-'
    stats.value[2].value = inquiries.data.data?.totalElements ?? '-'
    stats.value[3].value = '-'
  } catch {}
})
</script>

<style scoped>
.admin-dashboard { display: flex; flex-direction: column; gap: 24px; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.stat-card { background: var(--surface); border: 0.5px solid var(--border2); border-radius: var(--radius); padding: 18px; display: flex; align-items: center; gap: 14px; box-shadow: var(--shadow); }
.stat-icon { width: 44px; height: 44px; border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.stat-value { font-size: 22px; font-weight: 700; color: var(--t1); letter-spacing: -0.5px; }
.stat-label { font-size: 12px; color: var(--t3); margin-top: 2px; }
.quick-links { background: var(--surface); border: 0.5px solid var(--border2); border-radius: var(--radius); padding: 18px; box-shadow: var(--shadow); }
.ql-title { font-size: 14px; font-weight: 600; color: var(--t1); margin-bottom: 14px; }
.ql-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.ql-item { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 16px; border: 0.5px solid var(--border); border-radius: var(--radius-sm); color: var(--t2); font-size: 13px; transition: var(--transition); text-decoration: none; }
.ql-item i { font-size: 22px; color: var(--accent); }
.ql-item:hover { background: var(--accent-bg); color: var(--accent); border-color: var(--accent); }
@media (max-width: 768px) { .stat-grid, .ql-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
