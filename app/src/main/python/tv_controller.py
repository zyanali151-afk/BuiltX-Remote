"""BuiltX Remote - Python TV Controller"""
import os
import asyncio
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
    """Send a key command (for D-pad, volume, etc.)"""
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
    """Send text via IME channel (for letters/numbers)"""
    global _tv_instance
    if not _tv_instance:
        return "Not connected"
    try:
        _tv_instance.send_text(text)
        return "OK"
    except Exception as e:
        return f"Error: {e}"


def reconnect():
    """Force reconnect to refresh IME"""
    global _tv_instance, _connected
    try:
        if _tv_instance:
            _tv_instance.disconnect()
    except Exception:
        pass
    _tv_instance = None
    _connected = False
    return "Reconnected"


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
