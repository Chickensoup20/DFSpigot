package me.wonk2.utilities.actions;

import me.wonk2.utilities.DFUtilities;
import me.wonk2.utilities.actions.pointerclasses.Action;
import me.wonk2.utilities.actions.pointerclasses.Conditional;
import me.wonk2.utilities.enums.DFType;
import me.wonk2.utilities.values.DFValue;
import me.wonk2.utilities.values.DFVar;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class IfVariable extends Conditional {
	LivingEntity[] targets;
	HashMap<String, DFValue> localStorage;
	
	public IfVariable(String targetName, HashMap<String, LivingEntity> targetMap, Object[] inputArray, String action, LivingEntity[] targets, HashMap<String, DFValue> localStorage){
		super(targetName, targetMap, inputArray, action);
		this.targets = targets;
		this.localStorage = localStorage;
	}
	
	public boolean evaluateCondition(){
		
		for(LivingEntity target : targets)
			switch(action){
				case "!=":
				case "=": {
					DFValue value = args.get("value");
					DFValue[] checks = (DFValue[]) args.get("checks").getVal();
					
					for(DFValue check : checks){
						if(value.getVal().equals(check.getVal())){
							if(action == "=") return true;
							if(action == "!=") return false;
						}
					}
					
					return action == "!=";
				}
				
				case ">":
				case ">=":
				case "<":
				case "<=": {
					double num = (double) args.get("num").getVal();
					double check = (double) args.get("check").getVal();
					
					switch(action){
						case ">": return num > check;
						case ">=": return num >= check;
						case "<": return num < check;
						case "<=": return num <= check;
					}
				}
				
				case "InRange": {
					DFValue val = args.get("value");
					DFValue lower = args.get("lower");
					DFValue upper = args.get("upper");
					
					if(val.type == DFType.NUM) return inRange((double) val.getVal(), (double) lower.getVal(), (double) upper.getVal());
					else if (val.type == DFType.LOC) return inRange((Location) val.getVal(), (Location) lower.getVal(), (Location) upper.getVal());
					
					return false;
				}
				
				case "LocIsNear": {
					Location loc = (Location) args.get("loc").getVal();
					Location[] checkLocs = DFValue.castLoc((DFValue[]) args.get("checkLocs").getVal());
					double radius = (double) args.get("radius").getVal();
					
					for(Location checkLoc : checkLocs)
						if(DFUtilities.locIsNear(target.getWorld(), checkLoc, loc, radius, tags.get("Shape"))) return true;
						
					return false;
				}
				
				case "TextMatches": {
					//TODO
				}
				
				case "Contains": {
					boolean ignoreCase = tags.get("Ignore Case") == "True";
					String text = (String) args.get("txt").getVal();
					if(ignoreCase) text = text.toLowerCase();
					
					String[] checkTxts = DFValue.castTxt((DFValue[]) args.get("checkTxts").getVal());
					
					for(String checkTxt : checkTxts)
						if(text.contains(ignoreCase ? checkTxt.toLowerCase() : checkTxt)) return true;
					
					
					return false;
				}
				
				case "StartsWith": {
					boolean ignoreCase = tags.get("Ignore Case") == "True";
					String text = ((String) args.get("txt").getVal()).split(" ")[0];
					if(ignoreCase) text = text.toLowerCase();
					
					String[] checkTxts = DFValue.castTxt((DFValue[]) args.get("checkTxts").getVal());
					
					for(String checkTxt : checkTxts)
						if(text == (ignoreCase ? checkTxt.toLowerCase() : checkTxt)) return true;
						
					return false;
				}
				
				case "EndsWidth": {
					boolean ignoreCase = tags.get("Ignore Case") == "True";
					String[] splitText = ((String) args.get("txt").getVal()).split(" ");
					String text = splitText[splitText.length - 1];
					if(ignoreCase) text = text.toLowerCase();
					
					String[] checkTxts = DFValue.castTxt((DFValue[]) args.get("checkTxts").getVal());
					
					for(String checkTxt : checkTxts)
						if(text == (ignoreCase ? checkTxt.toLowerCase() : checkTxt)) return true;
					
					return false;
				}
				
				case "VarExists": {
					DFVar var = (DFVar) args.get("var").getVal();
					return DFVar.varExists(var, localStorage);
				}
				
				case "VarIsType": {
					String typeTag = tags.get("Variable Type");
					DFValue value = args.get("value");
					
					HashMap<String, DFType> types = new HashMap<>(){{
						put("Number", DFType.NUM);
						put("Text", DFType.TXT);
						put("Location", DFType.LOC);
						put("Item", DFType.ITEM);
						put("List", DFType.LIST);
						put("Potion effect", DFType.POT);
						put("Sound", DFType.SND);
						put("Particle", DFType.ANY); //TODO
						put("Vector", DFType.ANY); //TODO
						put("Dictionary", DFType.ANY); //TODO
					}};
					
					return value.type == types.get(typeTag);
				}
				
				case "ItemEquals": {
					ItemStack item = (ItemStack) args.get("item").getVal();
					ItemStack[] checkItems = DFValue.castItem((DFValue[]) args.get("checkItems").getVal());
					
					for(ItemStack checkItem : checkItems)
						switch(tags.get("Comparison Mode")){
							case "Exactly equals":
								if(item == checkItem) return true;
								break;
							case "Ignore stack size":
								if(item.isSimilar(checkItem)) return true;
								break;
							case "Ignore durability and stack size": {
								((Damageable) checkItem).setDamage(((Damageable) item).getDamage());
								if(item.isSimilar(checkItem)) return true;
								break;
							}
							case "Material only": {
								if(item.getType() == checkItem.getType()) return true;
								break;
							}
						}
					
					return false;
				}
				
				case "ItemHasTag": {
					//TODO: https://www.spigotmc.org/threads/tutorial-the-complete-guide-to-itemstack-nbttags-attributes.131458/
				}
				
				case "ListContains": {
					List<DFValue> list = Arrays.asList((DFValue[]) args.get("list").getVal());
					DFValue[] values = (DFValue[]) args.get("values").getVal();
					
					for(DFValue value : values)
						if(list.contains(value)) return true;
						
					return false;
				}
				
				case "ListValueEq": {
					DFValue[] list = (DFValue[]) args.get("list").getVal();
					int index = args.get("index").getInt() - 1;
					DFValue[] values = (DFValue[]) args.get("values").getVal();
					
					for(DFValue value : values)
						if(list[index] == value) return true;
						
					return false;
				}
			}
		
		return false;
	}
	
	private static boolean inRange(double num, double lower, double upper){
		return num >= lower && num <= upper;
	}
	
	private static boolean inRange(Location loc, Location lowerLeft, Location upperRight){
		return (loc.getX() >= lowerLeft.getX() && loc.getX() <= upperRight.getX())
			&& (loc.getY() >= lowerLeft.getY() && loc.getY() <= upperRight.getY())
			&& (loc.getZ() >= lowerLeft.getZ() && loc.getZ() <= upperRight.getZ());
	}
}
