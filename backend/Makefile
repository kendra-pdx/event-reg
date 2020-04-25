build:
	@docker build \
	-t kendra/event-server \
	.

run:
	@docker run \
	-p 8080:8080 \
	--name app \
	--link database \
	-v `pwd`/backend-server/target/scala-2.13/:/app \
	kendra/event-server

run-dev:
	-@docker create \
	-p 8080:8080 \
	--name app \
	--link database \
	-v $(shell cd ./backend-server/target/scala-2.13 && pwd)/:/app \
	kendra/event-server

	@docker start \
	-ai \
	app

run-db:
	-@docker create \
	-e POSTGRES_PASSWORD=event-reg \
	-p 5432:5432 \
	-h postgres \
	-v $(shell cd pg_data && pwd):/var/lib/postgresql/data \
	--name database \
	postgres:12

	@docker start -ai database

clean:
	docker rmi (docker images -q) --force
	docker rm $(docker ps -q -f status=exited)