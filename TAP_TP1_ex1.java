import java.util.*;
import java.lang.*;
import java.math.*;
import java.io.*;

public class TAP_TP1_ex1
{
	private static int events[] = {10, 100, 1000, 10000};
	private static final int eventsSize = events.length;
	private static final Double[] probability = {0.5, 0.03125};
	private static int currentProb = 0;
	private static final int TREE_SIZE = 10000;
	private static final String[] TREE_FILENAME = {"tree_ex1_1-2.csv", "tree_ex1_1-32.csv"}; //tree1 is for 1/2 - tree2 is for 1/32 
	private static final Scanner scan = new Scanner(System.in);
	private static int simSize = 10000;
	private static int[][] resultCount1 = new int[4][10000]; //Count how many times each number is a result
	private static int[][] resultCount2 = new int[4][10000]; //resultCount1 is for 1/2  and resultCount2 is for 1/32
	private static final boolean DEBUG = true;

	public static void main(String[] args){

		int input;

		do{
			System.out.println();
			System.out.println(" ---      Ex 1 - Approximate Counting       ---");
			System.out.println("             with probability 1/x\n");
			System.out.println("   Please choose one option:");
			System.out.println("    1) Simulate count (prob. 1/2)");
			System.out.println("    2) Simulate count (prob. 1/32)");
			System.out.println("    3) Set simulation size ("+simSize+")");
			System.out.println("    4) Check tree value from file (Prob. Distribution)");
			System.out.println("    5) Compare theoric/real value (Prob. Distribution)");
			System.out.println("    0) Exit");
			System.out.println(" ---------------------------------------------- ");
			
			System.out.print("Option: ");
			
			input = scan.nextInt();	
					
			if(input==1){
				resultCount1 = new int[4][10000];
				currentProb=0;
				File treeFile = new File(TREE_FILENAME[currentProb]);
				if(!treeFile.exists()){
					System.out.println("Tree file not detected, generating new file.");
					generateTreeFile(probability[0], TREE_SIZE, TREE_FILENAME[currentProb]);
				}
				simulator(probability[currentProb], simSize, resultCount1);
			}
			else if(input==2){
				resultCount2 = new int[4][10000];
				currentProb=1;
				File treeFile = new File(TREE_FILENAME[currentProb]);
				if(!treeFile.exists()){
					System.out.println("Tree file not detected, generating new file.");
					generateTreeFile(probability[1], TREE_SIZE, TREE_FILENAME[currentProb]);
				}
				simulator(probability[currentProb], simSize, resultCount2);
			}
			else if(input==3){
				System.out.print("New simulation size: ");
				simSize = scan.nextInt();
			}
			else if(input==4)
			{
				int p;
				do{
					System.out.print("Choose probability \n 1. 1/2 \n 2. 1/32 \n -> ");
					p = scan.nextInt();
				}while(p!=1 && p!=2);
		
				System.out.print("N=");
				int n = scan.nextInt();
				System.out.print("J=");
				int j = scan.nextInt();

				System.out.println("Reading file...");
				System.out.println("Value="+readFileLine(n, TREE_FILENAME[p-1])[j]);
			}
			else if(input==5)
			{
				int p;
				do{
					System.out.print("Choose probability \n 1. 1/2 \n 2. 1/32 \n -> ");
					p = scan.nextInt();
				}while(p!=1 && p!=2);
				
				int n;
				do{
					System.out.print("Choose event size \n 1. 10 \n 2. 100 \n 3. 1000 \n 4. 10000 \n -> ");
					n = scan.nextInt();
				}while(n!=1 && n!=2 && n!=3 && n!=4);

				System.out.print("Insert count value to compare\n -> ");
				int j = scan.nextInt();

				double real;
				if(p==1){
					real = resultCount1[n-1][j]/(double)simSize;
				}
				else{
					real = resultCount2[n-1][j]/(double)simSize;
				}

				double theoric = readFileLine((events[n-1]-1), TREE_FILENAME[p-1])[j-1].doubleValue();
				double diff = real-theoric;

				System.out.printf("Real Value (Simulation)  -> %6.4f%%\n", real);
				System.out.printf("Theoric Value (Table)    -> %6.4f%%\n", theoric);
				System.out.printf("Difference (real-theoric)-> %6.4f%%\n", diff);
			}
			else if(input==0){
				System.out.println("Bye!! :'(");
			}
			else{
				System.out.println("Option "+ input + " not available.");
			}
		
		}while(input!=0);
	}

	public static void simulator(double prob, int simSize, int[][] countMatrix){

		double counts[] = new double[simSize];
		double mean[] = new double[4];
		double variance[] = new double[4];

		for (int i=0;i<simSize;i++) {
			if(DEBUG)
				System.out.println("----------- SIMULATION " + (i+1) + " ----------------");

			for (int j=0; j<4; j++) {
				int count = simCount(events[j], prob);
				countMatrix[j][count]++;
				mean[j] += count;
				double diff = calcResult(events[j], prob, count);
				variance[j] += Math.pow(diff,2);
			}
		}

		for (int i=0; i<4; i++) {
			mean[i] = mean[i]/(double)simSize;
			variance[i] = variance[i]/(double)simSize;
		}
	


		System.out.println("\n-------------- RESULTS -------------------");
		for (int i=0;i<4;i++) {
			System.out.printf("SimCount: %6d | Prob: %5.2f%% | Mean Count: %6.1f | Variance: %6.1f | Std. Dev.: %4.1f\n", events[i], prob*100, mean[i], variance[i], Math.sqrt(variance[i]));
		}
		System.out.println();
		for (int i=0;i<4;i++) {

			double expected = events[i];
			double probCount = mean[i]/prob;
			double diff = probCount-events[i];
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
			System.out.printf("SimCount: %6d | Probability Count: %6.0f | Relative error: %5.1f%% | Acc Ratio: %5.1f%%\n", events[i], probCount, relError, accRatio);
		}
		System.out.println("------------------------------------------");

	}

	public static void generateTreeFile(double p, int size, String treeFileName){
		
		try{
			
			double _p = 1-p;
			BigDecimal bigP = new BigDecimal(p);
			BigDecimal big_P = new BigDecimal(_p);
	      	FileWriter writer = new FileWriter(treeFileName);
	      	BigDecimal[] previousLine = new BigDecimal[1];
	      	BigDecimal[] currentLine = null;

	      	previousLine[0] = new BigDecimal(1.0);
	      	writer.append(String.valueOf(previousLine[0]));
	      	writer.append("\n");
	      	writer.flush();
			System.out.println("Generating file, line 1/"+size);

			for (int n=1; n<size; n++) {
				
				currentLine = new BigDecimal[n+1];
				
				currentLine[0] = previousLine[0].multiply(big_P, MathContext.DECIMAL32);
				writer.append(String.valueOf(currentLine[0]));
				writer.append(",");

				for (int j=1; j<n; j++){

					currentLine[j] = previousLine[j].multiply(big_P, MathContext.DECIMAL32).add(previousLine[j-1].multiply(bigP, MathContext.DECIMAL32));
					writer.append(String.valueOf(currentLine[j]));
					writer.append(",");

				}
				currentLine[n] = previousLine[n-1].multiply(bigP, MathContext.DECIMAL32);
				writer.append(String.valueOf(currentLine[n]));
				writer.append("\n");
				
				if((n+2)%1000 == 1){
					System.out.println("\nGenerating file, line " + (n+1) + "/" + size);
					writer.flush();
				}
				else if((n+2)%20 == 1){
					System.out.print(".");
				}

				previousLine = currentLine;

			}
			writer.flush();
			writer.close();
			System.out.println("Done! " + treeFileName + " generated.");
	    }        
	    catch(Exception e){
	      	e.printStackTrace();
	    }

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

	public static double calcResult(int size, double prob, int count){

		double expected = size*prob;

		double diff = count-expected;
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

		if(DEBUG)
			System.out.printf("SimCount: %6d |  Result: %6d | Expected value: %5.1f | Relative error: %5.1f%% | Acc Ratio: %5.1f%%\n", size, count, expected, relError, accRatio);

		return diff;
	}

	public static BigDecimal[] readFileLine(int n, String treeFileName){
		BufferedReader br = null;
		String line;
		String split = ",";
		int count = 0;
		String[] splitLine = null;
		Double[] nLineValues = new Double[10000];
		BigDecimal[] nLineValuesDecimal = new BigDecimal[10000];

		try{
			br = new BufferedReader(new FileReader(treeFileName));
			
			while((line = br.readLine()) != null){
			
				if(count == n){
					splitLine = line.split(split);
					break;
				}
				count++;
			}
			if(splitLine!=null){
				for (int i=0;i<splitLine.length; i++) {
					nLineValuesDecimal[i] = new BigDecimal(splitLine[i]);
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return nLineValuesDecimal;

	}
}
