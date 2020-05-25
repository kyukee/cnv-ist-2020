package pt.ulisboa.tecnico.cnv.autoscaler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class AutoScalerServer {

    private static AutoScalerMain autoscaler;

    private static final int instance_port = 8080;

    public static void main(final String[] args) throws Exception {

        final HttpServer server = HttpServer.create(new InetSocketAddress(instance_port), 0);

        // server.createContext("/sudoku", new MyHandler());

        // be aware! infinite pool of threads!
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        autoscaler = new AutoScalerMain();

        System.out.println("*******************************************************");
        System.out.println(new java.util.Date());
        System.out.println("Started load autoscaler on address: " + server.getAddress().toString());
        System.out.println("*******************************************************");

        autoscaler.autoScale();
    }

}
