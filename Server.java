import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

// Server class
class Server {
    public static void main(String[] args) {
        ServerSocket server = null;
        int client_id = 0;

        try {
            // server on port 2520
            server = new ServerSocket(2520);
            server.setReuseAddress(true);

            //Blocking Queue for sending messages between clients
            BlockingQueue<String> q = new ArrayBlockingQueue<>(1024);
            BlockingQueue<String> close = new ArrayBlockingQueue<>(16);

            //Blocking Queue for terminating everything
            BlockingQueue<Integer> term = new ArrayBlockingQueue<>(32);
            int once = 1;
            int closeserv = 0;
            //find client
            while (true) {

                //accept request
                Socket client = server.accept();
                client_id++;

                // connected
                System.out.println("New client connected " + client.getInetAddress().getHostAddress());


                // create a new thread object
                Producer listener = new Producer(client, q, client_id, term);
                Consumer writer = new Consumer(client, q, client_id, term, close);
                new Thread(listener).start();
                new Thread(writer).start();
                closeserv++;

                if(once == 1){
                    close.put("temp");
                    once++;
                }
                while(closeserv == 4){
                    Thread.sleep(600000);
                    closeserv++;
                }
                //close server socket
                while((close.peek()).matches("Stop")){
                    server.close();
                    System.exit(0);
                }
            }
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            if (server != null) {
                try {
                    server.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    //Thread for listening
    public static class Producer implements Runnable{
        private final Socket producersocket;
        private BlockingQueue<String> queue;
        private BlockingQueue<Integer> termin;
        private int client_id;

        public Producer(Socket socket, BlockingQueue<String> q, int client_id, BlockingQueue<Integer> term)
        {
            this.producersocket = socket;
            this.queue=q;
            this.client_id = client_id;
            this.termin = term;
        }

        public void run(){
            BufferedReader in = null;
            int stopcounter = 0;
            try {
                //input of client
                in = new BufferedReader(new InputStreamReader(producersocket.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("Sent from " + client_id + " " + line);
                    //matches so that we dont have ex: asdfasd Send i

                    if(line.matches("Send " + client_id + "(.*)"))
                        continue;

                    if(line.matches("Stop")){
                        //counter to make sure all of them dead before exiting
                        if (!termin.isEmpty()) {
                            stopcounter = termin.take();
                        }
                        stopcounter++;
                        termin.put(stopcounter);
                        queue.put("Stop");
                        break;
                    }

                    for(int i=1; i <= 4; i++){
                        if(line.matches("Send " + i + "(.*)")){
                            queue.put(line);
                        }

                        else if(line.matches("Send 0(.*)")){

                            //only send to the other clients
                            if(i==client_id){
                                continue;
                            }

                            queue.put("Send " + i + line.substring(6));
                        }

                        else
                            continue;
                    }
                }
            }
            catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    //Thread for writing
    public static class Consumer extends Thread implements Runnable {
        private final Socket consumersocket;
        private BlockingQueue<String> queue;
        private int client_id;
        private BlockingQueue<Integer> termin;
        private BlockingQueue<String> close;

        public Consumer(Socket socket, BlockingQueue<String> q, int client_id, BlockingQueue<Integer> term, BlockingQueue<String> close)
        {
            this.consumersocket = socket;
            this.client_id = client_id;
            this.queue = q;
            this.termin = term;
            this.close = close;
        }

        public void run(){
            PrintWriter out = null;
            int stopcount = 0;
            int finalizer = 0;
            int finalmessage[] = {1, 2, 3 , 4};
            try {
                //output of client
                out = new PrintWriter(consumersocket.getOutputStream(), true);
                String line;
                out.println("You are Client #" + client_id);
                //will check all inputs from client until stop is inputted
                //!(line = queue.take()).matches("Stop")
                while ((line = queue.take()) != null) {
                    //terminator
                    if(line.matches("Stop")){
                        stopcount = termin.take();
                        if(4 <= stopcount && stopcount <= 8){

                            //send termination message to all
                            stopcount++;
                            termin.put(stopcount);
                            //to make sure all get sent the message.
                            termin.put(client_id);
                            queue.put(line);
                            queue.put(line);
                            queue.put(line);
                            //for main server
                            close.take();
                            close.put(line);
                            out.println("All processes stopped.");
                            break;
                        }
                        termin.put(stopcount);
                        continue;
                    }

                    //make sure it sends the message to the right client
                    for(int i=1; i <=4; i++){
                        //only accept proper form
                        if(client_id == i){
                            //for send all
                            if(!line.contains("Send " + i)){
                                continue;
                            }
                            else{
                                //send message
                                out.println(line);
                                System.out.println("Sent to " + client_id + " " + line);
                                //no infinite loops
                                break;
                            }
                        }
                        else{
                            //to go to next thread if there arent any here
                            if(i==4)
                                queue.put(line);
                            continue;
                        }
                    }
                }
                //close server and clients

            }
            catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
