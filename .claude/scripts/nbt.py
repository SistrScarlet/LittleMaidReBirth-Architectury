#!/usr/bin/env python3
"""NBT file reader/writer for Minecraft structure files.

Usage:
    python3 .claude/scripts/nbt.py read <file.nbt>
    python3 .claude/scripts/nbt.py write <file.nbt> <file.json>
    python3 .claude/scripts/nbt.py write <file.nbt> -  # read JSON from stdin
"""

import gzip
import json
import struct
import sys
from io import BytesIO

# NBT tag type constants
TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12


# ---- Reader ----

def read_nbt_file(path):
    with gzip.open(path, "rb") as f:
        tag_type = _read_byte(f)
        if tag_type != TAG_COMPOUND:
            raise ValueError(f"Expected root TAG_Compound, got {tag_type}")
        name = _read_string(f)
        value = _read_compound(f)
        return {"_name": name, **value}


def _read_byte(f):
    return struct.unpack("b", f.read(1))[0]


def _read_ubyte(f):
    return struct.unpack("B", f.read(1))[0]


def _read_short(f):
    return struct.unpack(">h", f.read(2))[0]


def _read_ushort(f):
    return struct.unpack(">H", f.read(2))[0]


def _read_int(f):
    return struct.unpack(">i", f.read(4))[0]


def _read_long(f):
    return struct.unpack(">q", f.read(8))[0]


def _read_float(f):
    return struct.unpack(">f", f.read(4))[0]


def _read_double(f):
    return struct.unpack(">d", f.read(8))[0]


def _read_string(f):
    length = _read_ushort(f)
    return f.read(length).decode("utf-8")


def _read_tag(f, tag_type):
    if tag_type == TAG_BYTE:
        return _read_byte(f)
    elif tag_type == TAG_SHORT:
        return _read_short(f)
    elif tag_type == TAG_INT:
        return _read_int(f)
    elif tag_type == TAG_LONG:
        return _read_long(f)
    elif tag_type == TAG_FLOAT:
        return round(_read_float(f), 7)
    elif tag_type == TAG_DOUBLE:
        return _read_double(f)
    elif tag_type == TAG_BYTE_ARRAY:
        length = _read_int(f)
        return {"_type": "byte_array", "value": list(struct.unpack(f"{length}b", f.read(length)))}
    elif tag_type == TAG_STRING:
        return _read_string(f)
    elif tag_type == TAG_LIST:
        return _read_list(f)
    elif tag_type == TAG_COMPOUND:
        return _read_compound(f)
    elif tag_type == TAG_INT_ARRAY:
        length = _read_int(f)
        return {"_type": "int_array", "value": list(struct.unpack(f">{length}i", f.read(length * 4)))}
    elif tag_type == TAG_LONG_ARRAY:
        length = _read_int(f)
        return {"_type": "long_array", "value": list(struct.unpack(f">{length}q", f.read(length * 8)))}
    else:
        raise ValueError(f"Unknown tag type: {tag_type}")


def _read_list(f):
    list_type = _read_byte(f)
    length = _read_int(f)
    return [_read_tag(f, list_type) for _ in range(length)]


def _read_compound(f):
    result = {}
    while True:
        child_type = _read_ubyte(f)
        if child_type == TAG_END:
            break
        child_name = _read_string(f)
        result[child_name] = _read_tag(f, child_type)
    return result


# ---- Writer ----

def write_nbt_file(path, data):
    buf = BytesIO()
    name = data.pop("_name", "")
    _write_byte(buf, TAG_COMPOUND)
    _write_string(buf, name)
    _write_compound(buf, data)
    with gzip.open(path, "wb") as f:
        f.write(buf.getvalue())


def _write_byte(f, value):
    f.write(struct.pack("b", value))


def _write_short(f, value):
    f.write(struct.pack(">h", value))


def _write_int(f, value):
    f.write(struct.pack(">i", value))


def _write_long(f, value):
    f.write(struct.pack(">q", value))


def _write_float(f, value):
    f.write(struct.pack(">f", value))


def _write_double(f, value):
    f.write(struct.pack(">d", value))


def _write_string(f, value):
    encoded = value.encode("utf-8")
    f.write(struct.pack(">H", len(encoded)))
    f.write(encoded)


def _infer_tag_type(value):
    if isinstance(value, dict):
        if "_type" in value:
            type_name = value["_type"]
            if type_name == "byte_array":
                return TAG_BYTE_ARRAY
            elif type_name == "int_array":
                return TAG_INT_ARRAY
            elif type_name == "long_array":
                return TAG_LONG_ARRAY
        return TAG_COMPOUND
    elif isinstance(value, list):
        return TAG_LIST
    elif isinstance(value, str):
        return TAG_STRING
    elif isinstance(value, bool):
        return TAG_BYTE
    elif isinstance(value, int):
        if -128 <= value <= 127:
            return TAG_BYTE
        elif -32768 <= value <= 32767:
            return TAG_SHORT
        elif -2147483648 <= value <= 2147483647:
            return TAG_INT
        else:
            return TAG_LONG
    elif isinstance(value, float):
        return TAG_DOUBLE
    else:
        raise ValueError(f"Cannot infer NBT type for: {type(value)} = {value}")


def _write_tag(f, tag_type, value):
    if tag_type == TAG_BYTE:
        _write_byte(f, int(value))
    elif tag_type == TAG_SHORT:
        _write_short(f, int(value))
    elif tag_type == TAG_INT:
        _write_int(f, int(value))
    elif tag_type == TAG_LONG:
        _write_long(f, int(value))
    elif tag_type == TAG_FLOAT:
        _write_float(f, float(value))
    elif tag_type == TAG_DOUBLE:
        _write_double(f, float(value))
    elif tag_type == TAG_BYTE_ARRAY:
        arr = value["value"]
        _write_int(f, len(arr))
        f.write(struct.pack(f"{len(arr)}b", *arr))
    elif tag_type == TAG_STRING:
        _write_string(f, value)
    elif tag_type == TAG_LIST:
        _write_list(f, value)
    elif tag_type == TAG_COMPOUND:
        _write_compound(f, value)
    elif tag_type == TAG_INT_ARRAY:
        arr = value["value"]
        _write_int(f, len(arr))
        f.write(struct.pack(f">{len(arr)}i", *arr))
    elif tag_type == TAG_LONG_ARRAY:
        arr = value["value"]
        _write_int(f, len(arr))
        f.write(struct.pack(f">{len(arr)}q", *arr))


def _write_list(f, values):
    if not values:
        _write_byte(f, TAG_END)
        _write_int(f, 0)
        return
    list_type = _infer_tag_type(values[0])
    _write_byte(f, list_type)
    _write_int(f, len(values))
    for v in values:
        _write_tag(f, list_type, v)


def _write_compound(f, data):
    for key, value in data.items():
        if key.startswith("_"):
            continue
        tag_type = _infer_tag_type(value)
        _write_byte(f, tag_type)
        _write_string(f, key)
        _write_tag(f, tag_type, value)
    _write_byte(f, TAG_END)


# ---- Main ----

def main():
    if len(sys.argv) < 3:
        print(__doc__, file=sys.stderr)
        sys.exit(1)

    command = sys.argv[1]
    nbt_path = sys.argv[2]

    if command == "read":
        data = read_nbt_file(nbt_path)
        print(json.dumps(data, indent=2, ensure_ascii=False))

    elif command == "write":
        if len(sys.argv) < 4:
            print("Usage: nbt.py write <file.nbt> <file.json | ->", file=sys.stderr)
            sys.exit(1)
        json_source = sys.argv[3]
        if json_source == "-":
            data = json.load(sys.stdin)
        else:
            with open(json_source, "r") as f:
                data = json.load(f)
        write_nbt_file(nbt_path, data)
        print(f"Written: {nbt_path}", file=sys.stderr)

    else:
        print(f"Unknown command: {command}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
