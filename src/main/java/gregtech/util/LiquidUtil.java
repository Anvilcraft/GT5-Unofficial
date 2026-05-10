package gregtech.util;

import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.IFluidHandler;
import ic2.api.Direction;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.item.Item;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraft.block.Block;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraft.init.Blocks;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraft.world.World;

// Util class from IC2 experimental
public class LiquidUtil {
    public static LiquidData
    getLiquid(final World world, final int x, final int y, final int z) {
        final Block block = world.getBlock(x, y, z);
        Fluid liquid = null;
        boolean isSource = false;
        if (block instanceof IFluidBlock) {
            final IFluidBlock fblock = (IFluidBlock) block;
            liquid = fblock.getFluid();
            isSource = fblock.canDrain(world, x, y, z);
        } else if (block == Blocks.water || block == Blocks.flowing_water) {
            liquid = FluidRegistry.WATER;
            isSource = (world.getBlockMetadata(x, y, z) == 0);
        } else if (block == Blocks.lava || block == Blocks.flowing_lava) {
            liquid = FluidRegistry.LAVA;
            isSource = (world.getBlockMetadata(x, y, z) == 0);
        }
        if (liquid != null) {
            return new LiquidData(liquid, isSource);
        }
        return null;
    }

    public static int fillContainerStack(
        final ItemStack stack,
        final EntityPlayer player,
        final FluidStack fluid,
        final boolean simulate
    ) {
        final Item item = stack.getItem();
        if (!(item instanceof IFluidContainerItem)) {
            return 0;
        }
        final IFluidContainerItem container = (IFluidContainerItem) item;
        if (stack.stackSize == 1) {
            return container.fill(stack, fluid, !simulate);
        }
        final ItemStack testStack = StackUtil.copyWithSize(stack, 1);
        final int amount = container.fill(testStack, fluid, true);
        if (amount <= 0) {
            return 0;
        }
        if (StackUtil.storeInventoryItem(testStack, player, simulate)) {
            if (!simulate) {
                --stack.stackSize;
            }
            return amount;
        }
        return 0;
    }

    public static FluidStack drainContainerStack(
        final ItemStack stack,
        final EntityPlayer player,
        final int maxAmount,
        final boolean simulate
    ) {
        final Item item = stack.getItem();
        if (!(item instanceof IFluidContainerItem)) {
            return null;
        }
        final IFluidContainerItem container = (IFluidContainerItem) item;
        if (stack.stackSize == 1) {
            return container.drain(stack, maxAmount, !simulate);
        }
        final ItemStack testStack = StackUtil.copyWithSize(stack, 1);
        final FluidStack ret = container.drain(testStack, maxAmount, true);
        if (ret == null || ret.amount <= 0) {
            return null;
        }
        if (StackUtil.storeInventoryItem(testStack, player, simulate)) {
            if (!simulate) {
                --stack.stackSize;
            }
            return ret;
        }
        return null;
    }

    public static List<AdjacentFluidHandler> getAdjacentHandlers(final TileEntity source
    ) {
        final List<AdjacentFluidHandler> ret = new ArrayList<AdjacentFluidHandler>();
        for (final Direction dir : Direction.directions) {
            final TileEntity te = dir.applyToTileEntity(source);
            if (te instanceof IFluidHandler) {
                ret.add(new AdjacentFluidHandler((IFluidHandler) te, dir));
            }
        }
        return ret;
    }

    public static int
    distribute(final TileEntity source, final FluidStack stack, final boolean simulate) {
        int transferred = 0;
        for (final AdjacentFluidHandler handler : getAdjacentHandlers(source)) {
            final int amount
                = distributeTo(handler.handler, stack, handler.dir, simulate);
            transferred += amount;
            stack.amount -= amount;
            if (stack.amount <= 0) {
                break;
            }
        }
        stack.amount += transferred;
        return transferred;
    }

    public static int distributeTo(
        final IFluidHandler target,
        final FluidStack stack,
        final Direction dirTo,
        final boolean simulate
    ) {
        final int amount
            = target.fill(dirTo.getInverse().toForgeDirection(), stack, !simulate);
        return amount;
    }

    public static int distributeAll(final IFluidHandler source, int amount) {
        if (!(source instanceof TileEntity)) {
            throw new IllegalArgumentException("source has to be a tile entity");
        }
        final TileEntity srcTe = (TileEntity) source;
        int transferred = 0;
        for (final Direction dir : Direction.directions) {
            final TileEntity te = dir.applyToTileEntity(srcTe);
            if (te instanceof IFluidHandler) {
                final FluidStack stack
                    = transfer(source, dir, (IFluidHandler) te, amount);
                if (stack != null) {
                    amount -= stack.amount;
                    transferred += stack.amount;
                    if (amount <= 0) {
                        break;
                    }
                }
            }
        }
        return transferred;
    }

    public static FluidStack transfer(
        final IFluidHandler source,
        final Direction dir,
        final IFluidHandler target,
        int amount
    ) {
        FluidStack ret;
        do {
            ret = source.drain(dir.toForgeDirection(), amount, false);
            if (ret == null || ret.amount <= 0) {
                return null;
            }
            if (ret.amount > amount) {
                throw new IllegalStateException(
                    "The fluid handler " + source
                    + " drained more than the requested amount."
                );
            }
            final int cAmount
                = target.fill(dir.getInverse().toForgeDirection(), ret, false);
            if (cAmount > amount) {
                throw new IllegalStateException(
                    "The fluid handler " + target
                    + " filled more than the requested amount."
                );
            }
            amount = cAmount;
        } while (amount != ret.amount && amount > 0);
        if (amount <= 0) {
            return null;
        }
        ret = source.drain(dir.toForgeDirection(), amount, true);
        if (ret.amount != amount) {
            throw new IllegalStateException(
                "The fluid handler " + source + " drained inconsistently. Expected "
                + amount + ", got " + ret.amount + "."
            );
        }
        amount = target.fill(dir.getInverse().toForgeDirection(), ret, true);
        if (amount != ret.amount) {
            throw new IllegalStateException(
                "The fluid handler " + target + " filled inconsistently. Expected "
                + ret.amount + ", got " + amount + "."
            );
        }
        return ret;
    }

    public static boolean check(final FluidStack fs) {
        return fs.getFluid() != null;
    }

    public static boolean placeFluid(
        final FluidStack fs, final World world, final int x, final int y, final int z
    ) {
        if (fs == null || fs.amount < 1000) {
            return false;
        }
        final Fluid fluid = fs.getFluid();
        Block block = world.getBlock(x, y, z);
        if ((block.isAir((IBlockAccess) world, x, y, z) || !block.getMaterial().isSolid())
            && fluid.canBePlacedInWorld()
            && (block != fluid.getBlock() || !isFullFluidBlock(world, x, y, z, block))) {
            if (world.provider.isHellWorld && fluid == FluidRegistry.WATER) {
                world.playSoundEffect(
                    x + 0.5,
                    y + 0.5,
                    z + 0.5,
                    "random.fizz",
                    0.5f,
                    2.6f + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8f
                );
                for (int i = 0; i < 8; ++i) {
                    world.spawnParticle(
                        "largesmoke",
                        x + Math.random(),
                        y + Math.random(),
                        z + Math.random(),
                        0.0,
                        0.0,
                        0.0
                    );
                }
            } else {
                if (!world.isRemote && !block.getMaterial().isSolid()
                    && !block.getMaterial().isLiquid()) {
                    world.func_147480_a(x, y, z, true);
                }
                if (fluid == FluidRegistry.WATER) {
                    block = (Block) Blocks.flowing_water;
                } else if (fluid == FluidRegistry.LAVA) {
                    block = (Block) Blocks.flowing_lava;
                } else {
                    block = fluid.getBlock();
                }
                final int meta = (block instanceof BlockFluidBase)
                    ? ((BlockFluidBase) block).getMaxRenderHeightMeta()
                    : 0;
                if (!world.setBlock(x, y, z, block, meta, 3)) {
                    return false;
                }
            }
            fs.amount -= 1000;
            return true;
        }
        return false;
    }

    private static boolean isFullFluidBlock(
        final World world, final int x, final int y, final int z, final Block block
    ) {
        if (block instanceof IFluidBlock) {
            final IFluidBlock fBlock = (IFluidBlock) block;
            final FluidStack drained = fBlock.drain(world, x, y, z, false);
            return drained != null && drained.amount >= 1000;
        }
        return (block == Blocks.water || block == Blocks.flowing_water
                || block == Blocks.lava || block == Blocks.flowing_lava)
            && world.getBlockMetadata(x, y, z) == 0;
    }

    public static class LiquidData {
        public final Fluid liquid;
        public final boolean isSource;

        LiquidData(final Fluid liquid1, final boolean isSource1) {
            this.liquid = liquid1;
            this.isSource = isSource1;
        }
    }

    public static class AdjacentFluidHandler {
        public final IFluidHandler handler;
        public final Direction dir;

        private AdjacentFluidHandler(final IFluidHandler handler, final Direction dir) {
            this.handler = handler;
            this.dir = dir;
        }
    }

    public static class StackUtil {

        public static ItemStack copyWithSize(final ItemStack itemStack, final int newSize) {
            final ItemStack ret = itemStack.copy();
            ret.stackSize = newSize;
            return ret;
        }

        public static boolean storeInventoryItem(
            final ItemStack stack, final EntityPlayer player, final boolean simulate
        ) {
            if (simulate) {
                for (int i = 0; i < player.inventory.mainInventory.length; ++i) {
                    final ItemStack invStack = player.inventory.mainInventory[i];
                    if (invStack == null
                        || (isStackEqualStrict(stack, invStack)
                            && invStack.stackSize + stack.stackSize <= Math.min(
                                   player.inventory.getInventoryStackLimit(),
                                invStack.getMaxStackSize()
                            ))) {
                        return true;
                    }
                }
            } else if (player.inventory.addItemStackToInventory(stack)) {
                if (!FMLCommonHandler.instance().getEffectiveSide().isClient()) {
                    player.openContainer.detectAndSendChanges();
                }
                return true;
            }
            return false;
        }

        public static boolean isStackEqual(final ItemStack stack1, final ItemStack stack2) {
            return (stack1 == null && stack2 == null)
                || (stack1 != null && stack2 != null && stack1.getItem() == stack2.getItem()
                    && ((!stack1.getHasSubtypes() && !stack1.isItemStackDamageable())
                        || stack1.getItemDamage() == stack2.getItemDamage()));
        }

        public static boolean
        isStackEqualStrict(final ItemStack stack1, final ItemStack stack2) {
            return isStackEqual(stack1, stack2)
                && ItemStack.areItemStackTagsEqual(stack1, stack2);
        }

    }

}
