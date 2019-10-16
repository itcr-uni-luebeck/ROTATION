package rotation;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Class for hash and similarity calculation
 * 
 * @author Sebastian Germer
 */
public class MinHash {
	/**
	 * Method to generate 3-grams out of a string
	 * 
	 * @param input
	 *            The string to split
	 * @return An {@link ArrayList} with the shingles.
	 */
	private static ArrayList<String> shingles(String input) {
		int length = input.length();
		ArrayList<String> shingles = new ArrayList<>();
		if (length < 3) {
			shingles.add(input);
			return shingles;
		}

		for (int i = 0; i < length; i++) {
			if (i + 3 <= length) {
				shingles.add(input.substring(i, i + 3));
			}
		}
		return shingles;
	}

	/**
	 * Method to calculate the hashes of source and target descriptions.
	 * 
	 * @param sourceDescriptions
	 *            The decriptions of the sources (usually one, but who knows)
	 * @param targetDescriptions
	 *            The descriptions of the targets
	 * @return An {@link ArrayList} with the hash lists for source and target
	 */
	public ArrayList<ArrayList<Integer>> calcHash(ArrayList<String> sourceDescriptions,
			ArrayList<String> targetDescriptions) {

		HashSet<String> sourceShingles = new HashSet<>();
		HashSet<String> targetShingles = new HashSet<>();
		for (String sDesc : sourceDescriptions) {
			sourceShingles.addAll(shingles(sDesc));
		}
		for (String tDesc : targetDescriptions) {
			targetShingles.addAll(shingles(tDesc));
		}

		try (Scanner hashfile = new Scanner(this.getClass().getResource("Hashes.txt").openStream())) {
			ArrayList<Integer> hashesSource = new ArrayList<>();
			ArrayList<Integer> hashesTarget = new ArrayList<>();
			while (hashfile.hasNext()) {
				HashFunction hash = Hashing.murmur3_32(hashfile.nextInt());
				int min = Integer.MAX_VALUE;
				for (String shingle : sourceShingles) {
					HashCode code = hash.hashString(shingle, Charset.defaultCharset());
					min = Math.min(min, code.asInt());
				}
				hashesSource.add(min);
				min = Integer.MAX_VALUE;
				for (String shingle : targetShingles) {
					HashCode code = hash.hashString(shingle, Charset.defaultCharset());
					min = Math.min(min, code.asInt());
				}
				hashesTarget.add(min);
			}
			ArrayList<ArrayList<Integer>> result = new ArrayList<>();
			result.add(hashesSource);
			result.add(hashesTarget);
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<ArrayList<Integer>>();
		}
	}

	/**
	 * Method to calculate the Jaccard-similarity of two hash-arrays of the same
	 * length.
	 * 
	 * @param hash1
	 * @param hash2
	 * @return The similarity of both arrays.
	 */
	public double calcJaccardSim(ArrayList<Integer> hash1, ArrayList<Integer> hash2) {
		if (hash1.isEmpty() || hash2.isEmpty() || hash1.size() != hash2.size()) {
			return 0;
		} else {
			double numberSim = 0.0;
			Iterator<Integer> it1 = hash1.iterator();
			Iterator<Integer> it2 = hash2.iterator();
			while (it1.hasNext()) {
				if ((it1.next()).equals(it2.next())) {
					numberSim++;
				}
			}
			return numberSim / (double) (hash1.size());
		}
	}
}