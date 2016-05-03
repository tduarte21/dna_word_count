import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;

public class TAP_TP1_ex2
{
	private static int events[] = {10, 100, 1000, 10000};
	private static final int eventsSize = events.length;
	private static final Double[] probability = {0.5, 0.03125};
	private static int currentProb = 0;
	private static final int TREE_SIZE = 10000;
	private static final String[] TREE_FILENAME = {"tree_ex2_log2.csv"}; //tree1 is for 1/2 - tree2 is for 1/32 
	private static final Scanner scan = new Scanner(System.in);
	private static int simSize = 10000;
	private static int[][] resultCount = new int[4][10000]; //Count how many times each number is a result
	private static final boolean DEBUG = true;

	public static void main(String[] args){

		int input;

		do{
			System.out.println();
			System.out.println(" ---      Ex 2 - Approximate Counting       --- ");
			System.out.println("          with decreasing probability");
			System.out.println();
			System.out.println("   Please choose one option:");
			System.out.println("    1) Simulate count (prob. log2(x))");
			System.out.println("    2) Set simulation size ("+simSize+")");
			System.out.println("    3) Check tree value from file (Prob. Distribution)");
			System.out.println("    4) Compare theoric/real value (Prob. Distribution)");
			System.out.println("    0) Exit");
			System.out.println(" ---------------------------------------------- ");
			
			System.out.print("Option: ");
			
			input = scan.nextInt();	

			if(input==1){
				resultCount = new int[4][10000];
				File treeFile = new File(TREE_FILENAME[0]);
				if(!treeFile.exists()){
					System.out.println("Tree file not detected, generating new file.");
					generateTreeFile(probability[0], TREE_SIZE, TREE_FILENAME[0]);
				}
				simulator(simSize, resultCount);
			}
			else if(input==2){
				System.out.print("New simulation size: ");
				simSize = scan.nextInt();
			}
			else if(input==3){
				System.out.print("N=");
				int n = scan.nextInt();
				System.out.print("J=");
				int j = scan.nextInt();

				System.out.println("Reading file...");
				System.out.println("Value="+readFileLine(n, TREE_FILENAME[0])[j]);
			}
			else if(input==4){
				int n;
				do{
					System.out.print("Choose event size \n 1. 10 \n 2. 100 \n 3. 1000 \n 4. 10000 \n -> ");
					n = scan.nextInt();
				}while(n!=1 && n!=2 && n!=3 && n!=4);

				System.out.print("Insert count value to compare\n -> ");
				int j = scan.nextInt();

				double real = resultCount[n-1][j]/(double)simSize;
				double theoric = readFileLine((events[n-1]-1), TREE_FILENAME[0])[j-1].doubleValue();
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

	public static void simulator(int simSize, int[][] countMatrix)
	{
		double counts[] = new double[simSize];
		double mean[] = new double[4];
		double variance[] = new double[4];
		double prob;

		for (int i=0;i<simSize;i++) {
			if(DEBUG)
				System.out.println("----------- SIMULATION " + (i+1) + " ----------------");

			for (int j=0; j<4; j++) {
				int count = simCount(events[j]);
				countMatrix[j][count]++;
				mean[j] += count;
				double diff = calcResult(events[j], count);
				variance[j] += Math.pow(diff,2);
			}
		}

		for (int i=0; i<4; i++) {
			mean[i] = mean[i]/(double)simSize;
			variance[i] = variance[i]/(double)simSize;
		}

		System.out.println("-------------- RESULTS -------------------");
		for (int i=0;i<4;i++) {
			System.out.printf("SimCount: %6d | Prob: Log2(2,Count) | Mean Count: %4.1f | Variance: %3.1f | Std. Dev.: %3.1f\n", events[i], mean[i], variance[i], Math.sqrt(variance[i]));
		}

		System.out.println();
		for (int i=0;i<4;i++) {

			double expected = events[i]; //((int)Math.floor(log2(size)))+1;
			double probCount = Math.pow(2,(int)mean[i]+1);
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
			System.out.printf("SimCount: %6d | Probability Count: %6.0f | Relative error: %6.1f%% | Acc Ratio: %6.1f%%\n", events[i], probCount, relError, accRatio);
		}
		System.out.println("------------------------------------------");

	}

	public static int simCount(int size)
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

	public static double calcResult(int size, int count){

		double expected = ((int)Math.floor(log2(size)))+1;

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

		System.out.printf("SimCount: %6d |  Result: %6d | Expected value: %5.1f | Relative error: %5.1f%% | Acc Ratio: %5.1f%%\n", size, count, expected, relError, accRatio);

		return diff;
	}

	public static double logX(double a, double x){
		return Math.log(a) / Math.log(x);
	}

	public static double log2(double a){
		return logX(a,2);
	}


	public static void generateTreeFile(double p, int size, String treeFileName){
		
		try{

			final MathContext MC = new MathContext(16);
				
	      	FileWriter writer = new FileWriter(treeFileName);
	      	BigDecimal[] previousLine = new BigDecimal[1];
	      	BigDecimal[] currentLine = null;

	      	BigDecimal[] probValP = new BigDecimal[TREE_SIZE];
			BigDecimal[] probVal_P = new BigDecimal[TREE_SIZE];

	      	for (int i=0; i<TREE_SIZE; i++) {
	      		probValP[i] = new BigDecimal(1/(double)Math.pow(2,i));
	      		probVal_P[i] = new BigDecimal(1-(1/(double)Math.pow(2,i)));
	      	}

			previousLine[0] = new BigDecimal(1.0);
			writer.append(String.valueOf(previousLine[0]));
			writer.append(",");
			writer.append("\n");

	      	writer.flush();
			System.out.println("Generating file, line 1/"+size);

			for (int n=1; n<size; n++) {
				
				currentLine = new BigDecimal[n+1];
				currentLine[0] = previousLine[0].multiply(probValP[1], MC);
				writer.append(String.valueOf(currentLine[0]));
				writer.append(",");

				for (int k=1; k<n; k++){
					currentLine[k] = previousLine[k-1].multiply(probValP[k]    , MC)
								.add(previousLine[k]  .multiply(probVal_P[k+1] , MC));
					writer.append(String.valueOf(currentLine[k]));
					writer.append(",");

				}
				currentLine[n] = previousLine[n-1].multiply(probValP[n], MC);
				writer.append(String.valueOf(currentLine[n]));
				writer.append(",");
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

