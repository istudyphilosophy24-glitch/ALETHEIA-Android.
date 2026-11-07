#!/usr/bin/env python3
"""Builds a real offline SQLite dictionary using SQLite-3.7-era SQL only."""
import argparse
import csv
import gzip
import json
import sqlite3
from pathlib import Path
import unicodedata
import urllib.request

ROOT=Path(__file__).resolve().parents[1]
HINTS=('philosoph','ontolog','phenomenolog','metaphysic','logic','dialectic',
       'hermeneutic','epistemolog','existential','ethic','aesthetic','mathematics',
       'psychoanal','theolog','deconstruct','semiotic','linguistic','rhetoric')
COLUMNS='word,lookup,lang,pos,definition,tradition,thinkers,related,source,priority'
INSERT='INSERT INTO entries ('+COLUMNS+') VALUES(?,?,?,?,?,?,?,?,?,?)'

def norm(s):
    return ''.join(c for c in unicodedata.normalize('NFKD',s.lower())
                   if unicodedata.category(c) not in ('Mn','Mc','Me')).strip()

def stage_db(path):
    path.parent.mkdir(parents=True,exist_ok=True)
    if path.exists(): path.unlink()
    db=sqlite3.connect(str(path))
    db.execute('PRAGMA page_size=4096')
    db.execute('PRAGMA journal_mode=DELETE')
    db.execute('PRAGMA synchronous=NORMAL')
    db.executescript("""
    CREATE TABLE entries (
      id INTEGER PRIMARY KEY,
      word TEXT NOT NULL, lookup TEXT NOT NULL, lang TEXT NOT NULL,
      pos TEXT NOT NULL, definition TEXT NOT NULL,
      tradition TEXT NOT NULL DEFAULT '', thinkers TEXT NOT NULL DEFAULT '',
      related TEXT NOT NULL DEFAULT '', source TEXT NOT NULL DEFAULT '',
      priority INTEGER NOT NULL DEFAULT 0
    );
    CREATE TABLE metadata (key TEXT PRIMARY KEY,value TEXT NOT NULL);
    """)
    return db

def seeded(db):
    n=0
    with (ROOT/'glossary.tsv').open(encoding='utf-8',newline='') as f:
        for row in csv.DictReader(f,delimiter='\t'):
            word=row['term'].strip()
            definition=row['definition'].strip()
            if not word or not definition: continue
            db.execute(INSERT,(word,norm(word),row['language'],row.get('part_of_speech','term'),definition,
                               row['tradition'],row.get('thinkers',''),row.get('related',''),
                               'PHILOSOPHIA editorial seed',100))
            n+=1
    db.commit()
    return n

def source_rows(url):
    if url.startswith(('https://','http://')):
        req=urllib.request.Request(url,headers={'User-Agent':'PHILOSOPHIA-kitkat-lexicon/1.1'})
        f=urllib.request.urlopen(req,timeout=120)
    else: f=open(url,'rb')
    with f:
        if url.split('?')[0].endswith('.gz'):
            stream=gzip.GzipFile(fileobj=f)
        else: stream=f
        with stream if stream is not f else _no_close(stream) as rows:
            for line in rows:
                if line.strip(): yield line

class _no_close:
    def __init__(self,file): self.file=file
    def __enter__(self): return self.file
    def __exit__(self,*_): pass

def extract(obj):
    raw=obj.get('senses')
    if not isinstance(raw,list): raw=[obj]
    for s in raw:
        if not isinstance(s,dict):continue
        gl=s.get('glosses') or s.get('raw_glosses') or s.get('gloss') or s.get('definition') or []
        if isinstance(gl,str): gl=[gl]
        if not isinstance(gl,list):continue
        gloss=next((g.strip() for g in reversed(gl) if isinstance(g,str) and g.strip()),'')
        if not gloss:continue
        tags=s.get('tags') or []
        qual='; '.join(str(t) for t in tags[:3]) if isinstance(tags,list) else ''
        if qual:gloss='('+qual+') '+gloss
        yield gloss[:1700],s

def import_data(db,url):
    batch=[]
    count=0
    input_records=0
    for line in source_rows(url):
        input_records+=1
        try: obj=json.loads(line)
        except (ValueError,UnicodeError):continue
        if not isinstance(obj,dict):continue
        word=obj.get('word')
        if not isinstance(word,str) or not word.strip() or len(word)>160:continue
        lang=str(obj.get('lang') or obj.get('language') or 'Unknown')[:55]
        pos=str(obj.get('pos') or 'unspecified')[:35]
        primary=(word+' '+pos).lower()
        prior=25 if any(x in primary for x in HINTS) else 0
        for definition,s in extract(obj):
            topics=s.get('topics') or s.get('categories') or []
            terms=' '.join(str(x) for x in topics[:10]).lower() if isinstance(topics,list) else ''
            weight=max(prior,35 if any(x in terms for x in HINTS) else 0)
            batch.append((word,norm(word),lang,pos,definition,'','','',
                          'English Wiktionary / Wiktextract / Kaikki.org',weight))
            count+=1
        if len(batch)>=4000:
            db.executemany(INSERT,batch); db.commit();batch.clear()
        if input_records%100000==0:
            print(f'{input_records:,} source records => {count:,} definitions',flush=True)
    if batch:db.executemany(INSERT,batch);db.commit()
    print(f'Imported {count:,} definitions from {input_records:,} records',flush=True)
    return count

def finalize(db,input_name,curated):
    print('Building searchable index and real counts...',flush=True)
    db.execute('CREATE INDEX idx_entries_lookup ON entries(lookup,priority DESC,word)')
    db.execute('CREATE INDEX idx_entries_lang_lookup ON entries(lang,lookup)')
    db.commit()
    senses=db.execute('SELECT COUNT(*) FROM entries').fetchone()[0]
    heads=db.execute('SELECT COUNT(*) FROM (SELECT 1 FROM entries GROUP BY lang,lookup)').fetchone()[0]
    meta={
        'schema_version':'1','sense_count':str(senses),'headword_count':str(heads),
        'curated_count':str(curated),'source':'Wiktionary / Wiktextract / Kaikki.org' if input_name else 'PHILOSOPHIA editorial seed',
        'license':'CC BY-SA 4.0 and GFDL for Wiktionary-derived data; see DATA_LICENSE.md',
        'format':'SQLite 3.7+; B-tree, UTF-8, 4096-byte pages'
    }
    db.executemany('INSERT INTO metadata(key,value) VALUES(?,?)',list(meta.items()))
    db.execute('PRAGMA user_version=1')
    db.commit()
    return senses,heads

def main():
    p=argparse.ArgumentParser()
    p.add_argument('--demo',action='store_true')
    p.add_argument('--input',action='append',default=[])
    p.add_argument('--output',required=True)
    p.add_argument('--min-senses',type=int,default=0)
    a=p.parse_args()
    if not a.demo and not a.input:p.error('Supply --demo or --input')
    dest=Path(a.output).resolve()
    db=stage_db(dest)
    try:
        curated=seeded(db)
        for item in a.input: import_data(db,item)
        count,unique=finalize(db,a.input,curated)
        result=db.execute('PRAGMA integrity_check').fetchone()[0]
        if result!='ok':raise RuntimeError('SQLite integrity check failed: '+result)
    finally: db.close()
    print('PACK',dest,'DEFINITIONS',count,'DISTINCT HEADWORDS',unique,
          'BYTES',dest.stat().st_size,flush=True)
    if count<a.min_senses:
        dest.unlink(missing_ok=True)
        raise SystemExit(f'Expected {a.min_senses:,} definitions, got {count:,}')

if __name__=='__main__':main()
