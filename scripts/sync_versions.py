"""Copies version-26.2/src/{main,dev}/java to version-26.3 and re-applies the three 26.3 differences.

26.2 is the source of truth for the game-side code. Minecraft 26.3 differs in only three places here:
  - blocks no longer declare a codec (StationBlock),
  - Player.drop(ItemStack, boolean) is gone, so Crafter drops with Containers.dropItemStack,
  - loot tables use another layout (kept as separate resource files, not touched here).
Run after every change to version-26.2:  python scripts/sync_versions.py
"""
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "version-26.2" / "src"
DST = ROOT / "version-26.3" / "src"
PKG = "com/itmecasey/barelycrafting"


def copy_tree(name):
    src = SRC / name / "java"
    dst = DST / name / "java"
    if dst.exists():
        shutil.rmtree(dst)
    shutil.copytree(src, dst)


def patch_block():
    p = DST / "main/java" / PKG / "block/StationBlock.java"
    s = p.read_text(encoding="utf-8")
    s = s.replace("import com.mojang.serialization.MapCodec;\n", "")
    s = s.replace("    private static final MapCodec<StationBlock> CODEC = simpleCodec(StationBlock::new);\n\n", "")
    s = s.replace("""    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

""", "")
    assert "MapCodec" not in s
    p.write_text(s, encoding="utf-8", newline="\n")


def patch_crafter():
    p = DST / "main/java" / PKG / "Crafter.java"
    s = p.read_text(encoding="utf-8")
    s = s.replace("player.drop(rest, false);", "Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), rest);")
    s = s.replace("import net.minecraft.world.Container;\n", "import net.minecraft.world.Container;\nimport net.minecraft.world.Containers;\n")
    assert "player.drop(" not in s
    p.write_text(s, encoding="utf-8", newline="\n")


def main():
    copy_tree("main")
    copy_tree("dev")
    patch_block()
    patch_crafter()
    print("version-26.3 synced from version-26.2")


if __name__ == "__main__":
    main()
