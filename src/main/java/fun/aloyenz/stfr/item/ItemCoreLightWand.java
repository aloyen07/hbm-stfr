package fun.aloyenz.stfr.item;

import java.util.List;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.debug.StfrFakeCores;

/** Creative visual probe; block contents and actual reactors are never touched. */
public final class ItemCoreLightWand extends Item {
    public ItemCoreLightWand() {
        setRegistryName(Tags.MODID,"core_light_wand");setTranslationKey(Tags.MODID+".core_light_wand");
        setMaxStackSize(1);setCreativeTab(STFRBlocks.TAB);
    }
    private EnumActionResult use(EntityPlayer player,World world,BlockPos pos) {
        if(!player.capabilities.isCreativeMode)return EnumActionResult.FAIL;
        if(world.isRemote)return EnumActionResult.SUCCESS;
        if(player.isSneaking()) {
            int count=StfrFakeCores.clear(player.getServer());
            player.sendStatusMessage(new TextComponentTranslation("message.hbmstfr.light_wand.cleared",count),true);
        } else if(pos!=null) {
            StfrFakeCores.add((EntityPlayerMP)player,pos);
            player.sendStatusMessage(new TextComponentTranslation("message.hbmstfr.light_wand.placed"),true);
        }
        return EnumActionResult.SUCCESS;
    }
    @Override public EnumActionResult onItemUseFirst(EntityPlayer player,World world,BlockPos pos,EnumFacing face,float x,float y,float z,EnumHand hand) {
        return hand==EnumHand.MAIN_HAND ? use(player,world,pos) : EnumActionResult.PASS;
    }
    @Override public ActionResult<ItemStack> onItemRightClick(World world,EntityPlayer player,EnumHand hand) {
        return new ActionResult<>(player.isSneaking()?use(player,world,null):EnumActionResult.PASS,player.getHeldItem(hand));
    }
    @Override public boolean canDestroyBlockInCreative(World world,BlockPos pos,ItemStack stack,EntityPlayer player) { return false; }
    @SideOnly(Side.CLIENT)
    @Override public void addInformation(ItemStack stack,World world,List<String> lines,ITooltipFlag flag) {
        lines.add(I18n.format("tooltip.hbmstfr.light_wand"));
    }
}
