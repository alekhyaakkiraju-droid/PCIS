.PHONY: lint-prometheus lint-alertmanager test-prometheus-rules

lint-prometheus:
	bash observability/prometheus/promtool-check.sh

lint-alertmanager:
	bash observability/alertmanager/amtool-check.sh

test-prometheus-rules:
	bash observability/test/test-rules.sh
