/*-----------------------------------------------------------------------------

GORAN SOMIC
CSC 435--distributed systems
3/8/2020
HOST-SERVER ASSIGNMENT


  -------------------------------------------------------------------------------*/


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;



class AgentWorker extends Thread {

    Socket sock; //socket as usual for connection
    agentHolder parentAgentHolder;//confused by role of agent holder, realize it holds state but
    //still somewhat fuzzy
    int localPort; //port used


    AgentWorker (Socket ss, int prt, agentHolder x) {
        sock = ss;
        localPort = prt;
        parentAgentHolder = x;
    }
    //implement run() method for threads
    public void run() {

        //input and output created for communication
        PrintStream out = null;
        BufferedReader in = null;

        String NewHost = "localhost";
        //we can change port here if we don't wish to use 1565
        int NewHostMainPort = 1565;
        String buf = "";
        int newPort;
        Socket clientSock;
        BufferedReader fromHostServer;
        PrintStream toHostServer;

        try {
            //creating input and output via socket aka sock
            out = new PrintStream(sock.getOutputStream());
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            //usage of in/out from above
            String inLine = in.readLine();
            //below is for non-ie browsers
            StringBuilder htmlString = new StringBuilder();

            //request logging
            System.out.println();
            System.out.println("Request line: " + inLine);

            if(inLine.indexOf("migrate") > -1) {


                //server waiting at port chosen previously, 1565,
                // while new socket being created
                clientSock = new Socket(NewHost, NewHostMainPort);
                fromHostServer = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
                //request being sent to port
                toHostServer = new PrintStream(clientSock.getOutputStream());
                toHostServer.println("Requesting permission to be hosted by you. Send my port! [State=" + parentAgentHolder.agentState + "]");
                toHostServer.flush();

                //wait and read response to see what port we are on/using
                for(;;) {
                    //check valid port
                    buf = fromHostServer.readLine();
                    if(buf.indexOf("[Port=") > -1) {
                        break;
                    }
                }
                //we use format of port response to get port being used
                //see use of substring below:
                String tempbuf = buf.substring( buf.indexOf("[Port=")+6, buf.indexOf("]", buf.indexOf("[Port=")) );

                //look for int containing new port
                newPort = Integer.parseInt(tempbuf);
                //log to console
                //server console
                System.out.println("newPort is: " + newPort);


                //html response to user
                htmlString.append(AgentListener.sendHTMLheader(newPort, NewHost, inLine));
                //send confirmation to user
                htmlString.append("<h3>Migrating to host " + newPort + "</h3> \n");
                htmlString.append("<h3>View the source of this page to see how the client is informed of the new location.</h3> \n");
                //finish html
                htmlString.append(AgentListener.sendHTMLsubmit());

                //log that server being killed
                System.out.println("Parent listening loop being killed.");
                //grab the socket at the old port(stored in the parentAgentHolder)
                //parent agent holder has socket at old port
                ServerSocket ss = parentAgentHolder.sock;

                ss.close();


            } else if(inLine.indexOf("person") > -1) {
                //state being incremented w/ each occurrence and or event in game
                parentAgentHolder.agentState++;
                //sending html back to user
                htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
                htmlString.append("<h3>We are having a conversation with state   " + parentAgentHolder.agentState + "</h3>\n");
                htmlString.append(AgentListener.sendHTMLsubmit());

            } else {
                //inform user invalid request
                htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
                htmlString.append("You have not entered a valid request!\n");
                htmlString.append(AgentListener.sendHTMLsubmit());


            }
            //using the out created earlier as output to send html
            AgentListener.sendHTMLtoStream(htmlString.toString(), out);


            sock.close();


        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

}

class agentHolder {
    //server-socket object
    ServerSocket sock;

    int agentState;


    agentHolder(ServerSocket s) { sock = s;}
}

class AgentListener extends Thread {

    Socket sock;
    int localPort;


    AgentListener(Socket x, int prt) {
        sock = x;
        localPort = prt;
    }

    int agentState = 0;

    //initiated by start()
    //implementing run()
    public void run() {
        BufferedReader in = null;
        PrintStream out = null;
        String NewHost = "localhost";
        System.out.println("In AgentListener Thread");
        try {
            String buf;
            out = new PrintStream(sock.getOutputStream());
            in =  new BufferedReader(new InputStreamReader(sock.getInputStream()));


            buf = in.readLine();

            //parse and store request
            if(buf != null && buf.indexOf("[State=") > -1) {
                //same as previously, we are
                //using the format to get state
                String tempbuf = buf.substring(buf.indexOf("[State=")+7, buf.indexOf("]", buf.indexOf("[State=")));

                agentState = Integer.parseInt(tempbuf);
                //console log
                System.out.println("agentState is: " + agentState);

            }

            System.out.println(buf);
            //storing html response in stringBuilder
            StringBuilder htmlResponse = new StringBuilder();
            //output to user

            htmlResponse.append(sendHTMLheader(localPort, NewHost, buf));
            htmlResponse.append("Now in Agent Looper starting Agent Listening Loop\n<br />\n");
            htmlResponse.append("[Port="+localPort+"]<br/>\n");
            htmlResponse.append(sendHTMLsubmit());

            sendHTMLtoStream(htmlResponse.toString(), out);

            //openning a new connection
            ServerSocket servsock = new ServerSocket(localPort,2);
            //create a new agentholder and store the socket and agentState
            agentHolder agenthold = new agentHolder(servsock);
            agenthold.agentState = agentState;

            //inf. loop while waiting/listening for connections/clients
            while(true) {
                sock = servsock.accept();//accept--wait and accept incoming connection request

                System.out.println("Got a connection to agent at port " + localPort);
                //new agentWorker being created once connection received/conf.
                new AgentWorker(sock, localPort, agenthold).start();
            }

        } catch(IOException ioe) {
            //error message
            System.out.println("Connection Failure or killed listener loop for agent at port " + localPort);
            System.out.println(ioe);
        }
    }

    static String sendHTMLheader(int localPort, String NewHost, String inLine) {

        StringBuilder htmlString = new StringBuilder();

        htmlString.append("<html><head> </head><body>\n");
        htmlString.append("<h2>This is for submission to PORT " + localPort + " on " + NewHost + "</h2>\n");
        htmlString.append("<h3>You sent: "+ inLine + "</h3>");
        htmlString.append("\n<form method=\"GET\" action=\"http://" + NewHost +":" + localPort + "\">\n");
        htmlString.append("Enter text or <i>migrate</i>:");
        htmlString.append("\n<input type=\"text\" name=\"person\" size=\"20\" value=\"YourTextInput\" /> <p>\n");

        return htmlString.toString();
    }

    static String sendHTMLsubmit() {
        return "<input type=\"submit\" value=\"Submit\"" + "</p>\n</form></body></html>\n";
    }
    //browser configuration
    static void sendHTMLtoStream(String html, PrintStream out) {

        out.println("HTTP/1.1 200 OK");
        out.println("Content-Length: " + html.length());
        out.println("Content-Type: text/html");
        out.println("");
        out.println(html);
    }

}


public class HostServer {
    //port 3001
    public static int NextPort = 3000;

    public static void main(String[] a) throws IOException {
        int q_len = 6;
        int port = 1565;
        Socket sock;

        ServerSocket servsock = new ServerSocket(port, q_len);
        System.out.println("Goran's Host Server started at port 1565.");
        System.out.println("Connect from 1 to 3 browsers using \"http:\\\\localhost:1565\"\n");
        //listen on port 1565 for new requests OR migrate requests
        while(true) {
            //increment nextPort
            NextPort = NextPort + 1;
            //same as previously
            sock = servsock.accept();

            System.out.println("Starting AgentListener at port " + NextPort);
            //same as prev, create new agent listener
            new AgentListener(sock, NextPort).start();
        }

    }
}
