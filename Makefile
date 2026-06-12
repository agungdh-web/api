.PHONY: help migrate refresh

help:
	@echo "Available commands:"
	@echo "  make migrate     Run Flyway migrations"
	@echo "  make refresh     Drop all tables and re-run migrations"
	@echo "  make help        Show this help message"

migrate:
	./mvnw -q exec:java \
		-Dexec.mainClass="id.my.agungdh.api.cli.FlywayCli" \
		-Dexec.args="migrate"

refresh:
	@echo -n "This will DROP all database tables! Continue? [y/N]: "; \
	read REPLY; \
	if [ "$$REPLY" = "y" ]; then \
		./mvnw -q exec:java \
			-Dexec.mainClass="id.my.agungdh.api.cli.FlywayCli" \
			-Dexec.args="refresh"; \
	else \
		echo "Cancelled."; \
	fi
