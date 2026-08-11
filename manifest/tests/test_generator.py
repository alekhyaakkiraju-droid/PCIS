"""
Unit tests for manifest/generate_manifest.py (WO-001)

Run with:  python3 -m pytest manifest/tests/ -v
       or: python3 manifest/tests/test_generator.py
"""
import hashlib
import os
import sys
import tempfile
import unittest
from pathlib import Path

# Ensure manifest/ is on sys.path
sys.path.insert(0, str(Path(__file__).parent.parent))

from generate_manifest import (
    CobolPrologueParser,
    ProcedureDivisionScanner,
    DdsReferenceExtractor,
    ClMemberScanner,
    classify_file,
    compute_sha256,
    count_lines,
    dump_yaml,
    _yaml_scalar,
)

FIXTURES = Path(__file__).parent.parent / 'test-fixtures'
SAMPLE_PROLOGUE = FIXTURES / 'sample-prologue.cbl'
SAMPLE_NO_PROLOGUE = FIXTURES / 'sample-no-prologue.cbl'
SAMPLE_DSPF = FIXTURES / 'sample.dspf'
SAMPLE_CLLE = FIXTURES / 'sample.clle'


class TestCobolPrologueParser(unittest.TestCase):

    def test_full_prologue_calls(self):
        source = SAMPLE_PROLOGUE.read_text()
        result = CobolPrologueParser().parse(source)
        self.assertIn('AUDLOG01', result['calls'])
        self.assertIn('SECCHK01', result['calls'])

    def test_full_prologue_tables(self):
        source = SAMPLE_PROLOGUE.read_text()
        result = CobolPrologueParser().parse(source)
        self.assertIn('CUSTOMER_T', result['tables'])
        self.assertIn('POLICY_T', result['tables'])

    def test_full_prologue_ui(self):
        source = SAMPLE_PROLOGUE.read_text()
        result = CobolPrologueParser().parse(source)
        self.assertIn('CUSMNTD1', result['ui'])

    def test_no_prologue_returns_empty(self):
        source = SAMPLE_NO_PROLOGUE.read_text()
        result = CobolPrologueParser().parse(source)
        self.assertEqual(result['calls'], [])
        self.assertEqual(result['tables'], [])
        self.assertEqual(result['ui'], [])

    def test_no_duplicate_entries(self):
        source = SAMPLE_PROLOGUE.read_text()
        result = CobolPrologueParser().parse(source)
        self.assertEqual(len(result['calls']), len(set(result['calls'])))


class TestProcedureDivisionScanner(unittest.TestCase):

    def test_finds_call_literals(self):
        source = SAMPLE_PROLOGUE.read_text()
        result = ProcedureDivisionScanner().scan(source)
        self.assertIn('AUDLOG01', result['calls'])
        self.assertIn('SECCHK01', result['calls'])

    def test_finds_copy_statements(self):
        source = SAMPLE_PROLOGUE.read_text()
        result = ProcedureDivisionScanner().scan(source)
        self.assertIn('CUSTCOPY', result['copies'])

    def test_no_procedure_division_returns_empty(self):
        source = "       DATA DIVISION.\n       WORKING-STORAGE SECTION.\n"
        result = ProcedureDivisionScanner().scan(source)
        self.assertEqual(result['calls'], [])

    def test_call_not_before_procedure(self):
        # Ensure CALL in a comment before PROCEDURE DIVISION is not captured
        source = (
            "      * CALLS: TESTPGM\n"
            "       PROCEDURE DIVISION.\n"
            "       0000-MAIN.\n"
            "           CALL 'REALPGM' USING WS-X.\n"
        )
        result = ProcedureDivisionScanner().scan(source)
        self.assertIn('REALPGM', result['calls'])
        self.assertNotIn('TESTPGM', result['calls'])


class TestDdsReferenceExtractor(unittest.TestCase):

    def test_extracts_pfile(self):
        source = SAMPLE_DSPF.read_text()
        result = DdsReferenceExtractor().extract(source)
        self.assertIn('CUSTOMER_T', result['pfile'])

    def test_extracts_ref(self):
        source = SAMPLE_DSPF.read_text()
        result = DdsReferenceExtractor().extract(source)
        self.assertIn('CUSTOMER_T', result['ref'])

    def test_ignores_comment_lines(self):
        # Comment line (col 7 = *) should not contribute refs
        source = "     A*PFILE(IGNORED_T)\n     A          R RECF\n"
        result = DdsReferenceExtractor().extract(source)
        self.assertNotIn('IGNORED_T', result['pfile'])

    def test_no_refs_returns_empty(self):
        source = "     A          R BAREF\n     A            FLDNAME  10A\n"
        result = DdsReferenceExtractor().extract(source)
        self.assertEqual(result['pfile'], [])
        self.assertEqual(result['ref'], [])


class TestClMemberScanner(unittest.TestCase):

    def test_extracts_crtpgm(self):
        source = SAMPLE_CLLE.read_text()
        result = ClMemberScanner().scan(source)
        self.assertIn('TSTPROG1', result['crtpgm'])

    def test_extracts_crtdspf(self):
        source = SAMPLE_CLLE.read_text()
        result = ClMemberScanner().scan(source)
        self.assertIn('TSTDSPF1', result['crtdspf'])

    def test_extracts_crtprtf(self):
        source = SAMPLE_CLLE.read_text()
        result = ClMemberScanner().scan(source)
        self.assertIn('TSTPRTF1', result['crtprtf'])

    def test_extracts_sbmjob(self):
        source = SAMPLE_CLLE.read_text()
        result = ClMemberScanner().scan(source)
        self.assertIn('TSTPROG1', result['sbmjob'])

    def test_empty_cl_returns_empty_lists(self):
        result = ClMemberScanner().scan("PGM\nENDPGM\n")
        for key in ('calls', 'sbmjob', 'crtpgm', 'crtdspf', 'crtprtf',
                    'crtsqlcbli'):
            self.assertEqual(result[key], [], f"Expected empty {key}")


class TestFileClassification(unittest.TestCase):

    def test_cobol_extension(self):
        self.assertEqual(classify_file(Path('CLM006B.cbl')), ('cobol', 'shipped'))

    def test_dspf_extension(self):
        self.assertEqual(classify_file(Path('CLMFNLD1.dspf')), ('dds', 'shipped'))

    def test_cl_extension(self):
        self.assertEqual(classify_file(Path('PCIS_CRTOBJ.clle')), ('cl', 'shipped'))

    def test_design_doc_markdown(self):
        t, _ = classify_file(Path('CUS_Module_Design_Document.md'))
        self.assertEqual(t, 'design-doc')

    def test_modernization_doc_markdown(self):
        t, _ = classify_file(Path('PCIS_Enterprise_Architecture.md'))
        self.assertEqual(t, 'modernization-doc')

    def test_analysis_doc_markdown(self):
        t, _ = classify_file(Path('PCIS_Artifact_Gap_Analysis.md'))
        self.assertEqual(t, 'analysis-doc')


class TestChecksumAndLineCount(unittest.TestCase):

    def test_sha256_matches_hashlib(self):
        content = b'Hello, PCIS!\n'
        with tempfile.NamedTemporaryFile(delete=False, suffix='.cbl') as f:
            f.write(content)
            tmp = Path(f.name)
        try:
            expected = hashlib.sha256(content).hexdigest()
            actual = compute_sha256(tmp)
            self.assertEqual(actual, expected)
        finally:
            tmp.unlink()

    def test_line_count_accurate(self):
        lines = ['line 1\n', 'line 2\n', 'line 3\n']
        with tempfile.NamedTemporaryFile(
                delete=False, suffix='.cbl', mode='w') as f:
            f.writelines(lines)
            tmp = Path(f.name)
        try:
            self.assertEqual(count_lines(tmp), 3)
        finally:
            tmp.unlink()

    def test_sha256_of_sample_prologue(self):
        # Sanity: checksum should be non-empty hex string
        sha = compute_sha256(SAMPLE_PROLOGUE)
        self.assertEqual(len(sha), 64)
        self.assertTrue(all(c in '0123456789abcdef' for c in sha))


class TestYamlScalar(unittest.TestCase):

    def test_none_becomes_null(self):
        self.assertEqual(_yaml_scalar(None), 'null')

    def test_integer_is_bare(self):
        self.assertEqual(_yaml_scalar(268), '268')

    def test_simple_string_unquoted(self):
        self.assertEqual(_yaml_scalar('cobol'), 'cobol')

    def test_timestamp_gets_quoted(self):
        val = _yaml_scalar('2026-08-11T12:00:00Z')
        # Should be wrapped in quotes because it contains ':'
        self.assertTrue(val.startswith('"') or ':' not in val[1:-1])

    def test_yaml_keyword_gets_quoted(self):
        val = _yaml_scalar('null')
        self.assertTrue(val.startswith('"'))

    def test_roundtrip_via_dump_yaml(self):
        data = {'key': 'value', 'count': 42, 'nothing': None, 'deps': []}
        text = dump_yaml(data)
        self.assertIn('key: value', text)
        self.assertIn('count: 42', text)
        self.assertIn('nothing: null', text)
        self.assertIn('deps: []', text)


if __name__ == '__main__':
    unittest.main(verbosity=2)
