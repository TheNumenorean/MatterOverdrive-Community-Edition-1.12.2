
package matteroverdrive.items;

import matteroverdrive.api.IScannable;
import matteroverdrive.api.events.MOEventScan;
import matteroverdrive.api.inventory.IBlockScanner;
import matteroverdrive.api.matter.IMatterDatabase;
import matteroverdrive.client.sound.MachineSound;
import matteroverdrive.data.matter_network.ItemPattern;
import matteroverdrive.handler.SoundHandler;
import matteroverdrive.init.MatterOverdriveSounds;
import matteroverdrive.items.includes.MOBaseItem;
import matteroverdrive.util.MOPhysicsHelper;
import matteroverdrive.util.MOStringHelper;
import matteroverdrive.util.MatterDatabaseHelper;
import matteroverdrive.util.MatterHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.util.List;

public class MatterScanner extends MOBaseItem implements IBlockScanner {
    public static final String SELECTED_TAG_NAME = "lastSelected";
    public static final String PAGE_TAG_NAME = "page";
    public static final String PANEL_OPEN_TAG_NAME = "panelOpen";
    public static final int PROGRESS_PER_ITEM = 10;
    public static final int SCAN_TIME = 60;
    @SideOnly(Side.CLIENT)
    public static MachineSound scanningSound;

    public MatterScanner(String name) {
        super(name);
    }

    public static IMatterDatabase getLink(World world, ItemStack scanner) {
        if (!scanner.isEmpty() && scanner.getItem() instanceof MatterScanner) {
            if (scanner.hasTagCompound()) {
                if (scanner.getTagCompound().getBoolean("isLinked")) {
                    BlockPos pos = BlockPos.fromLong(scanner.getTagCompound().getLong("link"));
                    TileEntity e = world.getTileEntity(pos);
                    if (e instanceof IMatterDatabase) {
                        return (IMatterDatabase) e;
                    }
                }
            }
        }
        return null;
    }

    public static BlockPos getLinkPosition(ItemStack scanner) {
        if (!scanner.isEmpty() && scanner.getItem() instanceof MatterScanner) {
            if (scanner.hasTagCompound()) {
                return new BlockPos(scanner.getTagCompound().getInteger("link_x"), scanner.getTagCompound().getInteger("link_y"), scanner.getTagCompound().getInteger("link_z"));
            }
        }
        return new BlockPos(0, 0, 0);
    }

    public static boolean isLinked(ItemStack scanner) {
        if (!scanner.isEmpty() && scanner.getItem() instanceof MatterScanner) {
            if (scanner.hasTagCompound()) {
                return scanner.getTagCompound().getBoolean("isLinked");
            }
        }
        return false;
    }

    public static void unLink(World world, ItemStack scanner) {
        if (scanner.hasTagCompound()) {
            scanner.getTagCompound().setBoolean("isLinked", false);
        }
    }

    public static void link(World world, BlockPos pos, ItemStack scanner) {
        if (scanner.getItem() instanceof MatterScanner) {
            ((MatterScanner) scanner.getItem()).TagCompountCheck(scanner);
        }

        if (scanner.hasTagCompound()) {
            scanner.getTagCompound().setBoolean("isLinked", true);
            scanner.getTagCompound().setLong("link", pos.toLong());
        }
    }

    public static void setSelected(World world, ItemStack scanner, ItemStack itemStack) {
        if (scanner.hasTagCompound()) {
            ItemPattern itemPattern = getSelectedFromDatabase(world, scanner, itemStack);
            if (itemPattern == null) {
                itemPattern = new ItemPattern(itemStack);
            }

            setSelected(scanner, itemPattern);
        }
    }

    public static void setSelected(ItemStack scanner, ItemPattern itemPattern) {
        if (scanner.hasTagCompound()) {
            NBTTagCompound seletedNBT = new NBTTagCompound();
            itemPattern.writeToNBT(seletedNBT);
            scanner.getTagCompound().setTag(SELECTED_TAG_NAME, seletedNBT);
        }
    }

    public static ItemPattern getSelectedAsPattern(ItemStack scanner) {
        if (scanner.hasTagCompound() && scanner.getTagCompound().hasKey(SELECTED_TAG_NAME)) {
            return new ItemPattern(scanner.getTagCompound().getCompoundTag(SELECTED_TAG_NAME));
        }
        return null;
    }

    public static ItemStack getSelectedAsItem(ItemStack scanner) {
        if (scanner.hasTagCompound()) {
            return new ItemPattern(scanner.getTagCompound().getCompoundTag(SELECTED_TAG_NAME)).toItemStack(false);
        }
        return null;
    }

    public static ItemPattern getSelectedFromDatabase(World world, ItemStack scanner, ItemStack forItem) {
        IMatterDatabase database = getLink(world, scanner);
        if (database != null) {
            return database.getPattern(forItem);
        }
        return null;
    }

    public static int getLastPage(ItemStack scanner) {
        if (scanner.hasTagCompound()) {
            return scanner.getTagCompound().getShort(PAGE_TAG_NAME);
        }
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack itemstack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (hasDetails(itemstack)) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                addDetails(itemstack, Minecraft.getMinecraft().player, worldIn, tooltip);
            } else {
                tooltip.add(MOStringHelper.MORE_INFO);
            }
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public void addDetails(ItemStack itemstack, EntityPlayer player, @Nullable World worldIn, List<String> infos) {
        if (isLinked(itemstack)) {
            infos.add(TextFormatting.GREEN + "Online");

            ItemPattern lastSelected = getSelectedAsPattern(itemstack);
            if (lastSelected != null) {
                infos.add("Progress: " + lastSelected.getProgress() + " / " + 100 + " %");
                infos.add("Selected: " + lastSelected.getDisplayName());
            }
        } else {
            infos.add(TextFormatting.RED + "Offline");
        }
    }

    @Override
    public boolean hasDetails(ItemStack itemStack) {
        return true;
    }

    public int getItemStackLimit(ItemStack item) {
        return 1;
    }

    private void resetScanProgress(ItemStack item) {
        if (item.hasTagCompound()) {
            item.getTagCompound().setInteger(MatterDatabaseHelper.PROGRESS_TAG_NAME, 0);
        }
    }

    private boolean HarvestBlock(ItemStack scanner, EntityPlayer player, World world, BlockPos pos) {
        if (!world.isRemote) {
            return world.setBlockToAir(pos);
        }

        return false;
    }

    @Override
    public void InitTagCompount(ItemStack stack) {
        MatterDatabaseHelper.initTagCompound(stack);
    }

    /**
     * returns the action that specifies what animation to play when the items is being used
     */
    @Override
    public EnumAction getItemUseAction(ItemStack p_77661_1_) {
        return EnumAction.NONE;
    }

    /**
     * How long it takes to use or consume an item
     */
    @Override
    public int getMaxItemUseDuration(ItemStack scanner) {
        ItemStack selected = getSelectedAsItem(scanner);
        if (selected != null) {
            if (MatterHelper.CanScan(selected)) {
                return SCAN_TIME + MatterHelper.getMatterAmountFromItem(selected);
            }
        }

        return Integer.MAX_VALUE;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand) {
        ItemStack itemStackIn = playerIn.getHeldItem(hand);
        RayTraceResult hit = getScanningPos(itemStackIn, playerIn);
        if (hit != null && hit.typeOfHit != RayTraceResult.Type.MISS) {
            if (worldIn.isRemote) {
                playSound(playerIn.posX, playerIn.posY, playerIn.posZ);
            }

            playerIn.setActiveHand(hand);
            return ActionResult.newResult(EnumActionResult.SUCCESS, itemStackIn);
        }
        return ActionResult.newResult(EnumActionResult.PASS, itemStackIn);
    }

    @Override
    public void onUpdate(ItemStack itemStack, World world, Entity entity, int p_77663_4_, boolean p_77663_5_) {
        super.onUpdate(itemStack, world, entity, p_77663_4_, p_77663_5_);

        if (world.isRemote) {
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (player.isHandActive()) {

                } else {
                    stopScanSounds();
                }
            }
        }
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityLivingBase entityLiving) {
        if (!(entityLiving instanceof EntityPlayer)) {
            return stack;
        }
        if (worldIn.isRemote) {
            stopScanSounds();
            return stack;
        }

        RayTraceResult position = getScanningPos(stack, entityLiving);
        if (position != null && position.typeOfHit == RayTraceResult.Type.BLOCK) {
            if (!worldIn.isRemote) {
                ItemStack worldItem = MatterDatabaseHelper.GetItemStackFromWorld(worldIn, position.getBlockPos());

                //finished scanning
                if (!MinecraftForge.EVENT_BUS.post(new MOEventScan((EntityPlayer) entityLiving, stack, position))) {
                    Scan(worldIn, stack, (EntityPlayer) entityLiving, worldItem, position.getBlockPos());
                }
            }
        }
        return stack;
    }

    @Override
    public RayTraceResult getScanningPos(ItemStack itemStack, EntityLivingBase player) {
        return MOPhysicsHelper.rayTrace(player, player.world, 5, 0, new Vec3d(0, player.getEyeHeight(), 0), true, false);
    }

    @Override
    public boolean destroysBlocks(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean showsGravitationalWaves(ItemStack itemStack) {
        return false;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase player, int count) {
        if(isLinked(stack)) {
            RayTraceResult hit = getScanningPos(stack, player);

            if (hit != null) {
                if (hit.typeOfHit == RayTraceResult.Type.BLOCK) {
                    ItemStack lastSelected = getSelectedAsItem(stack);
                    ItemStack worldItem = MatterDatabaseHelper.GetItemStackFromWorld(player.world, hit.getBlockPos());

                    if (worldItem != null && !MatterDatabaseHelper.areEqual(lastSelected, worldItem)) {

                        setSelected(player.world, stack, worldItem);

                        player.resetActiveHand();
                        if (player.world.isRemote) {
                            stopScanSounds();
                        }
                    }
                }
            } else {
                if (player.world.isRemote) {
                    stopScanSounds();
                    player.resetActiveHand();
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void playSound(double x, double y, double z) {
        if (scanningSound == null) {
            scanningSound = new MachineSound(MatterOverdriveSounds.scannerScanning, SoundCategory.PLAYERS, new BlockPos(x, y, z), 0.6f, 1);
            Minecraft.getMinecraft().getSoundHandler().playSound(scanningSound);
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityLivingBase entityLiving, int timeLeft) {
        if (worldIn.isRemote) {
            stopScanSounds();
        }
    }

    @SideOnly(Side.CLIENT)
    private void stopScanSounds() {
        if (scanningSound != null) {
            scanningSound.stopPlaying();
            scanningSound = null;
        }
    }

    public boolean Scan(World world, ItemStack scanner, EntityPlayer player, ItemStack worldBlock, BlockPos pos) {
        this.TagCompountCheck(scanner);

        StringBuilder scanInfo = new StringBuilder();
        IMatterDatabase database = getLink(world, scanner);

        if (database != null && MatterHelper.CanScan(worldBlock)) {
            resetScanProgress(scanner);
            if (database.addItem(worldBlock, PROGRESS_PER_ITEM, false, scanInfo)) {
                //scan successful
                SoundHandler.PlaySoundAt(world, MatterOverdriveSounds.scannerSuccess, SoundCategory.PLAYERS, player);
                DisplayInfo(player, scanInfo, TextFormatting.GREEN);
                return HarvestBlock(scanner, player, world, pos);
            } else {
                //scan fail
                DisplayInfo(player, scanInfo, TextFormatting.RED);
                SoundHandler.PlaySoundAt(world, MatterOverdriveSounds.scannerFail, SoundCategory.PLAYERS, player);
                return false;
            }
        } else {
            if (world.getBlockState(pos).getBlock() instanceof IScannable) {
                ((IScannable) world.getBlockState(pos).getBlock()).onScan(world, pos.getX(), pos.getY(), pos.getZ(), player, scanner);
                return true;
            } else if (world.getTileEntity(pos) instanceof IScannable) {
                ((IScannable) world.getBlockState(pos).getBlock()).onScan(world, pos.getX(), pos.getY(), pos.getZ(), player, scanner);
                return true;
            }
        }

        return false;
    }

    private void DisplayInfo(EntityPlayer player, StringBuilder scanInfo, TextFormatting formatting) {
        if (player != null && !scanInfo.toString().isEmpty()) {
            player.sendStatusMessage(new TextComponentString(formatting + scanInfo.toString()), true);
        }
    }
}
