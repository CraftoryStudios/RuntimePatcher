package studio.craftory.runtimepatcher.plugin;

import studio.craftory.runtimepatcher.RuntimePatcher;
import studio.craftory.runtimepatcher.plugin.patches.EntityLivingPatch;
import studio.craftory.runtimepatcher.plugin.patches.SkullMetaPatch;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;


public class ExampleTransformationPlugin extends JavaPlugin {

    @Override
    public void onEnable() {

        new RuntimePatcher(
            EntityLivingPatch.class,
                SkullMetaPatch.class
        );

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ItemStack stack = ((Player) sender).getInventory().getItemInMainHand();
        SkullMeta itemMeta = (SkullMeta) stack.getItemMeta();
        itemMeta.setOwner("BrettSaunders");
        stack.setItemMeta(itemMeta);
        ((Player) sender).getInventory().setItemInMainHand(stack);
        return true;
    }
}
