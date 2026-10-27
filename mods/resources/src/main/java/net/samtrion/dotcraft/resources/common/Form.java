package net.samtrion.dotcraft.resources.common;

import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.samtrion.dotcraft.resources.common.conditions.Conditions;
import net.samtrion.dotcraft.resources.tool.ToolType;

/**
 * A form a {@link Material} can take, such as the ingot: one source file {@code forms/<id>.json} describes how
 * every item or block of the form is registered, modelled, tagged, named and crafted, so a new form is one more
 * file (plus the grayscale model and texture template) and no code. Every form is one registered item or block
 * per material, named by {@link #pathPattern()}; a hosted form such as the ore (see {@link BlockSpec#hosted()})
 * has one block per host and richness. Example:
 * <pre>
 * {
 *   "id": "block",
 *   "kind": "block",
 *   "order": 60,
 *   "configKey": "blocks",
 *   "template": "block",
 *   "tinted": true,
 *   "path": "%s_block",
 *   "tag": { "parent": "storage_blocks", "name": "%s" },
 *   "name": { "en_us": "Block of %s", "de_de": "%s-Block" },
 *   "block": { "mapColor": "metal", "sound": "metal", "requiresTool": true, "hardness": 5.0, "resistance": 6.0,
 *             "tool": "pickaxe", "tierGated": true, "loot": "self" },
 *   "conversions": [ { "from": "ingot", "kind": "pack", "ratio": 9 } ]
 * }
 * </pre>
 * Required are {@code id}, {@code kind}, {@code order}, {@code configKey}, {@code tag} and {@code name} (one pattern per supported
 * locale, see {@link Locales}, with the material name as {@code %s}), and the {@code block} entry of a block.
 * The {@code configKey} (plural style, e.g. {@code ingots}) names the form in the config, see {@link #CONFIG_KEY}.
 * Optional are {@code template} (name of the model and texture template, default the id), {@code tinted}
 * (default true, the template is colored with the material color), {@code variants} (default 1; with more the
 * model and texture are {@code <template>_<n>} and a material uses the one its id hashes to, see {@link #modelOf}), {@code path} (registry path pattern, default
 * {@code %s_<id>}), {@code mined} (the form is only found in the ground, so alloys lack it), {@code external}
 * ({@code excluded}, {@code default} or {@code optional}: whether a material of an external metal must not have
 * the form, has it by default or may list it), {@code icon} (the form shown as the creative tab icon),
 * {@code requires} (forms a material needs to have this one), {@code tool} (a crafting tool, see {@link ToolSpec}) and {@code conversions}. The standard
 * {@code neoforge:conditions} list (see {@link Conditions}) keeps the form out of the game while a condition fails: it is
 * not registered, not in the config and has no recipes, and so is every form that {@code requires} it or
 * names it as the {@code drop} of its block (the ore of the raw form). The data generation makes everything and writes the conditions into the data of the form.
 *
 * @param id          path of the form, also the file name without {@code .json}
 * @param kind        whether the form is an item or a block
 * @param order       position in the creative tab and in every list of forms, unique
 * @param configKey   name of the form in the config, {@code materials.<material>.<configKey>.enabled}, unique
 * @param template    name of the model and texture template {@code <kind>/material/<template>}
 * @param tinted      whether the model is colored with the color of the material
 * @param variants    the number of pre-rotated templates {@code <template>_<n>} a material picks from by a hash of its id, 1 for a single template
 * @param pathPattern registry path of an item or block, the material id is {@code %s}
 * @param tag         the convention tag of the form
 * @param names       display name pattern by locale, the material name is {@code %s}
 * @param block       the block properties, only for a block
 * @param mined       whether the form is only found in the ground
 * @param external     what a material of an external metal does with the form
 * @param icon        whether the form is the creative tab icon, the first one a material has wins
 * @param requires    ids of the forms a material needs to have this one
 * @param tool        set for a crafting tool such as the hammer: a damageable item that stays in the crafting grid, see {@link ToolSpec}
 * @param conversions how the form is made of other forms
 * @param conditions  the {@code neoforge:conditions} of the source, see {@link Conditions}; the form is not loaded while one fails
 */
public record Form(String id, Kind kind, int order, String configKey, String template, boolean tinted, int variants, String pathPattern, Tag tag,
                   Map<String, String> names, Optional<BlockSpec> block, boolean mined, ExternalRule external,
                   boolean icon, List<String> requires, Optional<ToolSpec> tool,
                   List<Conversion> conversions,
                   List<ICondition> conditions) {
    private static final Codec<String> ID = Codec.STRING.validate(s -> s.matches("[a-z][a-z0-9_]*")
            ? DataResult.success(s) : DataResult.error(() -> "an id must be lower case letters, digits and _: " + s));

    /**
     * A config key such as {@code ingots}: lower case letters, digits and {@code _}, and not {@code enabled} or {@code recipes}, which
     * is the switch of the same config table. The form is switched by {@code materials.<material>.<configKey>.enabled}.
     */
    public static final Codec<String> CONFIG_KEY = Codec.STRING.validate(s -> s.matches("[a-z][a-z0-9_]*") && !s.equals("enabled") && !s.equals("recipes")
            ? DataResult.success(s)
            : DataResult.error(() -> "a \"configKey\" must be lower case letters, digits and _, and not \"enabled\" or \"recipes\": " + s));

    /** The fields of a source except the conditions. */
    private static final MapCodec<Form> FIELDS = RecordCodecBuilder.<Form>mapCodec(i -> i.group(
            ID.fieldOf("id").forGetter(Form::id),
            Kind.CODEC.fieldOf("kind").forGetter(Form::kind),
            Codec.INT.fieldOf("order").forGetter(Form::order),
            CONFIG_KEY.fieldOf("configKey").forGetter(Form::configKey),
            Codec.STRING.optionalFieldOf("template").forGetter(f -> Optional.of(f.template)),
            Codec.BOOL.optionalFieldOf("tinted", true).forGetter(Form::tinted),
            Codec.STRING.optionalFieldOf("path").forGetter(f -> Optional.of(f.pathPattern)),
            Tag.CODEC.fieldOf("tag").forGetter(Form::tag),
            Locales.keyed(Codec.STRING).fieldOf("name").forGetter(Form::names),
            BlockSpec.CODEC.optionalFieldOf("block").forGetter(Form::block),
            Codec.BOOL.optionalFieldOf("mined", false).forGetter(Form::mined),
            ExternalRule.CODEC.optionalFieldOf("external", ExternalRule.OPTIONAL).forGetter(Form::external),
            Codec.BOOL.optionalFieldOf("icon", false).forGetter(Form::icon),
            ID.listOf().optionalFieldOf("requires", List.of()).forGetter(Form::requires),
            Conversion.CODEC.listOf().optionalFieldOf("conversions", List.of()).forGetter(Form::conversions)
    ).apply(i, Form::fromSource));

    /** Source format, see the class description; the conditions and the tool are read apart as the record has more fields than a group takes. */
    public static final Codec<Form> CODEC = Codec.mapPair(Codec.mapPair(Codec.mapPair(FIELDS, Conditions.FIELD),
                    ToolSpec.CODEC.optionalFieldOf("tool")), Codec.intRange(1, 16).optionalFieldOf("variants", 1)).codec()
            .xmap(p -> p.getFirst().getFirst().getFirst().withConditions(p.getFirst().getFirst().getSecond())
                            .withTool(p.getFirst().getSecond()).withVariants(p.getSecond()),
                    f -> Pair.of(Pair.of(Pair.of(f, f.conditions), f.tool), f.variants)).validate(Form::validate);

    /** Makes the collections immutable. */
    public Form {
        names = Map.copyOf(names);
        requires = List.copyOf(requires);
        conversions = List.copyOf(conversions);
        conditions = List.copyOf(conditions);
    }

    /**
     * The conversion that mixes alloys in this form.
     *
     * @return the {@code alloy} conversion, empty if alloys are not mixed in this form
     */
    public Optional<Conversion> alloy() {
        return conversions.stream().filter(c -> c.kind() == Conversion.Kind.ALLOY).findFirst();
    }

    /** Builds a form from the parsed source, with the defaults of {@code template} and {@code path}. */
    private static Form fromSource(String id, Kind kind, int order, String configKey, Optional<String> template, boolean tinted,
                                   Optional<String> path, Tag tag, Map<String, String> names, Optional<BlockSpec> block,
                                   boolean mined, ExternalRule external, boolean icon, List<String> requires,
                                   List<Conversion> conversions) {
        return new Form(id, kind, order, configKey, template.orElse(id), tinted, 1, path.orElse("%s_" + id), tag, names, block, mined,
                external, icon, requires, Optional.empty(), conversions, List.of());
    }

    /** A copy with other conditions. */
    Form withConditions(List<ICondition> conditions) {
        return new Form(id, kind, order, configKey, template, tinted, variants, pathPattern, tag, names, block, mined, external, icon, requires, tool,
                conversions, conditions);
    }

    /** A copy with another tool entry. */
    Form withVariants(int variants) {
        return new Form(id, kind, order, configKey, template, tinted, variants, pathPattern, tag, names, block, mined, external, icon, requires, tool,
                conversions, conditions);
    }

    Form withTool(Optional<ToolSpec> tool) {
        return new Form(id, kind, order, configKey, template, tinted, variants, pathPattern, tag, names, block, mined, external, icon, requires, tool,
                conversions, conditions);
    }

    /** Checks that the entries fit together and that the patterns can be formatted. */
    private DataResult<Form> validate() {
        if (kind == Kind.BLOCK && block.isEmpty()) return DataResult.error(() -> id + ": a block form needs a \"block\" entry");
        if (kind == Kind.ITEM && block.isPresent()) return DataResult.error(() -> id + ": an item form has no \"block\" entry");
        if (tool.isPresent() && kind != Kind.ITEM) return DataResult.error(() -> id + ": a \"tool\" form is an item");
        if (!pathPattern.contains("%s")) return DataResult.error(() -> id + ": \"path\" must contain %s");
        try {
            String.format(Locale.ROOT, pathPattern, "x");
            String.format(Locale.ROOT, tag.name(), "x");
            for (String pattern : names.values()) {
                if (hosted()) String.format(Locale.ROOT, pattern, "a", "b", "c");
                else String.format(Locale.ROOT, pattern, "x");
            }
        } catch (IllegalFormatException e) {
            return DataResult.error(() -> id + ": bad pattern: " + e.getMessage());
        }
        if (block.isPresent()) {
            BlockSpec spec = block.get();
            if (spec.hosted().isEmpty() && (spec.mapColor().isEmpty() || spec.hardness().isEmpty())) {
                return DataResult.error(() -> id + ": a block form needs \"mapColor\" and \"hardness\" unless it is \"hosted\"");
            }
            if (spec.loot() == BlockSpec.Loot.ORE && (spec.drop().isEmpty() || spec.hosted().isEmpty())) {
                return DataResult.error(() -> id + ": the \"ore\" loot needs a \"drop\" form and a \"hosted\" entry");
            }
        }
        for (Conversion c : conversions) {
            if (c.kind() == Conversion.Kind.PACK && c.ratio() != 4 && c.ratio() != 9) {
                return DataResult.error(() -> id + ": a \"pack\" conversion needs a \"ratio\" of 4 or 9");
            }
            if (c.kind() == Conversion.Kind.CRAFT && (c.ratio() < 1 || c.ratio() > 9)) {
                return DataResult.error(() -> id + ": a \"craft\" conversion needs a \"ratio\" of 1 to 9");
            }
            if (c.from().equals(id)) return DataResult.error(() -> id + ": a conversion cannot be from the form itself");
        }
        return DataResult.success(this);
    }

    /**
     * Checks that the form is translated into every supported locale, also the parts of a hosted form.
     *
     * @throws IllegalArgumentException if a supported locale is missing
     */
    void requireLocales() {
        Locales.requireAll(id, "\"name\"", names.keySet());
        block.flatMap(BlockSpec::hosted).ifPresent(h -> {
            Locales.requireAll(id, "hosted \"richness\" names", h.richness().keySet());
            Locales.requireAll(id, "hosted \"host\" names", h.host().keySet());
            for (String locale : Locales.SUPPORTED) {
                for (OreRichness r : OreRichness.values()) {
                    if (!h.richness().get(locale).containsKey(r.id())) {
                        throw new IllegalArgumentException(id + ": no " + locale + " name for the richness " + r.id());
                    }
                }
                for (OreHost host : OreHost.values()) {
                    if (!h.host().get(locale).containsKey(host.getSerializedName())) {
                        throw new IllegalArgumentException(id + ": no " + locale + " name for the host " + host.getSerializedName());
                    }
                }
            }
        });
    }

    /** @return {@code true} if the form is a block (with a block item), {@code false} for plain items */
    /**
     * The model of this form for a material: the {@link #template() template} itself, or when the form has several
     * {@link #variants() variants} (pre-rotated or mirrored textures, as of the nuggets, so that not all look alike)
     * {@code <template>_<n>} with {@code n} taken from a stable hash of the material id.
     *
     * @param materialId the id of the material
     * @return the name of the model and texture below {@code item/material/}
     */
    public String modelOf(String materialId) {
        return variants == 1 ? template : template + "_" + Math.floorMod(materialId.hashCode(), variants);
    }

    public boolean isBlock() {
        return kind == Kind.BLOCK;
    }

    /** @return {@code true} if the form has one block per host and richness, like the ore */
    public boolean hosted() {
        return block.flatMap(BlockSpec::hosted).isPresent();
    }

    /** @return the folder of the {@code c} convention tags of this form, e.g. {@code ingots} */
    public String tagFolder() {
        return tag.parent();
    }

    /**
     * The name inside the tag folder: storage blocks of the different forms share the folder, so
     * {@code c:storage_blocks/tin}, {@code c:storage_blocks/raw_tin} and {@code c:storage_blocks/tin_dust} differ.
     *
     * @param material the material
     * @return the tag name without the folder
     */
    public String tagName(Material material) {
        return String.format(Locale.ROOT, tag.name(), material.id());
    }

    /**
     * The display name of an item or block of this form.
     *
     * @param locale       the locale, e.g. {@code en_us}
     * @param materialName the display name of the material in that locale
     * @return e.g. {@code Tin Ingot}
     */
    public String displayName(String locale, String materialName) {
        if (hosted()) throw new IllegalArgumentException("hosted forms are named by the host and richness");
        return String.format(Locale.ROOT, names.get(locale), materialName);
    }

    /**
     * The display name of a block of a hosted form.
     *
     * @param locale       the locale, e.g. {@code en_us}
     * @param materialName the display name of the material in that locale
     * @param host         the block the ore sits in
     * @param richness     how much the ore yields
     * @return e.g. {@code Rich Deepslate Tin Ore}
     */
    public String displayName(String locale, String materialName, OreHost host, OreRichness richness) {
        BlockSpec.Hosted hosted = block.flatMap(BlockSpec::hosted).orElseThrow();
        return String.format(Locale.ROOT, names.get(locale), hosted.richness().get(locale).get(richness.id()),
                hosted.host().get(locale).get(host.getSerializedName()), materialName);
    }

    /** Whether the form is an item or a block. */
    public enum Kind implements StringRepresentable {
        /** A plain item. */
        ITEM,
        /** A block with a block item. */
        BLOCK;

        /** Codec that reads the lower case name. */
        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** What a material of an external metal does with a form. */
    public enum ExternalRule implements StringRepresentable {
        /** The external metal provides the form, the material must not have it. */
        EXCLUDED,
        /** The material has the form unless its source lists the forms. */
        DEFAULT,
        /** The material may list the form. */
        OPTIONAL;

        /** Codec that reads the lower case name. */
        public static final Codec<ExternalRule> CODEC = StringRepresentable.fromEnum(ExternalRule::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * The convention tag of a form: {@code c:<parent>/<name>}.
     *
     * @param parent folder and parent tag, e.g. {@code ingots}
     * @param name   name pattern inside the folder, the material id is {@code %s}
     */
    public record Tag(String parent, String name) {
        /** Source format: {@code {"parent": "storage_blocks", "name": "raw_%s"}}, the name defaults to {@code %s}. */
        public static final Codec<Tag> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("parent").forGetter(Tag::parent),
                Codec.STRING.optionalFieldOf("name", "%s").forGetter(Tag::name)
        ).apply(i, Tag::new));
    }

    /**
     * How the blocks of a form behave. Names such as the map color and the sound are resolved when the block is
     * registered.
     *
     * @param mapColor     map color name, e.g. {@code metal}; not for a hosted form, the host decides
     * @param sound        sound type name, e.g. {@code metal}; the default sound where absent
     * @param requiresTool whether a drop needs the right tool
     * @param hardness     how long it takes to mine; not for a hosted form
     * @param resistance   the explosion resistance, the hardness where absent
     * @param tool         the tool the block is mined with
     * @param tierGated    whether the tool needs the tier of the material
     * @param loot         what the block drops
     * @param drop         id of the form that the {@link Loot#ORE} loot drops
     * @param hosted       set for a form with one block per host and richness
     */
    public record BlockSpec(Optional<String> mapColor, Optional<String> sound, boolean requiresTool, Optional<Float> hardness,
                            Optional<Float> resistance, ToolType tool, boolean tierGated, Loot loot, Optional<String> drop,
                            Optional<Hosted> hosted) {
        /** Source format, see {@link Form}. */
        public static final Codec<BlockSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("mapColor").forGetter(BlockSpec::mapColor),
                Codec.STRING.optionalFieldOf("sound").forGetter(BlockSpec::sound),
                Codec.BOOL.optionalFieldOf("requiresTool", false).forGetter(BlockSpec::requiresTool),
                Codec.FLOAT.optionalFieldOf("hardness").forGetter(BlockSpec::hardness),
                Codec.FLOAT.optionalFieldOf("resistance").forGetter(BlockSpec::resistance),
                ToolType.CODEC.fieldOf("tool").forGetter(BlockSpec::tool),
                Codec.BOOL.optionalFieldOf("tierGated", false).forGetter(BlockSpec::tierGated),
                Loot.CODEC.optionalFieldOf("loot", Loot.SELF).forGetter(BlockSpec::loot),
                Codec.STRING.optionalFieldOf("drop").forGetter(BlockSpec::drop),
                Hosted.CODEC.optionalFieldOf("hosted").forGetter(BlockSpec::hosted)
        ).apply(i, BlockSpec::new));

        /** What a block drops when mined. */
        public enum Loot implements StringRepresentable {
            /** The block itself. */
            SELF,
            /** The {@code drop} form as vanilla ores do, in the count of the richness, and the ore with silk touch. */
            ORE;

            /** Codec that reads the lower case name. */
            public static final Codec<Loot> CODEC = StringRepresentable.fromEnum(Loot::values);

            @Override
            public String getSerializedName() {
                return name().toLowerCase(Locale.ROOT);
            }
        }

        /**
         * The name parts of a form with one block per host and richness, by locale and then by the name of the
         * host or richness; the display name pattern of the form gets richness, host and material name.
         *
         * @param richness the part in front of the name by richness, e.g. {@code Rich }
         * @param host     the part in front of the name by host, e.g. {@code Deepslate }
         */
        public record Hosted(Map<String, Map<String, String>> richness, Map<String, Map<String, String>> host) {
            /** Source format: {@code {"richness": {"en_us": {"poor": "Poor "}}, "host": {"en_us": {"stone": ""}}}}. */
            public static final Codec<Hosted> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Locales.keyed(Codec.unboundedMap(Codec.STRING, Codec.STRING)).fieldOf("richness").forGetter(Hosted::richness),
                    Locales.keyed(Codec.unboundedMap(Codec.STRING, Codec.STRING)).fieldOf("host").forGetter(Hosted::host)
            ).apply(i, Hosted::new));
        }
    }

    /**
     * A crafting tool: a damageable item that stays in the crafting grid and loses one durability per craft, so a recipe
     * that has it as an ingredient (by its tag) uses it up slowly instead of consuming it. Example, a hammer that makes
     * its head of the material:
     * <pre>
     * "tool": { "durability": 1.0, "pattern": ["##", "#|", " |"], "key": { "|": "c:rods/wooden" } }
     * </pre>
     * The recipe of the tool itself is shaped with the {@code pattern} where there is one, else shapeless. {@code #} is
     * the ingredient of the material, its {@code tools} entry (see {@link Material.Tool}); every other symbol is in the
     * {@code key}, which names the item tag of each. Without a pattern the shapeless recipe takes the material
     * ingredient and one of each tag of the {@code key}. A tool form is never a default form of a material: only
     * materials that list it have it.
     *
     * @param durability factor of the durability of vanilla tools of the tier of the material, {@code 1.0} for the same
     * @param pattern    the rows of the shaped recipe, empty for a shapeless one
     * @param key        item tag id by one-character symbol
     */
    public record ToolSpec(float durability, List<String> pattern, Map<String, Identifier> key) {
        /** The symbol of the ingredient of the material. */
        public static final String MATERIAL = "#";

        /** Source format, see the class description. */
        public static final Codec<ToolSpec> CODEC = RecordCodecBuilder.<ToolSpec>create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("durability", 1.0F).forGetter(ToolSpec::durability),
                Codec.STRING.listOf().optionalFieldOf("pattern", List.of()).forGetter(ToolSpec::pattern),
                Codec.unboundedMap(Codec.STRING, Identifier.CODEC).optionalFieldOf("key", Map.of()).forGetter(ToolSpec::key)
        ).apply(i, ToolSpec::new)).validate(ToolSpec::validate);

        /** Makes the collections immutable. */
        public ToolSpec {
            pattern = List.copyOf(pattern);
            key = Map.copyOf(key);
        }

        /** Checks the durability, the symbols of the key and that the pattern uses the material and only known symbols. */
        private DataResult<ToolSpec> validate() {
            if (durability <= 0) return DataResult.error(() -> "a tool \"durability\" must be above 0");
            for (String symbol : key.keySet()) {
                if (symbol.length() != 1 || symbol.equals(MATERIAL) || symbol.equals(" ")) {
                    return DataResult.error(() -> "a tool \"key\" symbol must be one character, not # or a space: \"" + symbol + "\"");
                }
            }
            if (!pattern.isEmpty()) {
                String rows = String.join("", pattern);
                if (!rows.contains(MATERIAL)) return DataResult.error(() -> "a tool \"pattern\" must use the material symbol #");
                for (char c : rows.toCharArray()) {
                    if (c != ' ' && c != '#' && !key.containsKey(String.valueOf(c))) {
                        return DataResult.error(() -> "a tool \"pattern\" symbol is not in the \"key\": " + c);
                    }
                }
            }
            return DataResult.success(this);
        }
    }

    /**
     * How the form is made of another form of the same material; the recipes are made where the material has
     * both forms.
     *
     * @param from            id of the other form, empty for an {@code alloy}
     * @param kind            {@code pack}: {@code ratio} items of the other form make one of this form and one makes
     *                        {@code ratio} back; {@code smelt}: one item is smelted and blasted into one of this form; {@code blast}: one item is only blasted into one of this form;
     *                        {@code craft}: {@code ratio} items of the other form make one of this form, shapeless and
     *                        one way, also for an external material whose source names a tag for the other form
     * @param ratio           items of a pack, {@code 4} or {@code 9}; of a craft, {@code 1..9}
     *                        {@code alloy}: alloys are mixed in this form, from the forms of the alloy inputs
     *                        (see {@link Material.Alloy}); {@code external} is the key of the {@code external} item of an input that lacks the form;
     *                        {@code crush}: like a craft, but the hammer tag is an ingredient as well (the hammer stays and
     *                        wears) and {@code ratio} is the number of items that come out
     * @param xp              experience of a smelt or blast
     * @param materials       ids of the materials the conversion is made for, all where empty
     * @param external        of an {@code alloy}: the {@code external} key that resolves an input that lacks the form
     * @param externalRequired whether a material of an external metal that has the other form but not this one must
     *                        name the external item of this form, which then is the result of the smelt or blast
     */
    public record Conversion(String from, Kind kind, int ratio, float xp, List<String> materials, Optional<String> external,
                             boolean externalRequired) {
        /** Source format: {@code {"from": "nugget", "kind": "pack", "ratio": 9}}, {@code {"from": "dust", "kind": "smelt", "xp": 0.7}} or {@code {"from": "ingot", "kind": "craft", "ratio": 1}}. */
        public static final Codec<Conversion> CODEC = RecordCodecBuilder.<Conversion>create(i -> i.group(
                Codec.STRING.optionalFieldOf("from", "").forGetter(Conversion::from),
                Kind.CODEC.fieldOf("kind").forGetter(Conversion::kind),
                Codec.INT.optionalFieldOf("ratio", 9).forGetter(Conversion::ratio),
                Codec.FLOAT.optionalFieldOf("xp", 0.0F).forGetter(Conversion::xp),
                Codec.STRING.listOf().optionalFieldOf("materials", List.of()).forGetter(Conversion::materials),
                Codec.STRING.optionalFieldOf("external").forGetter(Conversion::external),
                Codec.BOOL.optionalFieldOf("externalRequired", false).forGetter(Conversion::externalRequired)
        ).apply(i, Conversion::new)).validate(c -> (c.kind == Kind.ALLOY) == c.from.isEmpty() ? DataResult.success(c)
                : DataResult.error(() -> c.kind == Kind.ALLOY ? "an \"alloy\" has no \"from\"" : "a conversion needs a \"from\""));

        /** The kind of a conversion. */
        public enum Kind implements StringRepresentable {
            /** Crafting in both directions. */
            PACK,
            /** Smelting and blasting. */
            SMELT,
            /** Blasting only. */
            BLAST,
            /** Shapeless crafting in one direction. */
            CRAFT,
            /** Shapeless crafting with a hammer in one direction, the hammer wears. */
            CRUSH,
            /** The alloys are mixed in this form, one shapeless recipe of the inputs of the alloy. */
            ALLOY;

            /** Codec that reads the lower case name. */
            public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

            @Override
            public String getSerializedName() {
                return name().toLowerCase(Locale.ROOT);
            }
        }
    }
}
