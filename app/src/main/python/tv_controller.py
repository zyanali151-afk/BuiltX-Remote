"""BuiltX Remote - Python TV Controller"""
import os
import asyncio
import time
from androidtvremote2 import AndroidTVRemote
from androidtvremote2.remote import RemoteKeyCode

_tv_instance = None
_loop = None
_connected = False
TV_IP = "192.168.1.2"


def _get_loop():
    global _loop
    if _loop is None or _loop.is_closed():
        _loop = asyncio.new_event_loop()
        asyncio.set_event_loop(_loop)
    return _loop


def connect(certfile, keyfile):
    global _tv_instance, _connected
    if _connected and _tv_instance is not None:
        return "Already connected"

    async def _do():
        tv = AndroidTVRemote(
            client_name="BuiltX Remote",
            certfile=certfile,
            keyfile=keyfile,
            host=TV_IP,
            enable_ime=True,
        )
        await tv.async_connect()
        return tv

    try:
        _tv_instance = _get_loop().run_until_complete(_do())
        _connected = True
        return "Connected"
    except Exception as e:
        _connected = False
        return f"Error: {e}"


def send_key(keycode_name):
    global _tv_instance
    if not _tv_instance:
        return "Not connected"
    keycode = getattr(RemoteKeyCode, keycode_name, None)
    if not keycode:
        return f"Unknown key: {keycode_name}"
    try:
        _tv_instance.send_key_command(keycode)
        return "OK"
    except Exception as e:
        return f"Error: {e}"


def send_text(text):
    """Send text - with reconnect and delay"""
    global _tv_instance, _connected
    if not _tv_instance:
        return "Not connected"
    
    try:
        # Reconnect fresh
        try:
            _tv_instance.disconnect()
        except Exception:
            pass
        _tv_instance = None
        _connected = False
        time.sleep(0.2)
        
        # Reconnect
        certfile = os.path.join(os.environ.get("BUILTX_CERT", ""), "cert.pem") if os.environ.get("BUILTX_CERT") else None
        keyfile = os.path.join(os.environ.get("BUILTX_CERT", ""), "key.pem") if os.environ.get("BUILTX_CERT") else None
        
        # Use global cert paths set by Kotlin
        global _CERT_PATH, _KEY_PATH
        if not _CERT_PATH:
            return "No cert path set"
        
        result = connect(_CERT_PATH, _KEY_PATH)
        if "Error" in result:
            return result
        
        # CRITICAL: Wait for IME to establish
        time.sleep(2.5)
        
        # Now send
        _tv_instance.send_text(text)
        return "OK"
    except Exception as e:
        return f"Error: {e}"


_CERT_PATH = ""
_KEY_PATH = ""


def set_cert_paths(certfile, keyfile):
    """Kotlin calls this on startup to register cert paths"""
    global _CERT_PATH, _KEY_PATH
    _CERT_PATH = certfile
    _KEY_PATH = keyfile
    return "Paths set"


def disconnect():
    global _tv_instance, _connected
    try:
        if _tv_instance:
            _tv_instance.disconnect()
    except Exception:
        pass
    _tv_instance = None
    _connected = False
    return "Disconnected"


def test_hello():
    return "BuiltX Python OK"
