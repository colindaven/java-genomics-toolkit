package edu.unc.genomics.wigmath;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.ngs.Autocorrelation;
import edu.unc.utils.FloatHistogram;

public class ValueDistribution extends CommandLineTool {

	private static final Logger log = Logger.getLogger(Autocorrelation.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-l", "--min"}, description = "Minimum bin value")
	public Float min;
	@Parameter(names = {"-h", "--max"}, description = "Maximum bin value")
	public Float max;
	@Parameter(names = {"-n", "--bins"}, description = "Number of bins")
	public int numBins = 40;
	@Parameter(names = {"-o", "--output"}, description = "Output file")
	public Path outputFile;
	
	public void run() throws IOException {
		log.debug("Generating histogram of Wig values");
		if (min == null) {
			min = (float) inputFile.min();
		}
		if (max == null) {
			max = (float) inputFile.max();
		}
		FloatHistogram hist = new FloatHistogram(numBins, min, max);
		
		for (String chr : inputFile.chromosomes()) {
			int start = inputFile.getChrStart(chr);
			int stop = inputFile.getChrStop(chr);
			log.debug("Processing chromosome " + chr + " region " + start + "-" + stop);
			
			// Process the chromosome in chunks
			int bp = start;
			while (bp < stop) {
				int chunkStart = bp;
				int chunkStop = Math.min(bp+DEFAULT_CHUNK_SIZE-1, stop);
				log.debug("Processing chunk "+chr+":"+chunkStart+"-"+chunkStop);
				
				Iterator<WigItem> iter;
				try {
					iter = inputFile.query(chr, chunkStart, chunkStop);
				} catch (WigFileException e) {
					e.printStackTrace();
					throw new RuntimeException("Error getting data from Wig file!");
				}
				while (iter.hasNext()) {
					WigItem item = iter.next();
					for (int i = item.getStartBase(); i <= item.getEndBase(); i++) {
						if (i >= chunkStart && i <= chunkStop) {
							hist.addValue(item.getWigValue());
						}
					}
				}
				
				bp = chunkStop + 1;
			}
		}
		
		if (outputFile != null) {
			log.debug("Writing to output file");
			try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
				writer.write(hist.toString());
			}
		} else {
			System.out.println(hist.toString());
		}
		
		inputFile.close();
	}
	
	public static void main(String[] args) throws IOException, WigFileException {
		new ValueDistribution().instanceMain(args);
	}

}
