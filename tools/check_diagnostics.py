"""Source safety gate. It complements javac/Lint/tests; never replaces them."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
android = '{http://schemas.android.com/apk/res/android}'
sources = root / 'app/src/main/java/com/erp/carmusic'
service = (sources / 'MediaDiagnosticService.java').read_text()
for hidden in ['.isActive()', '.getPlayerState()', '.getClientUid()', '.getClientPid()']:
    assert hidden not in service, 'Non-public playback API: ' + hidden
for name in ['MediaDiagnosticActivity.java', 'MediaDiagnosticService.java', 'MediaDiagnosticLog.java', 'MediaAccessListener.java']:
    text = (sources / name).read_text()
    for forbidden in ['AudioRecord', 'setStreamVolume(', 'dispatchMediaButtonEvent(', 'setActive(true)', 'getDeclaredMethod(']:
        assert forbidden not in text, name + ': unexpected active media/capture/reflection path'
manifest = ET.parse(root / 'app/src/debug/AndroidManifest.xml')
player = manifest.find("./application/service[@" + android + "name='.PlaybackService']")
assert player is not None and player.get(android + 'enabled') == 'false'
assert player.get(android + 'exported') == 'false'
paths = ET.parse(root / 'app/src/main/res/xml/diagnostic_file_paths.xml')
assert [p.get('path') for p in paths.getroot()] == ['media-diagnostic/']
print('PASS: public playback API, passive observer, disabled debug player, scoped log sharing')
