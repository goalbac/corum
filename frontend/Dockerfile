# ---- Build stage ----
FROM node:22-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- Runtime stage: nginx로 정적 파일 서빙 ----
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
# nginx 설정은 docker-compose에서 볼륨으로 주입
EXPOSE 80
