package me.wonk2.utilities;

import me.wonk2.utilities.enums.DFType;
import me.wonk2.utilities.values.DFValue;
import me.wonk2.utilities.values.DFVar;
import me.wonk2.utilities.values.GameValue;
import me.wonk2.utilities.values.Parameter;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ParamManager {
	public static HashMap<String, Parameter[][]> argInfo;
	
	
	HashMap<Integer, DFValue> input;
	HashMap<String, String> tags;
	public String actionName;
	public HashMap<String, DFValue> localStorage;
	
	public ParamManager(HashMap<Integer, DFValue> input, HashMap<String, String> tags, String actionName, HashMap<String, DFValue> localStorage){
		this.input = input;
		this.tags = tags;
		this.actionName = actionName;
		this.localStorage = localStorage;
	}
	
	
	public Object[] formatParameters(HashMap<String, LivingEntity[]> targetMap) {
		HashMap<String, DFValue> args = new HashMap<>();
		
		if(!argInfo.containsKey(actionName)) throw new NotImplementedException("\"" + actionName + "\" is not supported yet!");
		Parameter[] params = getParamTemplate(input, argInfo.get(actionName), localStorage);
		
		Parameter param;
		int paramIndex = 0;
		
		ArrayList<Integer> keySet = new ArrayList<>(input.keySet());
		DFValue currentArg;
		
		for(int i = 0; i < params.length; i++) args.put(params[i].name, new DFValue(params[i].defaultValue, i, params[i].type)); // Assign default values
		
		for (int i = 0; i < 27; i++) {
			if(paramIndex >= params.length) break;
			
			param = params[paramIndex];
			DFType paramType = param.type;
			
			
			// If this slot is not empty (has an argument)
			if(keySet.contains(i)){
				currentArg = sanitizeValue(input.get(i), paramType, targetMap, localStorage).clone();
				
				// If this argument does not match the type needed, then give it its default value and move on
				if((currentArg.type != paramType && paramType != DFType.ANY)) {
					// Remember that the default value is already assigned before the loop
					paramIndex++;
					i--;
					continue;
				}
			}
			else continue; // If the slot is empty, keep searching for a filled slot
			
			if (param.repeating) {
				ArrayList<DFValue> repeatedValues = new ArrayList<>();
				while ((currentArg.type == paramType || paramType == DFType.ANY) && i < 27) {
					if (currentArg.getVal().getClass().isArray() && paramType != DFType.LIST) {
						repeatedValues = new ArrayList<>(List.of((DFValue[]) currentArg.getVal()));
						break;
					}
					if(currentArg.type == DFType.GAMEVAL) currentArg = ((GameValue) currentArg.getVal()).getVal(targetMap);
					if(keySet.contains(i)) repeatedValues.add(currentArg);
					if(keySet.contains(++i)) currentArg = sanitizeValue(input.get(i), paramType, targetMap, localStorage).clone();
				}
				
				// Set array as the value for this repeating parameter.
				args.put(param.name, new DFValue(repeatedValues.toArray(new DFValue[0]), --i, DFType.LIST));
			} else args.put(param.name, currentArg);
			paramIndex++;
		}
		
		
		return new Object[]{args, tags, input};
	}
	
	private static DFValue sanitizeValue(DFValue currentArg, DFType paramType, HashMap<String, LivingEntity[]> targetMap, HashMap<String, DFValue> localStorage){
		switch (currentArg.type) {
			case GAMEVAL -> {    // If it's a game value retrieve its actual value
				GameValue gameValue = (GameValue) currentArg.getVal();
				currentArg = gameValue.getVal(targetMap);
			}
			
			case NUM -> currentArg.setVal(Double.valueOf(DFUtilities.textCodes(String.valueOf(currentArg.getRawVal()), targetMap, localStorage)));
			
			case TXT -> currentArg.setVal(DFUtilities.textCodes((String) currentArg.getRawVal(), targetMap, localStorage));
			
			case VAR -> {
				DFVar var = (DFVar) currentArg.getVal();
				var.name = DFUtilities.textCodes(var.name, targetMap, localStorage);
				if (!DFVar.varExists(var, localStorage) && paramType == DFType.TXT)
					currentArg = new DFValue("0", currentArg.slot, DFType.TXT);
				else if (paramType != DFType.VAR) // Do we need the var itself here, or its value? ⬅
					currentArg = DFVar.getVar((DFVar) currentArg.getVal(), localStorage); // Get var value
			}
		}
		
		if(paramType == DFType.ITEM && currentArg.type == DFType.TXT)
			currentArg = new DFValue(DFUtilities.interpretText((String) currentArg.getVal()), DFType.ITEM);
		
		return currentArg;
	}
	
	private static Parameter[] getParamTemplate(HashMap<Integer, DFValue> input, Parameter[][] templates, HashMap<String, DFValue> localStorage){
		if(templates.length == 1) return templates[0];
		DFValue[] args = input.values().toArray(DFValue[]::new);
		
		for(Parameter[] template : templates){
			int globalI = 0;
			int paramIndex = 0;
			DFType argType = null;
			Parameter param = template[0];
			
			for(int i = 0; i < args.length && paramIndex < template.length; i++){
				param = template[paramIndex];
				argType = getArgType(args[i], param.type, localStorage);
				
				if(param.repeating){
					while(argType == param.type && i < input.size()){
						argType = getArgType(args[i], param.type, localStorage);
						i++;
					}
				}
				else if(argType != param.type) break;
				paramIndex++;
				globalI = i;
			}
			
			if(argType == param.type && globalI == args.length - 1) return template;
		}
		
		return templates[0];
	}
	
	private static DFType getArgType(DFValue arg, DFType paramType, HashMap<String, DFValue> localStorage){
		if(arg.type == DFType.VAR && paramType != DFType.VAR)
			return DFVar.getVar((DFVar) arg.getVal(), localStorage).type;
		else return arg.type;
	}
}