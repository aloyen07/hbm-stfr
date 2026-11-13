import unittest
from import_structure_roles import import_roles


class ReferenceImportTest(unittest.TestCase):
    def test_primary_undefined_excludes_extras_but_misc_undefined_keeps_structure(self):
        defined = dict(pos=[1, 2, 3], block="hbm:trapdoor_steel", role="HATCH", miscRole="UNDEFINED", meta=8,
                       properties={"half": "top", "open": "true"})
        injector = dict(pos=[4, 5, 6], block="hbm:block_lead", role="RADIATION_SHIELD", miscRole="INJECTOR_PLACEABLE",
                        injectorState={"block": "hbmstfr:solid_injector", "properties": {"facing": "west"}})
        extra = dict(pos=[7, 8, 9], block="uninstalled:pipe", role="UNDEFINED", miscRole="ROTABLE")
        old_extra = dict(pos=[8, 8, 9], block="hbm:red_copper_wire", role="UNASSIGNED")
        export = dict(formatVersion=1, heightOffset=14, cells=[defined, injector, extra, old_extra],
                      rotorAnnotations=[dict(pos=[1, 2, 3], axis="x"), dict(pos=[7, 8, 9], axis="z")])
        result = import_roles(export, dict(formatVersion=1))
        self.assertEqual(result["cells"], [defined, injector])
        self.assertEqual(result["rotorAnnotations"], [dict(pos=[1, 2, 3], axis="x")])
        self.assertEqual(result["heightOffset"], 14)
        result["cells"][0]["properties"]["open"] = "false"
        self.assertEqual(defined["properties"]["open"], "true")

    def test_bad_coordinates_do_not_overwrite_packaged_reference(self):
        for cells in [[dict(pos=[29, 0, 0], block="minecraft:stone", role="EXACT")],
                      [dict(pos=[1, 2, 3], block="minecraft:stone", role="EXACT")] * 2]:
            with self.assertRaises(ValueError):
                import_roles(dict(formatVersion=1, cells=cells), dict(formatVersion=1))


if __name__ == "__main__":
    unittest.main()
