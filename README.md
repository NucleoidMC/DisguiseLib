# Disguise Lib

A server-side library that allows disguising entities as other ones.
Features built-in `/disguise` command as well.

## Dependecy
```gradle
repositories {
	maven {
		url 'https://jitpack.io'
	}
}

dependencies {
  // By version tag
  modImplementation 'com.github.samolego:DisguiseLib:VERSION_TAG'
  
  // Or by branch
  modImplementation 'com.github.samolego:DisguiseLib:master-SNAPSHOT'
}
```
# API

Use the provided interface `EntityDisguise` on any class extending `net.minecraft.entity.Entity`.

```java
/* Disguising */
// Disguises as creeper, true is if the Creeper class extends LivingEntity
((EntityDisguise) entity).disguiseAs(EntityType.CREEPER, true);

// Disguising using Identifier
((EntityDisguise) entity).disguiseAs(new Identifier("minecraft", "player")); // Disguising entity as player
        
((EntityDisguise) entity).isDisguised(); // Tells whether entity is disguised or not
((EntityDisguise) entity).removeDisguise(); // Removes disguise
```

