import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

SOURCE = Path(__file__).resolve().parents[2]


class FormatHookTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        shutil.copytree(SOURCE / 'scripts', self.root / 'scripts')
        shutil.copytree(SOURCE / '.githooks', self.root / '.githooks')
        (self.root / 'src/main/java').mkdir(parents=True)
        (self.root / 'src/main/java/Example.java').write_text('class Example {}\n')
        (self.root / 'gradlew').write_text(
            '#!/bin/sh\necho checked >> format-invocations\n'
            'test ! -f fail-format\n'
        )
        self.run_command('git', 'init', '-q')
        self.run_command('git', 'config', 'user.email', 'test@example.invalid')
        self.run_command('git', 'config', 'user.name', 'Test')
        self.run_command('git', 'add', '.')
        self.run_command('git', 'commit', '-qm', 'fixture')

    def run_command(self, *args, input=None):
        return subprocess.run(args, cwd=self.root, input=input, text=True,
                              capture_output=True)

    def stop(self, active=False):
        result = self.run_command(
            'python3', 'scripts/format-stop-hook.py',
            input=json.dumps({'stop_hook_active': active}),
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        return json.loads(result.stdout)

    def change_java(self):
        (self.root / 'src/main/java/Example.java').write_text('class Example { }\n')

    def test_stop_skips_clean_and_document_only_work(self):
        (self.root / 'note.md').write_text('note')
        self.assertEqual(self.stop(), {})
        self.assertFalse((self.root / 'format-invocations').exists())

    def test_stop_checks_changed_java_and_blocks_failure(self):
        self.change_java()
        (self.root / 'fail-format').touch()
        self.assertEqual(self.stop()['decision'], 'block')
        self.assertTrue((self.root / 'format-invocations').exists())

    def test_stop_checks_untracked_java(self):
        (self.root / 'src/main/java/New.java').write_text('class New {}')
        self.assertEqual(self.stop(), {})
        self.assertTrue((self.root / 'format-invocations').exists())

    def test_stop_does_not_loop_after_repair_turn(self):
        self.change_java()
        (self.root / 'fail-format').touch()
        self.assertEqual(self.stop(active=True), {})
        self.assertFalse((self.root / 'format-invocations').exists())

    def test_precommit_rejects_unstaged_fix_over_staged_java(self):
        self.change_java()
        self.run_command('git', 'add', 'src')
        (self.root / 'src/main/java/Example.java').write_text('class Example {}\n')
        result = self.run_command('bash', '.githooks/pre-commit')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.root / 'format-invocations').exists())

    def test_precommit_runs_check_without_modifying_index(self):
        self.change_java()
        self.run_command('git', 'add', 'src')
        before = self.run_command('git', 'write-tree').stdout
        (self.root / 'fail-format').touch()
        result = self.run_command('bash', '.githooks/pre-commit')
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.run_command('git', 'write-tree').stdout, before)
        self.assertTrue((self.root / 'format-invocations').exists())

    def test_prepush_rejects_uncommitted_java_fix(self):
        self.change_java()
        self.run_command('git', 'add', 'src')
        result = self.run_command('bash', '.githooks/pre-push')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.root / 'format-invocations').exists())

    def test_prepush_propagates_formatter_failure(self):
        (self.root / 'fail-format').touch()
        self.assertNotEqual(self.run_command('bash', '.githooks/pre-push').returncode, 0)

    def test_installer_enables_hooks(self):
        self.assertEqual(self.run_command('bash', 'scripts/install-hooks.sh').returncode, 0)
        self.assertEqual(self.run_command('git', 'config', 'core.hooksPath').stdout.strip(), '.githooks')
        self.assertEqual(self.run_command('bash', 'scripts/install-hooks.sh').returncode, 0)

    def test_installer_preserves_existing_hook_path(self):
        self.run_command('git', 'config', 'core.hooksPath', 'custom-hooks')
        self.assertNotEqual(self.run_command('bash', 'scripts/install-hooks.sh').returncode, 0)
        self.assertEqual(self.run_command('git', 'config', 'core.hooksPath').stdout.strip(), 'custom-hooks')

    def test_installer_preserves_existing_executable_hook(self):
        hook = self.root / '.git/hooks/pre-commit'
        hook.write_text('#!/bin/sh\nexit 0\n')
        hook.chmod(0o755)
        self.assertNotEqual(self.run_command('bash', 'scripts/install-hooks.sh').returncode, 0)
        self.assertEqual(self.run_command('git', 'config', 'core.hooksPath').stdout, '')


if __name__ == '__main__':
    unittest.main()
