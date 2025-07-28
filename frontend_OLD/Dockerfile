# ==================================
# STAGE 1: Build Angular Application
# ==================================
FROM node:20-alpine AS builder

# Set working directory
WORKDIR /app

# Copy package files first for better dependency caching
COPY package*.json ./

# Install dependencies with npm ci (faster, more reliable)
RUN npm ci && npm cache clean --force

# Copy Angular configuration files
COPY angular.json tsconfig*.json ./

# Copy source code
COPY src/ ./src/

# Build the app for production
RUN npm run build --configuration=production

# ==================================
# STAGE 2: Serve with Nginx
# ==================================
FROM nginx:1.25-alpine

# Install wget for health checks
RUN apk add --no-cache wget

# Copy built app from builder stage
#COPY --from=builder /app/dist/frontend /usr/share/nginx/html
COPY --from=builder /app/dist/frontend/browser/ /usr/share/nginx/html/

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Create nginx user and set proper permissions
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Health check for container monitoring
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/health || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
