import java.io.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Client {
    public static void main(String[] args) {
        //get exit message
        BlockingQueue<String> q = new ArrayBlockingQueue<>(1024);

        //make connection to port 2520
        try (Socket socket = new Socket("127.0.0.1", 2520)) {

            Listen listener = new Listen(socket, q);
            new Thread(listener).start();
            // writing to server
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // object of scanner class
            Scanner sc = new Scanner(System.in);
            String line = null;

            while (!"Stop".equalsIgnoreCase(line)) {
                // reading from user
                line = sc.nextLine();

                // sending the user input to server
                out.println(line);
                out.flush();
            }
            //wait until all processes stop
            while(!(line = q.take()).matches("All processes stopped.")){

            }
            // closing the scanner object
            sc.close();
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }
    //Thread for listening
    private static class Listen implements Runnable{
        private final Socket listensocket;
        private BlockingQueue<String> queue;

        public Listen(Socket socket, BlockingQueue<String> q)
        {
            this.listensocket = socket;
            this.queue=q;
        }

        public void run(){
            BufferedReader in = null;
            try {
                //input of client
                in = new BufferedReader(new InputStreamReader(listensocket.getInputStream()));
                String line;
                while (!(line = in.readLine()).matches("All processes stopped.")) {
                    System.out.println(line);
                }
                System.out.println(line);
                queue.put("All processes stopped.");
            }
            catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
