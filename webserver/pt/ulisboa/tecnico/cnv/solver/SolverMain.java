package pt.ulisboa.tecnico.cnv.solver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONArray;

public class SolverMain {

    public static void main(final String[] args) {
        solve(args);
    }

    public static JSONArray solve(final String[] args) {
        // Get user-provided flags.
        final SolverArgumentParser ap = new SolverArgumentParser(args);

        // Create solver instance from factory.
        final Solver s = SolverFactory.getInstance().makeSolver(ap);

        return s.solveSudoku();
    }
}
