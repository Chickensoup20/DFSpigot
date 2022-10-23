package me.wonk2.utilities.actions;

import me.wonk2.utilities.DFUtilities;
import me.wonk2.utilities.actions.pointerclasses.Action;
import me.wonk2.utilities.actions.pointerclasses.Conditional;
import me.wonk2.utilities.values.DFValue;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class IfPlayer extends Conditional {
	public LivingEntity[] targets;
	
	public IfPlayer(String targetName, HashMap<String, LivingEntity> targetMap, Object[] inputArray, String action){
		super(targetName, targetMap, inputArray, action);
		targets = new LivingEntity[]{super.getTarget()}; //TODO
	}
	
	public boolean evaluateCondition(){
		for(Player target : Arrays.copyOf(targets, targets.length, Player[].class))
			switch(action){
				case "IsSneaking":
					return target.isSneaking();
				case "IsSprinting":
					return target.isSprinting();
				case "IsGliding":
					return target.isGliding();
				case "IsFlying":
					return target.isFlying();
				case "IsGrounded":
					return ((LivingEntity) target).isOnGround();
				case "IsSwimming":
					return target.isSwimming();
				case "IsBlocking":
					return target.isBlocking();
					
				case "IsLookingAt": {
					Material[] transparent = tags.get("Fluid Mode") == "Ignore fluids" ?
						new Material[]{Material.AIR, Material.VOID_AIR} :
						new Material[]{Material.WATER, Material.LAVA, Material.AIR, Material.VOID_AIR};
					Block targetBlock = target.getTargetBlock(Set.of(transparent), (int) args.get("distance").getVal());
					
					if(args.get("blocks") != null){
						Material[] blocks = (Material[]) Arrays.stream(DFValue.castItem((DFValue[]) args.get("blocks").getVal())).map(ItemStack::getType).toArray();
						return Arrays.asList(blocks).contains(targetBlock.getType());
					}
					else{
						Location[] locations = DFValue.castLoc((DFValue[]) args.get("locs").getVal());
						return Arrays.asList(locations).contains(targetBlock.getLocation());
					}
				}
				
				case "StandingOn": {
					Block standingBlock = target.getWorld().getBlockAt(target.getLocation().subtract(new Location(target.getWorld(), 0f, -1f, 0f)));
					if(args.get("blocks") != null){
						Material[] blocks = (Material[]) Arrays.stream(DFValue.castItem((DFValue[]) args.get("blocks").getVal())).map(ItemStack::getType).toArray();
						return Arrays.asList(blocks).contains(standingBlock.getType());
					}
					else{
						Block[] locs = (Block[]) Arrays.stream(DFValue.castLoc((DFValue[]) args.get("locs").getVal())).map(Location::getBlock).toArray();
						return Arrays.asList(locs).contains(standingBlock);
					}
				}
				
				case "IsNear": {
					Location[] locs = DFValue.castLoc((DFValue[]) args.get("locs").getVal());
					Location tL = target.getLocation();
					double radius = (double) args.get("radius").getVal();
					
					for(Location loc : locs)
						if(DFUtilities.locIsNear(target.getWorld(), target.getLocation(), loc, radius, tags.get("Shape"))) return true;
					
					return false;
				}
				
				case "InWorldBorder": {
					WorldBorder border = target.getWorldBorder();
					Location loc = args.get("loc").getVal() == null ? target.getLocation() : (Location) args.get("loc").getVal();
					
					return border.isInside(loc);
				}
				
				case "IsHolding": {
					ItemStack[] items = DFValue.castItem((DFValue[]) args.get("items").getVal());
					PlayerInventory targetInv = target.getInventory();
					
					for(ItemStack item : items){
						switch(tags.get("Hand Slot")){
							case "Either hand": {
								if(targetInv.getItemInOffHand() == item || targetInv.getItemInMainHand() == item) return true;
								break;
							}
							case "Main hand": {
								if(targetInv.getItemInMainHand() == item) return true;
								break;
							}
							case "Off hand": {
								if(targetInv.getItemInOffHand() == item) return true;
								break;
							}
						}
					}
					
					return false;
				}
				
				case "HasItem": {
					ItemStack[] items = DFValue.castItem((DFValue[]) args.get("items").getVal());
					PlayerInventory targetInv = target.getInventory();
					String checkMode = tags.get("Check Mode");
					
					for(ItemStack item : items){
						if(targetInv.contains(item) && checkMode == "Has Any Item") return true;
						else if(!targetInv.contains(item)) return false;
					}
					
					return checkMode == "Has All Items";
					// If the player is supposed to have all items, then the loop above would have returned false earlier
					// if they didn't.
					
					// If the player is supposed to have any item, then the loop should've returned true if they did.
				}
				
				case "IsWearing": {
					ItemStack[] items = DFValue.castItem((DFValue[]) args.get("items").getVal());
					List<ItemStack> armor = Arrays.asList(target.getInventory().getArmorContents());
					
					if(tags.get("Check Mode") == "Is Wearing All") return armor.containsAll(Arrays.asList(items));
					
					for(ItemStack item : items) if(armor.contains(item)) return true;
					return false;
				}
				
				case "IsUsingItem": {
					break; //TODO
				}
				
				case "NoItemCooldown": {
					Material[] items = (Material[]) Arrays.stream(DFValue.castItem((DFValue[]) args.get("items").getVal())).map(ItemStack::getType).toArray();
					for(Material item : items) if(target.getCooldown(item) != 0) return true;
					return false;
				}
				
				case "HasSlotItem": {
					Integer[] slots = (Integer[]) Arrays.stream((DFValue[]) args.get("slots").getVal()).map(DFValue::getInt).toArray();
					List<ItemStack> items = Arrays.asList(DFValue.castItem((DFValue[]) args.get("items").getVal()));
					
					for(int slot : slots) if(items.contains(target.getInventory().getItem(slot))) return true;
					return false;
				}
				
				case "MenuSlotEquals": {
					Integer[] slots = (Integer[]) Arrays.stream((DFValue[]) args.get("slots").getVal()).map(DFValue::getInt).toArray();
					List<ItemStack> items = Arrays.asList(DFValue.castItem((DFValue[]) args.get("items").getVal()));
					
					for(int slot : slots) if(items.contains(target.getOpenInventory().getItem(slot))) return true;
					return false;
				}
				
				case "CursorItem": {
					List<ItemStack> items = Arrays.asList(DFValue.castItem((DFValue[]) args.get("items").getVal()));
					return items.contains(target.getItemOnCursor());
				}
				
				case "HasRoomForItem": {
					//TODO
				}
				
				case "NameEquals": {
					String[] names = DFValue.castTxt((DFValue[]) args.get("names").getVal());
					Bukkit.broadcastMessage(String.join(", ", names));
					Bukkit.broadcastMessage(target.getUniqueId() + " | " + target.getName());
					
					
					for(String name : names)
						try{
							if(target.getUniqueId() == UUID.fromString(name)) return true;
						} catch (IllegalArgumentException ignored){
							if(target.getName().equals(name)) return true;
						}
					
					return false;
				}
				
				case "SlotEquals": {
					int slot = args.get("slot").getInt();
					return target.getInventory().getHeldItemSlot() == slot;
				}
			}
		
		return false;
	}
}
