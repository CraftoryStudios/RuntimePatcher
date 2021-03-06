# RuntimePatcher

A tool allowing for easy class modification at runtime, when using a normal javaagent at startup would be too inconvenient.
Note, this method comes with disadvantages, for example method modifiers may not be altered, new methods can not be created and neither can class inheritance be changed.

## Credit
This project is based of [RuntimeTransformer](https://github.com/Yamakaja/RuntimeTransformer) made by [Yamakaja](https://github.com/Yamakaja), [games647](https://github.com/games647) and 
[MiniDigger](https://github.com/MiniDigger). All credit for the original idea and code goes to them, I am just responsible for updating and adding my own additions to this project.

## Usage

Let's assume we want to inject an event handler into the `setHealth` method of `EntityLiving`,
therefore the method should something like this after transformation:

```java
public void setHealth(float newHealth) {
    ImaginaryEvent event = ImaginaryEventBus.callEvent(new ImaginaryEvent(this, newHealth));
    
    if (event.isCancelled())
        return;
        
    newHealth = event.getNewHealth();
    
    // Minecraft Code
}
```
 
To get there, we first need to define a patcher, this should optimally be in its own class and look something like this:

```java
@Patcher(EntityLiving.class) // The class we want to transform
public class EntityLivingPatcher extends EntityLiving { // Extending EntityLiving in our patcher makes things easier, but isn't required (Which, for example, allows you to patch final classes)
    
    @Inject(InjectionType.INSERT) // Our goal is to insert code at the beginning of the method, and leave everything else intact
    public void setHealth(float newHealth) { // Then just "override" the method as usual, if it is final add an _INJECTED to the method name
        ImaginaryEvent event = ImaginaryEventBus.callEvent(new ImaginaryEvent(this, newHealth)); // Our event handling code from above
            
        if (event.isCancelled())
            return;
            
        newHealth = event.getNewHealth();
        
        throw null; // Pass execution on to the rest of the method. This will be removed at runtime but is required for compilation (At least when the method doesn't return void, so it's not necessary in this case)
        
    }
    
} 
```

And that's pretty much it, now we just need to create our runtime patcher:

```java
new RuntimePatcher( EntityLivingPatcher.class );
```

For Java 9+ runtimes, self attaching an agent was disabled. To work around this, the library will run the Agent in a separate process. This means that you shouldn't have to do anything extra to 
get it to work!

And we're done.

You can find more examples in the example plugin.

## "Documentation"

There are three types of Injection:

- INSERT (Inserts your code at the beginning of the method)
- OVERWRITE (Overwrites the method with your code)
- APPEND (Adds code to the end of the method, only works on methods returning void)

## Compiling

Run this command to build the api project.
`./gradlew jar`

## Installation - Local Maven

To install the api jar into your local maven repo run
`./gradlew publishToMavenLocal`

The correct artifact can then be included using the following dependency definition:

```xml

<dependency>
  <groupId>studio.craftory.runtimepatcher</groupId>
  <artifactId>api</artifactId>
  <version>1.0.0</version>
</dependency>
```

Don't forget to actually include the artifact in your final jar, using the `maven-shade-plugin` or an equivalent alternative!

## Installation

The best way to use this package is by using the Github packages that are generated every release