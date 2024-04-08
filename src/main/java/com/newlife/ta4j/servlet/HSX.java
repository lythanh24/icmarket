package com.newlife.ta4j.servlet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

public class HSX {
	
	public static String pid="HSX";
	
	private static String vn30 = "ACB;BCM;BID;BVH;CTG;FPT;GAS;GVR;HDB;HPG;MBB;MSN;MWG;PLX;POW;SAB;SHB;SSB;SSI;STB;TCB;TPB;VCB;VHM;VIB;VIC;VJC;VNM;VPB;VRE";
	private static String vn50 = "ACB;CTG;DCM;DGC;DIG;DPM;EIB;FPT;GEX;GMD;HCM;HDB;HPG;HSG;IDC;KBC;KDC;KDH;LPB;MBB;MSB;MSN;MWG;NLG;PDR;PNJ;POW;PVD;PVS;SBT;SHB;SHS;SSI;STB;TCB;TPB;VCB;VCG;VCI;VGC;VHC;VHM;VIB;VIC;VJC;VND;VNM;VPB;VPI;VRE;SHB;SHS;SSI;STB;TCB;TPB;VCB;VCI;VHC;VHM;VIB;VIC;VJC;VNM;VPB;VRE;ACB;BID;BVH;CTG;DGW;DIG;DPM;DRC;FPT;GAS;GMD;GVR;HCM;HDB;HPG;HSG;KBC;KDH;LPB;MBB;MSB;MSN;MWG;NLG;NVL;PHR;PLX;PNJ;POW;PVD;PVS;PVT;REE;SBT";
	private static String volumeOverMillion = "ABR;ABT;ACC;ACG;ACL;ADG;ADP;AGG;APC;APG;ASM;AST;BCE;BFC;BIC;BRC;BTP;BWE;C32;C47;CAV;CCI;CDC;CHP;CIG;CLC;CMG;CMX;CRC;CRE;CSV;CTI;CTR;CVT;DAT;DBT;DC4;DGC;DHC;DHM;DLG;DPR;DRL;DSN;DTA;DTL;DTT;DXV;EMC;EVE;EVG;FIR;FIT;FPT;GEX;GMC;HAP;HBC;HHS;HNA;HNG;HRC;HTG;HTL;HTV;HU1;HU3;HUB;HVN;ICT;ILB;IMP;ITD;KHG;KHP;KMR;L10;LAF;LBM;LEC;LGL;LHG;LM8;MCP;MDG;MIG;NAF;NHT;NNC;NO1;NSC;NTL;NVT;PAC;PDN;PGC;PGD;PGI;PHC;PLP;PMG;PPC;PSH;PTC;PVP;QBS;QNP;RAL;RDP;S4A;SAB;SAM;SAV;SBA;SBG;SBV;SC5;SCD;SFC;SFG;SGN;SGT;SHA;SHI;SHP;SII;SMB;SPM;SRC;SSC;ST8;SVC;TBC;TCM;TCO;TCT;TDG;TDM;TDP;TDW;TEG;THG;TIX;TLD;TMP;TMS;TMT;TN1;TNC;TNH;TNI;TNT;TPC;TRA;TRC;TSC;TTA;TTE;TVB;TYA;UIC;VAF;VDP;VFG;VGC;VID;VIP;VMD;VNE;VNG;VNL;VPD;VPS;VRE;VSC;VSH;VSI;VTO;YBM";

	public static int VN30 = 0;
	public static int VN50 = 1;
	public static int VOLUME_MILLIION = 2;
	
	
	public static ArrayList<String> getVN30() {
		String[] parts = vn30.trim().split(";");
		ArrayList<String> vn30list = new ArrayList<String>();
		for(int i=0;i<parts.length;i++) {
			if(!vn30list.contains(parts[i]))
				vn30list.add(parts[i]);
		}
		return vn30list;
	}
	
	public static ArrayList<String> getStockList(int GroupType) {
		String[] parts = null;
		if(GroupType == 0) {
			parts = vn30.trim().split(";");
		} else if(GroupType == 1) {
			parts = vn50.trim().split(";");
		}else if(GroupType == 2) {
			parts = volumeOverMillion.trim().split(";");
		}
		ArrayList<String> resultlist = new ArrayList<String>();
		for(int i=0;i<parts.length;i++) {
			if(!resultlist.contains(parts[i]))
				resultlist.add(parts[i]);
		}
		return resultlist;
	}
	public static ArrayList<String> getStockList(String symbolList) {
		
		if("VN30".equalsIgnoreCase(symbolList)) {
			symbolList = vn30;
		} else if("VN50".equalsIgnoreCase(symbolList)) {
			symbolList = vn50;
		}
		String[] parts = symbolList.trim().split(";");
		ArrayList<String> resultlist = new ArrayList<String>();
		for(int i=0;i<parts.length;i++) {
			if(!resultlist.contains(parts[i]))
				resultlist.add(parts[i]);
		}
		return resultlist;
	}
	
	public static ZonedDateTime getCreated(String summaryStr) {
		String[] parts = summaryStr.trim().split(";");
		String dateStr = parts[1];
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		LocalDateTime time = LocalDate.parse(dateStr, formatter).atStartOfDay();
		
		ZonedDateTime created = time.atZone(ZoneId.systemDefault());
		return created;
	}
	
	public static void main(String[] args) {
		ArrayList<String> vn30 = HSX.getVN30();
		String symbol = "VIC";
		for(int i = 0; i<vn30.size();i++) {
			System.out.println("vn30 "+1+" "+ vn30.get(i));
			if(vn30.contains(symbol)) {
				System.out.println("yeahhhhhhhh it have " + symbol);
			}
		}
		
		vn30 = HSX.getStockList(VN50);
		System.out.println("VN50: "+vn30.size());
		
		String date = "VN30;20240402;1,283.44;1,292.3;1,273.16;1292.3;365454";
		
		ZonedDateTime time =getCreated(date);
		System.out.println(date+" Time: "+time);
		
		date ="VRE;20240320;26.9;26.9;26.1;26.35;11622200";
		System.out.println(date+" Time: "+getCreated(date));
		
		Date dateCreated = Date.from(time.toInstant());
		System.out.println(dateCreated+" Time: "+time);
		
	}
}
