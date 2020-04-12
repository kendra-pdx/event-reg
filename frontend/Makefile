build:
	@docker build \
	-t kendra/event-ui \
	.

run:
	@docker run \
	-p 8081:80 \
	kendra/event-ui

run-dev:
	@docker run \
	-p 8081:80 \
	-v `pwd`/dist:/usr/share/nginx/html \
	kendra/event-ui

