import subprocess
import time
import json
import urllib.request
import sys
import os

if sys.stdout.encoding.lower() != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

print("=" * 65)
print("  CYBER SHADOW TWIN - PUBLIC NGROK DEPLOYMENT LAUNCHER")
print("=" * 65)

ngrok_path = r"C:\Users\hp\Desktop\ngrok.exe"

if not os.path.exists(ngrok_path):
    print(f"Error: ngrok.exe not found at {ngrok_path}")
    sys.exit(1)

print("\n[1/2] Starting ngrok tunnel on port 8080...")
proc = subprocess.Popen([ngrok_path, "http", "8080"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)

time.sleep(3)

print("[2/2] Fetching live public HTTPS endpoint from ngrok...")
public_url = None
for i in range(10):
    try:
        req = urllib.request.urlopen("http://127.0.0.1:4040/api/tunnels")
        data = json.loads(req.read().decode())
        tunnels = data.get("tunnels", [])
        for t in tunnels:
            if t.get("proto") == "https":
                public_url = t.get("public_url")
                break
        if not public_url and tunnels:
            public_url = tunnels[0].get("public_url")
        if public_url:
            break
    except Exception:
        time.sleep(1)

if public_url:
    print("\n" + "=" * 65)
    print("  SUCCESS! NGROK PUBLIC TUNNEL IS LIVE!")
    print("=" * 65)
    print(f"  Public Base Endpoint:   {public_url}")
    print(f"  Mobile Honeypot Trap:   {public_url}/api/admin-database-backup")
    print(f"  Mobile Sandbox:         {public_url}/sandbox.html")
    print("=" * 65)
    print("\n INSTRUCTIONS FOR LIVE FACULTY DEMO:")
    print(" 1. Open your SOC Dashboard on your laptop screen.")
    print(" 2. Scan or type the Mobile Honeypot Trap URL on your mobile phone:")
    print(f"    --> {public_url}/api/admin-database-backup")
    print(" 3. Your phone will receive an 'Intrusion Detected (403 Forbidden)' response,")
    print("    and your laptop screen will pop up a red Honeypot Breach alert in real time!")
    print("=" * 65 + "\n")
else:
    print("\nCould not retrieve ngrok tunnel URL automatically.")
    print("You can start ngrok manually by running:")
    print(f'"{ngrok_path}" http 8080')
