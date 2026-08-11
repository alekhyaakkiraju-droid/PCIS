.PHONY: lint lint-prometheus lint-alertmanager lint-runbooks test-prometheus-rules

lint: lint-prometheus lint-alertmanager lint-runbooks

lint-prometheus:
	bash observability/prometheus/promtool-check.sh

lint-alertmanager:
	bash observability/alertmanager/amtool-check.sh

lint-runbooks:
	bash observability/runbooks/validate-runbook-links.sh

test-prometheus-rules:
	bash observability/test/test-rules.sh
