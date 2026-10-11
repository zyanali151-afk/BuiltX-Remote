"""
BuiltX Remote - Python TV Controller
"""
import os
import asyncio
import threading
from androidtvremote2 import AndroidTVRemote
from androidtvremote2.remote import RemoteKeyCode

# Global state
_tv_instance = None
_loop = None
_connected = False

# CHANGE THIS to your TV IP
TV_IP = "192.168.1.2"


def _init_loop():
    global _loop
    if _loop is None:
        _loop = asyncio.new_event_loop()
        asyncio.set_event_loop(_loop)


def _run_async(coro):
    """Run async function in the loop"""
    _init_loop()
    return _loop.run_until_complete(coro)


def connect(certfile, keyfile):
    """Connect to TV"""
    global _tv_instance, _connected
    
    async def _do_connect():
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
        _tv_instance = _run_async(_do_connect())
        _connected = True
        return "Connected"
    except Exception as e:
        return f"Error: {e}"


def send_key(keycode_name):
    """Send a key command"""
    global _tv_instance
    if not _tv_instance:
        return "Not connected"
    keycode = getattr(RemoteKeyCode, keycode_name, None)
    if not keycode:
        return f"Unknown key: {keycode_name}"
    try:
        _tv_instance.send_key_command(keycode)
        return "Sent"
    except Exception as e:
        return f"Error: {e}"


def send_text(text):
    """Send text to TV"""
    global _tv_instance
    if not _tv_instance:
        return "Not connected"
    try:
        _tv_instance.send_text(text)
        return "Sent"
    except Exception as e:
        return f"Error: {e}"


def get_tv_ip():
    """Return TV IP"""
    return TV_IP


def test_hello():
    """Test function"""
    return "BuiltX Python OK"
