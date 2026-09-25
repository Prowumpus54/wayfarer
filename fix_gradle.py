from pathlib import Path
p=Path(r'C:\Wayfarer\app\build.gradle')
s=p.read_text(encoding='utf-8')
block="def localProps = new Properties()\ndef localPropsFile = rootProject.file('local.properties')\nif (localPropsFile.exists()) localPropsFile.withInputStream { localProps.load(it) }\n\n"
s=s.replace(block,'',1)
needle="plugins {\n    id 'com.android.application'\n    id 'org.jetbrains.kotlin.android'\n    id 'org.jetbrains.kotlin.plugin.compose'\n    id 'com.google.gms.google-services'\n}\n\n"
s=s.replace(needle,needle+block,1)
p.write_text(s,encoding='utf-8')
print('fixed')
