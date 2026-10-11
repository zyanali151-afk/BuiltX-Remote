"""BuiltX Remote - Full keyboard via send_key_command"""
import os
import asyncio
from androidtvremote2 import AndroidTVRemote
from androidtvremote2.remote import RemoteKeyCode

_tv_instance = None
_loop = None
_connected = False
TV_IP = "192.168.1.2"

# Character → KEYCODE map (Android HID codes)
CHAR_MAP = {
    'a': 'KEYCODE_A', 'b': 'KEYCODE_B', 'c': 'KEYCODE_C', 'd': 'KEYCODE_D',
    'e': 'KEYCODE_E', 'f': 'KEYCODE_F', 'g': 'KEYCODE_G', 'h': 'KEYCODE_H',
    'i': 'KEYCODE_I', 'j': 'KEYCODE_J', 'k': 'KEYCODE_K', 'l': 'KEYCODE_L',
    'm': 'KEYCODE_M', 'n': 'KEYCODE_N', 'o': 'KEYCODE_O', 'p': 'KEYCODE_P',
    'q': 'KEYCODE_Q', 'r': 'KEYCODE_R', 's': 'KEYCODE_S', 't': 'KEYCODE_T',
    'u': 'KEYCODE_U', 'v': 'KEYCODE_V', 'w': 'KEYCODE_W', 'x': 'KEYCODE_X',
    'y': 'KEYCODE_Y', 'z': 'KEYCODE_Z',
    '0': 'KEYCODE_0', '1': 'KEYCODE_1', '2': 'KEYCODE_2', '3': 'KEYCODE_3',
    '4': 'KEYCODE_4', '5': 'KEYCODE_5', '6': 'KEYCODE_6', '7': 'KEYCODE_7',
    '8': 'KEYCODE_8', '9': 'KEYCODE_9',
    ' ': 'KEYCODE_SPACE',
    '\n': 'KEYCODE_ENTER',
    '.': 'KEYCODE_PERIOD', ',': 'KEYCODE_COMMA', '?': 'KEYCODE_SLASH',
    '!': 'KEYCODE_1', '-': 'KEYCODE_MINUS', '=': 'KEYCODE_EQUALS',
    ';': 'KEYCODE_SEMICOLON', "'": 'KEYCODE_APOSTROPHE',
    '/': 'KEYCODE_SLASH', '\\': 'KEYCODE_BACKSLASH',
    '[': 'KEYCODE_LEFT_BRACKET', ']': 'KEYCODE_RIGHT_BRACKET',
}


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
        return f"Unknown: {keycode_name}"
    try:
        _tv_instance.send_key_command(keycode)
        return "OK"
    except Exception as e:
        return f"Error: {e}"


def send_char(char):
    """Send a single character as a key command"""
    global _tv_instance
    if not _tv_instance:
        return "Not connected"
    
    # Get keycode name
    keycode_name = CHAR_MAP.get(char.lower())
    if not keycode_name:
        return f"No keycode for '{char}'"
    
    keycode = getattr(RemoteKeyCode, keycode_name, None)
    if not keycode:
        return f"Missing keycode: {keycode_name}"
    
    try:
        _tv_instance.send_key_command(keycode)
        return "OK"
    except Exception as e:
        return f"Error: {e}"


def send_text(text):
    """Fallback: send text via IME (only works in text fields)"""
    global _tv_instance
    if not _tv_instance:
        return "Not connected"
    try:
        _tv_instance.send_text(text)
        return "OK"
    except Exception as e:
        return f"Error: {e}"


def send_modifier(name):
    """Send modifier key (SHIFT, CTRL, ALT)"""
    global _tv_instance
    if not _tv_instance:
        return "Not connected"
    keycode = getattr(RemoteKeyCode, name, None)
    if not keycode:
        return f"Unknown modifier: {name}"
    try:
        _tv_instance.send_key_command(keycode)
        return "OK"
    except Exception as e:
        return f"Error: {e}"


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
