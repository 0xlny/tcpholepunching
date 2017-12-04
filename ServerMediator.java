/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author user
 */
public class ServerMediator {

    private int tcpDiscussionPort = 9000;
    private int tcpPunchPort = 9001;

    private BufferedReader inConnectA, inPunchA;
    private BufferedOutputStream outConnectA, outPunchA;

    private BufferedReader inConnectB, inPunchB;
    private BufferedOutputStream outConnectB, outPunchB;

    private ServerSocket socketConnect, socketPunch;

    private Socket clientAConnect, clientAPunch, clientBConnect, clientBPunch;


    private boolean readClientA = false;
    private String clientAIp = "";
    private String clientAPort = "";
    private String clientAPortLocal = "";

    private boolean readClientB = false;
    private String clientBIp = "";
    private String clientBPort = "";
    private String clientBPortLocal = "";

    //Constructor using default tcp discussion/punch ports
    public ServerMediator() {
        try {
            runServer();
        } catch (IOException ex) {
            Logger.getLogger(ServerMediator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //Constructor specify tcp discussion/punch ports
    public ServerMediator(int userTcpPort, int userUdpPort) {
        this.tcpDiscussionPort = userTcpPort;
        this.tcpPunchPort = userUdpPort;
        try {
            runServer();
        } catch (IOException ex) {
            Logger.getLogger(ServerMediator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {//Give args
            new ServerMediator(Integer.parseInt(args[0].trim()), Integer.parseInt(args[1].trim()));
        } else {//Give no args
            new ServerMediator();
        }
    }

    //Run server listening clients
    void runServer() throws IOException {
        //Create Server Socket for accepting Client TCP connections

        System.out.println("Server started with ports, TCP connection: " + tcpDiscussionPort + " TCP: " + tcpPunchPort);

        runDiscussionServer();
        runPunchServer();
    }

    private void runDiscussionServer(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    socketConnect = new ServerSocket(tcpDiscussionPort);

                    System.out.println("Waiting for Client A");

                    //Accept first client connection
                    clientAConnect = socketConnect.accept();
                    System.out.println("Client 1 connected " + clientAConnect.getInetAddress() + " " + clientAConnect.getPort());

                    //Create input and output streams to read/write messages for CLIENT A
                    inConnectA = new BufferedReader(new InputStreamReader(clientAConnect.getInputStream()));
                    outConnectA = new BufferedOutputStream(clientAConnect.getOutputStream());

                    System.out.println("Waiting for Client B");

                    //Accept second client connection
                    clientBConnect = socketConnect.accept();
                    System.out.println("Client 2 connected " + clientBConnect.getInetAddress() + " " + clientBConnect.getPort());

                    //Create input and output streams to read/write messages for CLIENT B
                    inConnectB = new BufferedReader(new InputStreamReader(clientBConnect.getInputStream()));
                    outConnectB = new BufferedOutputStream(clientBConnect.getOutputStream());
                }catch (IOException ioe){
                    ioe.printStackTrace();
                }
            }
        }).start();
    }

    private void runPunchServer(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    socketPunch = new ServerSocket(tcpPunchPort);

                    System.out.println("Waiting for Client A punch");

                    //Accept first client connection
                    clientAPunch = socketPunch.accept();
                    clientAIp = ((InetSocketAddress)clientAPunch.getRemoteSocketAddress()).getAddress().getHostAddress().trim();
                    clientAPortLocal = String.valueOf(clientAPunch.getPort());
                    clientAPort = String.valueOf(clientAPunch.getLocalPort());

                    System.out.println("Client A punch " + clientAPunch.getInetAddress() + " " + clientAPunch.getPort());

                    //Create input and output streams to read/write messages for CLIENT A
                    inPunchA = new BufferedReader(new InputStreamReader(clientAPunch.getInputStream()));
                    outPunchA = new BufferedOutputStream(clientAPunch.getOutputStream());


                    System.out.println("Waiting for Client B punch");
                    //Accept second client connection
                    clientBPunch = socketPunch.accept();
                    clientBIp = ((InetSocketAddress)clientBPunch.getRemoteSocketAddress()).getAddress().getHostAddress().trim();
                    clientBPortLocal = String.valueOf(clientBPunch.getPort());
                    clientBPort = String.valueOf(clientBPunch.getLocalPort());

                    System.out.println("Client 2 punch " + clientBPunch.getInetAddress() + " " + clientBPunch.getPort());

                    //Create input and output streams to read/write messages for CLIENT B
                    inPunchB = new BufferedReader(new InputStreamReader(clientBPunch.getInputStream()));
                    outPunchB = new BufferedOutputStream(clientBPunch.getOutputStream());


                    //Once the two clients have punched
                    proceedInfosExchange();
                }catch (IOException ioe){
                    ioe.printStackTrace();
                }
            }
        }).start();
    }

    private void proceedInfosExchange() throws IOException{
        /**
         *
         * *** FIRST CLIENT'S PUBLIC IP AND PORTS ****
         */
        while (!readClientA) {
            String message = inPunchA.readLine();
            if (message.trim().equals("one")) {
                readClientA = true;
                System.out.println("Initial punch message from CLIENT A: " + message);
            }
        }

        System.out.println("******CLIENT A IP AND PORT DETECTED " + clientAIp + ":" +  clientAPortLocal + "->" + clientAPort + " *****");

        /**
         *
         * *** SECOND CLIENT'S PUBLIC IP AND PORTS ****
         */
        while (!readClientB) {
            String message = inPunchB.readLine();   //Get Data from tcp packet into a string
            if (message.trim().equals("two")) {
                readClientB = true;
                System.out.println("Initial punch message from CLIENT B: " + message);
            }
        }
        System.out.println("******CLIENT B IP AND PORT DETECTED " + clientBIp + ":" +  clientBPortLocal + "->" + clientBPort + " *****");

        /*
         !!!!!!!!!!!CRITICAL PART!!!!!!!!
         The core of hole punching depends on this part.
         */

        System.out.println("***** Exchanging public IP and port between the clients *****");
        while (true) {
            String string = clientAIp + "~~" + clientAPort + "~~" + clientAPortLocal + "~~" + clientBIp + "~~" + clientBPort + "~~" + clientBPortLocal;
            outConnectA.write(string.getBytes());      //SENDING CLIENT B's public IP & PORT TO CLIENT A
            outConnectA.write('\n');
            outConnectA.flush();

            String string1 = clientBIp + "~~" + clientBPort + "~~" + clientBPortLocal + "~~" + clientAIp + "~~" + clientAPort + "~~" + clientAPortLocal;
            outConnectB.write(string1.getBytes());     //SENDING CLIENT A's public IP & PORT TO CLIENT B
            outConnectB.write('\n');
            outConnectB.flush();
        }
    }
}
