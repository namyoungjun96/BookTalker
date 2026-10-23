#!/bin/bash
#source /etc/bash.bashrc
source /home/ubuntu/.env

NGINX_LINK=/etc/nginx/sites-enabled/booktalker
BLUE_CONF=/etc/nginx/sites-available/booktalker-blue
GREEN_CONF=/etc/nginx/sites-available/booktalker-green

if [ "$(readlink $NGINX_LINK)" == "$BLUE_CONF" ]; then
    CURRENT_PORT=8010; NEXT_PORT=8011
    CURRENT_CONF=$BLUE_CONF; NEXT_CONF=$GREEN_CONF
else
    CURRENT_PORT=8011; NEXT_PORT=8010
    CURRENT_CONF=$GREEN_CONF; NEXT_CONF=$BLUE_CONF
fi

rollback() {
        sh backend.sh stop $NEXT_PORT
        curl -X POST -H "Content-Type: application/json" \
                -d "{\"content\": \"배포 실패 - $1\"}" \
                "$DISCORD_WEBHOOK_URL"
	echo "$1"
        exit 1
}

JAR_PATH="/home/ubuntu/apps/backend/versions/$(ls -t /home/ubuntu/apps/backend/versions/ | head -1)"

sh backend.sh stop $NEXT_PORT $JAR_PATH

sh backend.sh start $NEXT_PORT $JAR_PATH

for _ in {1..20} ; do
        response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$NEXT_PORT/actuator/health")
        if [ "$response" == "200" ]; then break
        fi
        sleep 10
done

if [ "$response" != "200" ]; then
        rollback "healthCheck failed"
fi

echo "healthCheck success"

sudo ln -sf "$NEXT_CONF" "$NGINX_LINK"

if ! sudo nginx -t; then
        echo "nginx is failed"
        sudo ln -sf "$CURRENT_CONF" "$NGINX_LINK"
        rollback "nginx config test failed"
fi
        
sudo systemctl reload nginx
ln -sf "$JAR_PATH" /home/ubuntu/apps/backend/book_talker_backend.jar
sh backend.sh stop $CURRENT_PORT
echo "deploy success"
