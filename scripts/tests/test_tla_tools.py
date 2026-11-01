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
        revision = 'dec7ff413fc2b88d2fa8cf6af1cd03bcae6fe92d'
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
            '395e67b9caf17762d13a5dcaafcf3b1c4a2d4a041af393be75a58d4605fa8448',
            hashlib.sha256(fake_archive).hexdigest(),
        ))
        revision = 'dec7ff413fc2b88d2fa8cf6af1cd03bcae6fe92d'
        (self.cache / f'formal-verify-{revision}.tar.gz').write_bytes(fake_archive)
        (self.cache / 'tlc').mkdir()
        (self.cache / 'tlc/tla2tools-v1.7.4.jar').write_bytes(b'corrupt jar')
        self.env['TLC_JAR'] = '/some/unchecked.jar'
        result = self.run_tool('check')
        self.assertEqual(result.returncode, 3)
        self.assertIn('SHA256 mismatch', result.stderr)
        self.assertIn('tla2tools-v1.7.4.jar', result.stderr)
        self.assertFalse(list(self.cache.glob('run.*')))
        self.assertNotIn('PASS', result.stdout)


if __name__ == '__main__':
    unittest.main()
