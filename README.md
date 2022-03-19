# Disguise Lib

A server-side library that allows disguising entities as other ones.
~~Features built-in `/disguise` command as well.~~
[`/disguise` command with some other features has been moved to a separate mod.](https://github.com/samolego/MobDisguises)

## Dependecy
```gradle
repositories {
	maven {
		url 'https://maven.nucleoid.xyz'
	}
	// OR
	maven {
        url 'https://jitpack.io'
    }
}

dependencies {
    // Common module
    modImplementation "xyz.nucleoid:DisguiseLib:${project.disguiselib_version}"
  
    // Fabric
    modImplementation "xyz.nucleoid:disguiselib-fabric:${project.disguiselib_version}"
    
    // Forge
    implementation fg.deobf "com.github.NucleoidMC.DisguiseLib:disguiselib-forge:${project.disguiselib_version}"
    
}
```
# API

Use the provided interface `EntityDisguise` on any class extending `net.minecraft.entity.Entity`.

```java
import xyz.nucleoid.disguiselib.api.EntityDisguise;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

public class MyDisguises {
    public static void disguise() {
        // Make sure you are executing disguise on the server side
        if (world.isClient)
            return;

        // Disguises as creeper
        ((EntityDisguise) entityToDisguise).disguiseAs(EntityType.CREEPER);

        // Disguise as aCustomEntity (net.minecraft.entity)
        ((EntityDisguise) entityToDisguise).disguiseAs(aCustomEntity);

        // If you disguise it as EntityType.PLAYER, you can apply custom GameProfile as well
        ((EntityDisguise) entityToDisguise).setGameProfile(aCustomGameProfile);

        ((EntityDisguise) entityToDisguise).isDisguised(); // Tells whether entity is disguised or not
        ((EntityDisguise) entityToDisguise).removeDisguise(); // Clears the disguise


        // Not that useful (mainly for internal use)
        ((EntityDisguise) entityToDisguise).getDisguiseType(); // Gets the EntityType of the disguise
        ((EntityDisguise) entityToDisguise).disguiseAlive(); // Whether the entity from the disguise is an instance of LivingEntity
    }
}

```

