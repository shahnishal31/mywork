package com.tdameritrade.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class csvFileReader {
	
	static String cvsSplitBy = "\\~\\[";
	static String metaIn = System.getProperty("metaIn");
	static String listOut = System.getProperty("listOut");
	static String symbolOut = System.getProperty("symbolOut");
	static String discardOut = System.getProperty("discardOut");
	static int maxSymbols = Integer.parseInt(System.getProperty("maxSymbols"));
	static int maxWatchLists = Integer.parseInt(System.getProperty("maxWatchLists"));		
	static String seqList = System.getProperty("seqList");
	static String seqSymbol = System.getProperty("seqSymbol");
	static GZIPInputStream inputStream = null;
	static Scanner sc = null;
	static String previousLine = null;
	static String key1AcctId = null;
	static String key2AcctId = null;
	static String currentLineAcct = null;
	static String wlKey1WlId = "";
	static String wlKey2WlId = "";
	static String wlName1 = "";
	static String wlName2 = "";
	static String wlSymbol1 = "";
	static String wlSymbol2 = "";
	static HashSet<String> checkDP = new HashSet<String>();
	static ArrayList<String> checkWLNameDP = new ArrayList<String>();	
	static HashSet<String> checkWLCount = new HashSet<String>();		
	static Integer WLDPCounter = 0;
	static boolean badSymbol = false;
	static Integer WLSymbCount = 0;
	static String WLMaxCheck = "";
	static String SymbolMaxCheck = "";
	static Integer seqListP = Integer.parseInt(seqList);
	static Integer seqSymbolP = Integer.parseInt(seqSymbol);

	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		} catch (NullPointerException e) {
			return false;
		}
		// only got here if we didn't return false
		return true;
	}
	
	public static boolean isBadSymbol(String s) {
		
		if (s.split(cvsSplitBy)[4].replaceAll("\"", "").startsWith(".")
				&& s.split(cvsSplitBy)[4].replaceAll("\"", "").length() <= 1) {
			return  true;
		}

		if (s.split(cvsSplitBy)[4].replaceAll("\"", "").startsWith(".")
				&& s.split(cvsSplitBy)[4].replaceAll("\"", "").length() >= 9 && !s.split(cvsSplitBy)[4].replaceAll("\"", "").matches(".*\\d+.*")) {
			return true;
		}
		
		//Symbols whose length is greater than 9 and is not an option is a bad symbol
		if(!s.split(cvsSplitBy)[4].startsWith(".") && s.split(cvsSplitBy)[4].length() >= 9){
			return true;
		}
		
		return false;
	}

	public static String cleanStringDiscard(String s, String log) {
		String[] line = s.split("\\~\\[");

		StringBuilder sb = new StringBuilder();	
		sb.append(log+" :");
		for (int i = 0; i < line.length; i++) {
			sb.append(line[i] + "~[");
		}
		return sb.toString();

	}

	public static String cleanStringList(String s, String wlName) {
		String[] line = s.split("\\~\\[");

		StringBuilder sb = new StringBuilder();
		sb.append(line[0] + "~[" + line[1] + "~[" + wlName);
		return sb.toString();

	}

	public static String cleanStringSymbol(String s) {
		String[] line = s.split("\\~\\[");
		
		//Options Conversion
		if(line[4].startsWith(".") && line[4].length()>=9 && line[4].contains(" ")){
			// TD's AAPL_011918C42.5        AAPL Jan 19 2018 42.5 Call    &&   ISRG_110317P323.33     ISRG Nov 03 2017 323.33 Put
			// SCT's:  .AAPL  140719C007500
			try{
			int symbolwhitespace1 = line[4].indexOf(" ");
			int symbolwhitespace2 = line[4].lastIndexOf(" ");
			
			String symb = line[4].substring(1, symbolwhitespace1);
			String year = line[4].substring((symbolwhitespace2+1), (symbolwhitespace2+3));
			String month = line[4].substring((symbolwhitespace2+3), (symbolwhitespace2+5));
			String day = line[4].substring((symbolwhitespace2+5), (symbolwhitespace2+7));
			String callorput = line[4].substring((symbolwhitespace2+7), (symbolwhitespace2+8));
			String dollar = line[4].substring((symbolwhitespace2+8), (symbolwhitespace2+13)).replaceFirst("^0+(?!$)", "");
			String decimal = line[4].substring((symbolwhitespace2+13), line[4].length());
			
			if(!decimal.equals("0")){
				if(decimal.contains("0")){
					decimal = decimal.substring(0, decimal.lastIndexOf("0"));
				}
			} else {
				decimal = "";
			}
			if(decimal != ""){
				line[4] = symb+"_"+month+day+year+callorput+dollar+"."+decimal;			
			} else {
				line[4] = symb+"_"+month+day+year+callorput+dollar+decimal;
			}
			if(Integer.parseInt(year) >= 18){
				System.out.println(line[4]);
			}
			} catch(Exception e) {
				line[4] = "";				
			}
		}
		
		//Converting the symbol: ^VGPMX -> VGPMX (Fund Conversion)
		if(line[4].startsWith("^")){			
			line[4] = line[4].replace("^", "");
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(line[4]);
		return sb.toString();

	}
	
	public static void main(String[] args) throws IOException {

		if (!metaIn.isEmpty() && !listOut.isEmpty() && !symbolOut.isEmpty() && !discardOut.isEmpty()) {
				try {
				if (seqListP > 0 && seqSymbolP > 0)	{
					try	{
						BufferedWriter writerList = new BufferedWriter( new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(listOut))));
						try	{
							BufferedWriter writerSymbol = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(symbolOut))));
							try
							{
								BufferedWriter writerDiscard = new BufferedWriter(new OutputStreamWriter( new GZIPOutputStream(new FileOutputStream(discardOut))));
								try 
								{
									inputStream = new GZIPInputStream(new FileInputStream(metaIn));
									sc = new Scanner(inputStream, "UTF-8");
									previousLine = null;
									key2AcctId = "9";
									while (sc.hasNextLine()) {

										String currentline = sc.nextLine();

										badSymbol = isBadSymbol(currentline);

										if (!currentline.equals(previousLine) && !checkDP.contains(currentline)	&& !currentline.split(cvsSplitBy)[4].replaceAll("\"", "").isEmpty() && badSymbol == false && currentline.split(cvsSplitBy)[4].startsWith(".")) {
											
											checkDP.add(currentline);
											WLSymbCount = WLSymbCount + 1; 
											
											//Fetching data per line (Acct Id, WL Id, WL Name, Symbol etc)
											if (currentline != null) {
												//Fetching the CDI
												key1AcctId = currentline.split(cvsSplitBy)[0].replaceAll("\"", "");
											}
											if (previousLine != null) {
												key2AcctId = previousLine.split(cvsSplitBy)[0].replaceAll("\"", "");
											}
											if (currentline != null) {
												//Fetching the Current Line Acct Number
												currentLineAcct = currentline.split(cvsSplitBy)[1].replaceAll("\"", "");
											}
											if (currentline != null && !currentline.split(cvsSplitBy)[2].isEmpty()) {
												//Fetching the Watchlist Id
												wlKey1WlId = currentline.split(cvsSplitBy)[2].toString();
											}
											if (previousLine != null && !previousLine.split(cvsSplitBy)[2].isEmpty()) {
												wlKey2WlId = previousLine.split(cvsSplitBy)[2].toString();
											}
											if (currentline != null && !currentline.split(cvsSplitBy)[3].isEmpty()) {
												//Fetching Watchlist Name
												wlName1 = currentline.split(cvsSplitBy)[3].toString();	
												if(wlName1.length()>=50){													
													wlName1 = wlName1.substring(0, 50);
												}
											}
											if (previousLine != null && !previousLine.split(cvsSplitBy)[3].isEmpty()) {
												wlName2 = previousLine.split(cvsSplitBy)[3].toString();
											}
											if (currentline != null && !currentline.split(cvsSplitBy)[4].isEmpty()) {
												//Fetching Symbol
												wlSymbol1 = currentline.split(cvsSplitBy)[4].toString();
											}
											if (previousLine != null && !previousLine.split(cvsSplitBy)[4].isEmpty()) {
												wlSymbol2 = previousLine.split(cvsSplitBy)[4].toString();
											}

											// Code that creates the WatchList AcctPort File
											createAcctPort(writerList, writerDiscard, currentline);
											
											// Code that creates the Symbol File
											createSymbolFile(writerSymbol, writerDiscard, currentline);
											

										} 
										previousLine = currentline;
										badSymbol = false;
										if (wlKey1WlId != null && wlKey2WlId != null && !wlKey1WlId.equals(wlKey2WlId)) {											
											wlKey1WlId = null;
											wlKey2WlId = null;
										}

									}
								} catch (FileNotFoundException e) {
									System.out.println("Problem reading Input File");
									System.exit(-1);
								} catch (ArrayIndexOutOfBoundsException e) {
									System.out.println("Either Account Id or Symbol has buggy data");
									System.exit(-1);
								} finally {									
									if (inputStream != null) {
										inputStream.close();
									}
									if (sc != null) {
										sc.close();
									}
									writerList.flush();
									writerList.close();
									writerSymbol.flush();
									writerSymbol.close();
									writerDiscard.flush();
									writerDiscard.close();
								}
							} catch (Exception e) {
								e.printStackTrace();
								System.exit(-1);
							}
						} catch (Exception e) {
							e.printStackTrace();
							System.exit(-1);
						}
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(-1);
					}
				} else {
					System.out.println("Sequence Number are Invalid");
					System.exit(-1);
				}
			
		} catch (NullPointerException e) {
			System.out.println("Required Parameters are Invalid");
			System.exit(-1);
		}
		}
		
	}

	private static void createSymbolFile(BufferedWriter writerSymbol, BufferedWriter writerDiscard, String currentline) throws IOException {
		if(WLSymbCount <= maxSymbols){
			if (!wlSymbol1.equals(wlSymbol2)) {
				seqSymbolP = seqSymbolP + 1;
				writerSymbol.write(seqSymbolP + "~[" + seqListP + "~[" + cleanStringSymbol(currentline) + "~[" +WLSymbCount + "~[" +currentLineAcct);
				writerSymbol.newLine();
			} else if (wlSymbol1.equals(wlSymbol2) && !wlKey1WlId.equals(wlKey2WlId)) {
				// System.out.println("Same Symbol but different WL Symbol1"+wlSymbol1+" symbol2"+wlSymbol2+"account id1"+wlKey1WlId+"acct2 "+wlKey2WlId);
				seqSymbolP = seqSymbolP + 1;
				writerSymbol.write(seqSymbolP + "~[" + seqListP + "~[" + cleanStringSymbol(currentline) + "~[" +WLSymbCount + "~[" +currentLineAcct);
				writerSymbol.newLine();
			} else if (wlSymbol1.equals(wlSymbol2) && key1AcctId.equals(key2AcctId)) {
				// System.out.println("Same	// Symbol but different// AccountId");
				seqSymbolP = seqSymbolP + 1;
				writerSymbol.write(seqSymbolP + "~[" + seqListP + "~[" + cleanStringSymbol(currentline) + "~[" +WLSymbCount + "~[" +currentLineAcct);
				writerSymbol.newLine();
			}
		} else {
			if(!SymbolMaxCheck.equals(wlKey1WlId)){													
				writerDiscard.write("toomanysymbols:"+currentLineAcct+","+key1AcctId+","+wlKey1WlId);
				writerDiscard.newLine();
				SymbolMaxCheck = wlKey1WlId;
			}
		}
	}

	private static void createAcctPort(BufferedWriter writerList, BufferedWriter writerDiscard,String currentline) throws IOException {
		
		if (key1AcctId != null && key2AcctId != null) {
				
				if (!wlKey1WlId.equals(wlKey2WlId)) {
					WLSymbCount = 0;
					if(!key1AcctId.equals(key2AcctId) && key2AcctId != "9"){														
								checkWLNameDP.removeAll(checkWLNameDP);
								checkWLCount.removeAll(checkWLCount);	
								WLDPCounter = 0;																
					}
					checkWLNameDP.add(wlName1.toLowerCase());			
					checkWLCount.add(wlName1);
						
						if(checkWLCount.size() <= maxWatchLists){
								if(!key1AcctId.equals(key2AcctId) && key2AcctId != "9" && wlSymbol1!=wlSymbol2){														
									WLDPCounter = 0;
								}
								
								WLDPCounter = Collections.frequency(checkWLNameDP, wlName1.toLowerCase());
								
								if(WLDPCounter > 1){																	
									if(wlName1.length()>=49 && WLDPCounter <= 9){
										wlName1 = wlName1.substring(0,49);
									}
									if(wlName1.length()>=49 && WLDPCounter >= 10){
										wlName1 = wlName1.substring(0,48);
									}
									wlName1 = wlName1+(WLDPCounter-1);														
									if(checkWLNameDP.contains(wlName1)){
										wlName1 = wlName1+WLDPCounter;
									}															
									checkWLCount.add(wlName1);
								}
								
								seqListP = seqListP + 1;
								if (key2AcctId != "9") {
									checkDP.removeAll(checkDP);														
								}
								
								writerList.write(seqListP + "~[" + cleanStringList(currentline, wlName1));
								writerList.newLine();
							} else {
								if(!WLMaxCheck.equals(key1AcctId)){																	
									writerDiscard.write("toomanylists:"+ currentLineAcct+","+key1AcctId);
									writerDiscard.newLine();
									WLMaxCheck = key1AcctId;																	
								}
							}
						
				}

			}
			
	}
	
}
