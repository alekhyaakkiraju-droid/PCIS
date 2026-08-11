.PHONY: lint lint-prometheus lint-alertmanager lint-runbooks test-prometheus-rules validate-data-dictionary test-data-dictionary

lint: lint-prometheus lint-alertmanager lint-runbooks

validate-data-dictionary:
	python3 build/scripts/validate-data-dictionary.py

test-data-dictionary:
	python3 -m unittest discover -s docs/tests -v
	python3 -m unittest discover -s build/tests -p 'test_validate_data_dictionary.py' -v

lint-prometheus:
	bash observability/prometheus/promtool-check.sh

lint-alertmanager:
	bash observability/alertmanager/amtool-check.sh

lint-runbooks:
	bash observability/runbooks/validate-runbook-links.sh

test-prometheus-rules:
	bash observability/test/test-rules.sh
