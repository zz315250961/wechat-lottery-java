#!/usr/bin/env bash
set -euo pipefail

release_root=/opt/wechat-lottery-java
stage=/tmp/lottery-java-deploy
backup_root=/opt/backups/lottery-$(date +%Y%m%d-%H%M%S)
domain=lottery.zperme.top

mkdir -p "$backup_root" "$release_root/backend" "$release_root/frontend" "$release_root/uploads"
cp -a /etc/systemd/system/wechat-lottery.service "$backup_root/" 2>/dev/null || true
cp -a /etc/nginx/conf.d/lottery.zperme.top.conf "$backup_root/" 2>/dev/null || true
tar -C /opt -czf "$backup_root/old-app.tar.gz" wechat-lottery
mysqldump --single-transaction wechat_lottery_prod > "$backup_root/wechat_lottery_prod.sql"

mysql -e "CREATE DATABASE IF NOT EXISTS wechat_lottery_java CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
mysql --default-character-set=utf8mb4 wechat_lottery_java < "$stage/ry_20260417.sql"
mysql --default-character-set=utf8mb4 wechat_lottery_java < "$stage/quartz.sql"
mysql --default-character-set=utf8mb4 < "$stage/lottery_schema.sql"
mysql --default-character-set=utf8mb4 < "$stage/test_data.sql"

install -m 0644 "$stage/ruoyi-admin.jar" "$release_root/backend/ruoyi-admin.jar"
rm -rf "$release_root/frontend"/*
tar -C "$release_root/frontend" -xzf "$stage/frontend-dist.tar.gz"

db_password=$(openssl rand -hex 24)
mysql -e "CREATE USER IF NOT EXISTS 'lottery_app'@'localhost' IDENTIFIED BY '$db_password'; ALTER USER 'lottery_app'@'localhost' IDENTIFIED BY '$db_password'; GRANT ALL PRIVILEGES ON wechat_lottery_java.* TO 'lottery_app'@'localhost'; FLUSH PRIVILEGES"
mysql -e "DROP USER IF EXISTS 'lottery_app'@'127.0.0.1'; FLUSH PRIVILEGES"
token_secret=$(openssl rand -hex 32)
cat > "$release_root/.env" <<EOF
MYSQL_URL=jdbc:mysql://localhost:3306/wechat_lottery_java?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia%2FShanghai&allowPublicKeyRetrieval=true
MYSQL_USERNAME=lottery_app
MYSQL_PASSWORD=$db_password
TOKEN_SECRET=$token_secret
LOTTERY_UPLOAD_PATH=$release_root/uploads
LOTTERY_PUBLIC_BASE_URL=https://$domain
WECHAT_REQUIRE_FOLLOW=false
EOF
chmod 600 "$release_root/.env"

cat > /etc/systemd/system/wechat-lottery.service <<'EOF'
[Unit]
Description=Lucky Lottery Java Service
After=network.target mysql.service redis.service

[Service]
Type=simple
WorkingDirectory=/opt/wechat-lottery-java/backend
EnvironmentFile=/opt/wechat-lottery-java/.env
ExecStart=/usr/bin/java -Xms128m -Xmx384m -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC -jar /opt/wechat-lottery-java/backend/ruoyi-admin.jar
MemoryHigh=600M
MemoryMax=700M
CPUQuota=100%
Nice=5
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
EOF

cat > /etc/nginx/conf.d/lottery.zperme.top.conf <<'EOF'
server {
    listen 80;
    server_name lottery.zperme.top;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    http2 on;
    server_name lottery.zperme.top;
    ssl_certificate /etc/letsencrypt/live/lottery.zperme.top/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/lottery.zperme.top/privkey.pem;

    root /opt/wechat-lottery-java/frontend;
    index index.html;
    client_max_body_size 20m;
    gzip_static on;

    location /dev-api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /prod-api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ^~ /profile/ {
        proxy_pass http://127.0.0.1:8080/profile/;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ~* \.(?:js|css|png|jpg|jpeg|gif|svg|ico|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public, immutable";
        try_files $uri =404;
    }

    location / {
        add_header Cache-Control "no-cache";
        try_files $uri $uri/ /index.html;
    }
}
EOF

nginx -t
systemctl daemon-reload
systemctl enable redis wechat-lottery
systemctl restart redis wechat-lottery

for attempt in $(seq 1 30); do
  if curl -fsS http://127.0.0.1:8080/lottery/public >/dev/null; then
    systemctl reload nginx
    echo "DEPLOY_OK backup=$backup_root"
    exit 0
  fi
  sleep 2
done

journalctl -u wechat-lottery -n 80 --no-pager
exit 1
