import sys, zipfile
base, cat, out = sys.argv[1:4]
zin = zipfile.ZipFile(base)
with zipfile.ZipFile(out, 'w') as zout:
    for i in zin.infolist():
        if i.filename.startswith('META-INF/'):
            continue
        data = zin.read(i.filename)
        if i.filename == 'assets/catalog.json':
            data = open(cat, 'rb').read()
        zout.writestr(zipfile.ZipInfo(i.filename, (2020, 1, 1, 0, 0, 0)), data,
                      compress_type=i.compress_type)
