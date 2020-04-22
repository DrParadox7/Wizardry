package electroblob.wizardry.block;

import electroblob.wizardry.Settings;
import electroblob.wizardry.Wizardry;
import electroblob.wizardry.WizardryGuiHandler;
import electroblob.wizardry.registry.WizardryTabs;
import electroblob.wizardry.tileentity.TileEntityBookshelf;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.Properties;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class BlockBookshelf extends BlockHorizontal implements ITileEntityProvider {

	/** When a bookshelf block (of any kind specified in the config) is added or removed, players within this range will
	 * be notified of the change. */
	public static final double PLAYER_NOTIFY_RANGE = 32;
	public static final int SLOT_COUNT = 12;

	public static final UnlistedPropertyBool[] BOOKS = new UnlistedPropertyBool[SLOT_COUNT];

	static {
		for(int i=0; i<SLOT_COUNT; i++){
			BOOKS[i] = new UnlistedPropertyBool("book" + i);
		}
	}

	public BlockBookshelf(){
		super(Material.WOOD);
		this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
		this.setCreativeTab(WizardryTabs.WIZARDRY);
		this.setHardness(2.0F);
		this.setResistance(5.0F);
		this.setSoundType(SoundType.WOOD);
	}

	@Override
	protected BlockStateContainer createBlockState(){
//		IProperty<?>[] properties = { FACING };
//		return new BlockStateContainer(this, ArrayUtils.addAll(properties, BOOKS));
		return new BlockStateContainer.Builder(this).add(FACING).add(BOOKS).build();
	}

	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face){
		return state.getValue(FACING).getAxis() == face.getAxis() ? BlockFaceShape.UNDEFINED : BlockFaceShape.SOLID;
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer){
		return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	}

	@Override
	public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state){
		super.onBlockAdded(worldIn, pos, state);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState block){

		TileEntity tileentity = world.getTileEntity(pos);

		if(tileentity instanceof TileEntityBookshelf){
			InventoryHelper.dropInventoryItems(world, pos, (TileEntityBookshelf)tileentity);
		}

		super.breakBlock(world, pos, block); // For blocks that don't extend BlockContainer, this removes the TE
	}

	@Override
	public IBlockState getStateFromMeta(int meta){
		EnumFacing enumfacing = EnumFacing.byIndex(meta);
		if(enumfacing.getAxis() == EnumFacing.Axis.Y) enumfacing = EnumFacing.NORTH;
		return this.getDefaultState().withProperty(FACING, enumfacing);
	}

	@Override
	public int getMetaFromState(IBlockState state){
		return state.getValue(FACING).getIndex();
	}

	@Override
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos){

		IExtendedBlockState s = (IExtendedBlockState)super.getExtendedState(state, world, pos);

		if(world.getTileEntity(pos) instanceof TileEntityBookshelf){
			TileEntityBookshelf tileentity = ((TileEntityBookshelf)world.getTileEntity(pos));
			for(int i = 0; i < tileentity.getSizeInventory(); i++){
				s = s.withProperty(BOOKS[i], !tileentity.getStackInSlot(i).isEmpty());
			}
		}

		return s;
	}

	@Nullable
	@Override
	public TileEntity createNewTileEntity(World world, int meta){
		return new TileEntityBookshelf();
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState block, EntityPlayer player, EnumHand hand,
									EnumFacing side, float hitX, float hitY, float hitZ){

		TileEntity tileEntity = world.getTileEntity(pos);

		if(tileEntity == null || player.isSneaking()){
			return false;
		}

		player.openGui(Wizardry.instance, WizardryGuiHandler.BOOKSHELF, world, pos.getX(), pos.getY(), pos.getZ());
		return true;
	}

	@Override
	public boolean hasComparatorInputOverride(IBlockState state){
		return true;
	}

	@Override
	public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos){

		TileEntity tileEntity = world.getTileEntity(pos);

		if(tileEntity instanceof TileEntityBookshelf){

			int slotsOccupied = 0;

			for(int i = 0; i < ((TileEntityBookshelf)tileEntity).getSizeInventory(); i++){
				if(!((TileEntityBookshelf)tileEntity).getStackInSlot(i).isEmpty()) slotsOccupied++;
			}

			return slotsOccupied;

		}

		return super.getComparatorInputOverride(state, world, pos);
	}

	@Override
	public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param){
		super.eventReceived(state, world, pos, id, param);
		TileEntity tileentity = world.getTileEntity(pos);
		return tileentity != null && tileentity.receiveClientEvent(id, param);
	}

	// Copied from BlockFluidBase, only reason it exists is because of java's weird restrictions on generics
	private static final class UnlistedPropertyBool extends Properties.PropertyAdapter<Boolean> {

		public UnlistedPropertyBool(String name){
			super(PropertyBool.create(name));
		}
	}

	/**
	 * Returns a list of nearby bookshelves' inventories, where 'bookshelves' are any tile entities with inventories
	 * whose blocks are specified in the config file under the {@code bookshelfBlocks} option.
	 * @param world The world to search in
	 * @param centre The position to search around
	 * @param exclude Any tile entities that should be excluded from the returned list
	 * @return A list of nearby {@link IInventory} objects that count as valid bookshelves
	 */
	public static List<IInventory> findNearbyBookshelves(World world, BlockPos centre, TileEntity... exclude){

		List<IInventory> bookshelves = new ArrayList<>();

		int searchRadius = Wizardry.settings.bookshelfSearchRadius;

		for(int x = -searchRadius; x <= searchRadius; x++){
			for(int y = -searchRadius; y <= searchRadius; y++){
				for(int z = -searchRadius; z <= searchRadius; z++){

					BlockPos pos = centre.add(x, y, z);

					if(Settings.containsMetaBlock(Wizardry.settings.bookshelfBlocks, world.getBlockState(pos))){
						TileEntity te = world.getTileEntity(pos);
						if(te instanceof IInventory && !ArrayUtils.contains(exclude, te)) bookshelves.add((IInventory)te);
					}

				}
			}
		}

		return bookshelves;

	}

	@SubscribeEvent
	public static void onWorldLoadEvent(WorldEvent.Load event){
		event.getWorld().addEventListener(Listener.instance);
	}

	@SubscribeEvent
	public static void onWorldUnloadEvent(WorldEvent.Unload event){
		event.getWorld().removeEventListener(Listener.instance);
	}

	public static class Listener implements IWorldEventListener {

		public static final Listener instance = new Listener();

		private Listener(){}

		@Override
		public void notifyBlockUpdate(World world, BlockPos pos, IBlockState oldState, IBlockState newState, int flags){

			if(oldState == newState) return; // Probably won't happen but just in case

			if(Settings.containsMetaBlock(Wizardry.settings.bookshelfBlocks, oldState) // Bookshelf removed
					|| Settings.containsMetaBlock(Wizardry.settings.bookshelfBlocks, newState)){ // Bookshelf placed
				// It is also possible (with commands) for a bookshelf to be replaced with another bookshelf, in which
				// case this should still just be called once
				Wizardry.proxy.notifyBookshelfChange(world, pos);
			}

		}

		// Dummy implementations
		@Override public void notifyLightSet(BlockPos pos){}
		@Override public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2){}
		@Override public void playSoundToAllNearExcept(@Nullable EntityPlayer player, SoundEvent soundIn, SoundCategory category, double x, double y, double z, float volume, float pitch){}
		@Override public void playRecord(SoundEvent soundIn, BlockPos pos){}
		@Override public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters){}
		@Override public void spawnParticle(int id, boolean ignoreRange, boolean minimiseParticleLevel, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int... parameters){}
		@Override public void onEntityAdded(Entity entityIn){}
		@Override public void onEntityRemoved(Entity entityIn){}
		@Override public void broadcastSound(int soundID, BlockPos pos, int data){}
		@Override public void playEvent(EntityPlayer player, int type, BlockPos blockPosIn, int data){}
		@Override public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress){}

	}

}
