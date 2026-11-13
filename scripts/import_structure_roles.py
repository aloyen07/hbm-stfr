#!/usr/bin/env python3
"""Import an analyzer reference, preserving independent annotations and excluding undefined primary roles."""
import argparse
import copy
import json
from collections import Counter
from pathlib import Path


def import_roles(exported, template):
    if exported.get("formatVersion") != 1 or template.get("formatVersion") != 1:
        raise ValueError("Unsupported role/template format")
    positions = set()
    cells = []
    for cell in exported["cells"]:
        pos = tuple(cell["pos"])
        if len(pos) != 3 or any(not 0 <= n < 29 for n in pos) or pos in positions:
            raise ValueError(f"Invalid/duplicate reference position: {pos}")
        positions.add(pos)
        if cell.get("role") in (None, "UNDEFINED", "UNASSIGNED"):
            continue
        cells.append(copy.deepcopy(cell))
    if not cells:
        raise ValueError("No cells with defined primary roles")
    result = {key: copy.deepcopy(value) for key, value in exported.items() if key != "cells"}
    result.update(size=[29, 29, 29], center=[14, 14, 14], cells=cells)
    defined = {tuple(c["pos"]) for c in cells if c["block"] != "minecraft:air"}
    result["rotorAnnotations"] = [copy.deepcopy(mark) for mark in exported.get("rotorAnnotations", [])
                                  if tuple(mark["pos"]) in defined]
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("roles", type=Path)
    parser.add_argument("template", type=Path)
    parser.add_argument("--rotors", type=Path, help="Update the packaged rotor map as well")
    args = parser.parse_args()
    exported = json.loads(args.roles.read_text(encoding="utf-8"))
    template = json.loads(args.template.read_text(encoding="utf-8"))
    result = import_roles(exported, template)
    result["source"] = str(args.roles)
    args.template.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if args.rotors:
        args.rotors.write_text(json.dumps({"formatVersion": 1, "rotorAnnotations": result["rotorAnnotations"]},
                                          ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Imported {len(result['cells'])} cells; excluded {len(exported['cells'])-len(result['cells'])} undefined primary roles")
    print(Counter(cell["role"] for cell in result["cells"]))


if __name__ == "__main__":
    main()
