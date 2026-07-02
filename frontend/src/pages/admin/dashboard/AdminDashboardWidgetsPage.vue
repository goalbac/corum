<template>
  <div class="adm-page">
    <AdminPageHeader title="대시보드 관리" desc="대시보드를 선택하고 위젯을 관리하세요">
      <div style="display:flex;gap:8px;align-items:center">
        <transition name="fade">
          <button v-if="sortChanged" class="adm-btn ghost" :disabled="sortSaving" @click="saveSortOrder">
            <i :class="['ti', sortSaving ? 'ti-loader-2 spinning' : 'ti-check']"></i>
            {{ sortSaving ? '저장 중...' : '순서 저장' }}
          </button>
        </transition>
        <button class="adm-btn primary" @click="openCreate"><i class="ti ti-plus"></i> 위젯 추가</button>
      </div>
    </AdminPageHeader>

    <!-- 대시보드 선택 -->
    <div class="db-selector">
      <button
        v-for="db in dashboards"
        :key="db.isHome ? 'home' : db.menuId"
        :class="['db-tab', { active: selectedMenuId === (db.menuId ?? null) }]"
        @click="selectDashboard(db)"
      >
        <i :class="['ti', db.isHome ? 'ti-home' : 'ti-layout-dashboard']"></i>
        <span class="db-tab-name">{{ db.menuName }}</span>
        <span v-if="!db.isHome" class="db-tab-path">{{ db.menuPath }}</span>
        <span class="db-tab-count">{{ db.widgetCount }}</span>
      </button>
    </div>

    <div v-if="!loading && widgets.length" ref="widgetSortable" class="widget-list">
      <div
        v-for="w in widgets"
        :key="w.id"
        :data-id="w.id"
        :class="['wcard', { 'is-inactive': !w.isActive }]"
      >
        <div class="wcard-top">
          <i class="ti ti-grip-vertical drag-handle" title="드래그해서 순서 변경"></i>
          <span class="wtype-badge">{{ typeLabel(w.widgetType) }}</span>
          <div class="wtitle-wrap">
            <span class="wtitle">{{ w.title || '(제목 없음)' }}</span>
            <span v-if="w.description" class="wdesc">{{ w.description }}</span>
          </div>
          <div class="wcard-meta">
            <span
              v-if="hasSizePicker(w.widgetType)"
              class="wsize-indicator"
              :title="getSize(w) === 'full' ? '1칸 (전체 너비)' : '반칸 (절반 너비)'"
            >
              <span :class="['wsize-block', 'block-l', getSize(w) === 'full' ? 'active' : '']"></span>
              <span :class="['wsize-block', 'block-r', getSize(w) === 'full' ? 'active' : '']"></span>
              <span class="wsize-label">{{ getSize(w) === 'full' ? '1칸' : '반칸' }}</span>
            </span>
            <span v-if="!w.isActive" class="wbadge-inactive">비활성</span>
          </div>
          <div class="wcard-actions">
            <button class="act-btn" @click="openEdit(w)"><i class="ti ti-edit"></i> 수정</button>
            <button class="act-btn danger" @click="deleteWidget(w.id)"><i class="ti ti-trash"></i></button>
          </div>
        </div>

        <div class="wcard-preview">
          <!-- RECENT_POSTS / RECENT_GALLERY -->
          <template v-if="w.widgetType === 'RECENT_POSTS' || w.widgetType === 'RECENT_GALLERY'">
            <div class="wp-post-list">
              <div v-for="post in (w.posts || []).slice(0,4)" :key="post.id" class="wp-post-row">
                <span class="wp-post-title">{{ post.title }}</span>
                <span class="wp-post-date">{{ formatDate(post.createdAt) }}</span>
              </div>
              <div v-if="!(w.posts||[]).length" class="wp-empty">
                <i :class="w.widgetType === 'RECENT_GALLERY' ? 'ti ti-photo' : 'ti ti-file-text'"></i>
                {{ w.targetBoardName ? w.targetBoardName + ' 게시판' : '전체 게시판' }} 최신 글 표시
              </div>
            </div>
          </template>

          <!-- IMAGE_SLIDER -->
          <template v-else-if="w.widgetType === 'IMAGE_SLIDER'">
            <div class="wp-slides">
              <template v-if="parseConfig(w).slides?.length">
                <div
                  v-for="(s,i) in parseConfig(w).slides.slice(0,4)"
                  :key="i"
                  class="wp-slide"
                  :style="s.imageUrl ? `background-image:url(${s.imageUrl})` : ''"
                >
                  <span v-if="!s.imageUrl">{{ s.title || `슬라이드 ${i+1}` }}</span>
                </div>
              </template>
              <div v-else class="wp-empty"><i class="ti ti-photo"></i> 슬라이드 없음</div>
            </div>
          </template>

          <!-- LINK_LIST -->
          <template v-else-if="w.widgetType === 'LINK_LIST'">
            <div class="wp-links">
              <template v-if="parseConfig(w).links?.length">
                <span v-for="(l,i) in parseConfig(w).links.slice(0,8)" :key="i" class="wp-link-chip">{{ l.label || l.url }}</span>
              </template>
              <div v-else class="wp-empty"><i class="ti ti-link"></i> 링크 없음</div>
            </div>
          </template>

          <!-- QUICK_LINKS -->
          <template v-else-if="w.widgetType === 'QUICK_LINKS'">
            <div class="wp-links">
              <template v-if="parseConfig(w).links?.length">
                <span v-for="(l,i) in parseConfig(w).links.slice(0,8)" :key="i" class="wp-link-chip">
                  <i v-if="l.icon" :class="'ti ' + l.icon" style="margin-right:4px"></i>{{ l.label || l.url }}
                </span>
              </template>
              <div v-else class="wp-empty"><i class="ti ti-home"></i> 바로가기 없음</div>
            </div>
          </template>

          <!-- CALENDAR_WEEKLY -->
          <template v-else-if="w.widgetType === 'CALENDAR_WEEKLY'">
            <div class="wp-empty" style="gap:6px">
              <i class="ti ti-calendar-week"></i>
              {{ parseConfig(w).calendarId ? (calendarName(parseConfig(w).calendarId)) : '모든 캘린더' }} 이번 주 일정 표시
            </div>
          </template>

          <!-- CALENDAR_MONTHLY -->
          <template v-else-if="w.widgetType === 'CALENDAR_MONTHLY'">
            <div class="wp-empty" style="gap:6px">
              <i class="ti ti-calendar-month"></i>
              {{ parseConfig(w).calendarId ? (calendarName(parseConfig(w).calendarId)) : '모든 캘린더' }} 월간 캘린더 표시
            </div>
          </template>

          <!-- IMAGE_GRID -->
          <template v-else-if="w.widgetType === 'IMAGE_GRID'">
            <div class="wp-slides">
              <template v-if="parseConfig(w).images?.length">
                <div
                  v-for="(img,i) in parseConfig(w).images.slice(0,4)"
                  :key="i"
                  class="wp-slide"
                  :style="img.imageUrl ? `background-image:url(${img.imageUrl})` : ''"
                >
                  <span v-if="!img.imageUrl">{{ img.title || `이미지 ${i+1}` }}</span>
                </div>
              </template>
              <div v-else class="wp-empty"><i class="ti ti-layout-grid"></i> 이미지 없음</div>
            </div>
          </template>

          <!-- MEMBER_STATS -->
          <template v-else-if="w.widgetType === 'MEMBER_STATS'">
            <div class="wp-stats">
              <div class="wp-stat"><span class="wp-stat-n">—</span><span class="wp-stat-l">전체 회원</span></div>
              <div class="wp-stat"><span class="wp-stat-n">—</span><span class="wp-stat-l">게시판</span></div>
              <div class="wp-stat"><span class="wp-stat-n">—</span><span class="wp-stat-l">미처리 문의</span></div>
            </div>
          </template>

          <!-- VISIT_STATS -->
          <template v-else-if="w.widgetType === 'VISIT_STATS'">
            <div class="wp-stats">
              <div class="wp-stat"><span class="wp-stat-n">—</span><span class="wp-stat-l">전체 방문</span></div>
              <div class="wp-stat"><span class="wp-stat-n">—</span><span class="wp-stat-l">순 방문자</span></div>
              <div class="wp-stat"><span class="wp-stat-n">—</span><span class="wp-stat-l">로그인</span></div>
            </div>
          </template>

          <!-- WELCOME -->
          <template v-else-if="w.widgetType === 'WELCOME'">
            <div class="wp-welcome">
              <div class="wp-welcome-name">안녕하세요, <span class="wp-welcome-hl">{{ '홍길동' }}</span>님</div>
              <div class="wp-welcome-sub">{{ parseConfig(w).subText || '오늘도 좋은 하루 보내세요 ☀️' }}</div>
              <div v-if="parseConfig(w).showClock !== false" class="wp-welcome-time">00:00</div>
            </div>
          </template>

          <!-- DIVIDER -->
          <template v-else-if="w.widgetType === 'DIVIDER'">
            <div class="wp-divider">
              <span v-if="w.title" class="wp-divider-label">{{ w.title }}</span>
              <hr v-else class="wp-divider-line" />
            </div>
          </template>

          <!-- CUSTOM -->
          <template v-else-if="w.widgetType === 'CUSTOM'">
            <div class="wp-custom" v-html="sanitizeHtml(parseConfig(w).content) || ''" />
            <div v-if="!parseConfig(w).content" class="wp-empty"><i class="ti ti-pencil"></i> 본문 없음</div>
          </template>
        </div>
      </div>
    </div>

    <div v-else-if="loading" class="adm-card" style="padding:60px;text-align:center;color:var(--t3)">
      <i class="ti ti-loader-2 spinning" style="font-size:28px"></i>
    </div>
    <div v-else class="adm-card" style="padding:60px;text-align:center;color:var(--t3)">
      <i class="ti ti-layout-dashboard" style="font-size:40px;display:block;margin-bottom:12px"></i>
      등록된 위젯이 없습니다. 위젯을 추가해보세요.
    </div>

    <!-- 위젯 폼 다이얼로그 -->
    <el-dialog
      v-model="showForm"
      :title="editing ? '위젯 수정' : '위젯 추가'"
      :width="form.widgetType === 'CUSTOM' ? '780px' : '560px'"
      destroy-on-close
    >
      <div class="dlg-form">
        <div class="dlg-row">
          <div class="dlg-field">
            <label>위젯 유형</label>
            <el-select v-model="form.widgetType" style="width:100%" @change="onTypeChange">
              <el-option-group label="기본">
                <el-option value="WELCOME"         label="웰컴 카드 (안녕하세요, {이름}님)" />
              </el-option-group>
              <el-option-group label="게시판">
                <el-option value="RECENT_POSTS"    label="최신 글 (텍스트 목록)" />
                <el-option value="RECENT_GALLERY"  label="최신 글 (갤러리/웹진)" />
              </el-option-group>
              <el-option-group label="캘린더">
                <el-option value="CALENDAR_WEEKLY"  label="캘린더 (주간)" />
                <el-option value="CALENDAR_MONTHLY" label="캘린더 (월간)" />
              </el-option-group>
              <el-option-group label="콘텐츠">
                <el-option value="IMAGE_SLIDER"    label="이미지 슬라이더" />
                <el-option value="IMAGE_GRID"      label="이미지 그리드 (4칸)" />
                <el-option value="LINK_LIST"       label="링크 목록" />
                <el-option value="QUICK_LINKS"     label="바로가기" />
                <el-option value="CUSTOM"          label="커스텀" />
              </el-option-group>
              <el-option-group label="통계">
                <el-option value="MEMBER_STATS"    label="회원 현황" />
                <el-option value="VISIT_STATS"     label="접속 통계" />
              </el-option-group>
              <el-option-group label="레이아웃">
                <el-option value="DIVIDER"         label="구분선" />
              </el-option-group>
            </el-select>
          </div>
          <div class="dlg-field">
            <label>제목</label>
            <el-input v-model="form.title" />
          </div>
        </div>

        <!-- 설명 (DIVIDER 제외) -->
        <div v-if="form.widgetType !== 'DIVIDER'" class="dlg-row" style="margin-top:-4px">
          <div class="dlg-field" style="flex:1">
            <label>설명 <span style="color:var(--t4);font-weight:400">(선택 · 위젯 제목 아래 표시)</span></label>
            <el-input v-model="form.description" placeholder="위젯에 대한 간단한 설명" />
          </div>
        </div>

        <!-- 크기 선택 -->
        <div v-if="hasSizePicker(form.widgetType)" class="dlg-row">
          <div class="dlg-field">
            <label>위젯 크기</label>
            <div class="size-picker">
              <button type="button" :class="['size-opt', config.size === 'full' ? 'active' : '']" @click="config.size = 'full'">
                <div class="size-preview full-preview"><div class="sp-block"></div></div>
                <span>1칸 (전체 너비)</span>
              </button>
              <button type="button" :class="['size-opt', config.size === 'half' ? 'active' : '']" @click="config.size = 'half'">
                <div class="size-preview half-preview"><div class="sp-block"></div><div class="sp-empty"></div></div>
                <span>반칸 (절반 너비)</span>
              </button>
            </div>
          </div>
          <div class="dlg-field">
            <label>활성화</label>
            <div style="padding-top:6px"><label class="chk-item"><el-checkbox v-model="form.isActive" />활성화</label></div>
          </div>
        </div>

        <div v-if="!hasSizePicker(form.widgetType)" style="margin-bottom:8px">
          <label class="chk-item"><el-checkbox v-model="form.isActive" />활성화</label>
        </div>

        <!-- RECENT_POSTS / RECENT_GALLERY -->
        <template v-if="form.widgetType === 'RECENT_POSTS' || form.widgetType === 'RECENT_GALLERY'">
          <hr class="dlg-divider"/>
          <div class="dlg-row">
            <div class="dlg-field">
              <label>대상 게시판</label>
              <el-select v-model="form.targetBoardId" clearable placeholder="전체" style="width:100%">
                <el-option v-for="b in boards" :key="b.id" :value="b.id" :label="boardLabel(b)" />
              </el-select>
            </div>
            <div class="dlg-field">
              <label>표시 개수</label>
              <el-input-number v-model="form.postCount" :min="1" :max="20" style="width:100%" />
            </div>
          </div>
        </template>

        <!-- CALENDAR_WEEKLY / CALENDAR_MONTHLY 공통 설정 -->
        <template v-if="form.widgetType === 'CALENDAR_WEEKLY' || form.widgetType === 'CALENDAR_MONTHLY'">
          <hr class="dlg-divider"/>
          <div class="dlg-row">
            <div class="dlg-field">
              <label>대상 캘린더</label>
              <el-select v-model="config.calendarId" clearable placeholder="모든 캘린더" style="width:100%">
                <el-option v-for="c in calendars.filter(c => c.calendarType !== 'HOLIDAY')" :key="c.id" :value="c.id" :label="c.name">
                  <span style="display:flex;align-items:center;gap:8px">
                    <span v-if="c.color" :style="{ display:'inline-block', width:'12px', height:'12px', borderRadius:'50%', background:c.color }"></span>
                    {{ c.name }}
                  </span>
                </el-option>
              </el-select>
              <div v-if="form.widgetType === 'CALENDAR_MONTHLY'" style="font-size:12px;color:var(--t3);margin-top:4px">
                <i class="ti ti-info-circle" style="margin-right:3px"></i>월간 위젯은 대한민국 공휴일을 항상 함께 표시합니다.
              </div>
            </div>
          </div>
        </template>

        <!-- IMAGE_SLIDER -->
        <template v-if="form.widgetType === 'IMAGE_SLIDER'">
          <hr class="dlg-divider"/>
          <div class="dlg-section-title">슬라이드</div>
          <div ref="slidesSortableEl">
            <div v-for="(s, i) in config.slides" :key="i" class="sub-item">
              <div class="sub-item-header">
                <i class="ti ti-grip-vertical sub-drag-handle" title="드래그해서 순서 변경"></i>
                <span class="sub-item-label">슬라이드 {{ i + 1 }}</span>
                <button class="act-btn danger" @click="config.slides.splice(i,1)"><i class="ti ti-trash"></i></button>
              </div>
              <div class="img-upload-row">
                <div
                  class="img-upload-preview"
                  :style="s.imageUrl ? `background-image:url(${s.imageUrl})` : ''"
                  @click="triggerSliderUpload(i)"
                  :title="s.imageUrl ? '클릭해서 이미지 변경' : '클릭해서 이미지 업로드'"
                >
                  <template v-if="sliderUploading[i]">
                    <i class="ti ti-loader-2 spinning" style="font-size:22px;color:#fff"></i>
                  </template>
                  <template v-else-if="!s.imageUrl">
                    <i class="ti ti-cloud-upload" style="font-size:28px;color:var(--t4)"></i>
                    <span style="font-size:12px;color:var(--t4);margin-top:4px">클릭해서 업로드</span>
                  </template>
                  <div v-if="s.imageUrl && !sliderUploading[i]" class="img-upload-change-overlay">
                    <i class="ti ti-refresh"></i> 변경
                  </div>
                </div>
                <input
                  :ref="el => sliderInputRefs[i] = el"
                  type="file"
                  accept="image/*"
                  style="display:none"
                  @change="e => uploadSliderImage(e, i)"
                />
                <div class="img-upload-fields">
                  <div class="dlg-field"><label>제목 (선택)</label><el-input v-model="s.title" /></div>
                  <div class="dlg-field"><label>링크 URL (선택)</label><el-input v-model="s.url" placeholder="https:// 또는 /path" /></div>
                  <label class="chk-item" style="margin-top:4px"><el-checkbox v-model="s.newWindow" />새 창</label>
                </div>
              </div>
            </div>
          </div>
          <button class="adm-btn ghost" style="width:100%" @click="config.slides.push({title:'',imageUrl:'',url:'',newWindow:false})">
            <i class="ti ti-plus"></i> 슬라이드 추가
          </button>
        </template>

        <!-- IMAGE_GRID -->
        <template v-if="form.widgetType === 'IMAGE_GRID'">
          <hr class="dlg-divider"/>
          <div class="dlg-section-title">이미지 (최대 4개)</div>
          <div ref="imagesSortableEl">
            <div v-for="(img, i) in config.images" :key="i" class="sub-item">
              <div class="sub-item-header">
                <i class="ti ti-grip-vertical sub-drag-handle" title="드래그해서 순서 변경"></i>
                <span class="sub-item-label">이미지 {{ i + 1 }}</span>
                <button class="act-btn danger" @click="config.images.splice(i,1)"><i class="ti ti-trash"></i></button>
              </div>
              <div class="img-upload-row">
                <div
                  class="img-upload-preview"
                  :style="img.imageUrl ? `background-image:url(${img.imageUrl})` : ''"
                  @click="triggerImgUpload(i)"
                  :title="img.imageUrl ? '클릭해서 이미지 변경' : '클릭해서 이미지 업로드'"
                >
                  <template v-if="imgUploading[i]">
                    <i class="ti ti-loader-2 spinning" style="font-size:22px;color:#fff"></i>
                  </template>
                  <template v-else-if="!img.imageUrl">
                    <i class="ti ti-cloud-upload" style="font-size:28px;color:var(--t4)"></i>
                    <span style="font-size:12px;color:var(--t4);margin-top:4px">클릭해서 업로드</span>
                  </template>
                  <div v-if="img.imageUrl && !imgUploading[i]" class="img-upload-change-overlay">
                    <i class="ti ti-refresh"></i> 변경
                  </div>
                </div>
                <input
                  :ref="el => imgInputRefs[i] = el"
                  type="file"
                  accept="image/*"
                  style="display:none"
                  @change="e => uploadGridImage(e, i)"
                />
                <div class="img-upload-fields">
                  <div class="dlg-field"><label>제목 (선택)</label><el-input v-model="img.title" /></div>
                  <div class="dlg-field"><label>설명 (선택)</label><el-input v-model="img.desc" /></div>
                  <div class="dlg-field"><label>링크 URL (선택)</label><el-input v-model="img.linkUrl" placeholder="https:// 또는 /path" /></div>
                  <label class="chk-item" style="margin-top:4px"><el-checkbox v-model="img.newWindow" />새 창</label>
                </div>
              </div>
            </div>
          </div>
          <button v-if="config.images.length < 4" class="adm-btn ghost" style="width:100%"
                  @click="config.images.push({imageUrl:'',title:'',desc:'',linkUrl:'',newWindow:false})">
            <i class="ti ti-plus"></i> 이미지 추가 ({{ config.images.length }}/4)
          </button>
        </template>

        <!-- LINK_LIST -->
        <template v-if="form.widgetType === 'LINK_LIST'">
          <hr class="dlg-divider"/>
          <div class="dlg-section-title">링크</div>
          <div ref="linksSortableEl">
            <div v-for="(l, i) in config.links" :key="i" class="sub-item">
              <div class="sub-item-header">
                <i class="ti ti-grip-vertical sub-drag-handle" title="드래그해서 순서 변경"></i>
                <span class="sub-item-label">링크 {{ i + 1 }}</span>
                <button class="act-btn danger" @click="config.links.splice(i,1)"><i class="ti ti-trash"></i></button>
              </div>
              <div class="dlg-row">
                <div class="dlg-field"><label>레이블</label><el-input v-model="l.label" /></div>
                <div class="dlg-field"><label>URL</label><el-input v-model="l.url" /></div>
              </div>
              <label class="chk-item"><el-checkbox v-model="l.newWindow" />새 창</label>
            </div>
          </div>
          <button class="adm-btn ghost" style="width:100%" @click="config.links.push({label:'',url:'',newWindow:false})">
            <i class="ti ti-plus"></i> 링크 추가
          </button>
        </template>

        <!-- QUICK_LINKS -->
        <template v-if="form.widgetType === 'QUICK_LINKS'">
          <hr class="dlg-divider"/>
          <div class="dlg-section-title">바로가기 항목</div>
          <div v-for="(l, i) in config.links" :key="i" class="sub-item">
            <div class="dlg-row">
              <div class="dlg-field"><label>레이블</label><el-input v-model="l.label" /></div>
              <div class="dlg-field">
                <label>아이콘 <a href="https://tabler.io/icons" target="_blank" style="font-size:11px;color:var(--accent-t);margin-left:4px">아이콘 목록 →</a></label>
                <div style="display:flex;gap:8px;align-items:center">
                  <el-input v-model="l.icon" placeholder="ti-home" style="flex:1" />
                  <span style="font-size:22px;width:32px;text-align:center;color:var(--accent-t)">
                    <i v-if="l.icon" :class="'ti ' + l.icon"></i>
                  </span>
                </div>
              </div>
            </div>
            <div class="dlg-row">
              <div class="dlg-field"><label>URL</label><el-input v-model="l.url" placeholder="/board/1 또는 https://..." /></div>
              <div class="dlg-field" style="flex-direction:row;align-items:flex-end;gap:10px;justify-content:space-between;padding-bottom:2px">
                <label class="chk-item"><el-checkbox v-model="l.newWindow" />새 창</label>
                <button class="act-btn danger" @click="config.links.splice(i,1)"><i class="ti ti-trash"></i></button>
              </div>
            </div>
          </div>
          <button class="adm-btn ghost" style="width:100%" @click="config.links.push({label:'',icon:'ti-link',url:'',newWindow:false})">
            <i class="ti ti-plus"></i> 항목 추가
          </button>
        </template>

        <!-- WELCOME -->
        <template v-if="form.widgetType === 'WELCOME'">
          <hr class="dlg-divider"/>
          <div class="dlg-row">
            <div class="dlg-field" style="flex:1">
              <label>부제목 <span style="color:var(--t4);font-weight:400">(이름 아래에 표시)</span></label>
              <el-input v-model="config.subText" placeholder="오늘도 좋은 하루 보내세요 ☀️" />
            </div>
            <div class="dlg-field" style="flex:0 0 auto;justify-content:flex-end;padding-top:22px">
              <label class="chk-item"><el-checkbox v-model="config.showClock" />날짜/시각 표시</label>
            </div>
          </div>
        </template>

        <!-- CUSTOM -->
        <template v-if="form.widgetType === 'CUSTOM'">
          <hr class="dlg-divider"/>
          <div class="dlg-field">
            <label>더보기 URL <span style="font-weight:400;color:var(--adm-muted);font-size:11px">입력 시 위젯 우상단에 더보기 링크 표시</span></label>
            <el-input v-model="config.moreUrl" placeholder="예) /menu/12 또는 https://example.com" clearable />
          </div>
          <div class="dlg-field">
            <label>본문 내용</label>
            <RichEditor v-model="config.content" placeholder="위젯에 표시될 내용을 입력하세요." />
          </div>
        </template>
      </div>

      <template #footer>
        <button class="adm-btn ghost" @click="showForm = false">취소</button>
        <button class="adm-btn primary" :disabled="saving" @click="saveWidget">
          <i v-if="saving" class="ti ti-loader-2 spinning"></i>{{ saving ? '저장 중...' : '저장' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Sortable from 'sortablejs'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import RichEditor from '@/components/common/RichEditor.vue'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'
import { sanitizeHtml } from '@/utils/sanitize'

const menuStore = useMenuStore()

const dashboards  = ref([])   // DashboardInfoResponse[]
const selectedMenuId = ref(null)   // null = 홈, number = 메뉴 대시보드
const widgets     = ref([])
const boards      = ref([])
const calendars   = ref([])
const loading     = ref(false)
const saving      = ref(false)
const imgInputRefs    = ref([])
const imgUploading    = ref({})
const sliderInputRefs = ref([])
const sliderUploading = ref({})
const sortSaving  = ref(false)
const sortChanged = ref(false)
const showForm    = ref(false)
const editing     = ref(null)
const widgetSortable    = ref(null)
const slidesSortableEl  = ref(null)
const imagesSortableEl  = ref(null)
const linksSortableEl   = ref(null)
let sortableInstance   = null
let slidesSortable     = null
let imagesSortable     = null
let linksSortable      = null

// 크기 선택이 있는 위젯 유형
const SIZE_PICKER_TYPES = ['RECENT_POSTS', 'RECENT_GALLERY', 'CALENDAR_WEEKLY', 'CALENDAR_MONTHLY', 'LINK_LIST', 'QUICK_LINKS', 'CUSTOM']
function hasSizePicker(type) { return SIZE_PICKER_TYPES.includes(type) }

const defaultForm = () => ({
  widgetType: 'RECENT_POSTS', title: '', description: '',
  targetBoardId: null, postCount: 5, isActive: true,
  menuId: selectedMenuId.value
})
const form   = ref(defaultForm())
const config = ref({ slides: [], links: [], images: [], content: '', size: 'half', calendarId: null, subText: '오늘도 좋은 하루 보내세요 ☀️', showClock: true })

function parseConfig(w) {
  try { return w.extraConfig ? (typeof w.extraConfig === 'string' ? JSON.parse(w.extraConfig) : w.extraConfig) : {} }
  catch { return {} }
}
function getSize(w) { return parseConfig(w).size || 'half' }
function typeLabel(t) {
  const MAP = {
    WELCOME:         '웰컴 카드',
    RECENT_POSTS:    '최신 글',
    RECENT_GALLERY:  '갤러리 최신글',
    CALENDAR_WEEKLY:  '캘린더 주간',
    CALENDAR_MONTHLY: '캘린더 월간',
    IMAGE_SLIDER:    '슬라이더',
    IMAGE_GRID:      '이미지 그리드',
    LINK_LIST:       '링크',
    QUICK_LINKS:     '바로가기',
    MEMBER_STATS:    '회원 현황',
    VISIT_STATS:     '접속 통계',
    CUSTOM:          '커스텀',
    DIVIDER:         '구분선',
  }
  return MAP[t] || t
}
function calendarName(id) {
  return calendars.value.find(c => c.id === id)?.name || `캘린더 #${id}`
}
function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d), now = new Date()
  if (dt.toDateString() === now.toDateString()) return dt.toTimeString().slice(0, 5)
  return `${dt.getMonth()+1}.${String(dt.getDate()).padStart(2,'0')}`
}

async function fetchDashboards() {
  const r = await api.get('/admin/dashboard/list')
  dashboards.value = r.data.data || []
  // 홈 대시보드가 없으면 추가
  if (!dashboards.value.find(d => d.isHome)) {
    dashboards.value.unshift({ isHome: true, menuId: null, menuName: '홈 대시보드', menuPath: '홈 대시보드', widgetCount: 0 })
  }
}
async function fetchWidgets() {
  loading.value = true
  try {
    const params = selectedMenuId.value !== null ? { menuId: selectedMenuId.value } : {}
    const r = await api.get('/admin/dashboard/widgets', { params })
    widgets.value = r.data.data || []
  } finally {
    loading.value = false
  }
  await nextTick()
  initSortable()
}
function selectDashboard(db) {
  selectedMenuId.value = db.menuId ?? null
  fetchWidgets()
}
async function fetchBoards() {
  const r = await api.get('/boards')
  boards.value = r.data.data || []
}
async function fetchCalendars() { const r = await api.get('/calendars'); calendars.value = r.data.data || [] }

// 게시판의 메뉴 경로 빌드 (예: "홈 > 공지사항")
// 트리를 순회하면서 각 menuId → 조상 경로 문자열 맵을 미리 계산
const menuPathMap = computed(() => {
  const map = {}
  function walk(nodes, prefix) {
    for (const m of nodes || []) {
      const path = prefix ? `${prefix} > ${m.name}` : m.name
      map[m.id] = path
      if (m.children?.length) walk(m.children, path)
    }
  }
  walk(menuStore.menus, null)
  return map
})
function boardLabel(b) {
  const path = b.menuId ? menuPathMap.value[b.menuId] : null
  return path ? `${path} > ${b.name}` : b.name
}

function triggerImgUpload(idx) {
  imgInputRefs.value[idx]?.click()
}
function triggerSliderUpload(idx) {
  sliderInputRefs.value[idx]?.click()
}
async function uploadSliderImage(e, idx) {
  const file = e.target.files?.[0]
  if (!file) return
  sliderUploading.value = { ...sliderUploading.value, [idx]: true }
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await api.post('/files/inline-image', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    config.value.slides[idx].imageUrl = res.data.data?.url || ''
  } catch {
    ElMessage.error('이미지 업로드에 실패했습니다.')
  } finally {
    sliderUploading.value = { ...sliderUploading.value, [idx]: false }
    e.target.value = ''
  }
}
async function uploadGridImage(e, idx) {
  const file = e.target.files?.[0]
  if (!file) return
  imgUploading.value = { ...imgUploading.value, [idx]: true }
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await api.post('/files/inline-image', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    config.value.images[idx].imageUrl = res.data.data?.url || ''
  } catch {
    ElMessage.error('이미지 업로드에 실패했습니다.')
  } finally {
    imgUploading.value = { ...imgUploading.value, [idx]: false }
    e.target.value = ''
  }
}

function initSortable() {
  if (sortableInstance) { sortableInstance.destroy(); sortableInstance = null }
  if (!widgetSortable.value) return
  sortableInstance = Sortable.create(widgetSortable.value, {
    animation: 200,
    handle: '.drag-handle',
    ghostClass: 'sortable-ghost',
    dragClass: 'sortable-drag',
    onEnd: () => { sortChanged.value = true }
  })
}

function moveItem(arr, from, to) {
  if (from === to) return
  const [item] = arr.splice(from, 1)
  arr.splice(to, 0, item)
}

function destroyDialogSortables() {
  slidesSortable?.destroy(); slidesSortable = null
  imagesSortable?.destroy(); imagesSortable = null
  linksSortable?.destroy();  linksSortable  = null
}

function initDialogSortables() {
  destroyDialogSortables()
  nextTick(() => {
    const opts = (arr) => ({
      animation: 150,
      handle: '.sub-drag-handle',
      ghostClass: 'sub-sortable-ghost',
      onEnd({ oldIndex, newIndex }) { moveItem(arr, oldIndex, newIndex) },
    })
    if (slidesSortableEl.value)
      slidesSortable = Sortable.create(slidesSortableEl.value, opts(config.value.slides))
    if (imagesSortableEl.value)
      imagesSortable = Sortable.create(imagesSortableEl.value, opts(config.value.images))
    if (linksSortableEl.value)
      linksSortable  = Sortable.create(linksSortableEl.value,  opts(config.value.links))
  })
}

watch(showForm, (val) => {
  if (val) nextTick(initDialogSortables)
  else destroyDialogSortables()
})

async function saveSortOrder() {
  if (!widgetSortable.value) return
  sortSaving.value = true
  try {
    const ids = Array.from(widgetSortable.value.children)
      .map(el => parseInt(el.dataset.id)).filter(Boolean)
    await api.put('/admin/dashboard/widgets/sort', ids)
    ElMessage.success('순서가 저장되었습니다.')
    sortChanged.value = false
    await fetchWidgets()
  } catch { ElMessage.error('순서 저장에 실패했습니다.') }
  finally { sortSaving.value = false }
}

function onTypeChange() {
  config.value = {
    slides: [], links: [], images: [], content: '', moreUrl: '',
    size: config.value.size || 'half', calendarId: null,
    subText: '오늘도 좋은 하루 보내세요 ☀️', showClock: true,
  }
  form.value.targetBoardId = null
  form.value.postCount = 5
  nextTick(initDialogSortables)
}

function openCreate() {
  editing.value = null
  form.value = { ...defaultForm(), menuId: selectedMenuId.value, description: '' }
  config.value = { slides: [], links: [], images: [], content: '', moreUrl: '', size: 'half', calendarId: null, subText: '오늘도 좋은 하루 보내세요 ☀️', showClock: true }
  showForm.value = true
}

function openEdit(w) {
  editing.value = w; form.value = { ...w, description: w.description || '' }
  const ec = parseConfig(w)
  config.value = {
    slides:     ec.slides     || [],
    links:      ec.links      || [],
    images:     ec.images     || [],
    content:    ec.content    || '',
    moreUrl:    ec.moreUrl    || '',
    size:       ec.size       || 'half',
    calendarId: ec.calendarId || null,
    subText:    ec.subText    ?? '오늘도 좋은 하루 보내세요 ☀️',
    showClock:  ec.showClock  !== false,
  }
  showForm.value = true
}

async function saveWidget() {
  saving.value = true
  try {
    const extra = {}
    if (hasSizePicker(form.value.widgetType))          extra.size       = config.value.size
    if (form.value.widgetType === 'IMAGE_SLIDER')       extra.slides     = config.value.slides
    if (form.value.widgetType === 'IMAGE_GRID')         extra.images     = config.value.images
    if (['LINK_LIST','QUICK_LINKS'].includes(form.value.widgetType)) extra.links = config.value.links
    if (form.value.widgetType === 'CUSTOM')             extra.content    = config.value.content
    if (form.value.widgetType === 'CALENDAR_WEEKLY' || form.value.widgetType === 'CALENDAR_MONTHLY') extra.calendarId = config.value.calendarId
    if (form.value.widgetType === 'WELCOME') {
      extra.subText   = config.value.subText
      extra.showClock = config.value.showClock
    }

    const payload = { ...form.value, extraConfig: JSON.stringify(extra), menuId: selectedMenuId.value }
    editing.value
      ? await api.put(`/admin/dashboard/widgets/${editing.value.id}`, payload)
      : await api.post('/admin/dashboard/widgets', payload)
    ElMessage.success('저장되었습니다.')
    showForm.value = false; fetchWidgets()
  } finally { saving.value = false }
}

async function deleteWidget(id) {
  await ElMessageBox.confirm('위젯을 삭제하시겠습니까?', '삭제', { type:'warning', confirmButtonText:'삭제', cancelButtonText:'취소' })
  await api.delete(`/admin/dashboard/widgets/${id}`)
  ElMessage.success('삭제되었습니다.'); fetchWidgets()
}

onMounted(() => { menuStore.fetchMenus(); fetchDashboards(); fetchWidgets(); fetchBoards(); fetchCalendars() })
onBeforeUnmount(() => { if (sortableInstance) sortableInstance.destroy(); destroyDialogSortables() })
</script>

<style scoped>
@import '@/assets/admin-table.css';

/* 대시보드 선택 탭 */
.db-selector {
  display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 4px;
}
.db-tab {
  display: flex; align-items: center; gap: 7px;
  padding: 8px 14px; border-radius: var(--radius-sm);
  border: 1px solid var(--border); background: var(--surface);
  font-size: 13px; font-weight: 600; color: var(--t2);
  cursor: pointer; transition: all .15s;
}
.db-tab:hover { border-color: var(--accent); color: var(--accent); background: var(--accent-bg); }
.db-tab.active { border-color: var(--accent); background: var(--accent-bg); color: var(--accent); }
.db-tab-path { font-size: 11px; color: var(--t4); font-weight: 400; }
.db-tab-count {
  font-size: 11px; font-weight: 700; min-width: 20px; height: 18px;
  border-radius: 10px; background: var(--border); color: var(--t3);
  display: flex; align-items: center; justify-content: center; padding: 0 5px;
}
.db-tab.active .db-tab-count { background: var(--accent); color: #fff; }

.widget-list { display: flex; flex-direction: column; gap: 10px; }

.wcard {
  background: var(--surface);
  border: 1px solid var(--border2);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  overflow: hidden;
  user-select: none;
  transition: box-shadow 0.15s ease;
}
.wcard:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.wcard.is-inactive { opacity: 0.6; }

.wcard-top {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: var(--surface2);
  border-bottom: 0.5px solid var(--border2);
  cursor: default;
}
.drag-handle { font-size: 18px; color: var(--t4); cursor: grab; flex-shrink: 0; transition: color 0.15s; padding: 2px; }
.drag-handle:active { cursor: grabbing; }
.wcard:hover .drag-handle { color: var(--accent-t); }

.wtype-badge {
  font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: 5px;
  background: var(--accent-bg); color: var(--accent-t);
  border: 1px solid color-mix(in srgb, var(--accent) 30%, transparent);
  flex-shrink: 0;
}
.wtitle-wrap { flex: 1; min-width: 0; }
.wtitle {
  display: block; font-size: 14px; font-weight: 700; color: var(--t1);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.wdesc {
  display: block; font-size: 11px; color: var(--t4); font-weight: 400;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 1px;
}
.wcard-meta { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }

.wsize-indicator {
  display: inline-flex; align-items: center; gap: 3px;
  padding: 3px 8px 3px 5px; border: 1px solid var(--border);
  border-radius: 5px; background: var(--surface);
}
.wsize-block {
  display: inline-block; width: 10px; height: 10px;
  border-radius: 2px; background: var(--border); transition: background 0.15s;
}
.wsize-block.active { background: var(--accent); }
.wsize-label { font-size: 11px; font-weight: 700; color: var(--t3); margin-left: 3px; }

.wbadge-inactive {
  font-size: 10px; font-weight: 700; padding: 2px 6px; border-radius: 4px;
  background: #fef3cd; color: #b45309;
}
.wcard-actions { display: flex; gap: 5px; flex-shrink: 0; }
.wcard-preview { padding: 12px 16px; }

.wp-post-list { display: flex; flex-direction: column; }
.wp-post-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 5px 0; border-bottom: 0.5px solid var(--border2); gap: 12px;
}
.wp-post-row:last-child { border-bottom: none; }
.wp-post-title { font-size: 13px; color: var(--t2); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wp-post-date  { font-size: 12px; color: var(--t4); flex-shrink: 0; }

.wp-slides { display: flex; gap: 8px; }
.wp-slide {
  flex: 1; height: 60px; border-radius: 6px;
  background: var(--surface2) center/cover no-repeat;
  border: 0.5px solid var(--border2);
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; color: var(--t3);
}

.wp-links { display: flex; flex-wrap: wrap; gap: 5px; }
.wp-link-chip {
  font-size: 12px; padding: 3px 10px; border-radius: 20px;
  background: var(--surface2); border: 0.5px solid var(--border); color: var(--t2); white-space: nowrap;
}

.wp-stats { display: flex; gap: 0; }
.wp-stat { flex: 1; text-align: center; padding: 8px; }
.wp-stat-n { display: block; font-size: 20px; font-weight: 800; color: var(--t4); line-height: 1; margin-bottom: 4px; }
.wp-stat-l { font-size: 11px; color: var(--t4); }

.wp-custom {
  font-size: 13px; color: var(--t2); line-height: 1.6; overflow: hidden;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical;
}
.wp-custom :deep(p) { margin: 0 0 4px; }

.wp-welcome {
  background: linear-gradient(135deg, #4f6ef7 0%, #7c4ff7 45%, #e05fc4 80%, #f97040 100%);
  border-radius: 8px; padding: 14px 18px;
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
}
.wp-welcome-name { font-size: 14px; font-weight: 800; color: #fff; }
.wp-welcome-hl {
  display: inline-block; background: rgba(255,255,255,0.22);
  border-radius: 5px; padding: 1px 7px;
}
.wp-welcome-sub { font-size: 12px; color: rgba(255,255,255,0.8); margin-top: 4px; }
.wp-welcome-time { font-size: 18px; font-weight: 800; color: #fff; flex-shrink: 0; }

.wp-empty { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--t4); padding: 6px 0; }
.wp-divider { padding: 6px 0; }
.wp-divider-line { border: none; border-top: 1.5px solid var(--border2); margin: 4px 0; }
.wp-divider-label {
  display: flex; align-items: center; gap: 10px;
  font-size: 12px; font-weight: 700; color: var(--t3);
}
.wp-divider-label::before, .wp-divider-label::after {
  content: ''; flex: 1; border-top: 1.5px solid var(--border2);
}

/* 크기 선택 UI */
.size-picker { display: flex; gap: 10px; }
.size-opt {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  gap: 7px; padding: 10px 8px; border: 1.5px solid var(--border);
  border-radius: var(--radius-xs); background: var(--surface); cursor: pointer; transition: all .15s;
  font-size: 12px; color: var(--t2);
}
.size-opt:hover { border-color: var(--accent); background: var(--accent-bg); }
.size-opt.active { border-color: var(--accent); background: var(--accent-bg); color: var(--accent); font-weight: 700; }
.size-preview { display: flex; gap: 3px; width: 80px; height: 22px; }
.sp-block { height: 100%; background: var(--accent); border-radius: 3px; opacity: 0.7; }
.sp-empty { height: 100%; background: var(--border); border-radius: 3px; }
.full-preview .sp-block { flex: 1; }
.half-preview .sp-block, .half-preview .sp-empty { flex: 1; }

.sub-item {
  background: var(--surface2); border: 0.5px solid var(--border2);
  border-radius: var(--radius-xs); padding: 12px;
  display: flex; flex-direction: column; gap: 10px; margin-bottom: 8px;
}
.sub-item-header {
  display: flex; align-items: center; gap: 8px;
  padding-bottom: 8px; border-bottom: 0.5px solid var(--border2);
  margin-bottom: 2px;
}
.sub-drag-handle {
  font-size: 16px; color: var(--t4); cursor: grab; flex-shrink: 0;
  padding: 2px 3px; border-radius: 4px; transition: color 0.15s, background 0.15s;
}
.sub-drag-handle:hover { color: var(--accent-t); background: var(--accent-bg); }
.sub-drag-handle:active { cursor: grabbing; }
.sub-item-label { flex: 1; font-size: 12px; font-weight: 700; color: var(--t3); }

/* 이미지 그리드 업로드 */
.img-upload-row {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.img-upload-preview {
  flex-shrink: 0;
  width: 120px;
  height: 90px;
  border-radius: 8px;
  border: 1.5px dashed var(--border);
  background: var(--surface) center/cover no-repeat;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.15s;
}
.img-upload-preview:hover { border-color: var(--accent); }
.img-upload-change-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.45);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}
.img-upload-preview:hover .img-upload-change-overlay { opacity: 1; }
.img-upload-fields {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s, transform 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateX(8px); }

@keyframes spin { to { transform: rotate(360deg); } }
.spinning { animation: spin 0.7s linear infinite; display: inline-block; }
</style>

<style>
.sortable-ghost { opacity: 0.35 !important; background: var(--accent-bg) !important; border: 2px dashed var(--accent) !important; }
.sortable-drag  { opacity: 0.96 !important; box-shadow: 0 8px 32px rgba(0,0,0,0.18) !important; }
.sub-sortable-ghost { opacity: 0.3 !important; border: 1.5px dashed var(--accent) !important; }
</style>
