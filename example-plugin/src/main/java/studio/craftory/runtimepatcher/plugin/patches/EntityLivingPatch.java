package studio.craftory.runtimepatcher.plugin.patches;

import studio.craftory.runtimepatcher.annotation.Inject;
import studio.craftory.runtimepatcher.annotation.InjectionType;

import net.minecraft.server.v1_16_R3.DamageSource;
import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.World;

import org.bukkit.Bukkit;
import studio.craftory.runtimepatcher.annotation.Patch;


@Patch(EntityLiving.class)
public abstract class EntityLivingPatch extends EntityLiving {

    protected EntityLivingPatch(EntityTypes<? extends EntityLiving> entitytypes, World world) {
        super(entitytypes, world);
    }

    @Inject(InjectionType.INSERT)
    public void _init_(EntityTypes<? extends EntityLiving> entitytypes, World world) {
        Bukkit.broadcastMessage("Entity created!");
        throw null;
    }

    // Injecting into a private method
    @Inject(InjectionType.INSERT)
    private boolean f(DamageSource src) {
        Bukkit.broadcastMessage("Checked " + this.getName() + " for totem @ " + (int) this.locX() + ", " + (int) this.locY() + ", " + (int) this.locZ() + "!");
        throw null; // Let original method continue execution
    }

    @Inject(InjectionType.INSERT)
    public void setHealth(float f) {
        Bukkit.broadcastMessage(this.getName() + ": " + this.getHealth() + " -> " + f);
        // Accessing the health with super.getHealth() would compile, but inserting into EntityLiving would fail,
        // because the superclass of EntityLiving, Entity, doesn't have a getHealth method
    }

    @Inject(InjectionType.APPEND)
    public void setHealth_INJECTED(float f) {
        Bukkit.broadcastMessage("Finished setting health");
    }

    // Overriding a final method
    @Inject(InjectionType.INSERT)
    public final float getMaxHealth_INJECTED() {
        throw null; // Let original method continue execution
    }

}
