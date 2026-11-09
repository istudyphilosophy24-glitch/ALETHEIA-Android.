#!/usr/bin/env python3
"""Download and validate real, openly licensed font families from Google Fonts.
Every font file is packaged in the Android APK; Android needs no Internet.
Print and enforce real installed-font count. Do not synthesize fake font names.
"""
import json, os, pathlib, urllib.request, time, re
ROOT=pathlib.Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/assets'
FONTS=BASE/'fonts'
LICENSES=BASE/'font_licenses'
FONTS.mkdir(parents=True,exist_ok=True); LICENSES.mkdir(parents=True,exist_ok=True)
# Distinct font families selected for prose, classics, accessibility and technical text.
CANDIDATES='gentiumbookplus atkinsonhyperlegible atkinsonhyperlegiblenext literata notoserif notosans librebaskerville librecaslontext lora vollkorn crimsonttext crimsonpro spectral sourceserif4 sourceserifpro sourcesans3 sourcesanspro cardo alegreya alegreyasans'
MORE='ebgaramond cormorantgaramond cormorant scada amiri oldstandardtt oldstandardts montaga marcellus marcellussc tinos arimo cousine cabin cabincondensed karla rubik inter manrope outfit nunito nunitosans mulish publicsans worksans raleway ubuntu oxygen merriweather merriweathersans ptserif ptsans droidsans droidserif bebasneue oswald barlow barlowcondensed barlowsemicondensed hind hindmadurai mukta asul biorhyme biorhymeexpanded josefinsans josefinslab sora dmseriftext dmserifdisplay dmsans dominee rokkitt zillaslab notosansmono ibmplexserif ibmplexsans ibmplexmono inknutantiqua firasans firasanscondensed firamono gentiumplus gentiumbasic robotoserif roboto robotoslab libertinusserif gfsdidot librefranklin librebarcode39extended ubuntucondensed urbanist poppins montserrat manjari plusjakartasans petrona breeserif abeezee quicksand signika bitter bitterpro newsreader varta tenorsans prata arvo chiaradisplay tirobangla texturina chakrapetch glegoo manuale heptaslab'
# Slugs are normalized lowercase; unavailable candidates are skipped and logged.
CANDIDATES=list(dict.fromkeys((CANDIDATES+' '+MORE).split()))
TOKEN=os.environ.get('GITHUB_TOKEN','')
HEAD={'User-Agent':'FOLIO-legacy-eink-font-build/1.0','Accept':'application/vnd.github+json'}
if TOKEN:HEAD['Authorization']='Bearer '+TOKEN

def get(url,headers=HEAD):
    req=urllib.request.Request(url,headers=headers)
    with urllib.request.urlopen(req,timeout=65) as resp:return resp.read()

def listing(slug,group,folder=''):
    url='https://api.github.com/repos/google/fonts/contents/'+group+'/'+slug+('/'+folder if folder else '')
    return json.loads(get(url))

def pick(files):
    fs=[f for f in files if f.get('type')=='file' and f['name'].lower().endswith('.ttf')]
    def priority(x):
        n=x['name'].lower()
        return (0 if n.endswith('-regular.ttf') else 1 if 'regular' in n else 2 if 'variable' not in n and '[' not in n else 3,len(n))
    return sorted(fs,key=priority)[0] if fs else None

results=[]
for slug in CANDIDATES:
    if len(results)>=58:break
    ok=False
    for group in ('ofl','apache','ufl'):
        try:
            items=listing(slug,group)
            if not isinstance(items,list):continue
            license_info=next((x for x in items if x.get('name','').lower() in ('ofl.txt','license.txt','ufl.txt')),None)
            if not license_info:continue
            font=pick(items)
            static=next((x for x in items if x.get('type')=='dir' and x.get('name')=='static'),None)
            if static:
                try:font=pick(listing(slug,group,'static')) or font
                except Exception:pass
            if not font:continue
            raw=get(font['download_url'],headers={'User-Agent':'FOLIO font downloader'})
            if not (2000<len(raw)<5000000 and (raw[:4] in (b'OTTO',b'true',b'typ1',b'\x00\x01\x00\x00'))):
                print('SKIP invalid font',slug,len(raw),flush=True);continue
            license_data=get(license_info['download_url'],headers={'User-Agent':'FOLIO font downloader'})
            if b'icense' not in license_data.lower() and b'copyright' not in license_data.lower():continue
            (FONTS/(slug+'.ttf')).write_bytes(raw)
            (LICENSES/(slug+'_'+license_info['name'])).write_bytes(license_data)
            rawname=font['name'].replace('.ttf','').split('[')[0].replace('-Regular','').replace('_',' ')
            label=re.sub(r'(?<=[a-z])(?=[A-Z])',' ',rawname).strip()
            label=label if len(label)>3 else slug
            # Slug is also preserved to make the original family provenance clear.
            results.append((slug,label,group+'/'+slug))
            print('FONT',len(results),slug,len(raw),flush=True)
            ok=True;break
        except Exception as e:
            if group=='ufl':print('UNAVAILABLE',slug,str(e)[:110],flush=True)
    if not ok:continue
if len(results)<55:raise SystemExit('Only '+str(len(results))+' distinct font files found; need at least 55')
(BASE/'font_catalog.tsv').write_text(''.join('%s\t%s\t%s\n'%r for r in results),encoding='utf-8')
(BASE/'font_provenance.txt').write_text('Bundled original font files from github.com/google/fonts, with their per-family license texts under font_licenses/.\nEach font is used unchanged.\n'+''.join('%s %s\n'%(a,c) for a,b,c in results),encoding='utf-8')
print('VERIFIED_BUNDLED_DISTINCT_FONTS',len(results),flush=True)
