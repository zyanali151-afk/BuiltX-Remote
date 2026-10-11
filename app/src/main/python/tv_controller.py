"""
BuiltX Remote - Python TV Controller
Uses androidtvremote2 library
"""
import os
from androidtvremote2 import AndroidTVRemote
from androidtvremote2.remote import RemoteKeyCode
import asyncio
import threading

# Global state
_tv_instance = None
_loop = None


def get_cert_paths(app_files_dir):
    """Return paths to cert files"""
    cert = os.path.join(app_files_dir, "cert.pem")
    key = os.path.join(app_files_dir, "key.pem")
    return cert, key


async def _connect_async(host, certfile, keyfile):
    """Async connect to TV"""
    tv = AndroidTVRemote(
        client_name="BuiltX Remote",
        certfile=certfile,
        keyfile=keyfile,
        host=host,
        enable_ime=True,
    )
    await tv.async_connect()
    return tv


async def _pair_async(host, certfile, keyfile):
    """Async pairing"""
    tv = AndroidTVRemote(
        client_name="BuiltX Remote",
        certfile=certfile,
        keyfile=keyfile,
        host=host,
    )
    await tv.async_start_pairing()
    return tv


def connect(host, certfile, keyfile):
    """Connect to TV (blocking)"""
    global _tv_instance, _loop
    _loop = asyncio.new_event_loop()
    asyncio.set_event_loop(_loop)
    _tv_instance = _loop.run_until_complete(_connect_async(host, certfile, keyfile))
    return True


def send_key(keycode_name):
    """Send a key command"""
    global _tv_instance
    if not _tv_instance:
        return False
    keycode = getattr(RemoteKeyCode, keycode_name, None)
    if not keycode:
        return False
    _tv_instance.send_key_command(keycode)
    return True


def send_text(text):
    """Send text to TV"""
    global _tv_instance
    if not _tv_instance:
        return False
    _tv_instance.send_text(text)
    return True


def disconnect():
    """Disconnect from TV"""
    global _tv_instance
    if _tv_instance:
        _tv_instance.disconnect()
        _tv_instance = None
    return True


def test_hello():
    """Simple test function"""
    return "BuiltX Python is working!"
