build:
	@docker build \
	-t kendra/event-server \
	.

run:
	@docker run \
	-p 8080:8080 \
	kendra/event-server

run-dev:
	@docker rm -f app || true

	@docker run \
	-p 8080:8080 \
	--name app \
	--link database \
	-v `pwd`/backend-server/target/scala-2.13/:/app \
	kendra/event-server

run-db:
	@docker rm database || true

	@docker run \
	-e POSTGRES_PASSWORD=event-reg \
	-p 5432:5432 \
	-h postgres \
	--name database \
	postgres:12

clean:
	docker rmi (docker images -q) --force
	docker rm $(docker ps -q -f status=exited)