package net.samtrion.dotcraft.resources.common;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import net.samtrion.dotcraft.resources.tool.ToolTier;
import net.samtrion.dotcraft.resources.tool.ToolType;

/**
 * A block that gets {@link CompressedTier#MAX} compression tiers. Bases are not coded but listed in the
 * source files {@code compressed/*.json} and read by {@link CompressedBases}; a source belongs to one mod,
 * and its bases are only used when that mod is loaded.
 */
public final class CompressedBase {
    /** A color as {@code #RRGGBB}. */
    private static final Codec<String> TINT = Codec.STRING.validate(t -> t.matches("#[0-9a-fA-F]{6}")
            ? DataResult.success(t) : DataResult.error(() -> "\"tint\" must look like #RRGGBB: " + t));

    /** Grass color of the plains, the default tint of {@link CompressedLook#GRASS} blocks. */
    private static final String DEFAULT_TINT = "#91BD59";

    /**
     * Source format of a single base: {@code mod} (filled in from the source file), {@code id} (path in that
     * mod), and optional {@code tool}, {@code tier}, {@code look}, {@code side}, {@code end} (top) and
     * {@code bottom} and, for {@code grass} blocks, {@code tint}. The bottom defaults to the end texture and is not
     * allowed for {@code log} blocks, which can be rotated. Textures are {@code <namespace>:<path>} or just {@code <path>} in the namespace of the mod.
     */
    public static final Codec<CompressedBase> CODEC = RecordCodecBuilder.<CompressedBase>create(i -> i.group(
            Codec.STRING.fieldOf("mod").forGetter(b -> b.id.getNamespace()),
            Codec.STRING.fieldOf("id").forGetter(b -> b.id.getPath()),
            ToolType.CODEC.optionalFieldOf("tool", ToolType.NONE).forGetter(b -> b.tool),
            ToolTier.CODEC.optionalFieldOf("tier", ToolTier.NONE).forGetter(b -> b.need),
            CompressedLook.CODEC.optionalFieldOf("look", CompressedLook.CUBE).forGetter(b -> b.look),
            Codec.STRING.optionalFieldOf("side").forGetter(b -> b.sideSource),
            Codec.STRING.optionalFieldOf("end").forGetter(b -> b.endSource),
            Codec.STRING.optionalFieldOf("bottom").forGetter(b -> b.bottomSource),
            TINT.optionalFieldOf("tint").forGetter(b -> b.tintSource)
    ).apply(i, CompressedBase::new)).validate(b -> b.validate());

    /** Registry id of the base block; the namespace is the mod that has to be loaded. */
    public final Identifier id;
    /** The tool the block is mined with. */
    public final ToolType tool;
    /** The lowest tool tier that gets a drop; {@link ToolTier#NONE} means any tool, even bare hands. */
    public final ToolTier need;
    /** The texture layout. */
    public final CompressedLook look;
    private final Optional<String> sideSource;
    private final Optional<String> endSource;
    private final Optional<String> bottomSource;
    private final Optional<String> tintSource;
    private final int tint;
    private final Identifier side;
    private final Identifier end;
    private final Identifier bottom;

    private CompressedBase(String mod, String path, ToolType tool, ToolTier need, CompressedLook look,
                           Optional<String> side, Optional<String> end, Optional<String> bottom, Optional<String> tint) {
        this.id = Identifier.fromNamespaceAndPath(mod, path);
        this.tool = tool;
        this.need = need;
        this.look = look;
        this.sideSource = side;
        this.endSource = end;
        this.bottomSource = bottom;
        this.tintSource = tint;
        this.tint = 0xFF000000 | Integer.parseInt(tint.orElse(DEFAULT_TINT).substring(1), 16);
        this.side = side.map(t -> texture(mod, t)).orElseGet(() -> derived(this.id, look.side));
        this.end = end.map(t -> texture(mod, t)).orElseGet(() -> derived(this.id, look.end));
        this.bottom = bottom.map(t -> texture(mod, t)).orElse(this.end);
    }

    private DataResult<CompressedBase> validate() {
        if (bottomSource.isPresent() && look == CompressedLook.LOG) {
            return DataResult.error(() -> "\"bottom\" is not allowed for \"log\" blocks, they can be rotated");
        }
        if (tintSource.isPresent() && look != CompressedLook.GRASS) {
            return DataResult.error(() -> "\"tint\" is only allowed for \"grass\" blocks");
        }
        if (look == CompressedLook.GRASS && bottomSource.isEmpty()) {
            return DataResult.error(() -> "\"grass\" blocks need a \"bottom\" texture");
        }
        return DataResult.success(this);
    }

    private static Identifier texture(String mod, String texture) {
        return texture.contains(":") ? Identifier.parse(texture) : Identifier.fromNamespaceAndPath(mod, texture);
    }

    private static Identifier derived(Identifier block, String suffix) {
        return Identifier.fromNamespaceAndPath(block.getNamespace(), "block/" + block.getPath() + suffix);
    }

    /**
     * The tool tier a compressed block of the given tier needs, see {@link ToolTier#progressed(int)}.
     *
     * @param tier the tier, {@code 1..MAX}
     * @return the required tool tier
     */
    public ToolTier required(int tier) {
        return need.progressed(tier);
    }

    /**
     * Tells whether the mod of the base block is loaded; other bases are neither registered nor generated.
     *
     * @return {@code true} for vanilla and for loaded mods
     */
    public boolean enabled() {
        return modId().equals("minecraft") || ModList.get().isLoaded(modId());
    }

    /**
     * The mod the base block comes from.
     *
     * @return the namespace of {@link #id}
     */
    public String modId() {
        return id.getNamespace();
    }

    /**
     * The base block; only valid when {@link #enabled()} and the mod has registered its blocks.
     *
     * @return the compressed block
     */
    public Block block() {
        return BuiltInRegistries.BLOCK.getValue(id);
    }

    /** @return texture of the sides */
    public Identifier sideTexture() {
        return side;
    }

    /** @return texture of the top, and of the bottom unless {@link #bottomTexture()} differs */
    public Identifier endTexture() {
        return end;
    }

    /** @return the opaque ARGB tint of {@link CompressedLook#GRASS} blocks */
    public int tint() {
        return tint;
    }

    /** @return the tinted overlay of the sides of {@link CompressedLook#GRASS} blocks, {@code <id>_side_overlay} */
    public Identifier tintOverlayTexture() {
        return derived(id, "_side_overlay");
    }

    /** @return texture of the bottom, by default the one of the top */
    public Identifier bottomTexture() {
        return bottom;
    }

    /**
     * Recipe path that is grouped by the mod the base block comes from, so recipes for blocks of
     * other mods stay apart, e.g. {@code compress/minecraft/stone_3}.
     *
     * @param kind {@code compress} or {@code decompress}
     * @param tier the tier, {@code 1..MAX}
     * @return the recipe path inside this mod's namespace
     */
    public String recipePath(String kind, int tier) {
        return kind + "/" + modId() + "/" + id.getPath() + "_" + tier;
    }

    /**
     * Registry path of one tier, grouped by the mod of the base block so every generated file
     * (blockstate, model, item, loot table) lands in the same folder, e.g. {@code compressed/minecraft/stone_3}.
     *
     * @param tier the tier, {@code 1..MAX}
     * @return the block and item path
     */
    public String path(int tier) {
        return "compressed/" + modId() + "/" + id.getPath() + "_" + tier;
    }
}
