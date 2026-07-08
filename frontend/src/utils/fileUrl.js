// 백엔드가 내려주는 파일 경로(/api/files/...)는 프론트와 백엔드가 같은 오리진일 때만
// 그대로 쓸 수 있다. 프론트(Cloudflare Pages)와 백엔드(Tunnel)가 서로 다른 도메인인
// 배포에서는 파일 서버 오리진을 붙여줘야 하므로, 이미지/파일 URL을 렌더링하는 곳에서는
// 항상 이 함수를 거치게 한다.
const API_BASE = import.meta.env.VITE_API_BASE_URL || ''
const FILE_ORIGIN = API_BASE.replace(/\/api\/?$/, '')

export function resolveFileUrl(path) {
  if (!path) return path
  if (!FILE_ORIGIN) return path
  return path.startsWith('/api/') ? FILE_ORIGIN + path : path
}
