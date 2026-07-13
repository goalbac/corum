<template>
  <div class="board-page">

    <!-- ===== 목록 툴바 (건수 + 글쓰기 [+ 카테고리 없을 때 검색]) ===== -->
    <div class="list-toolbar">
      <p class="pg-count" v-if="!loading || total > 0">
        총 <strong>{{ total }}</strong>건의 {{ boardCountLabel }}이 등록되어 있습니다.
      </p>

      <!-- 카테고리 없을 때: 검색+정렬을 툴바 오른쪽에 인라인 배치 -->
      <template v-if="!boardCategories.length">
        <div class="toolbar-right">
          <select v-if="!isDocumentBoard && !isGalleryBoard" v-model="searchType" class="search-type-select" @change="handleSearch">
            <option value="all">전체</option>
            <option value="title">제목</option>
            <option value="writer">작성자</option>
            <option value="content">내용</option>
          </select>
          <div class="search-box">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
            <input
              v-model="keyword"
              class="search-input"
              :placeholder="isDocumentBoard ? '파일명 검색' : isGalleryBoard ? '제목 검색' : '검색어 입력'"
              @keyup.enter="handleSearch"
            />
            <button v-if="keyword" class="search-clear" @click="keyword=''; handleSearch()">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
        </div>
      </template>

      <button v-if="canWrite" class="action-btn" @click="goWrite">
        <svg v-if="isDocumentBoard" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
        <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        {{ writeLabel }}
      </button>
    </div>

    <!-- ===== 필터 바 (카테고리 있을 때만) ===== -->
    <div v-if="boardCategories.length" class="filter-bar">
      <div class="cat-chips">
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

      <div class="filter-right">
        <select v-if="!isDocumentBoard && !isGalleryBoard" v-model="searchType" class="search-type-select" @change="handleSearch">
          <option value="all">전체</option>
          <option value="title">제목</option>
          <option value="writer">작성자</option>
          <option value="content">내용</option>
        </select>
        <div class="search-box">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input
            v-model="keyword"
            class="search-input"
            :placeholder="isDocumentBoard ? '파일명 검색' : isGalleryBoard ? '제목 검색' : '검색어 입력'"
            @keyup.enter="handleSearch"
          />
          <button v-if="keyword" class="search-clear" @click="keyword=''; handleSearch()">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
      </div>
    </div>

    <!-- ===== 갤러리 뷰 ===== -->
    <template v-if="isGalleryBoard">
      <div v-loading="loading" class="gallery-grid">
        <div v-for="post in posts" :key="post.id" class="gallery-card" @click="goDetail(post)">
          <div class="gallery-thumb">
            <template v-if="displayUrls(post).length > 0">
              <div class="carousel-track" :style="{ transform: `translateX(-${getImgIdx(post.id, displayUrls(post).length) * 100}%)` }">
                <img v-for="(url, i) in displayUrls(post)" :key="i" :src="resolveFileUrl(url)" :alt="post.title" class="carousel-img" />
              </div>
              <template v-if="displayUrls(post).length > 1">
                <button class="carousel-btn prev" @click.stop="prevImg(post.id, displayUrls(post).length)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
                </button>
                <button class="carousel-btn next" @click.stop="nextImg(post.id, displayUrls(post).length)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
                </button>
                <div class="carousel-dots">
                  <span v-for="(_, i) in displayUrls(post)" :key="i" :class="['carousel-dot', i === getImgIdx(post.id, displayUrls(post).length) ? 'active' : '']"></span>
                </div>
              </template>
            </template>
            <template v-else-if="post.thumbnailUrl">
              <img :src="resolveFileUrl(post.thumbnailUrl)" :alt="post.title" class="thumb-img" />
            </template>
            <div v-else class="no-thumb">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
            </div>
            <span v-if="post.isNotice" class="gallery-notice-badge">공지</span>
            <span v-if="isNew(post.createdAt)" class="gallery-new-badge">NEW</span>
          </div>
          <div class="gallery-info">
            <div class="gallery-title">{{ post.title }}</div>
            <div class="gallery-meta">
              <span class="gallery-author-date">{{ post.writerName }} · {{ formatDate(post.createdAt) }}</span>
              <div class="gallery-stats">
                <span v-if="board?.useLike" class="gallery-stat">❤️ {{ post.likeCount }}</span>
                <span class="gallery-stat">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.4 8.4 0 0 1-9 8 9 9 0 0 1-4-1l-4 1 1-3.5A8.4 8.4 0 1 1 21 11.5z"/></svg>
                  {{ post.commentCount }}
                </span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="!loading && !posts.length" class="board-empty">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h8"/><path d="m14 17 3 3 4-4"/></svg>
          <p>등록된 게시글이 없습니다.</p>
        </div>
      </div>
    </template>

    <!-- ===== 자료실 뷰 ===== -->
    <template v-else-if="isDocumentBoard">
      <div v-loading="loading" class="doc-file-table card">
        <div class="ft-head">
          <span>파일명</span>
          <span class="tc">버전</span>
          <span class="tc">크기</span>
          <span class="tc">등록자</span>
          <span class="tc">등록일</span>
          <span class="tc">받기</span>
        </div>
        <div v-for="post in noticePosts" :key="`n-${post.id}`" class="ft-row notice-row" @click="goDetail(post)">
          <div class="ft-name-cell">
            <span class="ft-ext-badge" :style="extStyle(post.primaryFileName)">{{ extLabel(post.primaryFileName) }}</span>
            <div class="ft-name-info">
              <span class="ft-filename notice-name">{{ post.title }}</span>
              <span class="ft-cat" v-if="post.categoryName">{{ post.categoryName }}</span>
            </div>
          </div>
          <div class="tc"><span class="ft-version"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><polyline points="3 3 3 8 8 8"/><polyline points="12 7 12 12 15 14"/></svg>v1.0</span></div>
          <span class="tc ft-meta">{{ formatFileSize(post.primaryFileSize) }}</span>
          <span class="tc ft-meta">{{ post.writerName }}</span>
          <span class="tc ft-meta">{{ formatDate(post.createdAt) }}</span>
          <div class="tc"><button class="dl-btn" @click.stop="goDetail(post)"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg></button></div>
        </div>
        <div v-if="noticePosts.length && normalPosts.length" class="notice-divider"></div>
        <div v-for="post in normalPosts" :key="post.id" class="ft-row" @click="goDetail(post)">
          <div class="ft-name-cell">
            <span class="ft-ext-badge" :style="extStyle(post.primaryFileName)">{{ extLabel(post.primaryFileName) }}</span>
            <div class="ft-name-info">
              <span class="ft-filename">{{ post.primaryFileName || post.title }}</span>
              <span class="ft-cat" v-if="post.categoryName">{{ post.categoryName }}</span>
            </div>
          </div>
          <div class="tc"><span class="ft-version"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><polyline points="3 3 3 8 8 8"/><polyline points="12 7 12 12 15 14"/></svg>v{{ post.documentVersion || '1.0' }}</span></div>
          <span class="tc ft-meta">{{ formatFileSize(post.primaryFileSize) }}</span>
          <span class="tc ft-meta">{{ post.writerName }}</span>
          <span class="tc ft-meta">{{ formatDate(post.createdAt) }}</span>
          <div class="tc"><button class="dl-btn" @click.stop="goDetail(post)"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg></button></div>
        </div>
        <div v-if="!loading && !posts.length" class="board-empty">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h8"/><path d="m14 17 3 3 4-4"/></svg>
          <p>등록된 자료가 없습니다.</p>
        </div>
      </div>
    </template>

    <!-- ===== 웹진 뷰 ===== -->
    <template v-else-if="isWebzineBoard">
      <div v-loading="loading" class="webzine-list card">
        <article v-for="post in posts" :key="post.id" :class="['webzine-item', { notice: post.isNotice }]" @click="goDetail(post)">
          <div class="webzine-thumb">
            <img v-if="post.thumbnailUrl" :src="resolveFileUrl(post.thumbnailUrl)" :alt="post.title" class="thumb-img" />
            <div v-else class="no-thumb"><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg></div>
          </div>
          <div class="webzine-body">
            <div class="webzine-kicker">
              <span v-if="post.isNotice" class="notice-tag">공지</span>
              <span>{{ formatDate(post.createdAt) }}</span>
            </div>
            <h3 class="webzine-title">{{ post.title }}<span v-if="isNew(post.createdAt)" class="new-badge" style="margin-left:6px">N</span></h3>
            <p v-if="post.excerpt" class="webzine-excerpt">{{ post.excerpt }}</p>
            <div class="webzine-meta">
              <span>{{ post.writerName }}</span>
              <span v-if="showViewCount">· 조회 {{ post.viewCount }}</span>
              <span>· 댓글 {{ post.commentCount }}</span>
            </div>
          </div>
        </article>
        <div v-if="!loading && !posts.length" class="board-empty">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h8"/><path d="m14 17 3 3 4-4"/></svg>
          <p>등록된 게시글이 없습니다.</p>
        </div>
      </div>
    </template>

    <!-- ===== 리스트형 뷰 ===== -->
    <template v-else-if="isListBoard">
      <div v-loading="loading" class="list-view card">
        <div v-for="post in posts" :key="post.id" :class="['lv-item', { notice: post.isNotice }]" @click="goDetail(post)">
          <div v-if="post.thumbnailUrl" class="lv-thumb"><img :src="resolveFileUrl(post.thumbnailUrl)" :alt="post.title" /></div>
          <div class="lv-body">
            <div class="lv-kicker">
              <span v-if="post.isNotice" class="notice-tag">공지</span>
              <span v-if="post.categoryName" class="cat-tag">{{ post.categoryName }}</span>
            </div>
            <div class="lv-title-row">
              <h3 class="lv-title">{{ post.title }}</h3>
              <span v-if="isNew(post.createdAt)" class="new-badge">N</span>
            </div>
            <p v-if="post.excerpt" class="lv-excerpt">{{ post.excerpt }}</p>
            <div class="lv-meta">
              <span>{{ post.writerName }}</span>
              <span class="lv-sep">·</span>
              <span>{{ formatRelativeDate(post.createdAt) }}</span>
              <template v-if="showViewCount"><span class="lv-sep">·</span><span>조회 {{ post.viewCount }}</span></template>
              <span class="lv-sep">·</span><span>댓글 {{ post.commentCount }}</span>
            </div>
          </div>
        </div>
        <div v-if="!loading && !posts.length" class="board-empty">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h8"/><path d="m14 17 3 3 4-4"/></svg>
          <p>등록된 게시글이 없습니다.</p>
        </div>
      </div>
    </template>

    <!-- ===== 일반 테이블 뷰 (POST 기본) ===== -->
    <template v-else>
      <div v-loading="loading" class="post-table card">
        <div class="pt-head">
          <span class="tc">번호</span>
          <span>제목</span>
          <span class="tc">작성자</span>
          <span class="tc">작성일</span>
          <span v-if="showViewCount" class="tc">조회</span>
        </div>

        <div v-for="post in noticePosts" :key="`n-${post.id}`" class="pt-row notice-row" @click="goDetail(post)">
          <div class="tc"><span class="notice-tag">공지</span></div>
          <div class="pt-title-cell">
            <span class="notice-tag pt-badges-notice">공지</span>
            <span v-if="post.categoryName && selectedCategoryId === null" class="cat-name-chip">{{ post.categoryName }}</span>
            <span class="pt-title notice-title">{{ post.title }}</span>
            <span v-if="post.commentCount > 0" class="comment-count">[{{ post.commentCount }}]</span>
            <span v-if="isNew(post.createdAt)" class="new-badge">N</span>
            <svg v-if="post.hasFile" class="file-clip" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5 12.5 20a5 5 0 0 1-7-7l8.5-8.5a3.3 3.3 0 0 1 4.7 4.7L9.7 16.5a1.6 1.6 0 0 1-2.3-2.3l7.8-7.8"/></svg>
          </div>
          <span class="tc pt-meta">{{ post.writerName }}</span>
          <span class="tc pt-meta">{{ formatDate(post.createdAt) }}</span>
          <span v-if="showViewCount" class="tc pt-meta">{{ post.viewCount }}</span>
          <div class="pt-mobile-meta">
            <span class="pt-mobile-author">{{ post.writerName }}</span>
            <span>{{ formatRelativeDate(post.createdAt) }}</span>
            <span v-if="showViewCount" class="pt-mobile-stat"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z"/><circle cx="12" cy="12" r="3"/></svg>{{ post.viewCount }}</span>
            <span v-if="post.commentCount > 0" class="pt-mobile-stat pt-mobile-comment"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.4 8.4 0 0 1-9 8 9 9 0 0 1-4-1l-4 1 1-3.5A8.4 8.4 0 1 1 21 11.5z"/></svg>{{ post.commentCount }}</span>
            <svg v-if="post.hasFile" class="pt-mobile-file" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5 12.5 20a5 5 0 0 1-7-7l8.5-8.5a3.3 3.3 0 0 1 4.7 4.7L9.7 16.5a1.6 1.6 0 0 1-2.3-2.3l7.8-7.8"/></svg>
          </div>
        </div>

        <div v-if="noticePosts.length && normalPosts.length" class="notice-divider"></div>

        <div v-for="post in normalPosts" :key="post.id" class="pt-row" @click="goDetail(post)">
          <div class="tc pt-num">{{ post.rowNum }}</div>
          <div class="pt-title-cell">
            <span v-if="post.categoryName && selectedCategoryId === null" class="cat-name-chip">{{ post.categoryName }}</span>
            <span class="pt-title">{{ post.title }}</span>
            <span v-if="post.commentCount > 0" class="comment-count">[{{ post.commentCount }}]</span>
            <span v-if="isNew(post.createdAt)" class="new-badge">N</span>
            <svg v-if="post.hasFile" class="file-clip" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5 12.5 20a5 5 0 0 1-7-7l8.5-8.5a3.3 3.3 0 0 1 4.7 4.7L9.7 16.5a1.6 1.6 0 0 1-2.3-2.3l7.8-7.8"/></svg>
          </div>
          <span class="tc pt-meta">{{ post.writerName }}</span>
          <span class="tc pt-meta">{{ formatDate(post.createdAt) }}</span>
          <span v-if="showViewCount" class="tc pt-meta">{{ post.viewCount }}</span>
          <div class="pt-mobile-meta">
            <span class="pt-mobile-author">{{ post.writerName }}</span>
            <span>{{ formatRelativeDate(post.createdAt) }}</span>
            <span v-if="showViewCount" class="pt-mobile-stat"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z"/><circle cx="12" cy="12" r="3"/></svg>{{ post.viewCount }}</span>
            <span v-if="post.commentCount > 0" class="pt-mobile-stat pt-mobile-comment"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.4 8.4 0 0 1-9 8 9 9 0 0 1-4-1l-4 1 1-3.5A8.4 8.4 0 1 1 21 11.5z"/></svg>{{ post.commentCount }}</span>
            <svg v-if="post.hasFile" class="pt-mobile-file" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5 12.5 20a5 5 0 0 1-7-7l8.5-8.5a3.3 3.3 0 0 1 4.7 4.7L9.7 16.5a1.6 1.6 0 0 1-2.3-2.3l7.8-7.8"/></svg>
          </div>
        </div>

        <div v-if="!loading && !posts.length" class="board-empty">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h8"/><path d="m14 17 3 3 4-4"/></svg>
          <p>등록된 게시글이 없습니다.</p>
        </div>
      </div>
    </template>

    <!-- ===== 페이지네이션 ===== -->
    <div v-if="totalPages > 1" class="pagination">
      <button class="pg-btn" :disabled="page <= 1" @click="changePage(page - 1)">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <button v-for="n in pageNums" :key="n" :class="['pg-btn', n === page ? 'active' : '']" @click="changePage(n)">{{ n }}</button>
      <button class="pg-btn" :disabled="page >= totalPages" @click="changePage(page + 1)">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
      </button>
    </div>

  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMenuStore } from '@/stores/menu'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'
import { resolveFileUrl } from '@/utils/fileUrl'

const route = useRoute()
const router = useRouter()
const menuStore = useMenuStore()
const authStore = useAuthStore()

const activeMenu = computed(() => menuStore.findMenuByRouteParams(route.params))
const boardId = computed(() => route.params.boardId || activeMenu.value?.targetId)
// activeMenu.url이 있으면(직접 지정 URL/자동 넘버링 둘 다) 그 경로를 그대로 쓴다 —
// /menu/{id} 접두어 없이 접속한 경로를 게시글 상세/글쓰기 링크에서도 유지하기 위함
const basePath = computed(() => activeMenu.value?.url || (route.params.menuId ? `/menu/${route.params.menuId}` : `/board/${boardId.value}`))
// 직접 지정 URL/자동 넘버링 경로에서는 /posts/ 없이 바로 글번호가 붙는다(예: /notice/10177)
const postBasePath = computed(() => activeMenu.value?.url ? basePath.value : `${basePath.value}/posts`)

const board = ref(null)
const posts = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(15)
const gallerySize = ref(9)
const keyword = ref('')
const searchType = ref('title')
const selectedCategoryId = ref(null)

const boardCategories = computed(() => board.value?.categories || [])
const imgIndexMap = ref({})

function displayUrls(post) { return (post.imageUrls || []).slice(0, 3) }
function getImgIdx(postId, len) { return Math.min(imgIndexMap.value[postId] ?? 0, len - 1) }
function nextImg(postId, len) { imgIndexMap.value[postId] = (getImgIdx(postId, len) + 1) % len }
function prevImg(postId, len) { imgIndexMap.value[postId] = (getImgIdx(postId, len) - 1 + len) % len }

const noticePosts = computed(() => posts.value.filter(p => p.isNotice))
const normalPosts = computed(() => posts.value.filter(p => !p.isNotice))
const isGalleryBoard  = computed(() => board.value?.boardType === 'GALLERY')
const isWebzineBoard  = computed(() => board.value?.boardType === 'WEBZINE')
const isListBoard     = computed(() => board.value?.boardType === 'LIST')
const isDocumentBoard = computed(() => board.value?.boardType === 'DOCUMENT')
const isVisualBoard   = computed(() => isGalleryBoard.value || isWebzineBoard.value)

const boardCountLabel = computed(() => {
  if (isGalleryBoard.value) return '사진 게시물'
  if (isDocumentBoard.value) return '파일'
  return '게시글'
})

const writeLabel = computed(() => {
  if (isDocumentBoard.value) return '파일 업로드'
  if (isGalleryBoard.value) return '사진 올리기'
  return '글쓰기'
})

const totalPages = computed(() => {
  const s = isVisualBoard.value ? gallerySize.value : size.value
  return Math.ceil(total.value / s)
})

const pageNums = computed(() => {
  const tp = totalPages.value
  const cur = page.value
  const start = Math.max(1, cur - 2)
  const end = Math.min(tp, cur + 2)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})

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

function restoreCategory() {
  const cats = board.value?.categories || []
  if (!cats.length) { selectedCategoryId.value = null; return }
  const saved = sessionStorage.getItem(categoryStorageKey())
  if (saved === 'all' && board.value?.useAllCategory) { selectedCategoryId.value = null; return }
  if (saved && saved !== 'all') {
    const id = Number(saved)
    if (cats.some(c => c.id === id)) { selectedCategoryId.value = id; return }
  }
  selectedCategoryId.value = board.value?.useAllCategory ? null : cats[0].id
}

async function fetchPosts() {
  if (!boardId.value) return
  loading.value = true
  try {
    const pageSize = isVisualBoard.value ? gallerySize.value : size.value
    const params = { page: page.value - 1, size: pageSize }
    if (keyword.value) { params.keyword = keyword.value; params.searchType = searchType.value }
    if (selectedCategoryId.value !== null) params.categoryId = selectedCategoryId.value
    const res = await api.get(`/boards/${boardId.value}/posts`, { params })
    posts.value = res.data.data?.content || []
    total.value = res.data.data?.totalElements || 0
  } finally {
    loading.value = false
  }
}

function changePage(n) {
  if (n < 1 || n > totalPages.value) return
  page.value = n
  fetchPosts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function handleSearch() { page.value = 1; fetchPosts() }
function selectCategory(id) {
  selectedCategoryId.value = id
  sessionStorage.setItem(categoryStorageKey(), id === null ? 'all' : String(id))
  page.value = 1
  fetchPosts()
}

function goWrite() { router.push(`${basePath.value}/write`) }
function goDetail(row) { router.push(`${postBasePath.value}/${row.id}`) }

function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d)
  const today = new Date()
  if (dt.toDateString() === today.toDateString()) return dt.toTimeString().slice(0, 5)
  return `${dt.getFullYear()}.${String(dt.getMonth() + 1).padStart(2, '0')}.${String(dt.getDate()).padStart(2, '0')}`
}

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

function isNew(d) { return d && Date.now() - new Date(d).getTime() < 3 * 86400000 }

function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}

const EXT_MAP = {
  pdf:  { bg: 'rgba(214,69,63,0.12)', color: '#d6453f', label: 'PDF' },
  doc:  { bg: '#e9f0fe', color: '#2f5fd6', label: 'DOC' },
  docx: { bg: '#e9f0fe', color: '#2f5fd6', label: 'DOC' },
  xls:  { bg: '#e3f5ec', color: '#1f9d6b', label: 'XLS' },
  xlsx: { bg: '#e3f5ec', color: '#1f9d6b', label: 'XLS' },
  ppt:  { bg: 'rgba(217,119,6,0.1)', color: '#d97706', label: 'PPT' },
  pptx: { bg: 'rgba(217,119,6,0.1)', color: '#d97706', label: 'PPT' },
  zip:  { bg: '#eef1f6', color: '#929aab', label: 'ZIP' },
  rar:  { bg: '#eef1f6', color: '#929aab', label: 'ZIP' },
  hwp:  { bg: 'rgba(14,138,128,0.1)', color: '#0e8a80', label: 'HWP' },
  hwpx: { bg: 'rgba(14,138,128,0.1)', color: '#0e8a80', label: 'HWP' },
}
function getExtInfo(filename) {
  const ext = filename?.split('.').pop()?.toLowerCase()
  return EXT_MAP[ext] || { bg: '#eef1f6', color: '#566072', label: (ext?.toUpperCase() || 'FILE').slice(0, 4) }
}
function extStyle(filename) {
  const { bg, color } = getExtInfo(filename)
  return { background: bg, color }
}
function extLabel(filename) { return getExtInfo(filename).label }

watch(boardId, async () => {
  page.value = 1; keyword.value = ''; selectedCategoryId.value = null
  await fetchBoard(); fetchPosts()
})

onMounted(async () => {
  if (route.params.menuId || route.params.customSlug) await menuStore.fetchMenus()
  await fetchBoard(); fetchPosts()
})
</script>

<style scoped>
.board-page {
  display: flex;
  flex-direction: column;
  color: var(--t1);
}

.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

/* ===== 목록 툴바 ===== */
.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 14px;
}

.pg-count { margin: 0; font-size: 13.5px; color: var(--t2); }
.pg-count strong { color: var(--t1); font-weight: 700; }

.toolbar-right { display: flex; align-items: center; gap: 8px; margin-left: auto; }

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: var(--primary);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  padding: 10px 18px;
  border-radius: 9px;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: background 0.15s;
}
.action-btn:hover { background: var(--primary-strong); }

/* ===== 필터 바 ===== */
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.cat-chips { display: flex; gap: 6px; flex-wrap: wrap; }
.cat-chips-spacer { flex: 1; }

.cat-chip {
  border: 1px solid var(--border-strong);
  background: var(--surface);
  color: var(--t2);
  font-weight: 600;
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 18px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.cat-chip:hover { border-color: var(--primary); color: var(--primary); }
.cat-chip.active { border-color: var(--primary); background: var(--primary); color: #fff; }

.filter-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

.search-type-select {
  height: 34px;
  padding: 0 8px;
  border: 1px solid var(--border-strong);
  border-radius: 9px;
  background: var(--surface);
  color: var(--t2);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  flex-shrink: 0;
}
.search-type-select:hover { background: var(--surface-2); }

.search-box {
  display: flex;
  align-items: center;
  gap: 7px;
  background: var(--surface);
  border: 1px solid var(--border-strong);
  border-radius: 9px;
  padding: 7px 12px;
  width: 218px;
}
.search-box svg { flex-shrink: 0; color: var(--t3); }

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-family: inherit;
  font-size: 13px;
  color: var(--t1);
  min-width: 0;
}
.search-input::placeholder { color: var(--t3); }

.search-clear {
  background: none;
  border: none;
  color: var(--t3);
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

/* ===== 공통 배지 ===== */
.notice-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 5px;
  background: var(--primary);
  color: #fff;
  font-size: 10.5px;
  font-weight: 800;
  flex-shrink: 0;
}

.cat-name-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  background: var(--surface-2);
  color: var(--t2);
  flex-shrink: 0;
  white-space: nowrap;
}

.new-badge {
  display: inline-flex;
  align-items: center;
  font-size: 9.5px;
  font-weight: 800;
  padding: 1px 5px;
  border-radius: 5px;
  color: var(--new);
  border: 1px solid var(--new);
  flex-shrink: 0;
  line-height: 1.6;
}

.notice-divider { height: 1px; background: var(--border-strong); }

.board-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 60px 0;
  color: var(--t3);
  font-size: 14px;
  grid-column: 1 / -1;
  width: 100%;
}

/* ===== POST 테이블 ===== */
.post-table { width: 100%; }

.pt-head {
  display: grid;
  grid-template-columns: 70px 1fr 100px 76px 64px;
  gap: 10px;
  padding: 12px 18px;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--t3);
  border-bottom: 1px solid var(--border-strong);
}

.pt-row {
  display: grid;
  grid-template-columns: 70px 1fr 100px 76px 64px;
  gap: 10px;
  align-items: center;
  padding: 13px 18px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  transition: background 0.12s;
}
.pt-row:last-child { border-bottom: none; }
.pt-row:hover { background: var(--surface-2); }
.pt-row:hover .pt-title { color: var(--primary); }
.notice-row { background: var(--primary-weak); }
.notice-row:hover { background: color-mix(in srgb, var(--primary) 12%, transparent); }

.tc { text-align: center; display: flex; align-items: center; justify-content: center; }

.pt-num { font-size: 13px; color: var(--t3); font-weight: 600; }

.pt-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.pt-badges { display: inline-flex; align-items: center; gap: 6px; flex-shrink: 0; }
/* 데스크톱은 번호 칸에 이미 "공지" 표시가 있어 제목 셀 안 배지는 숨김 */
.pt-badges-notice { display: none; }

.pt-mobile-meta { display: none; }

.pt-title {
  font-size: 15px;
  color: var(--t1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.12s;
}
.notice-title { font-weight: 700; }

.comment-count {
  font-size: 12.5px;
  color: var(--primary);
  font-weight: 700;
  flex-shrink: 0;
}

.file-clip { color: var(--t3); flex-shrink: 0; }

.pt-meta { font-size: 14px; color: var(--t2); }

/* ===== 자료실 파일 테이블 ===== */
.doc-file-table { width: 100%; }

.ft-head {
  display: grid;
  grid-template-columns: 1fr 96px 80px 100px 80px 70px;
  gap: 10px;
  padding: 12px 18px;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--t3);
  border-bottom: 1px solid var(--border-strong);
}

.ft-row {
  display: grid;
  grid-template-columns: 1fr 96px 80px 100px 80px 70px;
  gap: 10px;
  align-items: center;
  padding: 12px 18px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  transition: background 0.12s;
}
.ft-row:last-child { border-bottom: none; }
.ft-row:hover { background: var(--surface-2); }
.notice-row { background: var(--primary-weak); }
.notice-row:hover { background: color-mix(in srgb, var(--primary) 12%, transparent); }

.ft-name-cell { display: flex; align-items: center; gap: 11px; min-width: 0; }

.ft-ext-badge {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9.5px;
  font-weight: 800;
  flex-shrink: 0;
}

.ft-name-info { min-width: 0; display: flex; flex-direction: column; gap: 2px; }

.ft-filename {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--t1);
  transition: color 0.12s;
}
.ft-row:hover .ft-filename { color: var(--primary); }
.notice-name { font-weight: 700; }

.ft-cat { font-size: 11.5px; color: var(--t3); }

.ft-version {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  font-weight: 700;
  color: var(--t2);
  background: var(--surface-2);
  padding: 4px 9px;
  border-radius: 6px;
  white-space: nowrap;
}

.ft-meta { font-size: 13px; color: var(--t2); }

.dl-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: var(--primary-weak);
  color: var(--primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.dl-btn:hover { background: var(--primary); color: #fff; }

/* ===== 갤러리 ===== */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.gallery-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: border-color 0.15s;
}
.gallery-card:hover { border-color: var(--border-strong); }

.gallery-thumb {
  position: relative;
  aspect-ratio: 4 / 3;
  background: repeating-linear-gradient(135deg, var(--surface-2), var(--surface-2) 11px, var(--surface-3) 11px, var(--surface-3) 22px);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.thumb-img { width: 100%; height: 100%; object-fit: cover; }

.carousel-track {
  display: flex;
  width: 100%;
  height: 100%;
  transition: transform 0.32s cubic-bezier(0.4, 0, 0.2, 1);
}
.carousel-img { flex-shrink: 0; width: 100%; height: 100%; object-fit: cover; }
.gallery-card:hover .carousel-img { transform: scale(1.03); }

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
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.18s;
}
.carousel-btn.prev { left: 6px; }
.carousel-btn.next { right: 6px; }
.gallery-card:hover .carousel-btn { opacity: 1; }
.carousel-btn:hover { background: rgba(0,0,0,0.7); }

.carousel-dots {
  position: absolute;
  bottom: 7px;
  left: 0; right: 0;
  display: flex;
  justify-content: center;
  gap: 4px;
}
.carousel-dot {
  width: 5px; height: 5px;
  border-radius: 50%;
  background: rgba(255,255,255,0.5);
  transition: background 0.15s, transform 0.15s;
}
.carousel-dot.active { background: #fff; transform: scale(1.3); }

.no-thumb { color: var(--t3); display: flex; align-items: center; justify-content: center; }

.gallery-notice-badge {
  position: absolute;
  top: 10px; right: 10px;
  background: var(--primary);
  color: #fff;
  font-size: 10px; font-weight: 800;
  padding: 3px 8px;
  border-radius: 6px;
}

.gallery-new-badge {
  position: absolute;
  top: 10px; left: 10px;
  font-size: 9.5px; font-weight: 800;
  color: #fff;
  background: var(--new);
  padding: 3px 8px;
  border-radius: 6px;
}

.gallery-info { padding: 13px 15px; }

.gallery-title {
  font-size: 14.5px;
  font-weight: 700;
  letter-spacing: -0.01em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 9px;
  color: var(--t1);
  transition: color 0.12s;
}
.gallery-card:hover .gallery-title { color: var(--primary); }

.gallery-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.gallery-author-date { font-size: 12px; color: var(--t3); }

.gallery-stats {
  display: flex;
  align-items: center;
  gap: 11px;
  font-size: 12px;
  color: var(--t3);
  font-weight: 600;
}

.gallery-stat { display: inline-flex; align-items: center; gap: 3px; }

/* ===== 웹진 ===== */
.webzine-list { overflow: hidden; }

.webzine-item {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 18px;
  padding: 18px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  transition: background 0.12s;
}
.webzine-item:last-child { border-bottom: none; }
.webzine-item:hover { background: var(--surface-2); }
.webzine-item.notice { background: var(--primary-weak); }

.webzine-thumb {
  aspect-ratio: 16 / 10;
  border-radius: 8px;
  overflow: hidden;
  background: var(--surface-2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.webzine-body {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  min-width: 0;
}

.webzine-kicker {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--t3);
}

.webzine-title {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: var(--t1);
  letter-spacing: -0.02em;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  transition: color 0.12s;
}
.webzine-item:hover .webzine-title { color: var(--primary); }

.webzine-excerpt {
  margin: 0;
  font-size: 13px;
  color: var(--t2);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.webzine-meta { font-size: 12px; color: var(--t3); display: flex; gap: 4px; flex-wrap: wrap; }

/* ===== 리스트형 ===== */
.list-view { overflow: hidden; }

.lv-item {
  display: flex;
  align-items: stretch;
  gap: 14px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  transition: background 0.12s;
}
.lv-item:last-child { border-bottom: none; }
.lv-item:hover { background: var(--surface-2); }
.lv-item.notice { background: var(--primary-weak); }

.lv-thumb { flex-shrink: 0; width: 120px; border-radius: 8px; overflow: hidden; }
.lv-thumb img { width: 100%; height: 100%; object-fit: cover; }

.lv-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 5px; justify-content: center; }
.lv-kicker { display: flex; align-items: center; gap: 6px; }
.cat-tag { font-size: 12px; color: var(--t3); font-weight: 600; }

.lv-title-row { display: flex; align-items: center; gap: 5px; }

.lv-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--t1);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  transition: color 0.12s;
}
.lv-item:hover .lv-title { color: var(--primary); }

.lv-excerpt {
  margin: 0;
  font-size: 13px;
  color: var(--t3);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.lv-meta { display: flex; align-items: center; gap: 4px; font-size: 12px; color: var(--t3); }
.lv-sep { color: var(--border-strong); }

/* ===== 페이지네이션 ===== */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin-top: 24px;
}

.pg-btn {
  min-width: 34px;
  height: 34px;
  padding: 0 6px;
  border: 1px solid var(--border);
  background: var(--surface);
  border-radius: 9px;
  color: var(--t2);
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.12s;
  font-family: inherit;
}
.pg-btn:hover:not(:disabled) { background: var(--surface-2); }
.pg-btn.active { border: none; background: var(--primary); color: #fff; }
.pg-btn:disabled { opacity: 0.35; cursor: default; }

/* ===== 반응형 ===== */
@media (max-width: 768px) {
  .gallery-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }

  /* ===== 일반 테이블 뷰 → 모바일 리스트 카드로 전환 ===== */
  .pt-head { display: none; }
  .pt-row {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    padding: 16px 18px;
  }
  .pt-row > .tc,
  .pt-row > .pt-num,
  .pt-row > .pt-meta { display: none; }
  .pt-title-cell { flex-wrap: wrap; width: 100%; }
  .pt-badges-notice { display: inline-flex; }
  .pt-title {
    font-size: 15.5px;
    white-space: normal;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    flex-basis: 100%;
  }
  .comment-count, .file-clip { display: none; }
  .pt-mobile-meta {
    display: flex;
    align-items: center;
    gap: 10px;
    width: 100%;
    font-size: 12px;
    color: var(--t3);
  }
  .pt-mobile-author { font-weight: 600; color: var(--t2); }
  .pt-mobile-stat { display: inline-flex; align-items: center; gap: 3px; }
  .pt-mobile-comment { color: var(--primary); font-weight: 700; }
  .pt-mobile-file { color: var(--t3); margin-left: auto; }

  .ft-head,
  .ft-row { grid-template-columns: 1fr 70px; }
  .ft-head > *:nth-child(n+3),
  .ft-row > *:nth-child(n+3) { display: none; }

  /* ===== 상단 툴바(건수/검색/글쓰기) 정렬 ===== */
  .list-toolbar {
    flex-wrap: wrap;
    row-gap: 10px;
    column-gap: 8px;
  }
  .pg-count { flex-basis: 100%; }
  .toolbar-right { flex: 1; min-width: 0; margin-left: 0; }
  .action-btn { flex-shrink: 0; margin-left: auto; }

  /* ===== 카테고리 필터 바 정렬 (카테고리 칩은 가로 스크롤) ===== */
  .filter-bar { flex-direction: column; align-items: stretch; }
  .cat-chips { flex-wrap: nowrap; overflow-x: auto; padding-bottom: 2px; }
  .cat-chips::-webkit-scrollbar { display: none; }
  .filter-right { width: 100%; }

  .search-box { width: auto; flex: 1; min-width: 0; }

  /* ===== 게시판 목록: 모바일에서 좌우 여백 없이 꽉 차게 (모든 유형 공통) ===== */
  /* post-table/doc-file-table는 width:100%가 명시돼 있어 음수 마진만으로는
     폭이 늘어나지 않으므로 width:auto로 함께 재정의한다 */
  .post-table,
  .doc-file-table,
  .webzine-list,
  .list-view,
  .gallery-grid {
    margin: 0 -20px;
    width: auto;
  }
  .post-table,
  .doc-file-table,
  .webzine-list,
  .list-view {
    border-radius: 0;
    border-left: none;
    border-right: none;
    box-shadow: none;
  }
}

@media (max-width: 600px) {
  .post-table,
  .doc-file-table,
  .webzine-list,
  .list-view,
  .gallery-grid {
    margin: 0 -16px;
    width: auto;
  }
}
</style>
