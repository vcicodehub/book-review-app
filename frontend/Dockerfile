# Stage 1: Build Angular app
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

RUN npm run build

# Stage 2: Serve with nginx
FROM nginx:alpine

COPY --from=build /app/dist/pdf-ai-review-frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]