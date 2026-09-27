# RosaCore

RosaCore is a modular Java 8 library embedded and relocated into Minecraft plugins. It is not a standalone server plugin.

## Compatibility policy

- Target range: Minecraft 1.8 through 26.3.
- Shared modules emit Java 8 bytecode.
- Bukkit API is the lowest common platform boundary.
- Newer Paper and Folia features are accessed through optional capability adapters.
- Version-specific or reflective code belongs in the compatibility module, never in the public API.
- A feature may report itself unavailable instead of failing the whole plugin on an unsupported server.

## Modules

- `rosacore-compat`: server-family detection and compatibility capabilities.
- `rosacore-platform-bukkit`: plugin lifecycle, scheduling, YAML configuration, localization and commands.
- `rosacore-nms`: nms for all miencraft server version starting from 1.8

Consumer plugins will select the modules they need and relocate `pl.kiosel.rosacore`
into their own internal package during shading. This prevents different plugins from sharing global state or conflicting over embedded RosaCore
versions.

The platform module exposes a scheduler facade with separate global, asynchronous, location-owned and entity-owned operations. On Bukkit, Spigot and
regular Paper these operations use the classic scheduler. On Folia they are routed to the official Global, Async, Region and Entity schedulers without
linking modern Folia classes on legacy servers.

## Embedding

NMS implementations cover every revision detected by `Version`, from `v1_8_R1` through `v26_3_R1`. Core embeds these modules and `NmsResolver` loads
the implementation for the running server. See [the NMS revision and build matrix](NMS/README.md) for the server dependencies used to build each
module.

Consumer plugins depend on `rosacore-platform-bukkit`; Maven resolves the API and compatibility modules transitively. Their shade configuration should
include
`pl.kiosel.rosacore:*` and relocate the `pl.kiosel.rosacore` package into the consumer's internal package.

Because RosaCore embeds Adventure service providers, the consuming plugin's shade configuration must also transform service descriptors after applying
its final relocation:

```xml

<relocations>
    <relocation>
        <pattern>pl.kiosel.rosacore</pattern>
        <shadedPattern>your.plugin.internal.rosacore</shadedPattern>
    </relocation>
</relocations>
<transformers>
<transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
</transformers>
<createDependencyReducedPom>false</createDependencyReducedPom>
```

RosaCore does not shade optional server plugins or heavy JDBC drivers. The small database pool is embedded in Core, while the selected driver is
verified and cached at runtime under `plugins/Rosa/libraries`.

## Clean implementation

RosaCore is implemented from scratch. Other local libraries and plugins are used only to identify required capabilities and migration needs; their
source code is not copied.

## License

Copyright (C) 2026 Kiosel.

This project is licensed under the [GNU General Public License v3.0 only](LICENSE).
