<template>
  <div class="adm-page">
    <AdminPageHeader title="관리자 메뉴 권한" desc="운영 그룹별 관리자 패널 메뉴 접근 권한 설정">
      <button class="adm-btn primary" :disabled="saving" @click="saveAll">
        <i v-if="saving" class="ti ti-loader-2 spinning"></i>
        {{ saving ? '저장 중...' : '저장' }}
      </button>
    </AdminPageHeader>

    <div class="adm-card perm-card" v-loading="loading">
      <div class="perm-info">
        <i class="ti ti-info-circle"></i>
        <div>
          <strong>최고관리자(시스템 고정)</strong>는 항상 모든 메뉴에 접근 가능합니다.<br>
          아래에서는 그 외 운영 그룹에 대해 조회/편집 권한을 설정합니다.<br>
          <span class="perm-tip">조회 ✓ = 해당 메뉴 항목이 사이드바에 표시됩니다. 편집 ✓ = 해당 메뉴에서 수정 작업이 허용됩니다.</span>
        </div>
      </div>

      <div class="perm-table-wrap" v-if="groups.length && menus.length">
        <table class="perm-table">
          <thead>
            <tr>
              <th class="menu-col">메뉴 항목</th>
              <template v-for="g in groups" :key="g.id">
                <th class="group-header" colspan="2">
                  <div class="gh-label">{{ g.label }}</div>
                  <div class="gh-sub">
                    <span>조회</span>
                    <span>편집</span>
                  </div>
                </th>
              </template>
            </tr>
          </thead>
          <tbody>
            <template v-for="section in menus" :key="section.id">
              <!-- 섹션 헤더 행 -->
              <tr class="section-row">
                <td class="section-name">
                  <i :class="section.icon" style="margin-right:5px"></i>
                  {{ section.name }}
                </td>
                <template v-for="g in groups" :key="g.id">
                  <td class="perm-cell" colspan="2">
                    <!-- 섹션 전체 선택 토글 -->
                    <el-checkbox
                      :model-value="isSectionAllView(section, g.id)"
                      :indeterminate="isSectionPartial(section, g.id)"
                      @change="toggleSection(section, g.id, $event)"
                    />
                  </td>
                </template>
              </tr>
              <!-- 자식 메뉴 행들 -->
              <tr v-for="child in (section.children || [])" :key="child.id" class="child-row">
                <td class="child-name">
                  <span class="child-indent"></span>
                  <i :class="child.icon" style="margin-right:4px"></i>
                  {{ child.name }}
                </td>
                <template v-for="g in groups" :key="g.id">
                  <td class="perm-cell">
                    <el-checkbox v-model="child.groupPerms[g.id].canView" />
                  </td>
                  <td class="perm-cell">
                    <el-checkbox
                      v-model="child.groupPerms[g.id].canEdit"
                      :disabled="!child.groupPerms[g.id].canView"
                    />
                  </td>
                </template>
              </tr>
              <!-- 자식 없는 단독 메뉴 (대시보드 등) -->
              <tr v-if="!section.children?.length" class="child-row">
                <td class="child-name">
                  <span class="child-indent"></span>—
                </td>
                <template v-for="g in groups" :key="g.id">
                  <td class="perm-cell">
                    <el-checkbox v-model="section.groupPerms[g.id].canView" />
                  </td>
                  <td class="perm-cell">
                    <el-checkbox
                      v-model="section.groupPerms[g.id].canEdit"
                      :disabled="!section.groupPerms[g.id].canView"
                    />
                  </td>
                </template>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div v-else-if="!loading" class="at-empty">
        <i class="ti ti-lock"></i>
        <span>설정 가능한 그룹이 없습니다. 운영 그룹 하위에 그룹을 추가해주세요.</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const loading = ref(false)
const saving  = ref(false)
const menus   = ref([])
const groups  = ref([])

async function fetchData() {
  loading.value = true
  try {
    const res = await api.get('/admin/admin-menus')
    menus.value  = res.data.data?.menus  || []
    groups.value = res.data.data?.groups || []
  } finally {
    loading.value = false
  }
}

function isSectionAllView(section, groupId) {
  const children = section.children || []
  if (!children.length) return section.groupPerms?.[groupId]?.canView || false
  return children.every(c => c.groupPerms?.[groupId]?.canView)
}

function isSectionPartial(section, groupId) {
  const children = section.children || []
  if (!children.length) return false
  const views = children.map(c => c.groupPerms?.[groupId]?.canView || false)
  return views.some(Boolean) && !views.every(Boolean)
}

function toggleSection(section, groupId, val) {
  (section.children || []).forEach(c => {
    if (c.groupPerms?.[groupId]) {
      c.groupPerms[groupId].canView = val
      if (!val) c.groupPerms[groupId].canEdit = false
    }
  })
}

async function saveAll() {
  saving.value = true
  const payload = []

  function collectRows(menuItem) {
    if (!menuItem.groupPerms) return
    for (const [groupId, perm] of Object.entries(menuItem.groupPerms)) {
      payload.push({
        menuId:  menuItem.id,
        groupId: Number(groupId),
        canView: perm.canView,
        canEdit: perm.canEdit,
      })
    }
    ;(menuItem.children || []).forEach(collectRows)
  }

  menus.value.forEach(collectRows)

  try {
    await api.put('/admin/admin-menus/permissions', payload)
    ElMessage.success('권한이 저장되었습니다.')
  } finally {
    saving.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
@import '@/assets/admin-table.css';

.perm-card { padding: 0; overflow: hidden; }

.perm-info {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  background: color-mix(in srgb, var(--accent) 7%, transparent);
  border-bottom: 0.5px solid var(--border);
  padding: 14px 20px;
  font-size: 13px;
  color: var(--t2);
  line-height: 1.6;
}
.perm-info i  { color: var(--accent); font-size: 18px; flex-shrink: 0; margin-top: 1px; }
.perm-tip     { color: var(--t3); font-size: 12px; }

.perm-table-wrap { overflow-x: auto; padding: 0; }
.perm-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.perm-table th {
  background: var(--surface2);
  border-bottom: 1px solid var(--border);
  border-right: 0.5px solid var(--border);
  padding: 0;
}
.menu-col { width: 200px; padding: 10px 16px; text-align: left; }

.group-header { text-align: center; min-width: 110px; }
.gh-label { padding: 7px 4px 2px; font-size: 12px; font-weight: 700; color: var(--t2); border-bottom: 0.5px solid var(--border); }
.gh-sub { display: flex; justify-content: space-around; padding: 4px 0 5px; font-size: 11px; color: var(--t3); font-weight: 600; }

/* 섹션 행 */
.section-row { background: var(--surface2); border-bottom: 0.5px solid var(--border); }
.section-name {
  padding: 8px 16px;
  font-weight: 800;
  font-size: 12px;
  color: var(--t1);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-right: 0.5px solid var(--border);
}

/* 자식 행 */
.child-row { border-bottom: 0.5px solid var(--border2); }
.child-row:hover { background: var(--surface2); }
.child-name {
  padding: 8px 16px;
  color: var(--t1);
  font-size: 13px;
  border-right: 0.5px solid var(--border);
  display: flex;
  align-items: center;
}
.child-indent { display: inline-block; width: 20px; }

.perm-cell {
  text-align: center;
  padding: 8px 4px;
  border-right: 0.5px solid var(--border2);
  width: 55px;
}

@keyframes spin { to { transform: rotate(360deg); } }
.spinning { animation: spin 0.7s linear infinite; display: inline-block; }
</style>
