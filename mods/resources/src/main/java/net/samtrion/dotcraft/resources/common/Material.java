package net.samtrion.dotcraft.resources.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.samtrion.dotcraft.resources.common.conditions.Conditions;
import net.samtrion.dotcraft.resources.tool.ToolTier;

/**
 * A material such as tin: one source file {@code materials/<id>.json} gives the id, the color, the names and
 * optionally the forms and the worldgen and alloy settings; everything else (items, blocks, models, recipes,
 * tags, translations) is generated from it by its {@link Form}s. Example:
 * <pre>
 * {
 *   "id": "tin",
 *   "color": "#C9D3D6",
 *   "name": { "en_us": "Tin", "de_de": "Zinn" },
 *   "tier": "stone",
 *   "forms": ["raw", "ingot", "ore"],
 *   "ore": [ { "hosts": ["stone", "deepslate"], "veinSize": 8, "count": 12, "minY": -16, "maxY": 112, "biomeTag": "c:is_overworld" } ],
 *   "alloy": { "inputs": [ { "material": "copper", "count": 3 }, { "material": "tin", "count": 1 } ] }
 * }
 * </pre>
 * Required are {@code id}, {@code color}, the {@code tier} ({@code "none"} for a material that needs no tool, such as
 * coal) and a {@code name} for every supported locale (see {@link Locales};
 * {@link Materials} rejects a missing one). Without {@code forms} a material has all forms, or, with an
 * {@code alloy}, all but the {@link Form#mined() mined} ones. A material of a metal that is provided from outside, such
 * as iron of vanilla, has an {@code external} list of sources; a source maps a form id to the item tag that stands
 * for the form and, optionally, the concrete item that a recipe makes of it
 * ({@code "external": [{"forms": {"ingot": {"tag": "c:ingots/iron", "item": "minecraft:iron_ingot"}}}]}). A source may
 * name a {@code mod}: the recipes that use it are loaded only while that mod is, and its item need not exist when the
 * data is generated, so the integration of another mod is one more source and no code. A material with an
 * {@code external} list (also the empty source {@code [{}]}) registers none of the forms that the external metal
 * provides (see {@link Form.ExternalRule}) and has the forms that are {@code default} for it unless it lists its forms.
 * Its dust and grains smelt to the first external item of the form unless {@code "cook": false} turns all cook
 * conversions of the material off. The {@code ore} array and the {@code alloy} settings are validated here; the world
 * generation and the alloy recipes read them. The {@code ore} array has one config per element, see {@link OreSpec}. The standard {@code neoforge:conditions} list (see {@link Conditions}), e.g.
 * {@code [{"type": "neoforge:mod_loaded", "modid": "othermod"}]}, keeps the material out of the game while a condition fails: its items,
 * blocks, config switches and recipes do not exist; a form of the material that is left out by its own conditions is skipped.
 * A material lists the crafting tool forms it has (see {@link Form.ToolSpec}) and gives the ingredient of each in {@code tools}
 * ({@code "tools": {"hammer": {"ingredient": "c:ingots/iron", "durability": 600}}}, the durability optional).
 * The data generation makes everything, and every file of the material or of one of its forms carries the conditions of the
 * material and the form, see {@link #conditions(Form)}.
 *
 * @param id       path of the material, also the file name without {@code .json}; the namespace is this mod
 * @param color    opaque ARGB tint that colors the grayscale templates
 * @param names    display names by supported locale, e.g. {@code de_de}
 * @param tier     the lowest tool tier that gets a drop from the metal and ore blocks, {@link ToolTier#NONE} for no tier gating at all; required in the source
 * @param forms    the forms the material has, in creative tab order
 * @param ore      the world generation entries of the ore (see {@link OreSpec}), only with a hosted form; absent for one default entry
 * @param alloy    the recipe of an alloy
 * @param external the sources of a material that only adds parts to a metal provided from outside, empty for a plain material
 * @param cook     whether the {@code cook} conversions of the forms (smelting and blasting dust, grains, raw and ore
 *                 into the ingot or, for an external material, the external item) are made; {@code false} for materials
 *                 whose dust is converted by hand-written recipes, such as coal
 * @param tools    the ingredient and optional durability of each crafting tool form the material has, by form id, see {@link Tool}
 * @param conditions the {@code neoforge:conditions} of the source, see {@link Conditions}; the material is not loaded while one fails
 */
public record Material(String id, int color, Map<String, String> names, ToolTier tier, List<Form> forms,
                       Optional<List<OreSpec>> ore, Optional<Alloy> alloy, List<External> external, boolean cook, Map<String, Tool> tools,
                       List<ICondition> conditions) {
    private static final Codec<String> ID = Codec.STRING.validate(s -> s.matches("[a-z][a-z0-9_]*")
            ? DataResult.success(s) : DataResult.error(() -> "\"id\" must be lower case letters, digits and _: " + s));

    private static final Codec<String> COLOR = Codec.STRING.validate(s -> s.matches("#[0-9a-fA-F]{6}")
            ? DataResult.success(s) : DataResult.error(() -> "\"color\" must look like #RRGGBB: " + s));

    /**
     * The source format, see the class description.
     *
     * @param all the forms a source may refer to
     * @return the codec of a material source
     */
    public static Codec<Material> codec(List<Form> all) {
        return codec(all, List.of());
    }

    /**
     * The source format, see the class description; a form that is left out because of its conditions (see
     * {@link Forms}) may be listed and is skipped.
     *
     * @param all      the forms that are loaded
     * @param excluded the forms that are not
     * @return the codec of a material source
     */
    public static Codec<Material> codec(List<Form> all, List<Form> excluded) {
        Codec<Optional<Form>> form = Codec.STRING.comapFlatMap(id -> lookup(all, excluded, id), f -> f.orElseThrow().id());
        return RecordCodecBuilder.<Material>create(i -> i.group(
                ID.fieldOf("id").forGetter(Material::id),
                COLOR.fieldOf("color").forGetter(m -> String.format(Locale.ROOT, "#%06X", m.color & 0xFFFFFF)),
                Locales.keyed(Codec.STRING).optionalFieldOf("name", Map.of()).forGetter(Material::names),
                ToolTier.CODEC.fieldOf("tier").forGetter(Material::tier),
                form.listOf().optionalFieldOf("forms").forGetter(m -> Optional.of(m.forms.stream().map(Optional::of).toList())),
                OreSpec.LIST_CODEC.optionalFieldOf("ore").forGetter(Material::ore),
                Alloy.CODEC.optionalFieldOf("alloy").forGetter(Material::alloy),
                External.CODEC.listOf().optionalFieldOf("external", List.of()).forGetter(Material::external),
                Codec.BOOL.optionalFieldOf("cook", true).forGetter(Material::cook),
                Codec.unboundedMap(Codec.STRING, Tool.CODEC).optionalFieldOf("tools", Map.of()).forGetter(Material::tools),
                Conditions.FIELD.forGetter(Material::conditions)
        ).apply(i, (id, color, names, tier, forms, ore, alloy, external, cook, tools, conditions) ->
                fromSource(all, id, color, names, tier, forms, ore, alloy, external, cook, tools, conditions))).validate(m -> m.validate(all, excluded));
    }

    /** Looks up a form of a source: empty if it is left out, an error if it does not exist. */
    private static DataResult<Optional<Form>> lookup(List<Form> all, List<Form> excluded, String id) {
        for (Form f : all) {
            if (f.id().equals(id)) return DataResult.success(Optional.of(f));
        }
        if (excluded.stream().anyMatch(f -> f.id().equals(id))) return DataResult.success(Optional.empty());
        return DataResult.error(() -> "unknown form \"" + id + "\"");
    }

    /** Makes the collections immutable. */
    public Material {
        names = Map.copyOf(names);
        forms = List.copyOf(forms);
        external = List.copyOf(external);
        tools = Map.copyOf(tools);
        conditions = List.copyOf(conditions);
    }

    /** Builds a material from the parsed source, with the default forms where {@code forms} is missing. */
    private static Material fromSource(List<Form> all, String id, String color, Map<String, String> names, ToolTier tier,
                                       Optional<List<Optional<Form>>> forms, Optional<List<OreSpec>> ore, Optional<Alloy> alloy,
                                       List<External> external, boolean cook, Map<String, Tool> tools, List<ICondition> conditions) {
        List<Form> list = forms.map(l -> l.stream().flatMap(Optional::stream).toList()).orElseGet(() -> all.stream()
                .filter(f -> !external.isEmpty() ? f.tool().isEmpty() && f.external() == Form.ExternalRule.DEFAULT
                        : f.tool().isEmpty() && (alloy.isEmpty() || !f.mined()))
                .toList());
        List<Form> ordered = list.stream().distinct().sorted(Comparator.comparingInt(Form::order)).toList();
        return new Material(id, 0xFF000000 | Integer.parseInt(color.substring(1), 16), names, tier, ordered, ore, alloy, external, cook, tools, conditions);
    }

    /** Checks that the forms and settings fit together. */
    private DataResult<Material> validate(List<Form> all, List<Form> leftOut) {
        if (forms.isEmpty()) return DataResult.error(() -> id + ": \"forms\" must not be empty");
        for (Form form : forms) {
            for (String required : form.requires()) {
                if (!has(required)) {
                    return DataResult.error(() -> id + ": the \"" + form.id() + "\" form needs the \"" + required + "\" form");
                }
            }
        }
        if (ore.isPresent() && forms.stream().noneMatch(Form::hosted) && leftOut.stream().noneMatch(Form::hosted)) {
            return DataResult.error(() -> id + ": \"ore\" settings need a hosted form such as \"ore\"");
        }
        if (alloy.isPresent() && forms.stream().anyMatch(Form::mined)) {
            return DataResult.error(() -> id + ": an alloy is not mined, it has no mined forms such as \"raw\" and \"ore\"");
        }
        if (!external.isEmpty()) {
            if (alloy.isPresent() || ore.isPresent()) {
                return DataResult.error(() -> id + ": an \"external\" material has no \"alloy\" and \"ore\"");
            }
            List<String> excluded = all.stream().filter(f -> f.external() == Form.ExternalRule.EXCLUDED).map(Form::id).toList();
            if (forms.stream().anyMatch(f -> excluded.contains(f.id()))) {
                return DataResult.error(() -> id + ": an \"external\" material must not have the forms " + excluded);
            }
            Set<Optional<String>> mods = new HashSet<>();
            for (int n = 0; n < external.size(); n++) {
                External source = external.get(n);
                if (!mods.add(source.mod())) {
                    return DataResult.error(() -> id + ": two \"external\" sources have the same \"mod\": " + source.mod().orElse("(none)"));
                }
                if (source.mod().isEmpty() && n > 0) {
                    return DataResult.error(() -> id + ": the \"external\" source without a \"mod\" must be the first");
                }
                for (String key : source.forms().keySet()) {
                    if (all.stream().noneMatch(f -> f.id().equals(key)) && leftOut.stream().noneMatch(f -> f.id().equals(key))) {
                        return DataResult.error(() -> id + ": the \"external\" key \"" + key + "\" is not a form");
                    }
                }
            }
            for (Form to : all) {
                for (Form.Conversion c : to.conversions()) {
                    if (c.externalRequired() && has(c.from()) && !has(to) && externalItem(to.id()).isEmpty()) {
                        return DataResult.error(() -> id + ": the \"" + c.from() + "\" form of an \"external\" material needs an \""
                                + to.id() + "\" item");
                    }
                }
            }
        }
        for (Form form : forms) {
            if (form.tool().isPresent() && !tools.containsKey(form.id())) {
                return DataResult.error(() -> id + ": the tool form \"" + form.id() + "\" needs a \"tools\" entry");
            }
        }
        for (String key : tools.keySet()) {
            if (forms.stream().noneMatch(f -> f.id().equals(key) && f.tool().isPresent())) {
                return DataResult.error(() -> id + ": the \"tools\" key \"" + key + "\" is not a tool form of the material");
            }
        }
        return DataResult.success(this);
    }

    /**
     * The durability of the tool of a tool form: the {@code durability} of its {@link Tool entry} where it has one, else the
     * durability of vanilla tools of the {@link #tier() tier} times the factor of the form.
     *
     * @param form a tool form the material has
     * @return the durability, at least {@code 1}
     */
    public int toolDurability(Form form) {
        return tools.get(form.id()).durability()
                .orElseGet(() -> Math.max(1, Math.round(tier.durability() * form.tool().orElseThrow().durability())));
    }

    /**
     * What a material needs for one of its crafting tool forms.
     *
     * @param ingredient the item tag of the {@code #} of the recipe of the tool, e.g. {@code c:ingots/iron}
     * @param durability the durability of the tool, for a tier that has no fitting value of vanilla tools such as steel;
     *                   absent: that of the tier times the factor of the form
     */
    public record Tool(Identifier ingredient, Optional<Integer> durability) {
        /** Source format: {@code {"ingredient": "c:ingots/iron", "durability": 600}}, the durability is optional. */
        public static final Codec<Tool> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("ingredient").forGetter(Tool::ingredient),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("durability").forGetter(Tool::durability)
        ).apply(i, Tool::new));
    }

    /**
     * The conditions that the data of a form of this material carries: those of the material, then those of the form.
     *
     * @param form the form
     * @return the combined conditions without repeats, empty if neither has any
     */
    public List<ICondition> conditions(Form form) {
        return Conditions.merge(conditions, form.conditions());
    }

    /** @return {@code true} if a metal provided from outside, such as vanilla iron, supplies parts of the material */
    public boolean isExternal() {
        return !external.isEmpty();
    }

    /**
     * The first external source that names a result item for a form, the one the cook recipes make.
     *
     * @param formId the id of the form, e.g. {@code ingot}
     * @return the source, empty if none names an item
     */
    public Optional<External> externalItem(String formId) {
        return external.stream().filter(e -> e.item(formId).isPresent()).findFirst();
    }

    /**
     * Tells whether the material has a form.
     *
     * @param form the form
     * @return {@code true} if items or blocks of the form are registered
     */
    public boolean has(Form form) {
        return forms.contains(form);
    }

    /**
     * Tells whether the material has a form.
     *
     * @param formId the id of the form
     * @return {@code true} if items or blocks of the form are registered
     */
    public boolean has(String formId) {
        return forms.stream().anyMatch(f -> f.id().equals(formId));
    }

    /**
     * Registry path of an item or block of a form that is not hosted.
     *
     * @param form the form
     * @return e.g. {@code tin_dust_block}
     */
    public String path(Form form) {
        if (form.hosted()) throw new IllegalArgumentException("hosted forms have several variants, use orePath");
        return String.format(Locale.ROOT, form.pathPattern(), id);
    }

    /**
     * Registry path of an ore block, a block of the hosted form.
     *
     * @param host     the block the ore sits in
     * @param richness how much the ore yields
     * @return e.g. {@code tin_ore}, {@code deepslate_tin_ore}, {@code rich_nether_tin_ore}
     */
    public String orePath(OreHost host, OreRichness richness) {
        Form form = hostedForm().orElseThrow(() -> new IllegalStateException(id + " has no hosted form"));
        return richness.prefix() + host.prefix() + String.format(Locale.ROOT, form.pathPattern(), id);
    }

    /** @return the hosted form of the material, such as the ore, if it has one */
    public Optional<Form> hostedForm() {
        return forms.stream().filter(Form::hosted).findFirst();
    }

    /**
     * The ore blocks: the union over the {@link #oreSpecs() entries} of their hosts and richnesses, hosts in order of
     * their first appearance and for each host the richnesses in their declaration order.
     *
     * @return the ore variants, empty without a hosted form
     */
    public List<OreVariant> oreVariants() {
        List<OreVariant> variants = new ArrayList<>();
        if (hostedForm().isEmpty()) return variants;
        Set<OreHost> hosts = new LinkedHashSet<>();
        for (OreSpec spec : oreSpecs()) hosts.addAll(spec.hosts());
        for (OreHost host : hosts) {
            for (OreRichness richness : OreRichness.values()) {
                if (oreSpecs().stream().anyMatch(s -> s.hosts().contains(host) && s.richness().contains(richness))) {
                    variants.add(new OreVariant(host, richness));
                }
            }
        }
        return variants;
    }

    /**
     * The ore generation configs, the default entry where the source has no {@code ore}.
     *
     * @return the entries in source order, never empty
     */
    public List<OreSpec> oreSpecs() {
        return ore.orElse(List.of(OreSpec.DEFAULT));
    }

    /**
     * The display name of the material.
     *
     * @param locale the locale as in the language files, e.g. {@code en_us}
     * @return the name of the source
     * @throws IllegalStateException if the source has no name for the locale
     */
    public String displayName(String locale) {
        String name = names.get(locale);
        if (name == null) throw new IllegalStateException(id + ": no name for the locale " + locale);
        return name;
    }

    /**
     * One source of the parts of an external metal: the item tags that stand for its forms as ingredients and the
     * concrete items that recipes make of them. A source of another mod names that {@code mod}; without one the
     * source is always there (vanilla).
     *
     * @param mod   id of the mod that provides the source; the recipes that use it need the mod and its items need not
     *              be registered when the data is generated; empty for a source that is always present
     * @param forms the tag and optional item by form id, e.g. {@code ingot}; {@code nugget} is what the grains smelt to
     */
    public record External(Optional<String> mod, Map<String, Entry> forms) {
        /** A mod id: lower case letters, digits and {@code _}. */
        private static final Codec<String> MOD = Codec.STRING.validate(s -> s.matches("[a-z][a-z0-9_]{0,63}")
                ? DataResult.success(s) : DataResult.error(() -> "\"mod\" must be a mod id: " + s));

        /**
         * Source format: {@code {"mod": "othermod", "forms": {"ingot": {"tag": "c:ingots/iron", "item": "minecraft:iron_ingot"}}}},
         * both fields optional.
         */
        public static final Codec<External> CODEC = RecordCodecBuilder.create(i -> i.group(
                MOD.optionalFieldOf("mod").forGetter(External::mod),
                Codec.unboundedMap(Codec.STRING, Entry.CODEC).optionalFieldOf("forms", Map.of()).forGetter(External::forms)
        ).apply(i, External::new));

        /** Makes the map immutable. */
        public External {
            forms = Map.copyOf(forms);
        }

        /**
         * The tag and item of one form.
         *
         * @param tag  the item tag that stands for the form as an ingredient, e.g. {@code c:ingots/iron}
         * @param item the concrete item recipes make of the form, e.g. {@code minecraft:iron_ingot}; absent where the
         *             form is only used as an ingredient
         */
        public record Entry(Identifier tag, Optional<Identifier> item) {
            /** Source format: {@code {"tag": "c:ingots/iron", "item": "minecraft:iron_ingot"}}, the item is optional. */
            public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Identifier.CODEC.fieldOf("tag").forGetter(Entry::tag),
                    Identifier.CODEC.optionalFieldOf("item").forGetter(Entry::item)
            ).apply(i, Entry::new));
        }

        /**
         * The item tag of a form.
         *
         * @param formId the id of the form, e.g. {@code ingot}
         * @return the tag id, empty if the source has no entry for the form
         */
        public Optional<Identifier> tag(String formId) {
            return Optional.ofNullable(forms.get(formId)).map(Entry::tag);
        }

        /**
         * The result item of a form.
         *
         * @param formId the id of the form, e.g. {@code ingot}
         * @return the item id, empty if the source names none
         */
        public Optional<Identifier> item(String formId) {
            return Optional.ofNullable(forms.get(formId)).flatMap(Entry::item);
        }
    }

    /**
     * One ore block of a material.
     *
     * @param host     the block the ore sits in
     * @param richness how much the ore yields
     */
    public record OreVariant(OreHost host, OreRichness richness) {}

    /**
     * One world generation config of an ore, one element of the {@code ore} array of a material. Every entry gets its
     * own features and biome modifiers and its own config switch {@code materials.<material>.ores.<id>.enabled}.
     * Example, ore that only forms where it touches air:
     * <pre>{"id": "surface", "hosts": ["stone"], "richness": ["poor"], "minY": 40, "maxY": 120, "count": 6, "veinSize": 6, "exposure": "air"}</pre>
     *
     * @param id         unique name of the entry in the material, {@code [a-z][a-z0-9_]*} but not {@code enabled}; the
     *                   source may leave it out, the material then names it {@code ore_<index>}
     * @param hosts      the blocks the ore replaces, one ore block per host and richness is registered; default
     *                   {@link OreHost#DEFAULTS}
     * @param richness   the richnesses that are generated, default all; the blocks of the others are only registered if
     *                   another entry generates them
     * @param veinSize   the number of blocks of a vein, {@code 1..64}
     * @param count      veins per chunk, {@code 1..256}
     * @param minY       lowest height
     * @param maxY       highest height
     * @param biomeTag   biome tag the ore generates in, e.g. {@code c:is_overworld}; the dimension decides when absent
     * @param dimensions the dimensions the entry generates in; default the dimensions of its hosts ({@link OreHost#dimensionsOf}),
     *                   each must have a host that occurs in it
     * @param exposure   if not empty, a block of a vein is only placed if one of its 6 neighbours is of one of these kinds
     *                   ({@link OreExposure}); the source takes one value or a list
     */
    public record OreSpec(String id, List<OreHost> hosts, List<OreRichness> richness, int veinSize, int count, int minY,
                          int maxY, Optional<String> biomeTag, List<OreDimension> dimensions, List<OreExposure> exposure) {
        /** The name of an entry. */
        private static final Codec<String> ENTRY_ID = ID.validate(s -> s.equals("enabled")
                ? DataResult.error(() -> "an ore \"id\" must not be \"enabled\", it is the key of the switch of the ore form")
                : DataResult.success(s));

        /** Settings of a material without an {@code ore} entry: the only entry, named {@code ore_0}. */
        public static final OreSpec DEFAULT = new OreSpec("ore_0", OreHost.DEFAULTS, List.of(OreRichness.values()), 8, 12, -16, 112,
                Optional.empty(), OreHost.dimensionsOf(OreHost.DEFAULTS), List.of());

        /** Source format of one entry; all fields are optional, the id is {@code ""} where missing, see {@link #LIST_CODEC}. */
        public static final Codec<OreSpec> CODEC = RecordCodecBuilder.<OreSpec>create(i -> i.group(
                ENTRY_ID.optionalFieldOf("id", "").forGetter(OreSpec::id),
                OreHost.CODEC.listOf().optionalFieldOf("hosts", OreHost.DEFAULTS).forGetter(OreSpec::hosts),
                OreRichness.CODEC.listOf().optionalFieldOf("richness", DEFAULT.richness).forGetter(OreSpec::richness),
                Codec.intRange(1, 64).optionalFieldOf("veinSize", DEFAULT.veinSize).forGetter(OreSpec::veinSize),
                Codec.intRange(1, 256).optionalFieldOf("count", DEFAULT.count).forGetter(OreSpec::count),
                Codec.intRange(-64, 320).optionalFieldOf("minY", DEFAULT.minY).forGetter(OreSpec::minY),
                Codec.intRange(-64, 320).optionalFieldOf("maxY", DEFAULT.maxY).forGetter(OreSpec::maxY),
                Codec.STRING.optionalFieldOf("biomeTag").forGetter(OreSpec::biomeTag),
                OreDimension.CODEC.listOf().optionalFieldOf("dimensions").forGetter(o -> Optional.of(o.dimensions)),
                Codec.withAlternative(OreExposure.CODEC.listOf(), OreExposure.CODEC, List::of)
                        .optionalFieldOf("exposure", List.of()).forGetter(OreSpec::exposure)
        ).apply(i, (id, hosts, richness, veinSize, count, minY, maxY, biomeTag, dimensions, exposure) -> new OreSpec(id, hosts,
                richness, veinSize, count, minY, maxY, biomeTag, dimensions.orElseGet(() -> OreHost.dimensionsOf(hosts)), exposure)))
                .validate(OreSpec::validate);

        /**
         * Source format of the {@code ore} array: at least one entry; entries without an {@code id} are named
         * {@code ore_<index>} by their position, and the ids and the combinations of the entries are checked.
         */
        public static final Codec<List<OreSpec>> LIST_CODEC = CODEC.listOf().xmap(OreSpec::named, l -> l)
                .validate(OreSpec::validate);

        /** Makes the lists immutable. */
        public OreSpec {
            hosts = List.copyOf(hosts);
            richness = List.copyOf(richness);
            dimensions = List.copyOf(dimensions);
            exposure = List.copyOf(exposure);
        }

        /** Gives the entries without an id the name {@code ore_<index>}. */
        private static List<OreSpec> named(List<OreSpec> entries) {
            List<OreSpec> result = new ArrayList<>();
            for (int n = 0; n < entries.size(); n++) {
                OreSpec e = entries.get(n);
                result.add(e.id.isEmpty() ? new OreSpec("ore_" + n, e.hosts, e.richness, e.veinSize, e.count, e.minY, e.maxY,
                        e.biomeTag, e.dimensions, e.exposure) : e);
            }
            return result;
        }

        /** Checks one entry. */
        private DataResult<OreSpec> validate() {
            if (hosts.isEmpty()) return DataResult.error(() -> "\"hosts\" must not be empty");
            if (hosts.size() != Set.copyOf(hosts).size()) return DataResult.error(() -> "\"hosts\" must not repeat");
            if (richness.isEmpty()) return DataResult.error(() -> "\"richness\" must not be empty");
            if (richness.size() != Set.copyOf(richness).size()) return DataResult.error(() -> "\"richness\" must not repeat");
            if (minY > maxY) return DataResult.error(() -> "\"minY\" must not be above \"maxY\"");
            if (dimensions.isEmpty()) return DataResult.error(() -> "\"dimensions\" must not be empty");
            if (dimensions.size() != Set.copyOf(dimensions).size()) return DataResult.error(() -> "\"dimensions\" must not repeat");
            if (exposure.size() != Set.copyOf(exposure).size()) return DataResult.error(() -> "\"exposure\" must not repeat");
            for (OreDimension dimension : dimensions) {
                if (hosts(dimension).isEmpty()) {
                    return DataResult.error(() -> "no host occurs in the dimension \"" + dimension.id() + "\": " + hosts);
                }
            }
            return DataResult.success(this);
        }

        /** Checks the entries of a material together: not empty, unique ids, no duplicate generation. */
        private static DataResult<List<OreSpec>> validate(List<OreSpec> entries) {
            if (entries.isEmpty()) return DataResult.error(() -> "\"ore\" must have at least one entry");
            Set<String> ids = new HashSet<>();
            for (OreSpec e : entries) {
                if (!ids.add(e.id)) return DataResult.error(() -> "the ore id \"" + e.id + "\" is used twice");
            }
            for (int a = 0; a < entries.size(); a++) {
                for (int b = a + 1; b < entries.size(); b++) {
                    OreSpec x = entries.get(a);
                    OreSpec y = entries.get(b);
                    if (x.minY == y.minY && x.maxY == y.maxY && x.biomeTag.equals(y.biomeTag)
                            && Set.copyOf(x.exposure).equals(Set.copyOf(y.exposure)) && x.shares(y)) {
                        return DataResult.error(() -> "the ore entries \"" + x.id + "\" and \"" + y.id
                                + "\" generate the same host, richness and dimension at the same height");
                    }
                }
            }
            return DataResult.success(entries);
        }

        /** Whether both entries generate some host in some richness in some dimension. */
        private boolean shares(OreSpec other) {
            for (OreDimension dimension : dimensions) {
                if (!other.dimensions.contains(dimension)) continue;
                for (OreHost host : hosts(dimension)) {
                    if (other.hosts(dimension).contains(host) && richness.stream().anyMatch(other.richness::contains)) return true;
                }
            }
            return false;
        }

        /**
         * The hosts that are generated in a dimension: the hosts of the entry that occur in it.
         *
         * @param dimension the dimension
         * @return the hosts in source order, empty if the entry does not generate in the dimension
         */
        public List<OreHost> hosts(OreDimension dimension) {
            if (!dimensions.contains(dimension)) return List.of();
            return hosts.stream().filter(h -> h.dimensions().contains(dimension)).toList();
        }
    }

    /**
     * The recipe of an alloy: materials mixed in the given amounts, in every form the alloy has.
     *
     * @param inputs what the alloy is made of
     */
    public record Alloy(List<Input> inputs) {
        /** Source format: {@code {"inputs": [{"material": "copper", "count": 3}]}}. */
        public static final Codec<Alloy> CODEC = RecordCodecBuilder.<Alloy>create(i -> i.group(
                Input.CODEC.listOf().fieldOf("inputs").forGetter(Alloy::inputs)
        ).apply(i, Alloy::new)).validate(a -> a.inputs.size() >= 2 ? DataResult.success(a)
                : DataResult.error(() -> "an alloy needs at least 2 \"inputs\""));

        /**
         * The materials that are an alternative of some input, in source order; each is switched by
         * {@code materials.<alloy>.recipes.<material>.enabled}.
         *
         * @return the ids of the alternatives, empty for an alloy without alternatives
         */
        public Set<String> alternatives() {
            Set<String> ids = new LinkedHashSet<>();
            for (Input in : inputs) {
                if (in.hasAlternatives()) ids.addAll(in.materials());
            }
            return ids;
        }

        /**
         * One input of an alloy: a material, or alternatives of which one is used.
         *
         * @param materials ids of the materials of this mod, one for a plain input; with several, each alternative gets
         *                  its own recipes, its own config switch {@code materials.<alloy>.recipes.<material>.enabled}
         *                  and a recipe id suffix
         * @param count     how many of it, {@code 1..9}
         */
        public record Input(List<String> materials, int count) {
            /**
             * Source format: {@code {"material": "copper", "count": 3}} or with alternatives
             * {@code {"material": ["coal", "charcoal"]}}; the count defaults to 1.
             */
            public static final Codec<Input> CODEC = RecordCodecBuilder.<Input>create(i -> i.group(
                    Codec.withAlternative(ID.listOf(), ID, List::of).fieldOf("material").forGetter(Input::materials),
                    Codec.intRange(1, 9).optionalFieldOf("count", 1).forGetter(Input::count)
            ).apply(i, Input::new)).validate(in -> in.materials.isEmpty() ? DataResult.error(() -> "\"material\" must not be empty")
                    : in.materials.size() != Set.copyOf(in.materials).size() ? DataResult.error(() -> "\"material\" must not repeat")
                    : DataResult.success(in));

            /** Makes the list immutable. */
            public Input {
                materials = List.copyOf(materials);
            }

            /**
             * A plain input.
             *
             * @param material the id of the material
             * @param count    how many of it
             */
            public Input(String material, int count) {
                this(List.of(material), count);
            }

            /** @return {@code true} if the input lists alternatives */
            public boolean hasAlternatives() {
                return materials.size() > 1;
            }
        }
    }
}
