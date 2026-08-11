.PHONY: lint-prometheus test-prometheus-rules

lint-prometheus:
	bash observability/prometheus/promtool-check.sh

test-prometheus-rules:
	bash observability/test/test-rules.sh
