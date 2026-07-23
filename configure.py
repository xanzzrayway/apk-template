import os
import base64
from xml.sax.saxutils import escape

app_name = os.environ.get("APP_NAME", "MyApp")
package_name = os.environ.get("PACKAGE_NAME", "com.abidstudio.myapp")
mode = os.environ.get("MODE", "url")  # "url" atau "html"
content = os.environ.get("CONTENT", "")
icon_b64 = os.environ.get("ICON_B64", "")

# 1. Set applicationId
build_gradle_path = "app/build.gradle"
with open(build_gradle_path, "r", encoding="utf-8") as f:
    data = f.read()
data = data.replace("PLACEHOLDER_PACKAGE", package_name)
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

with open(manifest_path, "w", encoding="utf-8") as f:
    f.write(m)

print(f"Configured: app_name={app_name} package={package_name} mode={mode} custom_icon={bool(icon_b64)}")
