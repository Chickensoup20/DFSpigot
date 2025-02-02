package me.wonk2.utilities.internals;

import me.wonk2.DFPlugin;
import me.wonk2.utilities.DFUtilities;
import me.wonk2.utilities.actions.*;
import me.wonk2.utilities.actions.pointerclasses.brackets.Bracket;
import me.wonk2.utilities.actions.pointerclasses.brackets.ClosingBracket;
import me.wonk2.utilities.actions.pointerclasses.brackets.RepeatingBracket;
import me.wonk2.utilities.actions.pointerclasses.Action;
import me.wonk2.utilities.actions.pointerclasses.Conditional;
import me.wonk2.utilities.enums.SelectionType;
import me.wonk2.utilities.values.DFValue;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class CodeExecutor {
	private static void runCodeBlock(Object codeBlock, HashMap<String, LivingEntity[]> targetMap, HashMap<String, DFValue> localVars, @Nullable Cancellable event, SelectionType eventType){
		while(Bukkit.getOnlinePlayers().size() != 0) {
			while (isConditional(codeBlock)) {
				boolean condition = ((Conditional) codeBlock).evaluateCondition();
				if (((Conditional) codeBlock).inverted) condition = !condition;
				
				if (condition) codeBlock = getPointer(codeBlock);
				else{
					if(codeBlock instanceof Repeat) LoopData.loopVars.remove(((Repeat) codeBlock).id); // Clear loop data once it's done
					
					codeBlock = ((Conditional) codeBlock).bracketPointer;
					if(codeBlock instanceof RepeatingBracket) codeBlock = ((RepeatingBracket) codeBlock).pointer;
				}
			}
			if (codeBlock == null) return;
			
			Object codeBlockPointer = getPointer(codeBlock);
			
			
			if (codeBlock instanceof CallFunction) {
				runCodeBlock(((CallFunction) codeBlock).getFunc(targetMap, localVars).get(0), targetMap, localVars, event, eventType);
				return;
			} else if (codeBlock instanceof StartProcess) {
				StartProcess p = (StartProcess) codeBlock;
				
				HashMap<String, LivingEntity[]> targets = p.getTargets(targetMap);
				HashMap<String, DFValue> vars = p.getVars(localVars);
				
				if (p.targetMode == StartProcess.TargetMode.FOR_EACH) {
					for (LivingEntity e : targetMap.get("selection")) {
						targets = new HashMap<>() {{
							put("selection", new LivingEntity[]{e});
						}};
						
						SelectionType processType = targets.get("selection")[0] instanceof Player ? SelectionType.PLAYER : SelectionType.ENTITY;
						runCodeBlock(p.getProcess(targets, vars).get(0), targets, vars, null, processType);
					}
				} else runCodeBlock(p.getProcess(targets, vars).get(0), targets, vars, null, p.targetMode == StartProcess.TargetMode.COPY_ALL ? eventType : SelectionType.EITHER);
				return;
			}
			else if(codeBlock instanceof Control) ((Control) codeBlock).formatParams();
			
			switch (((Action) codeBlock).action) {
				case "Wait":
					Bukkit.getScheduler().runTaskLater(DFPlugin.plugin, () -> runCodeBlock(codeBlockPointer, targetMap, localVars, event, eventType),
						DFUtilities.getWait(((Control) codeBlock).args, ((Control) codeBlock).tags));
					return;
				
				case "Return":
				case "End":
					return;
				
				case "Skip":
					break;
				
				case "StopRepeat":
					break; //TODO
				
				case "UncancelEvent":
				case "CancelEvent":
					assert event != null;
					event.setCancelled(((Action) codeBlock).action.equals("CancelEvent"));
				
				default:
					if (codeBlock instanceof SelectObject) targetMap.put("selection", ((SelectObject) codeBlock).getSelectedEntities(targetMap));
					else ((Action) codeBlock).invokeAction();
			}
			
			codeBlock = codeBlockPointer;
			if(eventType == SelectionType.PLAYER) if(!((Player) targetMap.get("default")[0]).isOnline() && !isSelectionValid(targetMap)) return;
		}
	}
	
	private static boolean isSelectionValid(HashMap<String, LivingEntity[]> targetMap){
		if(!targetMap.containsKey("selection")) return false;
		if(!(targetMap.get("selection")[0] instanceof Player)) return true;
		
		LivingEntity[] selection = targetMap.get("selection");
		for(LivingEntity e : selection)
			if(((Player) e).isOnline()) return true;
		return false;
	}
	
	private static Object getPointer(Object codeBlock){
		return ((Action) codeBlock).pointer instanceof RepeatingBracket ?
			((RepeatingBracket) ((Action) codeBlock).pointer).pointer :
			((Action) codeBlock).pointer;
	}
		
	public static void executeThread(Object[] threadContents, HashMap<String, LivingEntity[]> targetMap, HashMap<String, DFValue> localVars, @Nullable Cancellable event, SelectionType eventType){
		//stringifyThread(assignPointers(new ObjectArrWrapper(threadContents)));
		runCodeBlock(assignPointers(new ObjectArrWrapper(threadContents)).get(0), targetMap, localVars, event, eventType);
	}
	
	public static void stringifyThread(ObjectArrWrapper thread){
		for(int i = 0; i < thread.length; i++){
			Object codeBlock = thread.get(i);
			if(codeBlock instanceof Action && !(codeBlock instanceof Conditional))
				Bukkit.broadcastMessage(stringifyAction((Action) codeBlock));
			else if(codeBlock instanceof Conditional)
				Bukkit.broadcastMessage(stringifyAction((Conditional) codeBlock) + " |>> " + stringifyPointer(((Conditional) codeBlock).bracketPointer));
			else if(isBracket(codeBlock))
				Bukkit.broadcastMessage(codeBlock instanceof ClosingBracket ? "< ClosingBracket >" : "< RepeatingBracket > | -> " + stringifyAction(((RepeatingBracket) codeBlock).pointer));
		}
	}
	
	public static ObjectArrWrapper assignPointers(ObjectArrWrapper threadContents){
		ArrayList<Repeat> repeats = new ArrayList<>();
		for(int i = 0; i < threadContents.length; i++) {
			Object codeBlock = threadContents.get(i);
			if(codeBlock instanceof RepeatingBracket){
				((RepeatingBracket) codeBlock).pointer = repeats.get(repeats.size() - 1);
				repeats.remove(repeats.size() - 1);
			}
			if(isBracket(codeBlock)) continue;
			
			assert codeBlock instanceof Action;
			((Action) codeBlock).setPointer(getClosestPointer(threadContents, i));
			
			if (isConditional(codeBlock)){
				assert codeBlock instanceof Conditional;
				
				int brackets = 1;
				for(int k = i + 1; k < threadContents.length - 1; k++){
					if(isConditional(threadContents.get(k))) brackets++;
					else if(isBracket(threadContents.get(k))) brackets--;
					
					if(brackets == 0){
						((Conditional) codeBlock).bracketPointer = getClosestPointer(threadContents, k);
						break;
					}
				}
				
				if(codeBlock instanceof Repeat) repeats.add((Repeat) codeBlock);
			}
		}
		return threadContents;
	}
	
	private static Object getClosestPointer(ObjectArrWrapper threadContents, int startIndex){
		for(int k = startIndex + 1; k < threadContents.length; k++)
			if(threadContents.get(k) == null || !(threadContents.get(k) instanceof ClosingBracket)) return threadContents.get(k);
		return null;
	}
	
	private static String stringifyAction(Action action){
		if(action == null) return "null";
		return action.action + " -> " + stringifyPointer(action.pointer);
	}
	
	private static String stringifyPointer(Object pointer){
		if(pointer == null) return "null";
		if(Action.class.isAssignableFrom(pointer.getClass())) return ((Action) pointer).action;
		else return "< RepeatingBracket >";
	}
	
	private static boolean isBracket(Object obj){
		return obj != null && Bracket.class.isAssignableFrom(obj.getClass());
	}
	
	private static boolean isConditional(Object obj){
		return obj != null && Conditional.class.isAssignableFrom(obj.getClass());
	}
	
}
