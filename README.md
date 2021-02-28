# Disguise Lib

A server-side library that allows disguising entities as other ones.
Features built-in `/disguise` command as well.

## Dependecy
```gradle
repositories {
	maven {
		url 'https://maven.nucleoid.xyz'
	}
}

dependencies {
  modImplementation "xyz.nucleoid:DisguiseLib:${project.disguiselib_version}"
}
```
# API

Use the provided interface `EntityDisguise` on any class extending `net.minecraft.entity.Entity`.

```java
// Disguises as creeper
((EntityDisguise) entity).disguiseAs(EntityType.CREEPER);

// Disguise as aCustomEntity (net.minecraft.entity)
((EntityDisguise) entity).disguiseAs(aCustomEntity);


        
((EntityDisguise) entity).isDisguised(); // Tells whether entity is disguised or not
((EntityDisguise) entity).removeDisguise(); // Clears the disguise

// Not that useful (mainly for internal use)
((EntityDisguise) entity).getDisguiseType(); // Gets the EntityType of the disguise
((EntityDisguise) entity).disguiseAlive(); // Whether the entity from the disguise is an instance of LivingEntity
```

