FROM node:20-slim
ENV NODE_ENV=production
WORKDIR /app

# Copilot CLI (komenda `copilot`) potrzebna dla copilot-sdk
RUN npm install -g --no-fund --no-audit @github/copilot

# Dependencies
COPY package.json package-lock.json* ./
RUN if [ -f package-lock.json ]; then npm ci --omit=dev; else npm install --omit=dev; fi

# App sources
COPY . .

EXPOSE 8788

RUN chown -R node:node /app
USER node

CMD ["node", "server.js"]
