import sqlite3
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

class PackTest(unittest.TestCase):
    def test_kitkat_compatible_starter(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'starter.sqlite'
            subprocess.check_call(['python3',str(ROOT/'tools/build_pack.py'),
                                   '--demo','--output',str(p)])
            with sqlite3.connect(str(p)) as conn:
                self.assertEqual(conn.execute('PRAGMA integrity_check').fetchone()[0],'ok')
                self.assertEqual(conn.execute("SELECT value FROM metadata WHERE key='schema_version'").fetchone()[0],'1')
                self.assertGreater(conn.execute('SELECT COUNT(*) FROM entries').fetchone()[0],55)
                self.assertEqual(conn.execute("SELECT word FROM entries WHERE lookup='dasein'").fetchone()[0],'Dasein')
            header=p.read_bytes()[:100]
            self.assertEqual(header[:16],b'SQLite format 3'+bytes([0]))
            self.assertEqual(header[18:20],bytes([1,1]))

if __name__=='__main__':unittest.main()
