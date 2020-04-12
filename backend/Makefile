build:
	@docker build \
	-t kendra/event-server \
	.

run:
	@docker run \
	-p 8080:8080 \
	kendra/event-server

run-dev:
	@docker run \
	-p 8080:8080 \
	-v `pwd`/backend-server/target/scala-2.13/:/app \
	kendra/event-server