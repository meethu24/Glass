package com.example.android.utils;

import java.util.Set;
import java.util.Stack;

public class PermutationsAndCombinations {

public static void permute(Stack<Integer> currentSet,String input, StringBuilder output, Set<String> permutations){
		
		for(int i = 0; i<input.length();i++) {
			if(currentSet.contains(i)) continue;
			currentSet.push(i);
			output.append(input.charAt(i));
			permute(currentSet,input, output,permutations );
			if(output.length() == input.length()) {
				StringBuilder permutedString = new StringBuilder(output);
				permutations.add(permutedString.toString());
			}	
			output.deleteCharAt(output.length()-1);
			currentSet.pop();
		}
	}

}
