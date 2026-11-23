from pathlib import Path
import json, re
root=Path("RVTEC_Android")
files=[
 root/"app/build.gradle",
 root/"app/src/main/java/com/rvtec/manutencao/MainActivity.java",
 root/"app/src/main/assets/index.html",
]
for path in files:
 print("\\n=== "+str(path)+" ===")
 try: lines=path.read_text(encoding="utf-8").splitlines()
 except Exception as e: print("READ ERROR",e); continue
 for i,line in enumerate(lines,1):
  if re.search(r"firebase-bom|firebase-appcheck|firebase-ai|firebase-vertex|playintegrity|debugprovider|FirebaseApp|FirebaseAppCheck|AppCheck|PlayIntegrity|onCreate|initializeApp|GeminiBridge|GenerativeModel|generateContent|Gemini|appCheck|APP_CHECK|applicationId|namespace|versionCode|versionName",line,re.I):
   print(f"{i}: {line[:500]}")
cfg=root/"app/google-services.json"
print("\\n=== google-services.json ===")
try:
 data=json.loads(cfg.read_text(encoding="utf-8"))
 print("project_id =",data.get("project_info",{}).get("project_id"))
 for c in data.get("client",[]):
  print("package =",c.get("client_info",{}).get("android_client_info",{}).get("package_name"))
  print("oauth_clients =",len(c.get("oauth_client",[])))
except Exception as e: print("ERROR",repr(e))
