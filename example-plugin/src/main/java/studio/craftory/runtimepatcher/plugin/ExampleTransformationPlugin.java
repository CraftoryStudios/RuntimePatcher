package studio.craftory.runtimePatcher.plugin;

import studio.craftory.runtimePatcher.RuntimePatcher;
import studio.craftory.runtimePatcher.plugin.transformer.EntityLivingTransformer;
import studio.craftory.runtimePatcher.plugin.transformer.SkullMetaTransformer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by Yamakaja on 19.05.17.
 */
public class ExampleTransformationPlugin extends JavaPlugin {

    @Override
    public void onEnable() {

        new RuntimePatcher(
                EntityLivingTransformer.class,
                SkullMetaTransformer.class
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
