package com.newlife.ta4j.servlet;

import java.util.ArrayList;

public class VN30 {
	
	private static String vn30String = "ACB;BCM;BID;BVH;CTG;FPT;GAS;GVR;HDB;HPG;MBB;MSN;MWG;PLX;POW;SAB;SHB;SSB;SSI;STB;TCB;TPB;VCB;VHM;VIB;VIC;VJC;VNM;VPB;VRE";

	final static String[] parts = vn30String.trim().split(";");
	
	public static ArrayList<String> getList() {
		ArrayList<String> vn30list = new ArrayList<String>();
		for(int i=0;i<parts.length;i++) {
			vn30list.add(parts[i]);
		}
		return vn30list;
	}
	
	public static void main(String[] args) {
		final ArrayList<String> vn30 = VN30.getList();
		String symbol = "VIC";
		for(int i = 0; i<vn30.size();i++) {
			System.out.println("vn30 "+1+" "+ vn30.get(i));
			if(vn30.contains(symbol)) {
				System.out.println("yeahhhhhhhh it have " + symbol);
			}
		}
	}
}
