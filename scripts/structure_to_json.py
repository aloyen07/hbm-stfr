#!/usr/bin/env python3
"""Convert a Minecraft 1.12.2 structure-block NBT export into STFR's JSON template.

No third-party Python packages are required. The source NBT must be gzip-compressed,
as written by the vanilla structure block.
"""

import argparse
import copy
import gzip
import json
import struct
from pathlib import Path


class NbtReader:
    def __init__(self, source):
        self.source = source

    def read(self, size):
        data = self.source.read(size)
        if len(data) != size:
            raise ValueError("Truncated NBT file")
        return data

    def number(self, code):
        return struct.unpack(">" + code, self.read(struct.calcsize(code)))[0]

    def string(self):
        return self.read(self.number("H")).decode("utf-8")

    def payload(self, tag):
        if tag in (1, 2, 3, 4, 5, 6):
            return self.number({1: "b", 2: "h", 3: "i", 4: "q", 5: "f", 6: "d"}[tag])
        if tag == 7:
            return self.read(self.number("i"))
        if tag == 8:
            return self.string()
        if tag == 9:
            element_type, count = self.number("B"), self.number("i")
            return [self.payload(element_type) for _ in range(count)]
        if tag == 10:
            result = {}
            while True:
                child_type = self.number("B")
                if child_type == 0:
                    return result
                name = self.string()
                result[name] = self.payload(child_type)
        if tag == 11:
            return [self.number("i") for _ in range(self.number("i"))]
        if tag == 12:
            return [self.number("q") for _ in range(self.number("i"))]
        raise ValueError("Unsupported NBT tag type: {}".format(tag))


def read_nbt(path):
    with gzip.open(path, "rb") as source:
        reader = NbtReader(source)
        if reader.number("B") != 10:
            raise ValueError("Expected a root NBT compound")
        reader.string()
        return reader.payload(10)


def horizontal_away_from_center(pos, center):
    dx, dz = pos[0] - center[0], pos[2] - center[2]
    return ('east' if dx > 0 else 'west') if abs(dx) > abs(dz) else ('south' if dz > 0 else 'north')


def convert(root, center):
    size = root.get("size")
    if not isinstance(size, list) or len(size) != 3 or any(n <= 0 for n in size):
        raise ValueError("Not a vanilla structure-block export: missing size[3]")
    if any(center[i] < 0 or center[i] >= size[i] for i in range(3)):
        raise ValueError("Center is outside the structure")
    palette = root.get("palette", [])
    cells = []
    seen = set()
    for entry in root.get("blocks", []):
        pos = entry.get("pos")
        index = entry.get("state")
        if not isinstance(pos, list) or len(pos) != 3 or not all(isinstance(n, int) for n in pos):
            raise ValueError("Invalid block position in structure")
        if not all(0 <= pos[i] < size[i] for i in range(3)):
            raise ValueError("Block position outside structure: {}".format(pos))
        if tuple(pos) in seen:
            raise ValueError("Duplicate block position: {}".format(pos))
        seen.add(tuple(pos))
        if not isinstance(index, int) or not 0 <= index < len(palette):
            raise ValueError("Invalid palette index at {}".format(pos))
        state = palette[index]
        block = state.get("Name")
        if not isinstance(block, str) or ":" not in block:
            raise ValueError("Invalid registry name at {}".format(pos))
        if block == "minecraft:air":
            continue
        properties = dict(state.get("Properties", {}))
        role = None
        if block == "minecraft:diamond_block":
            block = "hbmstfr:fuel_slot"
            properties = {"facing": "down", "handle_facing": horizontal_away_from_center(pos, center)}
            role = "FUEL_SLOT"
        elif block == "hbmstfr:fuel_slot":
            role = "FUEL_SLOT"
        elif block == "hbmstfr:multi_fluid_port":
            # Preserve the orientation actually exported by the structure block.
            role = "PORT"
        cell = {"pos": pos, "block": block}
        if properties:
            cell["properties"] = properties
        if role:
            cell["role"] = role
        cells.append(cell)
    cells.sort(key=lambda cell: (cell["pos"][1], cell["pos"][2], cell["pos"][0]))
    if not cells:
        raise ValueError("Structure contains no non-air blocks")
    return {"formatVersion": 1, "size": size, "center": center, "cells": cells}


def inherit_annotations(new_structure, old_template):
    """Carry editor roles into a fresh world export; new blocks get EXACT."""
    old_cells = {tuple(cell["pos"]): cell for cell in old_template.get("cells", [])}
    occupied = set()
    unchanged = set()
    for cell in new_structure["cells"]:
        pos = tuple(cell["pos"])
        occupied.add(pos)
        old = old_cells.get(pos)
        if old and old.get("block") == cell["block"]:
            unchanged.add(pos)
            for key in ("role", "miscRole", "caseBreach", "destructible", "injectorState"):
                if key in old:
                    cell[key] = copy.deepcopy(old[key])
        cell.setdefault("role", "EXACT")
    for pos, old in old_cells.items():
        if pos not in occupied and old.get("block") == "minecraft:air" \
                and old.get("role") in ("MUST_AIR", "MAYBE_AIR"):
            new_structure["cells"].append(copy.deepcopy(old))
    new_structure["cells"].sort(key=lambda cell: (cell["pos"][1], cell["pos"][2], cell["pos"][0]))
    for key in ("heightOffset", "liftBoxFirst", "liftBoxSecond", "liftReleaseY"):
        if key in old_template:
            new_structure[key] = copy.deepcopy(old_template[key])
    new_structure["rotorAnnotations"] = [copy.deepcopy(mark)
        for mark in old_template.get("rotorAnnotations", [])
        if tuple(mark["pos"]) in unchanged]
    return new_structure


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, help="structure-block .nbt file")
    parser.add_argument("output", type=Path, help="JSON template consumed by the mod")
    parser.add_argument("--center", nargs=3, type=int, metavar=("X", "Y", "Z"),
                        help="local coordinates of the empty reactor center (default: size midpoint)")
    parser.add_argument("--inherit", type=Path,
                        help="existing STFR JSON whose roles and rotor marks should be kept")
    args = parser.parse_args()
    root = read_nbt(args.source)
    size = root.get("size")
    if not isinstance(size, list) or len(size) != 3:
        parser.error("Input is not a vanilla structure-block NBT export")
    center = args.center or [n // 2 for n in size]
    result = convert(root, center)
    if args.inherit:
        inherited = json.loads(args.inherit.read_text(encoding="utf-8"))
        result = inherit_annotations(result, inherited)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Wrote {} cells to {}".format(len(result["cells"]), args.output))


if __name__ == "__main__":
    main()
