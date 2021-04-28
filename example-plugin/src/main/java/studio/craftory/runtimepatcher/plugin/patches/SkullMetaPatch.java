package studio.craftory.runtimepatcher.plugin.patches;

import com.mojang.authlib.GameProfile;

import studio.craftory.runtimepatcher.annotation.CallParameters;
import studio.craftory.runtimepatcher.annotation.Inject;
import studio.craftory.runtimepatcher.annotation.InjectionType;

import net.minecraft.server.v1_16_R3.GameProfileSerializer;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.TileEntitySkull;
import studio.craftory.runtimepatcher.annotation.PatchByName;

@PatchByName("org.bukkit.craftbukkit.v1_15_R1.inventory.CraftMetaSkull")
public class SkullMetaPatch {

    private GameProfile profile;

    @CallParameters(
            type = CallParameters.Type.SPECIAL,
            owner = "org/bukkit/craftbukkit/v1_16_R3/inventory/CraftMetaItem",
            name = "applyToItem",
            desc = "(Lnet/minecraft/server/v1_16_R3/NBTTagCompound;)V"
    )
    private native void super_applyToItem(NBTTagCompound tag);

    @Inject(InjectionType.OVERRIDE)
    void applyToItem(final NBTTagCompound tag) {
        super_applyToItem(tag);
        if (this.profile != null) {
            NBTTagCompound owner = new NBTTagCompound();
            GameProfileSerializer.serialize(owner, this.profile);
            tag.set("SkullOwner", owner);
            System.out.println("Set owner to " + owner);
            TileEntitySkull.b(this.profile, gameProfile -> {
                NBTTagCompound newOwner = new NBTTagCompound();
                GameProfileSerializer.serialize(newOwner, gameProfile);
                tag.set("SkullOwner", newOwner);
                System.out.println("Received game profile!");
                return false;
            }, true);
        }

    }

}
