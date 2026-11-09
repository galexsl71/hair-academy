import sys, zipfile, zlib, hashlib, struct
base, cat, out = sys.argv[1:4]
OLD, NEW = 'com.alex.hairacademy', 'com.alex.hairlesson2'
assert len(OLD) == len(NEW)

def u16(s): return s.encode('utf-16le')

def patch_manifest(b):
    assert b.count(u16(OLD)) == 1
    b = b.replace(u16(OLD), u16(NEW))
    # заметное отличие названия от оригинала (та же длина)
    b = b.replace(u16('Стрижка'), u16('Стрижки')).replace(u16('Уроки'), u16('Урок2'))
    return b

def patch_arsc(b):
    return b.replace(u16(OLD), u16(NEW))

def patch_dex(b):
    d = bytearray(b)
    o, n = OLD.replace('.', '/').encode(), NEW.replace('.', '/').encode()
    assert d.count(b'L' + o + b'/') == 4
    d = bytearray(bytes(d).replace(b'L' + o + b'/', b'L' + n + b'/'))
    d[12:32] = hashlib.sha1(bytes(d[32:])).digest()
    d[8:12] = struct.pack('<I', zlib.adler32(bytes(d[12:])) & 0xffffffff)
    return bytes(d)

zin = zipfile.ZipFile(base)
with zipfile.ZipFile(out, 'w') as zout:
    for i in zin.infolist():
        if i.filename.startswith('META-INF/'):
            continue
        data = zin.read(i.filename)
        if i.filename == 'assets/catalog.json': data = open(cat, 'rb').read()
        elif i.filename == 'AndroidManifest.xml': data = patch_manifest(data)
        elif i.filename == 'resources.arsc': data = patch_arsc(data)
        elif i.filename == 'classes.dex': data = patch_dex(data)
        zout.writestr(zipfile.ZipInfo(i.filename, (2020, 1, 1, 0, 0, 0)), data, compress_type=i.compress_type)
