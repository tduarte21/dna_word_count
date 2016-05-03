import java.util.Scanner;
import java.lang.Math;
import java.util.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.nio.*;
import java.nio.channels.*;

public class TAP_TP1_ex3_V2{

	private static final int MAX_THREADS = 4; //CONFIG!
	private static final int wordSize = 11;
	private static final String FilenameDNA = "dna.fa";
	private static final int events[] = {100, 1000, 10000, 100000};
	private static final byte[] ADN_BYTE = {"A".getBytes()[0], "C".getBytes()[0], "G".getBytes()[0], "T".getBytes()[0]};
	private static final Double[] probability = {0.5, 0.03125};
	private static final int eventsSize = events.length;
	private static int dnaFileLength;
	private static byte[] dnaFileByteArray; //= new byte[dnaFileLength]; //real file size is +/- 252513000
	private static StringBuffer dnaFileSB;
	private static MappedByteBuffer dnaFile;
	private static int[] wordCombCount = new int[wordSize];
	private static File wordCountFile = new File("wordCount");
	private static String[][] WORD_COUNT_FILENAME = new String[wordSize][4];
	private static List<ConcurrentHashMap<String, Integer>> DNAwords = new ArrayList<ConcurrentHashMap<String,Integer>>();
	//List - for multiple word length types (1,2,3,4,5,6,7...)
	//ConcurrentHashMap<char[], Integer> - Used by Threads, includes a word(char[]) and a word count

	public static void main(String[] args){

		System.out.println("\nStarting...");
		System.out.println(" - Generating word combinations - ");
		generateDNAWords();

		Scanner scan = new Scanner(System.in);

		int startCount = 100; // Starting size

		int simSize = 10;
		int input;

		String[] pTypeName = {"1","1-2","1-32","log2"};
		for (int j=0; j<4; j++) {
			for (int i=0; i<wordSize; i++) {
				WORD_COUNT_FILENAME[i][j] = "count/wordCount_length"+(i+1)+"_prob_" + pTypeName[j] + ".txt";			
			}
		}

		do{	
			System.out.println();
			System.out.println(" ---      Ex 3  Approximate Counting       --- ");
			System.out.println("       of word occurrences in a DNA file ");
			System.out.println();
			System.out.println("   Please choose one option:");
			System.out.println("    1) Count single word");
			System.out.println("    2) Count all words (Auto save to file)");
			System.out.println("    3) Count with probability (regenerate one file)");
			System.out.println("    4) Automatic count with probability (regenerate all files)");
			System.out.println("    5) Compare results");
			System.out.println("    0) Exit");
			System.out.println(" ---------------------------------------------- ");

			System.out.print("Option: ");
			
			input = scan.nextInt();	

			if(input==1){
				System.out.println("Allowed chars: {A,C,G,T}");
				System.out.print("Word: ");
				String word = scan.next();
				countWord(word);
			}
			else if(input==2){

				System.out.println("\nMultiple word length counter (threads). Please insert word length from LOWER to HIGHER");
				System.out.println("Examples: 2-6 -> 2,3,4,5,6   3-3 -> 3");
				System.out.print("\nLower word length: ");
				int start = scan.nextInt();
				System.out.print("Higher word length: ");
				int end = scan.nextInt();
				int probType = 1;

				CountWords[] cws = new CountWords[(end-start+1)];
				String ans = scan.nextLine(); //Avoid previous line

				for (int i=(start-1); i<end; i++) {
					String fileName = WORD_COUNT_FILENAME[i][probType-1];

					File file = new File(fileName);
					if(file.exists()){
						
						do{
							System.out.println("File " + fileName + " already exists!");
							System.out.print("Overwrite? (Y/N) ");							
							ans = scan.nextLine();	
						}while(!ans.toLowerCase().equals("n") && !ans.toLowerCase().equals("y"));
						
						if (ans.toLowerCase().equals("n")) {
							continue;
						}	
						else if(ans.toLowerCase().equals("y")){
							System.out.println("File will be overwritten.");
						}
					}

					cws[(i-(start-1))] = new CountWords(DNAwords.get(i), (i+1), dnaFileByteArray, dnaFileLength, probType, fileName, MAX_THREADS);
					cws[(i-(start-1))].start();
					System.out.println("Counting words with " + (i+1) + " char length...");

				}

				for (int i=0; i<cws.length; i++) {
					try{
						if(cws[i]!=null)
							cws[i].join();
					}catch(InterruptedException ex){
						System.out.println("ERROR: Thread " + i + " interrupted.");
						ex.printStackTrace();
					}
				}
			}
			else if(input==3){
				System.out.print("\nWord length to simulate: ");
				int length = scan.nextInt();
				int probType;
				do{
					System.out.println("Probability Type: ");
					System.out.println(" 1) 1/2");
					System.out.println(" 2) 1/32");
					System.out.println(" 3) log2(x)");
					System.out.print("Option: ");
					probType = scan.nextInt();
				}while(probType!=1 && probType!=2 && probType!=3);

				try{
					HashMap<String,Integer> wordMap = loadCountFile(length,0);

					String fileName = WORD_COUNT_FILENAME[length-1][probType];

					System.out.println("Saving in new file: " + fileName);
					File file = new File(fileName);
					FileWriter writer = new FileWriter(file);
					System.out.println("Simulating count...");

					long startTime = System.nanoTime();
    	        
					if (probType==3){  //Log2
						for (Map.Entry<String,Integer> entry : wordMap.entrySet()) {
							String word = entry.getKey();
							int count = entry.getValue();
							int newCount = simCountLog2(count);
							writer.append(word); //Word
							writer.append(" ");
							writer.append(String.valueOf(newCount)); //Count
							writer.append("\n");
						}
					}else{
						double prob = probability[probType-1];
						for (Map.Entry<String,Integer> entry : wordMap.entrySet()) {
							String word = entry.getKey();
							int count = entry.getValue();
							int newCount = simCount(count, prob);
							writer.append(word); //Word
							writer.append(" ");
							writer.append(String.valueOf(newCount)); //Count
							writer.append("\n");
						}
					}
					writer.flush();
					System.out.println(fileName + " saved!");
					long elapsedTime = (System.nanoTime() - startTime)/1000000;
    				System.out.println("Finished simulating count with length=" + length + " in " + elapsedTime + "ms ");

				}catch(IOException e){
					e.printStackTrace();
				}
			}
			else if(input==4){

				long startTime1 = System.nanoTime();
				
				for (int i=1; i<9; i++) {
					for (int j=1; j<4; j++) {
						
						System.out.print("\nWord length to simulate: " + i + "\n");
						int length = i;
						int probType;
						//do{
						System.out.println("Probability Type: ");
						System.out.println(" 1) 1/2");
						System.out.println(" 2) 1/32");
						System.out.println(" 3) log2(x)");
						System.out.print("Option: " + j + "\n");
						probType = j;
						//}while(probType!=1 && probType!=2 && probType!=3);

						try{
							HashMap<String,Integer> wordMap = loadCountFile(length,0);

							String fileName = WORD_COUNT_FILENAME[length-1][probType];

							System.out.println("Saving in new file: " + fileName);
							File file = new File(fileName);
							FileWriter writer = new FileWriter(file);
							System.out.println("Simulating count...");

							long startTime = System.nanoTime();
		    	        
							if (probType==3){  //Log2
								for (Map.Entry<String,Integer> entry : wordMap.entrySet()) {
									String word = entry.getKey();
									int count = entry.getValue();
									int newCount = simCountLog2(count);
									writer.append(word); //Word
									writer.append(" ");
									writer.append(String.valueOf(newCount)); //Count
									writer.append("\n");
								}
							}else{
								double prob = probability[probType-1];
								for (Map.Entry<String,Integer> entry : wordMap.entrySet()) {
									String word = entry.getKey();
									int count = entry.getValue();
									int newCount = simCount(count, prob);
									writer.append(word); //Word
									writer.append(" ");
									writer.append(String.valueOf(newCount)); //Count
									writer.append("\n");
								}
							}
							writer.flush();
							System.out.println(fileName + " saved!");
							long elapsedTime = (System.nanoTime() - startTime)/1000000;
		    				System.out.println("Finished simulating count with length=" + length + " in " + elapsedTime + "ms ");

						}catch(IOException e){
							e.printStackTrace();
						}
					}

				}

				long elapsedTime1 = (System.nanoTime() - startTime1)/1000000;
				System.out.println("BOT Finished simulating count in " + elapsedTime1 + "ms ");

			}
			else if(input==5){


				System.out.print("\nWord length to compare: ");
				int length = scan.nextInt();
				int probType;
				do{
					System.out.println("Probability Type: ");
					System.out.println(" 1) 1/2");
					System.out.println(" 2) 1/32");
					System.out.println(" 3) log2(x)");
					System.out.print("Option: ");
					probType = scan.nextInt();
				}while(probType!=1 && probType!=2 && probType!=3);

				try{
					HashMap<String,Integer> wordMap = loadCountFile(length,0);
					HashMap<String,Integer> wordMapProb = loadCountFile(length,probType);
					HashMap<String,Integer> wordMapCompare = new HashMap<String,Integer>();

					System.out.println("Comparing results...");

					if (probType==3){  //Log2
						int totalReal = 0;
						int totalProb = 0;
						int totalDiff = 0;
						for (Map.Entry<String,Integer> entry : wordMapProb.entrySet()) {
							String word = entry.getKey();
							int countProb = (int)Math.pow(2, entry.getValue());
							int diff = countProb - wordMap.get(word);
							totalReal += wordMap.get(word);
							totalProb += countProb;
							totalDiff += diff;
						}
						
						double avgError = totalDiff/(double)wordMap.size();
						double expected = totalReal; //((int)Math.floor(log2(size)))+1;
						double probCount = totalProb;
						double diff = totalDiff;
						double absError = Math.abs(diff);
						double relError;
						double accRatio;
						if(absError>expected){
							relError=100;
							accRatio=0;
						}
						else{	
							relError = (absError/expected)*100;
							accRatio = 100-relError; 
						}

						System.out.println("\n -------------- COMPARE RESULTS ---------------------------------");
						System.out.printf("| Total Count (Real): %10d | Total Count (Prob): %9d \n", totalReal, totalProb);
						System.out.printf("| Average Error (word): %8.2f | Relative error: %11.1f%%\n", avgError, relError);
						System.out.println(" ----------------------------------------------------------------");
						System.out.printf("| Total (Difference): %6d\n",totalDiff);
						System.out.printf("| Acc Ratio: %6.1f%%\n", accRatio);
						System.out.println(" ----------------------------------------------------------------\n");


					}else{
						double prob = probability[probType-1];
						int totalReal = 0;
						int totalProb = 0;
						int totalDiff = 0;

						for (Map.Entry<String,Integer> entry : wordMapProb.entrySet()) {
							String word = entry.getKey();
							int countProb = (int)(entry.getValue()/prob);
							int diff = countProb - wordMap.get(word);
							totalReal += wordMap.get(word);
							totalProb += countProb;
							totalDiff += diff;
						}
						
						double avgError = totalDiff/(double)wordMap.size();
						double expected = totalReal; //((int)Math.floor(log2(size)))+1;
						double probCount = totalProb;
						double diff = totalDiff;
						double absError = Math.abs(diff);
						double relError;
						double accRatio;
						if(absError>expected){
							relError=100;
							accRatio=0;
						}
						else{	
							relError = (absError/expected)*100;
							accRatio = 100-relError; 
						}


						System.out.println("\n -------------- COMPARE RESULTS ---------------------------------");
						System.out.printf("| Total Count (Real): %10d | Total Count (Prob): %9d \n", totalReal, totalProb);
						System.out.printf("| Average Error (word): %8.2f | Relative error: %11.1f%%\n", avgError, relError);
						System.out.println(" ----------------------------------------------------------------");
						System.out.printf("| Total (Difference): %6d\n",totalDiff);
						System.out.printf("| Acc Ratio: %6.1f%%\n", accRatio);
						System.out.println(" ----------------------------------------------------------------\n");
					}

				}catch(IOException e){
					e.printStackTrace();
				}

			}
			else if(input==0){
				System.out.println("Bye!! :'(");
			}
			else{
				System.out.println("Option " + input + " not available.");
			}
		
		}while(input!=0);
	}

	public static HashMap<String,Integer> loadCountFile(int wordLength, int probType) throws IOException{
		
		String fileName = WORD_COUNT_FILENAME[wordLength-1][probType];
		BufferedReader br = new BufferedReader(new FileReader(fileName)); //TODO LOAD

		HashMap<String,Integer> wordMap = new HashMap<String,Integer>();

		String line;
		while((line = br.readLine()) != null){
			String[] splitLine = line.split(" ");
			String word = splitLine[0];
			Integer count = Integer.parseInt(splitLine[1]);
			wordMap.put(word,count);
		}

		return wordMap;
	}

	public static int simCount(int size, double prob)
	{
		Random rand = new Random();
		int count = 0;
		for (int i=0;i<size;i++){
		 	if(rand.nextDouble() < prob){
				count++;
			}
		}
		return count;
	}

	public static int simCountLog2(int size)
	{
		Random rand = new Random();
		int count = 0;
		for (int i=0;i<size;i++) {
		 	if(rand.nextDouble() < 1/(double)Math.pow(2,count)) {
				count++;
			}
		}
		
		return count;
	}

	public static int calcResult(int size, int count){

		int expected = ((int)Math.floor(log2(size)))+1;

		int diff = count-expected;
		double absError = Math.abs(diff);
		double relError;
		double accRatio;

		if(absError>expected){
			relError=100;
			accRatio=0;
		}
		else{	
			relError = (absError/(double)expected)*100;
			accRatio = 100-relError; 
		}

		System.out.printf("SimCount: %6d |  Result: %6d | Expected value: %5d | Relative error: %5.1f%% | Acc Ratio: %5.1f%%\n", size, count, expected, relError, accRatio);

		return diff;
	}

	public static double logX(double a, double x){
		return Math.log(a) / Math.log(x);
	}

	public static double log2(double a){
		return logX(a,2);
	}

	public static double log2prob(int count){
		return 1/(double)Math.pow(2,count);
	}

	public static void generateDNAWords(){
		
		byte[] currentWord;
		//StringBuffer currentWord;
		int currentSize = 0;
		for (int i=0; i<wordSize; i++) {
			DNAwords.add(new ConcurrentHashMap<String, Integer>());			
		}

		for (int i=0; i<ADN_BYTE.length; i++) {
			DNAwords.get(0).put(String.valueOf((char)ADN_BYTE[i]), 0);
			wordCombCount[currentSize] = ADN_BYTE.length;
		}
		System.out.println("Generated " + ADN_BYTE.length + " words with length " + (currentSize+1));

		for (int i=1; i<wordSize; i++) {
			generate(DNAwords.get(i-1), DNAwords.get(i), i);
		}
	}

	private static void generate(ConcurrentHashMap<String, Integer> prevWordMap, ConcurrentHashMap<String, Integer> newWordMap, int size){

		int count = 0;
		byte[] currentWord=null;
		String prevWord;

		for (Map.Entry<String, Integer> entry : prevWordMap.entrySet()) {
			prevWord = entry.getKey();
			for (int j=0; j<ADN_BYTE.length; j++) {
				currentWord = new byte[size+1];
				System.arraycopy(prevWord.getBytes(), 0, currentWord, 0, prevWord.length());
				System.arraycopy(ADN_BYTE, j, currentWord, size, 1);
				newWordMap.put(new String(currentWord), 0);
				count++;
			}
		}
		wordCombCount[size] = count;
		System.out.println("Generated " + wordCombCount[size] + " words with length " + (size+1));
	}

	public static void countWord(String word){

		long startTime = System.nanoTime();
		int count = 0;
		int wLen = word.length();
		int wLen_1 = word.length()-1;
		int dnaFileLength_word = dnaFileLength-wLen+1;

		for (int i=0; i<dnaFileLength_word; i++) {
			boolean equal = true;
			for (int j=0; j<wLen; j++) {
				if(dnaFileByteArray[i+j]!=word.getBytes()[j]){
					equal = false;
					break;
				}
			}
			if (equal) {
				count++;
			}
		}

		long elapsedTime = (System.nanoTime() - startTime)/1000000;
		System.out.println("Found " + count + " occurrences of the word " + new String(word) + " in " + elapsedTime + "ms ");
	}
}

class CountWords extends Thread {

	private final ConcurrentHashMap<String,Integer> wordMap;
	private final int wordLength;
	private final byte[] dnaFile;
	private int dnaFileLength;
	private final int threads;
	private final ThreadPoolExecutor executor;
	private final String fileName;
	private final Double[] probability = {0.5, 0.03125};
	private final int probType;
	private FileWriter writer;
	private double[] probVals;
	private StringBuffer sb;
	private static final String FilenameDNA = "dna.fa";
	private static final byte[] ADN_BYTE = {"A".getBytes()[0], "C".getBytes()[0], "G".getBytes()[0], "T".getBytes()[0]};

	public CountWords(ConcurrentHashMap<String,Integer> wordMap, int wordLength, byte[] dnaFile, int dnaFileLength, int probType, String fileName, int threads){
		this.wordMap = wordMap;
		this.wordLength = wordLength;
		this.dnaFile = dnaFile;
		this.dnaFileLength = dnaFileLength;
		this.threads = threads;
		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
		this.fileName = fileName;
		this.probType = probType;
		this.probVals = new double[10000];
		for (int i=0; i<10000; i++) {
      		probVals[i] = 1/(double)Math.pow(2,i);
      	}
	}

    public void run() {

    	try{
	    	StringBuilder sb = new StringBuilder();

	    	List<Future<Boolean>> futs = new ArrayList<Future<Boolean>>();
	    	long startTime = System.nanoTime();

	    	dnaFileLength = (int) new File(FilenameDNA).length();

			BufferedReader br = new BufferedReader(new FileReader(FilenameDNA));
			StringBuffer builder = new StringBuffer();
			String line = br.readLine(); //First line is header

			if(line.contains(">")){
				int nextChar;
				Integer count = 0;
				while ((nextChar = br.read()) != -1) {
					if (Arrays.binarySearch(ADN_BYTE, 0, 3, (byte)nextChar)!=-1) {
						char ch = (char)nextChar;
						sb.append(ch);
						if ((count = wordMap.get(sb.toString())) != null) {
		    				wordMap.put(sb.toString(), ++count);				
						}
						if (sb.length()>=wordLength) {
		    				sb.deleteCharAt(0);	
						}	
					}
				}
				br.close();
			}
			else{
				System.out.println("Wrong file. Doesn't start with \">\"");
			}
	    	
	        long elapsedTime = (System.nanoTime() - startTime)/1000000000;
	    	System.out.println("Finished counting words with length=" + wordLength + " in " + elapsedTime + "s ");

	    	//Save result to file
			
			System.out.println("Saving count to file: " + fileName);
			FileWriter writer = new FileWriter(fileName); 
				
			for (Map.Entry<String, Integer> entry : wordMap.entrySet()) {
				writer.append(entry.getKey()); //Word
				writer.append(" ");
				writer.append(String.valueOf(entry.getValue())); //Count
				writer.append("\n");
			}
			writer.flush();
			System.out.println(fileName + " saved!");


		}catch(IOException ex){
			ex.printStackTrace();
		}
    }
}