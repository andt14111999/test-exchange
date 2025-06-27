APP_NAME=exchange-client
PORT=3000

build:
	docker build -t $(APP_NAME) .

run:
	docker run --name $(APP_NAME) -p $(PORT):3000 $(APP_NAME)

run-detached:
	docker run -d --name $(APP_NAME) -p $(PORT):3000 $(APP_NAME)

stop:
	docker stop $(APP_NAME) || true
	docker rm $(APP_NAME) || true

clean: stop
	docker rmi $(APP_NAME) || true
	docker system prune -f

logs:
	docker logs -f $(APP_NAME)
