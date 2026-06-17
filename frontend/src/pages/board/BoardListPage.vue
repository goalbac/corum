<template>
  <div class="board-list">

    <!-- ===== 카테고리 필터 칩 ===== -->
    <div v-if="boardCategories.length" class="cat-chips">
      <button
        v-if="board?.useAllCategory"
        :class="['cat-chip', selectedCategoryId === null ? 'active' : '']"
        @click="selectCategory(null)"
      >전체</button>
      <button
        v-for="cat in boardCategories"
        :key="cat.id"
        :class="['cat-chip', selectedCategoryId === cat.id ? 'active' : '']"
        @click="selectCategory(cat.id)"
      >{{ cat.name }}</button>
    </div>

    <!-- ===== 갤러리 뷰 ===== -->
    <template v-if="isGalleryBoard">
      <div v-loading="loading" class="gallery-grid">
        <div v-for="post in posts" :key="post.id" class="gallery-card" @click="goDetail(post)">
          <div class="gallery-thumb">
            <template v-if="displayUrls(post).length > 0">
              <!-- 트랙 방식 캐러셀: 이미지들을 가로 나열 후 translateX로 슬라이드 -->
              <div
                class="carousel-track"
                :style="{ transform: `translateX(-${getImgIdx(post.id, displayUrls(post).length) * 100}%)` }"
              >
                <img
                  v-for="(url, i) in displayUrls(post)"
                  :key="i"
                  :src="url"
                  :alt="post.title"
                  class="carousel-img"
                />
              </div>
              <template v-if="displayUrls(post).length > 1">
                <button class="carousel-btn prev" @click.stop="prevImg(post.id, displayUrls(post).length)">
                  <i class="ti ti-chevron-left"></i>
                </button>
                <button class="carousel-btn next" @click.stop="nextImg(post.id, displayUrls(post).length)">
                  <i class="ti ti-chevron-right"></i>
                </button>
                <div class="carousel-dots">
                  <span
                    v-for="(_, i) in displayUrls(post)"
                    :key="i"
                    :class="['carousel-dot', i === getImgIdx(post.id, displayUrls(post).length) ? 'active' : '']"
                  ></span>
                </div>
              </template>
            </template>
            <template v-else-if="post.thumbnailUrl">
              <div class="thumb-spinner"><div class="spinner-ring"></div></div>
              <img :src="post.thumbnailUrl" :alt="post.title" class="thumb-img"
                @load="e => { e.target.classList.add('loaded'); e.target.previousElementSibling.style.display='none' }"
                @error="e => { e.target.style.display='none'; e.target.previousElementSibling.style.display='none' }" />
            </template>
            <div v-else class="no-thumb"><i class="ti ti-photo-off"></i></div>
            <div v-if="post.isNotice" class="gallery-notice-badge">공지</div>
          </div>
          <div class="gallery-info">
            <div class="gallery-title-row">
              <span class="gallery-title">{{ post.title }}</span><i v-if="isNew(post.createdAt)" class="ti ti-point-filled gallery-new-dot"></i>
            </div>
            <div class="gallery-meta-row">
              <span class="gallery-avatar">
                <img v-if="post.writerProfileImageUrl" :src="post.writerProfileImageUrl" :alt="post.writerName" class="gallery-avatar-img" />
                <span v-else class="gallery-avatar-txt">{{ post.writerName?.charAt(0) }}</span>
              </span>
              <span class="gallery-writer">{{ post.writerName }}</span>
              <span class="gallery-dot">·</span>
              <span class="gallery-date">{{ formatDate(post.createdAt) }}</span>
            </div>
            <div class="gallery-stats">
              <span v-if="showViewCount" class="stat-chip">
                <i class="ti ti-eye"></i> {{ post.viewCount }}
              </span>
              <span v-if="board?.useLike" class="stat-chip">
                <i class="ti ti-heart"></i> {{ post.likeCount }}
              </span>
              <span class="stat-chip">
                <i class="ti ti-message-circle"></i> {{ post.commentCount }}
              </span>
              <span v-if="post.hasFile" class="stat-chip">
                <i class="ti ti-paperclip"></i>
              </span>
            </div>
          </div>
        </div>
        <div v-if="!loading && !posts.length" class="gallery-empty">
          <i class="ti ti-photo-off"></i>
          <p>등록된 게시글이 없습니다.</p>
        </div>
      </div>

      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="gallerySize" :total="total"
          layout="prev, pager, next" background small @current-change="fetchPosts" />
      </div>
      <div class="list-toolbar">
        <div class="search-group">
          <div class="search-type-wrap">
            <el-select v-model="searchType" size="small" class="search-type">
              <el-option value="title" label="제목" />
              <el-option value="content" label="내용" />
              <el-option value="writer" label="작성자" />
              <el-option value="all" label="전체" />
            </el-select>
          </div>
          <div class="search-input-wrap">
            <i class="ti ti-search search-icon"></i>
            <input v-model="keyword" class="search-input" placeholder="검색어 입력..."
              @keyup.enter="handleSearch" />
            <button v-if="keyword" class="search-clear" @click="keyword=''; handleSearch()">
              <i class="ti ti-x"></i>
            </button>
          </div>
          <button class="search-btn" @click="handleSearch">검색</button>
        </div>
        <button v-if="canWrite" class="write-btn" @click="goWrite">
          <i class="ti ti-pencil-plus"></i>
          <span>글쓰기</span>
        </button>
      </div>
    </template>

    <!-- ===== 웹진 뷰 ===== -->
    <template v-else-if="isWebzineBoard">
      <div v-loading="loading" class="webzine-list">
        <article
          v-for="post in posts"
          :key="post.id"
          :class="['webzine-item', { notice: post.isNotice }]"
          @click="goDetail(post)"
        >
          <div class="webzine-thumb">
            <template v-if="post.thumbnailUrl">
              <div class="thumb-spinner"><div class="spinner-ring"></div></div>
              <img :src="post.thumbnailUrl" :alt="post.title" class="thumb-img"
                @load="e => { e.target.classList.add('loaded'); e.target.previousElementSibling.style.display='none' }"
                @error="e => { e.target.style.display='none'; e.target.previousElementSibling.style.display='none' }" />
            </template>
            <div v-else class="no-thumb"><i class="ti ti-news"></i></div>
          </div>
          <div class="webzine-body">
            <div class="webzine-kicker">
              <span v-if="post.isNotice" class="notice-tag">Notice</span>
              <span>{{ formatDate(post.createdAt) }}</span>
            </div>
            <div class="webzine-title-row">
              <h3 class="webzine-title">{{ post.title }}</h3>
              <i v-if="isNew(post.createdAt)" class="ti ti-point-filled webzine-new-dot"></i>
            </div>
            <p v-if="post.excerpt" class="webzine-excerpt">{{ post.excerpt }}</p>
            <div class="webzine-meta">
              <span class="webzine-meta-chip webzine-writer-chip">
                <span class="webzine-avatar">
                  <img v-if="post.writerProfileImageUrl" :src="post.writerProfileImageUrl" :alt="post.writerName" class="webzine-avatar-img" />
                  <span v-else class="webzine-avatar-txt">{{ post.writerName?.charAt(0) }}</span>
                </span>
                {{ post.writerName }}
              </span>
              <span v-if="showViewCount" class="webzine-meta-chip"><i class="ti ti-eye"></i>{{ post.viewCount }}</span>
              <span v-if="showLikeCount && board?.useLike" class="webzine-meta-chip"><i class="ti ti-heart"></i>{{ post.likeCount }}</span>
              <span class="webzine-meta-chip"><i class="ti ti-message-circle"></i>{{ post.commentCount }}</span>
              <span v-if="post.hasFile" class="webzine-meta-chip"><i class="ti ti-paperclip"></i></span>
            </div>
          </div>
        </article>
        <div v-if="!loading && !posts.length" class="webzine-empty">
          <i class="ti ti-news-off"></i>
          <p>No posts yet.</p>
        </div>
      </div>

      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="gallerySize" :total="total"
          layout="prev, pager, next" background small @current-change="fetchPosts" />
      </div>
      <div class="list-toolbar">
        <div class="search-group">
          <div class="search-type-wrap">
            <el-select v-model="searchType" size="small" class="search-type">
              <el-option value="title" label="제목" />
              <el-option value="content" label="내용" />
              <el-option value="writer" label="작성자" />
              <el-option value="all" label="전체" />
            </el-select>
          </div>
          <div class="search-input-wrap">
            <i class="ti ti-search search-icon"></i>
            <input v-model="keyword" class="search-input" placeholder="검색어 입력..."
              @keyup.enter="handleSearch" />
            <button v-if="keyword" class="search-clear" @click="keyword=''; handleSearch()">
              <i class="ti ti-x"></i>
            </button>
          </div>
          <button class="search-btn" @click="handleSearch">검색</button>
        </div>
        <button v-if="canWrite" class="write-btn" @click="goWrite">
          <i class="ti ti-pencil-plus"></i>
          <span>글쓰기</span>
        </button>
      </div>
    </template>

    <!-- ===== 리스트형 뷰 ===== -->
    <template v-else-if="isListBoard">
      <div v-loading="loading" class="lv-list">
        <!-- 공지 -->
        <div
          v-for="post in noticePosts"
          :key="`notice-${post.id}`"
          class="lv-item lv-notice"
          @click="goDetail(post)"
        >
          <div class="lv-main">
            <div class="lv-upper">
              <div class="lv-kicker">
                <span class="lv-tag lv-tag-notice">공지</span>
                <span v-if="post.categoryName && selectedCategoryId === null" class="lv-category">{{ post.categoryName }}</span>
              </div>
              <div class="lv-title-row">
                <h3 class="lv-title">{{ post.title }}</h3>
                <i v-if="isNew(post.createdAt)" class="ti ti-point-filled lv-new-dot"></i>
              </div>
              <p v-if="post.excerpt" class="lv-excerpt">{{ post.excerpt }}</p>
            </div>
            <div class="lv-meta">
              <span class="lv-meta-chip lv-writer">
                <span class="lv-avatar">
                  <img v-if="post.writerProfileImageUrl" :src="post.writerProfileImageUrl" :alt="post.writerName" class="lv-avatar-img" />
                  <span v-else class="lv-avatar-txt">{{ post.writerName?.charAt(0) }}</span>
                </span>
                {{ post.writerName }}
              </span>
              <span class="lv-meta-chip"><i class="ti ti-clock"></i>{{ formatRelativeDate(post.createdAt) }}</span>
              <span v-if="post.hasFile" class="lv-meta-chip"><i class="ti ti-paperclip"></i></span>
              <span class="lv-meta-dot">·</span>
              <span v-if="showViewCount" class="lv-meta-chip"><i class="ti ti-eye"></i>{{ post.viewCount }}</span>
              <span v-if="showLikeCount && board?.useLike" class="lv-meta-chip"><i class="ti ti-heart"></i>{{ post.likeCount }}</span>
              <span class="lv-meta-chip"><i class="ti ti-message-circle"></i>{{ post.commentCount }}</span>
            </div>
          </div>
          <div v-if="post.thumbnailUrl" class="lv-thumb">
            <img :src="post.thumbnailUrl" :alt="post.title" />
          </div>
        </div>

        <div v-if="noticePosts.length && normalPosts.length" class="lv-notice-divider" />

        <!-- 일반 글 -->
        <div
          v-for="post in normalPosts"
          :key="post.id"
          class="lv-item"
          @click="goDetail(post)"
        >
          <div class="lv-main">
            <div class="lv-upper">
              <div v-if="post.categoryName && selectedCategoryId === null" class="lv-kicker">
                <span class="lv-category">{{ post.categoryName }}</span>
              </div>
              <div class="lv-title-row">
                <h3 class="lv-title">{{ post.title }}</h3>
                <i v-if="isNew(post.createdAt)" class="ti ti-point-filled lv-new-dot"></i>
              </div>
              <p v-if="post.excerpt" class="lv-excerpt">{{ post.excerpt }}</p>
            </div>
            <div class="lv-meta">
              <span class="lv-meta-chip lv-writer">
                <span class="lv-avatar">
                  <img v-if="post.writerProfileImageUrl" :src="post.writerProfileImageUrl" :alt="post.writerName" class="lv-avatar-img" />
                  <span v-else class="lv-avatar-txt">{{ post.writerName?.charAt(0) }}</span>
                </span>
                {{ post.writerName }}
              </span>
              <span class="lv-meta-chip"><i class="ti ti-clock"></i>{{ formatRelativeDate(post.createdAt) }}</span>
              <span v-if="post.hasFile" class="lv-meta-chip"><i class="ti ti-paperclip"></i></span>
              <span class="lv-meta-dot">·</span>
              <span v-if="showViewCount" class="lv-meta-chip"><i class="ti ti-eye"></i>{{ post.viewCount }}</span>
              <span v-if="showLikeCount && board?.useLike" class="lv-meta-chip"><i class="ti ti-heart"></i>{{ post.likeCount }}</span>
              <span class="lv-meta-chip"><i class="ti ti-message-circle"></i>{{ post.commentCount }}</span>
            </div>
          </div>
          <div v-if="post.thumbnailUrl" class="lv-thumb">
            <img :src="post.thumbnailUrl" :alt="post.title" />
          </div>
        </div>

        <div v-if="!loading && !posts.length" class="lv-empty">
          <i class="ti ti-list"></i>
          <p>등록된 게시글이 없습니다.</p>
        </div>
      </div>

      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total"
          layout="prev, pager, next" background small @current-change="fetchPosts" />
      </div>
      <div class="list-toolbar">
        <div class="search-group">
          <div class="search-type-wrap">
            <el-select v-model="searchType" size="small" class="search-type">
              <el-option value="title" label="제목" />
              <el-option value="content" label="내용" />
              <el-option value="writer" label="작성자" />
              <el-option value="all" label="전체" />
            </el-select>
          </div>
          <div class="search-input-wrap">
            <i class="ti ti-search search-icon"></i>
            <input v-model="keyword" class="search-input" placeholder="검색어 입력..."
              @keyup.enter="handleSearch" />
            <button v-if="keyword" class="search-clear" @click="keyword=''; handleSearch()">
              <i class="ti ti-x"></i>
            </button>
          </div>
          <button class="search-btn" @click="handleSearch">검색</button>
        </div>
        <button v-if="canWrite" class="write-btn" @click="goWrite">
          <i class="ti ti-pencil-plus"></i>
          <span>글쓰기</span>
        </button>
      </div>
    </template>

    <!-- ===== 일반/자료실 테이블 뷰 ===== -->
    <template v-else>
      <div class="post-table" v-loading="loading">
        <!-- 헤더 -->
        <div class="pt-head">
          <div class="pt-col num">번호</div>
          <div class="pt-col title">제목</div>
          <div class="pt-col writer">작성자</div>
          <div class="pt-col date">작성일</div>
          <div v-if="showViewCount" class="pt-col count">조회</div>
          <div v-if="showLikeCount && board?.useLike" class="pt-col count">좋아요</div>
        </div>

        <!-- 공지 행 -->
        <template v-for="post in noticePosts" :key="`notice-${post.id}`">
          <div class="pt-row notice-row" @click="goDetail(post)">
            <div class="pt-col num">
              <span class="notice-tag">공지</span>
            </div>
            <div class="pt-col title">
              <span v-if="post.categoryName && selectedCategoryId === null" class="cat-name-chip">{{ post.categoryName }}</span>
              <span class="pt-title notice-title">{{ post.title }}</span>
              <span v-if="post.commentCount > 0" class="comment-chip">
                <i class="ti ti-message-2"></i>{{ post.commentCount }}
              </span>
              <span v-if="post.hasFile" class="file-chip"><i class="ti ti-paperclip"></i></span>
              <i v-if="isNew(post.createdAt)" class="ti ti-point-filled pt-new-dot"></i>
            </div>
            <div class="pt-col writer">{{ post.writerName }}</div>
            <div class="pt-col date">{{ formatDate(post.createdAt) }}</div>
            <div v-if="showViewCount" class="pt-col count">{{ post.viewCount }}</div>
            <div v-if="showLikeCount && board?.useLike" class="pt-col count">{{ post.likeCount }}</div>
          </div>
        </template>

        <!-- 구분선 (공지 있을 때) -->
        <div v-if="noticePosts.length && normalPosts.length" class="notice-divider" />

        <!-- 일반 글 행 -->
        <template v-for="post in normalPosts" :key="post.id">
          <div class="pt-row" @click="goDetail(post)">
            <div class="pt-col num">
              <span class="row-num">{{ post.rowNum }}</span>
            </div>
            <div class="pt-col title">
              <span v-if="post.categoryName && selectedCategoryId === null" class="cat-name-chip">{{ post.categoryName }}</span>
              <span class="pt-title">{{ post.title }}</span>
              <span v-if="post.commentCount > 0" class="comment-chip">
                <i class="ti ti-message-2"></i>{{ post.commentCount }}
              </span>
              <span v-if="post.hasFile" class="file-chip"><i class="ti ti-paperclip"></i></span>
              <i v-if="isNew(post.createdAt)" class="ti ti-point-filled pt-new-dot"></i>
            </div>
            <div class="pt-col writer">{{ post.writerName }}</div>
            <div class="pt-col date">{{ formatDate(post.createdAt) }}</div>
            <div v-if="showViewCount" class="pt-col count">{{ post.viewCount }}</div>
            <div v-if="showLikeCount && board?.useLike" class="pt-col count">{{ post.likeCount }}</div>
          </div>
        </template>

        <!-- 빈 상태 -->
        <div v-if="!loading && !posts.length" class="pt-empty">
          <i class="ti ti-inbox"></i>
          <p>등록된 게시글이 없습니다.</p>
        </div>
      </div>

      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total"
          layout="prev, pager, next" background small @current-change="fetchPosts" />
      </div>
      <div class="list-toolbar">
        <div class="search-group">
          <div class="search-type-wrap">
            <el-select v-model="searchType" size="small" class="search-type">
              <el-option value="title" label="제목" />
              <el-option value="content" label="내용" />
              <el-option value="writer" label="작성자" />
              <el-option value="all" label="전체" />
            </el-select>
          </div>
          <div class="search-input-wrap">
            <i class="ti ti-search search-icon"></i>
            <input v-model="keyword" class="search-input" placeholder="검색어 입력..."
              @keyup.enter="handleSearch" />
            <button v-if="keyword" class="search-clear" @click="keyword=''; handleSearch()">
              <i class="ti ti-x"></i>
            </button>
          </div>
          <button class="search-btn" @click="handleSearch">검색</button>
        </div>
        <button v-if="canWrite" class="write-btn" @click="goWrite">
          <i class="ti ti-pencil-plus"></i>
          <span>글쓰기</span>
        </button>
      </div>
    </template>

  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMenuStore } from '@/stores/menu'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'

const route = useRoute()
const router = useRouter()
const menuStore = useMenuStore()
const authStore = useAuthStore()

const activeMenu = computed(() => menuStore.findMenuById(route.params.menuId))
const boardId = computed(() => route.params.boardId || activeMenu.value?.targetId)
const basePath = computed(() => route.params.menuId ? `/menu/${route.params.menuId}` : `/board/${boardId.value}`)

const board = ref(null)
const posts = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(15)
const gallerySize = ref(8)
const keyword = ref('')
const searchType = ref('title')
const selectedCategoryId = ref(null)

const boardCategories = computed(() => board.value?.categories || [])

// ===== 갤러리 이미지 캐러셀 =====
const imgIndexMap = ref({})   // postId -> currentIndex

function displayUrls(post) {
  return (post.imageUrls || []).slice(0, 3)
}
function getImgIdx(postId, total) {
  const idx = imgIndexMap.value[postId] ?? 0
  return Math.min(idx, total - 1)
}
function nextImg(postId, total) {
  imgIndexMap.value[postId] = (getImgIdx(postId, total) + 1) % total
}
function prevImg(postId, total) {
  imgIndexMap.value[postId] = (getImgIdx(postId, total) - 1 + total) % total
}

const noticePosts = computed(() => posts.value.filter(p => p.isNotice))
const normalPosts = computed(() => posts.value.filter(p => !p.isNotice))
const isGalleryBoard = computed(() => board.value?.boardType === 'GALLERY')
const isWebzineBoard = computed(() => board.value?.boardType === 'WEBZINE')
const isListBoard   = computed(() => board.value?.boardType === 'LIST')
const isVisualBoard = computed(() => isGalleryBoard.value || isWebzineBoard.value)

const canWrite = computed(() => {
  if (!board.value) return false
  if (authStore.member?.isAdmin) return true
  const perms = board.value.permissions || []
  if (!perms.length) return true
  if (!authStore.isLoggedIn) return false
  const memberGroupIds = authStore.member?.groupIds || []
  return perms.some(p => memberGroupIds.includes(p.groupId) && p.canWrite)
})

const showViewCount = ref(true)
const showLikeCount = ref(true)

async function fetchBoard() {
  if (!boardId.value) return
  try {
    const res = await api.get(`/boards/${boardId.value}`)
    board.value = res.data.data
    restoreCategory()
  } catch {}
}

const categoryStorageKey = () => `board_cat_${boardId.value}`

// 상세→목록 등 재진입 시 직전에 보던 카테고리 복원 (없으면 첫 번째)
function restoreCategory() {
  const cats = board.value?.categories || []
  if (!cats.length) { selectedCategoryId.value = null; return }
  const saved = sessionStorage.getItem(categoryStorageKey())
  if (saved === 'all' && board.value?.useAllCategory) { selectedCategoryId.value = null; return }
  if (saved && saved !== 'all') {
    const id = Number(saved)
    if (cats.some(c => c.id === id)) { selectedCategoryId.value = id; return }
  }
  // 기본값: '전체' 기능을 쓰면 전체(null), 아니면 첫 번째 카테고리
  selectedCategoryId.value = board.value?.useAllCategory ? null : cats[0].id
}

async function fetchPosts() {
  if (!boardId.value) return
  loading.value = true
  try {
    const pageSize = isVisualBoard.value ? gallerySize.value : size.value
    const params = { page: page.value - 1, size: pageSize }
    if (keyword.value) {
      params.keyword = keyword.value
      params.searchType = searchType.value
    }
    if (selectedCategoryId.value !== null) {
      params.categoryId = selectedCategoryId.value
    }
    const res = await api.get(`/boards/${boardId.value}/posts`, { params })
    posts.value = res.data.data?.content || []
    total.value = res.data.data?.totalElements || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.value = 1; fetchPosts() }
function selectCategory(id) {
  selectedCategoryId.value = id
  sessionStorage.setItem(categoryStorageKey(), id === null ? 'all' : String(id))
  page.value = 1
  fetchPosts()
}
function goWrite() { router.push(`${basePath.value}/write`) }
function goDetail(row) { router.push(`${basePath.value}/posts/${row.id}`) }

function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d)
  const today = new Date()
  if (dt.toDateString() === today.toDateString()) return dt.toTimeString().slice(0, 5)
  return `${dt.getFullYear()}.${String(dt.getMonth() + 1).padStart(2, '0')}.${String(dt.getDate()).padStart(2, '0')}`
}

function isNew(d) { return d && Date.now() - new Date(d).getTime() < 3 * 86400000 }

function formatRelativeDate(d) {
  if (!d) return ''
  const diff = Date.now() - new Date(d).getTime()
  const min = Math.floor(diff / 60000)
  if (min < 1) return '방금 전'
  if (min < 60) return `${min}분 전`
  const hr = Math.floor(min / 60)
  if (hr < 24) return `${hr}시간 전`
  const day = Math.floor(hr / 24)
  if (day < 7) return `${day}일 전`
  if (day < 30) return `${Math.floor(day / 7)}주 전`
  if (day < 365) return `${Math.floor(day / 30)}개월 전`
  return `${Math.floor(day / 365)}년 전`
}

watch(boardId, async () => {
  page.value = 1; keyword.value = ''; selectedCategoryId.value = null
  await fetchBoard(); fetchPosts()
})

onMounted(async () => {
  if (route.params.menuId) await menuStore.fetchMenus()
  await fetchBoard(); fetchPosts()
})
</script>

<style scoped>
/* ===== 카테고리 칩 ===== */
.cat-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 12px 20px 14px;
}

@media (max-width: 600px) {
  .cat-chips {
    flex-wrap: nowrap;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    padding: 10px 16px 12px;
  }
  .cat-chips::-webkit-scrollbar { display: none; }
}
.cat-chip {
  display: inline-flex;
  align-items: center;
  padding: 10px 14px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 500;
  border: none;
  background: var(--border2);
  color: var(--t2);
  cursor: pointer;
  transition: all .15s;
  white-space: nowrap;
}
.cat-chip:hover { background: var(--border); color: var(--accent); }
.cat-chip.active { background: var(--accent); color: #fff; }

.cat-name-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  border: none;
  background: var(--border2);
  color: var(--t2);
  white-space: nowrap;
  flex-shrink: 0;
}

.board-list {
  display: flex;
  flex-direction: column;
  color: var(--t1);
}

/* ===== 공통 배지 ===== */
.pt-new-dot {
  font-size: 14px;
  color: var(--accent);
  flex-shrink: 0;
}

/* ===== 테이블 뷰 ===== */
.post-table {
  width: 100%;
}

.pt-head {
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 44px;
  background: var(--surface2);
  border-bottom: 1px solid var(--border2);
  border-top: 1px solid var(--border2);
}

.pt-col {
  font-size: 13px;
  font-weight: 700;
  color: var(--t3);
  text-transform: uppercase;
  letter-spacing: 0.3px;
  white-space: nowrap;
}

.pt-col.num   { width: 64px; flex-shrink: 0; text-align: center; }
.pt-col.title { flex: 1; min-width: 0; }
.pt-col.writer{ width: 110px; flex-shrink: 0; text-align: center; }
.pt-col.date  { width: 106px; flex-shrink: 0; text-align: center; }
.pt-col.count { width: 66px; flex-shrink: 0; text-align: center; }

/* 행 */
.pt-row {
  display: flex;
  align-items: center;
  padding: 0 20px;
  min-height: 56px;
  border-bottom: 0.5px solid var(--border2);
  cursor: pointer;
  transition: background 0.12s;
}

.pt-row:hover { background: var(--surface2); }
.pt-row:hover .pt-title { color: var(--accent-t); }

.pt-row .pt-col {
  font-size: 15px;
  font-weight: 400;
  color: var(--t2);
  letter-spacing: 0;
  text-transform: none;
}

.pt-row .pt-col.title {
  display: flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
}

.pt-title {
  color: var(--t1);
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.12s;
}

.notice-row { background: var(--accent-bg); }
.notice-row:hover { background: color-mix(in srgb, var(--accent) 12%, transparent); }
.notice-title { font-weight: 700; color: var(--accent-t); }

.notice-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 7px;
  border-radius: 3px;
  background: var(--accent);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

.row-num { font-size: 14px; color: var(--t4); }

.comment-chip {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 13px;
  color: var(--t3);
  flex-shrink: 0;
}

.comment-chip i { font-size: 13px; }

.file-chip {
  font-size: 14px;
  color: var(--t3);
  flex-shrink: 0;
}

.notice-divider {
  height: 3px;
  background: linear-gradient(90deg, var(--accent) 0%, transparent 100%);
  opacity: 0.15;
}

.pt-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 60px 0;
  color: var(--t4);
  font-size: 14px;
}

.pt-empty i { font-size: 36px; }

/* ===== 갤러리 ===== */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  background: transparent;
  padding: 16px 20px;
}

.gallery-card {
  background: var(--surface);
  cursor: pointer;
  transition: var(--transition);
  overflow: hidden;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  border: 0.5px solid var(--border2);
}

.gallery-card:hover { background: var(--surface2); }
.gallery-card:hover .gallery-title { color: var(--accent-t); }

.gallery-thumb {
  position: relative;
  width: 100%;
  aspect-ratio: 4/3;
  background: var(--surface2);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.3s;
}
.thumb-img.loaded { opacity: 1; }

/* ===== 캐러셀 트랙 방식 ===== */
.carousel-track {
  display: flex;
  width: 100%;
  height: 100%;
  transition: transform 0.32s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform;
}
.carousel-img {
  flex-shrink: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}
.gallery-card:hover .carousel-img { transform: scale(1.04); }

.thumb-spinner {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.spinner-ring {
  width: 28px;
  height: 28px;
  border: 3px solid var(--border2);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.no-thumb { font-size: 28px; color: var(--t4); }

/* ===== 캐러셀 버튼 ===== */
.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 3;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(0,0,0,0.45);
  border: none;
  color: #fff;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.18s, background 0.15s;
  padding: 0;
  line-height: 1;
}
.carousel-btn.prev { left: 6px; }
.carousel-btn.next { right: 6px; }
.gallery-card:hover .carousel-btn { opacity: 1; }
.carousel-btn:hover { background: rgba(0,0,0,0.7); }

.carousel-dots {
  position: absolute;
  bottom: 7px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 4px;
  z-index: 3;
}
.carousel-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(255,255,255,0.5);
  transition: background 0.15s, transform 0.15s;
}
.carousel-dot.active {
  background: #fff;
  transform: scale(1.3);
}

.gallery-notice-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: var(--accent);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 8px;
}

.gallery-info { padding: 10px 12px 12px; }

.gallery-title-row {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  margin-bottom: 5px;
}

.gallery-title {
  display: inline;
  font-size: 14px;
  font-weight: 600;
  color: var(--t1);
  transition: color 0.12s;
}

.gallery-new-dot {
  display: inline;
  font-size: 12px;
  color: var(--accent);
  vertical-align: middle;
  margin-left: 2px;
}

.gallery-meta-row {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--t3);
  margin-bottom: 7px;
}

.gallery-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--accent);
  flex-shrink: 0;
}

.gallery-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gallery-avatar-txt {
  font-size: 9px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.gallery-dot { color: var(--t4); }

.gallery-stats {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: var(--t3);
  font-weight: 500;
}

.stat-chip i { font-size: 12px; }

.gallery-empty {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 60px 0;
  color: var(--t4);
  font-size: 14px;
}

.gallery-empty i { font-size: 36px; }

/* ===== Webzine ===== */
.webzine-list {
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--border2);
}

.webzine-item {
  display: grid;
  grid-template-columns: minmax(160px, 240px) 1fr;
  gap: 18px;
  padding: 18px 20px;
  border-bottom: 0.5px solid var(--border2);
  cursor: pointer;
  background: var(--surface);
  transition: background 0.15s;
}

.webzine-item:hover { background: var(--surface2); }
.webzine-item.notice { background: var(--accent-bg); }

.webzine-thumb {
  width: 100%;
  aspect-ratio: 16/10;
  border-radius: var(--radius-xs);
  overflow: hidden;
  background: var(--surface2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.webzine-item:hover .thumb-img { transform: scale(1.03); }

.webzine-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.webzine-kicker,
.webzine-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--t3);
}

.webzine-title-row {
  display: flex;
  align-items: center;
  gap: 5px;
}

.webzine-new-dot {
  font-size: 14px;
  color: var(--accent);
  flex-shrink: 0;
  margin-top: -2px;
}

.webzine-title {
  margin: 0;
  color: var(--t1);
  font-size: 20px;
  line-height: 1.35;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.webzine-item:hover .webzine-title { color: var(--accent-t); }

.webzine-excerpt {
  margin: 0;
  color: var(--t2);
  font-size: 13px;
  line-height: 1.55;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.webzine-meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 20px;
}

.webzine-meta-chip i {
  font-size: 13px;
  color: var(--t3);
}

.webzine-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--accent);
  flex-shrink: 0;
}

.webzine-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.webzine-avatar-txt {
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.webzine-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 60px 0;
  color: var(--t4);
  font-size: 14px;
}

.webzine-empty i { font-size: 36px; }

/* ===== 리스트형 ===== */
.lv-list {
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--border2);
}

.lv-item {
  display: flex;
  align-items: stretch;
  gap: 14px;
  padding: 20px 28px;
  border-bottom: 0.5px solid var(--border2);
  cursor: pointer;
  transition: background 0.12s;
}

.lv-item:hover { background: var(--surface2); }
.lv-item:hover .lv-title { color: var(--accent-t); }

.lv-notice { background: var(--accent-bg); }
.lv-notice:hover { background: color-mix(in srgb, var(--accent) 10%, transparent); }

.lv-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 0;
}

.lv-upper {
  display: flex;
  flex-direction: column;
  gap: 5px;
  flex: 1;
  min-width: 0;
}

.lv-kicker {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.lv-category {
  font-size: 12px;
  font-weight: 600;
  color: var(--t3);
}

.lv-tags {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.lv-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px 7px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.lv-tag-notice { background: var(--accent); color: #fff; }
.lv-tag-new    { background: var(--accent-bg); color: var(--accent); border: 1px solid var(--accent); opacity: 0.85; }

.lv-title {
  margin: 0 0 0 -1px;
  position: relative;
  top: -1px;
  font-size: 17px;
  font-weight: 700;
  color: var(--t1);
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  transition: color 0.12s;
  min-width: 0;
}

.lv-excerpt {
  margin: 0;
  font-size: 14px;
  color: var(--t3);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.lv-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 14px;
  color: var(--t3);
  flex-wrap: nowrap;
  margin-top: 10px;
  flex-shrink: 0;
}

.lv-meta-dot {
  color: var(--t4);
  font-size: 16px;
  line-height: 1;
  flex-shrink: 0;
}

.lv-meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 20px;
  white-space: nowrap;
}

.lv-meta-chip i {
  font-size: 14px;
  color: var(--t3);
}

.lv-writer { font-weight: 600; color: var(--t2); }

.lv-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--accent);
  flex-shrink: 0;
}

.lv-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.lv-avatar-txt {
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}
.lv-sep    { color: var(--border); }

.lv-title-row {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.lv-new-dot {
  font-size: 14px;
  color: var(--accent);
  flex-shrink: 0;
  margin-top: -3px;
}

.lv-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  flex-shrink: 0;
}

.lv-stat {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: var(--t3);
  font-weight: 500;
}

.lv-stat i { font-size: 12px; }
.lv-stat-comment { color: var(--accent-t); }

.lv-thumb {
  flex-shrink: 0;
  width: 130px;
  align-self: stretch;
  min-height: 90px;
  max-height: 140px;
  border-radius: var(--radius-xs);
  overflow: hidden;
  background: var(--surface2);
}

.lv-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.lv-notice-divider {
  height: 3px;
  background: linear-gradient(90deg, var(--accent) 0%, transparent 100%);
  opacity: 0.15;
}

.lv-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 60px 0;
  color: var(--t4);
  font-size: 14px;
}

.lv-empty i { font-size: 36px; }

/* ===== 툴바 ===== */
.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--border2);
  background: var(--surface2);
  flex-wrap: wrap;
}

.search-group {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.search-type-wrap { flex-shrink: 0; }
.search-type { width: 82px; }

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  font-size: 13px;
  color: var(--t3);
  pointer-events: none;
}

.search-input {
  padding: 7px 30px 7px 32px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--surface2);
  font-size: 13px;
  color: var(--t1);
  width: 200px;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  font-family: inherit;
}

.search-input:focus {
  border-color: var(--accent);
  background: var(--surface);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent) 12%, transparent);
}

.search-input::placeholder { color: var(--t4); }

.search-clear {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  color: var(--t3);
  font-size: 13px;
  cursor: pointer;
  padding: 2px;
  display: flex;
  align-items: center;
}

.search-btn {
  padding: 7px 14px;
  border-radius: var(--radius-xs);
  border: none;
  background: var(--accent);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
  white-space: nowrap;
}

.search-btn:hover { opacity: 0.88; }

.write-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border-radius: var(--radius-xs);
  border: 1px solid var(--accent);
  background: var(--accent);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s;
  white-space: nowrap;
  flex-shrink: 0;
}

.write-btn:hover { opacity: 0.88; }

/* ===== 페이지네이션 ===== */
.pagination { display: flex; justify-content: center; padding: 24px 0 20px; }

@media (max-width: 600px) {
  /* ===== 리스트형 ===== */
  .lv-item { padding: 14px 16px; gap: 12px; }
  .lv-thumb { width: 90px; min-height: 72px; max-height: 120px; }
  .lv-title { font-size: 15px; }
  .lv-excerpt { font-size: 13px; }

  /* ===== 테이블 ===== */
  .pt-col.writer, .pt-col.count { display: none; }
  .pt-col.date { width: 72px; font-size: 12px; }
  .search-input { width: 130px; }

  /* ===== 갤러리 : 모바일 2열 ===== */
  .gallery-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding: 14px;
  }

  /* ===== 웹진 : 이미지 상단 full-width 카드 ===== */
  .webzine-list {
    gap: 12px;
    padding: 12px;
  }

  .webzine-item {
    grid-template-columns: 1fr;
    gap: 0;
    padding: 0;
    border-radius: 16px;
    overflow: hidden;
    border-bottom: none;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
    border: 0.5px solid var(--border2);
    background: var(--surface);
  }

  .webzine-thumb {
    width: 100%;
    aspect-ratio: 16 / 9;
    border-radius: 0;
  }

  .webzine-body {
    padding: 12px 14px 14px;
    gap: 6px;
  }

  .webzine-title {
    font-size: 16px;
    -webkit-line-clamp: 2;
  }

  .webzine-excerpt {
    font-size: 12px;
    -webkit-line-clamp: 2;
  }

  .webzine-meta { gap: 5px; font-size: 11px; }
}
</style>
