#!/usr/bin/env python3
"""NBT file reader/writer and structure builder for Minecraft.

Low-level:
    nbt.py read <file.nbt>                              — JSON で出力
    nbt.py write <file.nbt> <file.json | ->              — JSON から NBT 生成

Structure builder:
    nbt.py create <file.nbt> <sizeX> <sizeY> <sizeZ>    — 空の構造体を作成
    nbt.py fill <file.nbt> <x1> <y1> <z1> <x2> <y2> <z2> <block> [properties...]
                                                         — 範囲をブロックで埋める
    nbt.py set <file.nbt> <x> <y> <z> <block> [properties...]
                                                         — 1ブロック配置
    nbt.py info <file.nbt>                               — サイズとパレット一覧

Block properties example:
    nbt.py set test.nbt 3 1 3 minecraft:furnace facing=north lit=true
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


def _infer_tag_type(value, hint=None):
    if hint is not None:
        return hint
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
        return TAG_INT
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


# ---- Structure Builder ----

# DataVersion for structure files.
# Update this to match your Minecraft version.
# Examples: 1.20.1 = 3465, 1.20.4 = 3700, 1.21 = 3953
DATA_VERSION = 3465


def _parse_block_arg(args):
    """Parse block name and optional properties from args.

    Returns {"Name": "minecraft:stone", "Properties": {"facing": "north"}} or just {"Name": ...}.
    """
    block_name = args[0]
    if ":" not in block_name:
        block_name = f"minecraft:{block_name}"
    entry = {"Name": block_name}
    if len(args) > 1:
        props = {}
        for prop in args[1:]:
            key, value = prop.split("=", 1)
            props[key] = value
        entry["Properties"] = props
    return entry


def _get_or_add_palette_index(data, block_entry):
    """Find or add a block entry in the palette, return its index."""
    for i, entry in enumerate(data["palette"]):
        if entry == block_entry:
            return i
    data["palette"].append(block_entry)
    return len(data["palette"]) - 1


def _set_block(data, x, y, z, state_index):
    """Set a block at position, replacing existing if present."""
    size = data["size"]
    if not (0 <= x < size[0] and 0 <= y < size[1] and 0 <= z < size[2]):
        raise ValueError(f"Position ({x}, {y}, {z}) out of bounds for size {size}")
    for block in data["blocks"]:
        if block["pos"] == [x, y, z]:
            block["state"] = state_index
            return
    data["blocks"].append({"pos": [x, y, z], "state": state_index})


def cmd_create(nbt_path, args):
    if len(args) < 3:
        print("Usage: nbt.py create <file.nbt> <sizeX> <sizeY> <sizeZ>", file=sys.stderr)
        sys.exit(1)
    sx, sy, sz = int(args[0]), int(args[1]), int(args[2])
    data = {
        "_name": "",
        "DataVersion": DATA_VERSION,
        "size": [sx, sy, sz],
        "palette": [{"Name": "minecraft:air"}],
        "blocks": [],
        "entities": [],
    }
    # Fill all positions with air (state 0)
    for y in range(sy):
        for z in range(sz):
            for x in range(sx):
                data["blocks"].append({"pos": [x, y, z], "state": 0})
    write_nbt_file(nbt_path, data)
    print(f"Created {sx}x{sy}x{sz} structure: {nbt_path}")


def cmd_fill(nbt_path, args):
    if len(args) < 7:
        print("Usage: nbt.py fill <file.nbt> <x1> <y1> <z1> <x2> <y2> <z2> <block> [props...]",
              file=sys.stderr)
        sys.exit(1)
    x1, y1, z1 = int(args[0]), int(args[1]), int(args[2])
    x2, y2, z2 = int(args[3]), int(args[4]), int(args[5])
    block_entry = _parse_block_arg(args[6:])

    data = read_nbt_file(nbt_path)
    state_index = _get_or_add_palette_index(data, block_entry)

    count = 0
    for y in range(min(y1, y2), max(y1, y2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            for x in range(min(x1, x2), max(x1, x2) + 1):
                _set_block(data, x, y, z, state_index)
                count += 1
    write_nbt_file(nbt_path, data)
    print(f"Filled {count} blocks with {block_entry['Name']}: {nbt_path}")


def cmd_set(nbt_path, args):
    if len(args) < 4:
        print("Usage: nbt.py set <file.nbt> <x> <y> <z> <block> [props...]", file=sys.stderr)
        sys.exit(1)
    x, y, z = int(args[0]), int(args[1]), int(args[2])
    block_entry = _parse_block_arg(args[3:])

    data = read_nbt_file(nbt_path)
    state_index = _get_or_add_palette_index(data, block_entry)
    _set_block(data, x, y, z, state_index)
    write_nbt_file(nbt_path, data)
    print(f"Set ({x}, {y}, {z}) to {block_entry['Name']}: {nbt_path}")


def cmd_info(nbt_path):
    data = read_nbt_file(nbt_path)
    size = data["size"]
    palette = data["palette"]
    block_count = len(data["blocks"])
    print(f"Size: {size[0]}x{size[1]}x{size[2]}")
    print(f"Blocks: {block_count}")
    print(f"Palette ({len(palette)}):")
    for i, entry in enumerate(palette):
        props = entry.get("Properties", {})
        props_str = f" [{', '.join(f'{k}={v}' for k, v in props.items())}]" if props else ""
        print(f"  [{i}] {entry['Name']}{props_str}")


# ---- Main ----

def main():
    if len(sys.argv) < 3:
        print(__doc__, file=sys.stderr)
        sys.exit(1)

    command = sys.argv[1]
    nbt_path = sys.argv[2]
    rest = sys.argv[3:]

    if command == "read":
        data = read_nbt_file(nbt_path)
        print(json.dumps(data, indent=2, ensure_ascii=False))

    elif command == "write":
        if not rest:
            print("Usage: nbt.py write <file.nbt> <file.json | ->", file=sys.stderr)
            sys.exit(1)
        json_source = rest[0]
        if json_source == "-":
            data = json.load(sys.stdin)
        else:
            with open(json_source, "r") as f:
                data = json.load(f)
        write_nbt_file(nbt_path, data)
        print(f"Written: {nbt_path}", file=sys.stderr)

    elif command == "create":
        cmd_create(nbt_path, rest)

    elif command == "fill":
        cmd_fill(nbt_path, rest)

    elif command == "set":
        cmd_set(nbt_path, rest)

    elif command == "info":
        cmd_info(nbt_path)

    else:
        print(f"Unknown command: {command}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
