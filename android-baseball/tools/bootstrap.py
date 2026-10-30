import concurrent.futures
import pathlib
import urllib.request
import zipfile
import xml.etree.ElementTree as ET

ROOT = pathlib.Path.home() / '.codex/android-deckball-tools'
ROOT.mkdir(parents=True, exist_ok=True)

def fetch(url: str, target: pathlib.Path) -> None:
    if not target.exists():
        urllib.request.urlretrieve(url, target)

def jdk() -> None:
    if (ROOT / 'jdk').exists():
        return
    url = 'https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip'
    archive = ROOT / 'jdk.zip'
    fetch(url, archive)
    with zipfile.ZipFile(archive) as z:
        top = z.namelist()[0].split('/')[0]
        z.extractall(ROOT)
    (ROOT / top).rename(ROOT / 'jdk')
    print('JDK ready', flush=True)

def sdk() -> None:
    xml = urllib.request.urlopen('https://dl.google.com/android/repository/repository2-1.xml').read()
    tree = ET.fromstring(xml)
    for package in tree.findall('remotePackage'):
        key = package.attrib['path']
        if key not in ('platforms;android-35', 'build-tools;35.0.0', 'platform-tools'):
            continue
        target = ROOT / key.replace(';', '/')
        if target.exists():
            continue
        for archive in package.findall('archives/archive'):
            host = archive.findtext('host-os')
            if host not in (None, 'windows'):
                continue
            filename = archive.findtext('complete/url')
            path = ROOT / filename
            fetch('https://dl.google.com/android/repository/' + filename, path)
            extract = ROOT / ('extract-' + key.split(';')[0])
            with zipfile.ZipFile(path) as z:
                z.extractall(extract)
            source = next(extract.iterdir())
            target.parent.mkdir(parents=True, exist_ok=True)
            source.rename(target)
            print(key + ' ready', flush=True)
            break

with concurrent.futures.ThreadPoolExecutor() as pool:
    futures = [pool.submit(jdk), pool.submit(sdk)]
    for future in futures:
        future.result()
