# Automatic Tool Swap
Swaps the tools to the effective one if it's in hotbar.

[![Curseforge](http://cf.way2muchnoise.eu/versions/For%20MC_361977_all.svg)](https://www.curseforge.com/minecraft/mc-mods/automatic-tool-swap)
[![CurseForge](http://cf.way2muchnoise.eu/full_361977_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/automatic-tool-swap)

[![Modrinth](https://img.shields.io/modrinth/game-versions/L9JLNLqk?color=00AF5C&label=modrinth&logo=modrinth)](https://modrinth.com/mod/automatic-tool-swap)
[![Modrinth](https://img.shields.io/modrinth/dt/L9JLNLqk?color=00AF5C&logo=modrinth)](https://modrinth.com/mod/automatic-tool-swap)

For mod devs who want to add compatibility with Automatic Tool Swap, you simply need to implement
`de.melanx.toolswap.DiggerLike` into your tool class if it's not a child of `DiggerItem`. Also add the one method,
alternatively the other methods. This way, your tool will be considered when swapping. Just add this to your
`build.gradle`:

```groovy
repositories {
    maven {
        url = "https://modmaven.dev/"
    }
}

dependencies {
    compileOnly fg.deobf("de.melanx:ToolSwap:1.20.1-5.0.3+")
}
```
