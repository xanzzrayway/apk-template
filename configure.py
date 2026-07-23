import os
import json
import base64
from xml.sax.saxutils import escape

app_name = os.environ.get("APP_NAME", "MyApp")
package_name = os.environ.get("PACKAGE_NAME", "com.abidstudio.myapp")
mode = os.environ.get("MODE", "url")  # "url" atau "html"
content = os.environ.get("CONTENT", "")
icon_b64 = os.environ.get("ICON_B64", "")
version_name = os.environ.get("VERSION", "1.0").strip() or "1.0"
version_code = os.environ.get("VERSION_CODE", "1").strip() or "1"

try:
    permissions = json.loads(os.environ.get("PERMISSIONS", "[]") or "[]")
    if not isinstance(permissions, list):
        permissions = []
except Exception:
    permissions = []

# 1. Set applicationId + versi
build_gradle_path = "app/build.gradle"
with open(build_gradle_path, "r", encoding="utf-8") as f:
    data = f.read()
data = data.replace("PLACEHOLDER_PACKAGE", package_name)
data = data.replace("PLACEHOLDER_VERSION_NAME", version_name)
data = data.replace("PLACEHOLDER_VERSION_CODE", str(int(version_code)) if version_code.isdigit() else "1")
with open(build_gradle_path, "w", encoding="utf-8") as f:
    f.write(data)

# 2. Set app_name + target_url
strings_path = "app/src/main/res/values/strings.xml"
with open(strings_path, "r", encoding="utf-8") as f:
    s = f.read()
s = s.replace("PLACEHOLDER_APP_NAME", escape(app_name))

if mode == "html":
    html_content = base64.b64decode(content).decode("utf-8")
    os.makedirs("app/src/main/assets/www", exist_ok=True)
    with open("app/src/main/assets/www/index.html", "w", encoding="utf-8") as f:
        f.write(html_content)
    s = s.replace("PLACEHOLDER_URL", "")
else:
    s = s.replace("PLACEHOLDER_URL", escape(content))

with open(strings_path, "w", encoding="utf-8") as f:
    f.write(s)

# 3. Icon - custom kalau ada, default drawable kalau kosong
manifest_path = "app/src/main/AndroidManifest.xml"
with open(manifest_path, "r", encoding="utf-8") as f:
    m = f.read()

if icon_b64:
    icon_bytes = base64.b64decode(icon_b64)
    mipmap_dir = "app/src/main/res/mipmap-xxxhdpi"
    os.makedirs(mipmap_dir, exist_ok=True)
    with open(os.path.join(mipmap_dir, "ic_launcher.png"), "wb") as f:
        f.write(icon_bytes)
    m = m.replace("PLACEHOLDER_ICON", "@mipmap/ic_launcher")
else:
    m = m.replace("PLACEHOLDER_ICON", "@drawable/ic_launcher")

# 4. Izin tambahan (skip INTERNET karena udah ada default)
extra_perms = [p for p in permissions if isinstance(p, str) and p.strip() and p.strip() != "android.permission.INTERNET"]
extra_perms = sorted(set(extra_perms))
perm_lines = "\n".join(f'    <uses-permission android:name="{escape(p)}" />' for p in extra_perms)
m = m.replace("<!--PLACEHOLDER_PERMISSIONS-->", perm_lines)

with open(manifest_path, "w", encoding="utf-8") as f:
    f.write(m)

print(f"Configured: app_name={app_name} package={package_name} mode={mode} version={version_name}({version_code}) custom_icon={bool(icon_b64)} permissions={extra_perms}")
