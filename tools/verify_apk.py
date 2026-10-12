import hashlib, struct, sys, zipfile, zlib
import xml.etree.ElementTree as ET
from pathlib import Path

apk, manifest = map(Path, sys.argv[1:])
root = ET.parse(manifest).getroot()
a = '{http://schemas.android.com/apk/res/android}'
assert root.get('package') == 'com.erp.carmusic.diag'
assert root.get(a + 'versionCode') == '3'
assert root.get(a + 'versionName') == '0.2.1-media-diagnostic'
assert root.find('uses-sdk').get(a + 'minSdkVersion') == '28'
assert root.find('uses-sdk').get(a + 'targetSdkVersion') == '28'
app = root.find('application')
launcher = [activity for activity in app.findall('activity') if any(
    c.get(a + 'name') == 'android.intent.category.LAUNCHER'
    for c in activity.findall('intent-filter/category'))]
assert len(launcher) == 1 and launcher[0].get(a + 'name').endswith('.MediaDiagnosticActivity')
player = [s for s in app.findall('service') if s.get(a + 'name').endswith('.PlaybackService')][0]
assert player.get(a + 'enabled') == 'false'
assert player.get(a + 'exported') == 'false'
provider = app.find('provider')
assert provider.get(a + 'authorities') == 'com.erp.carmusic.diag.files'
assert provider.get(a + 'exported') == 'false'
assert not root.findall("uses-permission[@" + a + "name='android.permission.RECORD_AUDIO']")
with zipfile.ZipFile(apk) as z:
    assert z.testzip() is None
    dexes = [n for n in z.namelist() if n.endswith('.dex')]
    assert dexes
    for name in dexes:
        data = z.read(name)
        assert data.startswith(b'dex\n')
        assert data[12:32] == hashlib.sha1(data[32:]).digest()
        assert struct.unpack_from('<I', data, 8)[0] == zlib.adler32(data[12:]) & 0xffffffff
print('PASS: package, version, API28, diagnostic launcher, disabled player, provider, ZIP/DEX')
print('SHA-256:', hashlib.sha256(apk.read_bytes()).hexdigest())
