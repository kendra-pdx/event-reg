build:
	@docker build \
	-t kendra/event-ui \
	.

run:
	@docker run \
	-p 8081:80 \
	kendra/event-ui

run-dev:
	-@docker create \
	-p 8081:80 \
	-v $(cd dist && pwd):/usr/share/nginx/html \
	--name ui \
	kendra/event-ui

	@docker start -ai ui

