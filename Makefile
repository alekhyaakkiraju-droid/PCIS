.PHONY: lint lint-prometheus lint-alertmanager lint-runbooks lint-semgrep test-prometheus-rules test-semgrep validate-data-dictionary test-data-dictionary test-monetary-precision

lint: lint-prometheus lint-alertmanager lint-runbooks

lint-semgrep:
	semgrep --config semgrep/rules --error services/

test-semgrep:
	semgrep --test --config semgrep/rules semgrep/tests

validate-data-dictionary:
	python3 build/scripts/validate-data-dictionary.py

test-data-dictionary:
	python3 -m unittest discover -s docs/tests -v
	python3 -m unittest discover -s build/tests -p 'test_validate_data_dictionary.py' -v

test-monetary-precision:
	cd shared-libs/pcis-schema && mvn test -Dtest='MonetaryPrecisionGateTest,EntityBigDecimalGateTest,MonetaryPrecisionValidatorTest,DataDictionaryMonetaryLoaderTest'

lint-prometheus:
	bash observability/prometheus/promtool-check.sh

lint-alertmanager:
	bash observability/alertmanager/amtool-check.sh

lint-runbooks:
	bash observability/runbooks/validate-runbook-links.sh

test-prometheus-rules:
	bash observability/test/test-rules.sh
