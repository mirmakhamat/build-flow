#!/usr/bin/env python3
import sys
import os
import hmac
import hashlib

def get_master_salt():
    # 1. Environment variable
    if os.environ.get("BUILDFLOW_MASTER_SALT"):
        return os.environ.get("BUILDFLOW_MASTER_SALT")

    # 2. local.properties fayli
    local_props_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "local.properties")
    if os.path.exists(local_props_path):
        with open(local_props_path, "r") as f:
            for line in f:
                if line.strip().startswith("BUILDFLOW_MASTER_SALT="):
                    return line.strip().split("=", 1)[1].strip().strip('"').strip("'")

    # 3. Fallback default
    return "BuildFlow_Default_OpenSource_Salt_Key"

def calculate_activation_key(device_id: str, salt: str = None) -> str:
    clean_id = device_id.strip().upper()
    active_salt = salt or get_master_salt()
    key_bytes = active_salt.encode('utf-8')
    data_bytes = clean_id.encode('utf-8')

    signature = hmac.new(key_bytes, data_bytes, hashlib.sha256).hexdigest().upper()
    return f"{signature[0:4]}-{signature[4:8]}"

def main():
    if len(sys.argv) < 2:
        print("==================================================")
        print("🔐 BuildFlow Device Activation Key Generator")
        print("==================================================")
        print("Foydalanish: python3 generate_key.py <DEVICE_ID> [OPTIONAL_SALT]")
        print("Misol:       python3 generate_key.py BF-8A42-99F1")
        print("==================================================")
        sys.exit(1)

    device_id = sys.argv[1]
    custom_salt = sys.argv[2] if len(sys.argv) > 2 else None
    activation_key = calculate_activation_key(device_id, custom_salt)

    print("\n--------------------------------------------------")
    print(f"📱 Qurilma ID (Device ID):    {device_id.upper()}")
    print(f"🔑 Aktivatsiya Kodi (Key):   {activation_key}")
    print("--------------------------------------------------")
    print("Ushbu kalitni xodimga yoki foydalanuvchiga bering.\n")

if __name__ == "__main__":
    main()
