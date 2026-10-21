#!/usr/bin/env python3
"""Check a public source development APK, not a credential-provisioned private build."""
from pathlib import Path
import sys
import zipfile

if len(sys.argv) != 2:
    raise SystemExit('Usage: check_roadlink_apk.py path/to/mobile-debug.apk')
with zipfile.ZipFile(sys.argv[1]) as archive:
    names = archive.namelist()
    forbidden = [name for name in names if name.startswith('assets/') and (
        '/offline-mfi/' in name or Path(name).suffix.lower() in {'.pk8','.p7b','.p12','.pfx','.jks','.keystore','.pem','.key'}
    )]
    if forbidden: raise SystemExit('Credential containers in public APK: '+', '.join(forbidden))
    if not any(name.endswith('libxcertplay_i2c.so') for name in names):
        raise SystemExit('Receiver native library is absent')
    if 'classes.dex' not in names: raise SystemExit('Android code is absent')
print('Public development APK: receiver code/native library present; no authentication credential assets.')
