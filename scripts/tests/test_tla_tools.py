import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]


class TlaBootstrapTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / 'scripts').mkdir()
        (self.root / 'tla').mkdir()
        shutil.copy2(ROOT / 'scripts/tla.sh', self.root / 'scripts/tla.sh')
        shutil.copy2(ROOT / 'tla/toolchain.env', self.root / 'tla/toolchain.env')
        self.pins = dict(
            line.split('=', 1)
            for line in (self.root / 'tla/toolchain.env').read_text().splitlines()
            if line and not line.startswith('#')
        )
        self.cache = self.root / 'cache'
        self.cache.mkdir()
        self.env = dict(os.environ, TLA_TOOLS_DIR=str(self.cache))

    def run_tool(self, *args):
        return subprocess.run(['bash', 'scripts/tla.sh', *args], cwd=self.root,
                              env=self.env, text=True, capture_output=True)

    def test_unknown_command_fails(self):
        result = self.run_tool('unknown')
        self.assertEqual(result.returncode, 2)
        self.assertIn('usage:', result.stderr)

    def test_missing_setup_is_not_reported_as_pass(self):
        result = self.run_tool('check')
        self.assertEqual(result.returncode, 3)
        self.assertIn('UNAVAILABLE', result.stderr)
        self.assertIn('setup', result.stderr)
        self.assertNotIn('PASS', result.stdout)

    def test_corrupt_helper_archive_fails_before_execution(self):
        revision = self.pins['FORMAL_VERIFY_REV']
        (self.cache / f'formal-verify-{revision}.tar.gz').write_bytes(b'corrupt archive')
        result = self.run_tool('check')
        self.assertEqual(result.returncode, 3)
        self.assertIn('SHA256 mismatch', result.stderr)
        self.assertFalse(list(self.cache.glob('run.*')))
        self.assertNotIn('PASS', result.stdout)

    def test_corrupt_tlc_jar_fails_even_with_inherited_jar_override(self):
        pins = self.root / 'tla/toolchain.env'
        fake_archive = b'checksum validation must precede extraction'
        pins.write_text(pins.read_text().replace(
            self.pins['FORMAL_VERIFY_SHA256'],
            hashlib.sha256(fake_archive).hexdigest(),
        ))
        revision = self.pins['FORMAL_VERIFY_REV']
        (self.cache / f'formal-verify-{revision}.tar.gz').write_bytes(fake_archive)
        (self.cache / 'tlc').mkdir()
        jar_name = f"tla2tools-v{self.pins['TLC_VERSION']}.jar"
        (self.cache / 'tlc' / jar_name).write_bytes(b'corrupt jar')
        self.env['TLC_JAR'] = '/some/unchecked.jar'
        result = self.run_tool('check')
        self.assertEqual(result.returncode, 3)
        self.assertIn('SHA256 mismatch', result.stderr)
        self.assertIn(jar_name, result.stderr)
        self.assertFalse(list(self.cache.glob('run.*')))
        self.assertNotIn('PASS', result.stdout)


if __name__ == '__main__':
    unittest.main()
