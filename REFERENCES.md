# API and game references

The local `references/` folder holds game/dependency reference material without bundling it into the mod.

For the five older ports, open [references/additional-versions/README.md](references/additional-versions/README.md). Each of **1.20.1, 1.21.1, 1.21.5, 1.21.8 and 1.21.10** has its own official game metadata, Fabric Loader metadata, publisher dependency listings, API source JARs and observed novice trade data.

Authoritative sources:

- [Mojang version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json)
- [Fabric version metadata](https://meta.fabricmc.net/v2/versions)
- [Fabric API sources](https://github.com/FabricMC/fabric) and [Maven artifacts](https://maven.fabricmc.net/)
- [YACL publisher](https://modrinth.com/mod/yacl) and [source](https://github.com/isXander/YetAnotherConfigLib)
- [Mod Menu publisher](https://modrinth.com/mod/modmenu) and [source](https://github.com/TerraformersMC/ModMenu)

Pinned dependencies are in `versions.json`. Loom resolves the matching Minecraft classes/mappings; run `:mc<VERSION>:genSources` for navigable sources in IntelliJ. The version-specific adaptations are in `gradle/older-sources.gradle` and `gradle/compatibility/`.

The older novice catalogs were checked against each running game's trade factories. The raw probe records price variants separately. The picker combines equivalent item/payment/quantity rows and defaults to the highest observed vanilla price for variable-price equipment, so its initial price limit accepts those offers. Users can lower that limit. Custom server trades can differ.

Local reference downloads remain outside release JARs and new Git additions. `REFERENCES.md` stays with the source project as the reproducible index.
