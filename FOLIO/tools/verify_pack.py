#!/usr/bin/env python3
"""Validate the already built PHILOSOPHIA GRAND lexicon and embed it unchanged."""
import gzip, os, sqlite3, sys
from pathlib import Path
source=Path(sys.argv[1]); target=Path(sys.argv[2]); target.parent.mkdir(parents=True,exist_ok=True)
assert source.exists(), 'Real full dictionary artifact is missing'
assert source.stat().st_size==393191424, ('Unexpected database byte count',source.stat().st_size)
db=sqlite3.connect('file:'+str(source.resolve())+'?mode=ro',uri=True)
try:
    assert db.execute('PRAGMA quick_check(1)').fetchone()[0]=='ok'
    meta=dict(db.execute('SELECT key,value FROM metadata').fetchall())
    assert meta['schema_version']=='1', meta
    assert int(meta['sense_count'])==1793921, meta
    assert int(meta['headword_count'])==1356788, meta
    assert int(meta['curated_count'])>=69, meta
finally:db.close()
print('VERIFIED_DICT_SENSES=1793921  LANGUAGE_HEADWORD_PAIRS=1356788')
with source.open('rb') as f, gzip.open(target,'wb',compresslevel=6) as g:
    while True:
        b=f.read(1<<20)
        if not b:break
        g.write(b)
assert target.stat().st_size>50_000_000, 'Unexpectedly tiny lexicon'
print('BUNDLED_LEXPACK_BYTES',target.stat().st_size,flush=True)
