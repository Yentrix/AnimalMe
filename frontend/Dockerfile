FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./

RUN npm install --legacy-peer-deps

COPY . .

RUN npm run build -- --configuration=production


FROM nginx:alpine

# Directorio para archivos compartidos (uploads)
RUN mkdir -p /usr/share/nginx/html/uploads

COPY --from=build /app/dist/animalme/browser/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

VOLUME ["/usr/share/nginx/html/uploads"]

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]