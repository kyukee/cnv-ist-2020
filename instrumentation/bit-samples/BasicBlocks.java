	/* ICount.java
 * Sample program using BIT -- counts the number of instructions executed.
 *
 * Copyright (c) 1997, The Regents of the University of Colorado. All
 * Rights Reserved.
 *
 * Permission to use and copy this software and its documentation for
 * NON-COMMERCIAL purposes and without fee is hereby granted provided
 * that this copyright notice appears in all copies. If you wish to use
 * or wish to have others use BIT for commercial purposes please contact,
 * Stephen V. O'Neil, Director, Office of Technology Transfer at the
 * University of Colorado at Boulder (303) 492-5647.
 */


import BIT.highBIT.*;
import pt.ulisboa.tecnico.cnv.data.LocalDatabase;

import java.io.*;
import java.util.*;
import java.math.BigInteger;


public class BasicBlocks {
    private static PrintStream out = null;
    private static Map<Long,BigInteger> b_count = new HashMap<Long, BigInteger>();


    /* main reads in all the files class files present in the input directory,
     * instruments them, and outputs them to the specified output directory.
     */
    public static void main(String argv[]) {
        File file_in = new File(argv[0]);
        String infilenames[] = file_in.list();

        for (int i = 0; i < infilenames.length; i++) {
            String infilename = infilenames[i];
            if (infilename.endsWith(".class")) {
				// create class info object
				ClassInfo ci = new ClassInfo(argv[0] + System.getProperty("file.separator") + infilename);

                // loop through all the routines
                // see java.util.Enumeration for more information on Enumeration class
                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();

                    for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
                        BasicBlock bb = (BasicBlock) b.nextElement();
                        bb.addBefore("BasicBlocks", "bbCount", new Integer(1));
                    }

                    if (infilename.endsWith("SolverMain.class")) {
                        routine.addAfter("BasicBlocks", "printBasicBlocks", ci.getClassName());
                    }
                }
                ci.write(argv[1] + System.getProperty("file.separator") + infilename);
            }
        }
    }

    private static synchronized BigInteger getBBCount(long threadID) {
        BigInteger bb_value;

        bb_value = b_count.get(threadID);

        if (bb_value == null)
            bb_value = new BigInteger("0");

        return bb_value;
    }

    public static synchronized void printBasicBlocks(String foo) {
        long threadID = Thread.currentThread().getId();

        BigInteger bb_value = getBBCount(threadID);
        LocalDatabase.setBBCount(threadID, bb_value);

        b_count.put(threadID, BigInteger.valueOf(0));
    }

    public static synchronized void bbCount(int incr) {
        long threadID = Thread.currentThread().getId();

        BigInteger bb_value = getBBCount(threadID);
        BigInteger bb_new_value = bb_value.add(BigInteger.valueOf(incr));

        b_count.put(threadID, bb_new_value);
    }

}
