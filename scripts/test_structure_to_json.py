import unittest

from structure_to_json import inherit_annotations


class InheritAnnotationsTest(unittest.TestCase):
    def test_keeps_unchanged_roles_and_resets_replaced_blocks(self):
        previous = {
            "cells": [
                {"pos": [1, 0, 1], "block": "hbm:trapdoor_steel", "role": "HATCH_FIXED",
                 "caseBreach": True},
                {"pos": [2, 0, 1], "block": "hbm:brick_compound", "role": "SEAL"},
                {"pos": [3, 0, 1], "block": "minecraft:air", "role": "MUST_AIR"},
            ],
            "rotorAnnotations": [{"pos": [1, 0, 1], "role": "ROTOR"},
                                 {"pos": [2, 0, 1], "role": "ROTOR"}],
        }
        fresh = {"cells": [
            {"pos": [1, 0, 1], "block": "hbm:trapdoor_steel", "properties": {"open": "true"}},
            {"pos": [2, 0, 1], "block": "hbm:reinforced_glass"},
            {"pos": [4, 0, 1], "block": "hbm:fusion_component"},
        ]}
        result = inherit_annotations(fresh, previous)
        cells = {tuple(cell["pos"]): cell for cell in result["cells"]}
        self.assertEqual(cells[(1, 0, 1)]["role"], "HATCH_FIXED")
        self.assertTrue(cells[(1, 0, 1)]["caseBreach"])
        self.assertEqual(cells[(2, 0, 1)]["role"], "EXACT")
        self.assertEqual(cells[(4, 0, 1)]["role"], "EXACT")
        self.assertEqual(cells[(3, 0, 1)]["role"], "MUST_AIR")
        self.assertEqual(result["rotorAnnotations"], previous["rotorAnnotations"][:1])


if __name__ == "__main__":
    unittest.main()
