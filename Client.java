package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.StringTokenizer;

public class Client extends JFrame
{
    public  String ip="192.168.0.147";
//    public  String ip="116.74.160.126";
    public int port = 6661;
    public DatagramSocket clientSocUDP;
    private JLabel heading = new JLabel("Client");
    private JTextArea messageArea = new JTextArea();
    private JTextField messageInp = new JTextField();
    private Font font = new Font("Roboto", Font.PLAIN, 16);
    byte[] file_contents = new byte[1000];
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    Socket clientSoc;
    DataInputStream din;
    DataOutputStream dout;

    public Client(String args)
    {
        try
        {
            if(args.length()==0)
            {
                System.out.println("No Username given\n");
                System.exit(0);
            }

            String LoginName=args;

            clientSocUDP = new DatagramSocket();
            System.out.println(clientSocUDP);
            clientSoc = new Socket(ip,6666) ;
            System.out.println("Connected to Server at localhost Port-6666(TCP)");
            din = new DataInputStream(clientSoc.getInputStream());
            dout = new DataOutputStream(clientSoc.getOutputStream());
            String a="HELLO SERVER!!";

            file_contents = a.getBytes();
/*For sending a packet via UDP, we should know 4 things, the message to send, its length, ipaddress of destination, port at which destination is listening.*/
            DatagramPacket initial = new DatagramPacket(file_contents,file_contents.length, InetAddress.getByName(ip),port);
            System.out.println(initial);
            clientSocUDP.send(initial); //send for testing
            dout.writeUTF(LoginName);

            createGUI();
            handleEvents();
            //Recieve messages
            new Thread(new RecievedMessagesHandler (din,LoginName,messageArea,clientSocUDP)).start();

            //Send messages

            String inputLine=null;

        }
        catch(Exception e) {
            System.out.println(e);
            System.exit(0);
        }
    }

    public Client() {

    }

    private int sayServer(String msgToSend)
    {
        try
        {

//            String inputLine=bufferedReader.readLine();
            dout.writeUTF(msgToSend);
            if(msgToSend.equals("LOGOUT"))
            {
                clientSoc.close();
                din.close();
                dout.close();
                System.out.println("Logged Out");
                System.exit(0);
            }


            StringTokenizer tokenedcommand = new StringTokenizer(msgToSend);
            //check file transfer

            String comm,fl,typ;
            comm = tokenedcommand.nextToken();


            if(comm.equals("reply"))
            {
                boolean isFile=false;
                if(tokenedcommand.hasMoreTokens())
                {
                    fl=tokenedcommand.nextToken();
                    if(tokenedcommand.hasMoreTokens())
                    {
                        typ=tokenedcommand.nextToken();
                        //file transfer
                        if(typ.equals("tcp"))
                        {
                            isFile=true;
                            File file = new File(fl);
                            FileInputStream fpin = new FileInputStream(file);
                            BufferedInputStream bpin = new BufferedInputStream(fpin);
                            long fileLength =  file.length(), current=0, start = System.nanoTime();
                            dout.writeUTF("LENGTH "+fileLength); //sending filelength to the server
                            int size = 1000; //sending file content in chunks of size 1000 bytes
                            while(current!=fileLength)
                            {
                                if(fileLength - current >= size) current+=size;
                                else {
                                    size = (int)(fileLength-current);
                                    current=fileLength;
                                }
                                file_contents = new byte[size];
                                bpin.read(file_contents,0,size);
                                dout.write(file_contents);
                                System.out.println("Sending file..."+(current*100/fileLength)+"% complete");
                                messageArea.append("Sending file..."+(current*100/fileLength)+"% complete\n");

                            }
                            fpin.close();
                            bpin.close();
                            System.out.println("TCP: Sent file");
                            messageArea.append("TCP: Sent file\n");
                        }
                        else if(typ.equals("udp"))
                        {
                            int size=1024;
                            isFile=true;
                            File file = new File(fl);
                            FileInputStream fpin = new FileInputStream(file);
                            BufferedInputStream bpin = new BufferedInputStream(fpin);
                            long fileLength = file.length(), current =0, start =System.nanoTime();
                            dout.writeUTF("LENGTH "+fileLength);
                            while(current!=fileLength)
                            {
                                if(fileLength - current >= size) current+=size;
                                else {
                                    size = (int)(fileLength-current);
                                    current=fileLength;
                                }
                                file_contents = new byte[size];
                                bpin.read(file_contents,0,size);
                                DatagramPacket sendPacket = new DatagramPacket(file_contents,size,InetAddress.getByName(ip),port);
                                clientSocUDP.send(sendPacket);
                                System.out.println("Sending file..."+(current*100/fileLength)+"% complete");
                                messageArea.append("Sending file..."+(current*100/fileLength)+"% complete\n");
                            }
                            fpin.close();
                            bpin.close();
                            System.out.println("UDP: Sent file");
                            messageArea.append("UDP: Sent file\n");
                        }
                        else
                        {
                            messageArea.append("You:" + msgToSend.substring(6) + "\n");
                        }
                    }
                    else
                    {
                        messageArea.append("You:" + msgToSend.substring(6) + "\n");
                    }

                }
            }
            else{
                messageArea.append("You: " + msgToSend+"\n");
            }
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
        return 1;
    }

    private void handleEvents() {

        messageInp.addKeyListener(new KeyListener() {


            @Override
            public void keyTyped(KeyEvent e) {
            }
            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                // TODO Auto-generated method stub
//                System.out.println("key released" + e.getKeyCode());
                // enter key code is 10
                if (e.getKeyCode() == 10)
                {
                    String msgToSend = messageInp.getText();
                    messageInp.setText("");
                    messageInp.requestFocus();
//                    messageArea.append("You:" + msgToSend + "\n");
                    sayServer(msgToSend);


                }
            }

        });
    }

    private void createGUI() {
        this.setTitle("Client Messager");
        this.setSize(550, 550);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        // components
        heading.setFont(font);
        messageArea.setFont(font);
        messageInp.setFont(font);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        heading.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        messageArea.setEditable(false);
        messageInp.setHorizontalAlignment(SwingConstants.CENTER);
        // layout
        this.setLayout(new BorderLayout());
        this.add(heading, BorderLayout.NORTH);
        JScrollPane jScrollPane = new JScrollPane(messageArea);
        this.add(jScrollPane, BorderLayout.CENTER);
        this.add(messageInp, BorderLayout.SOUTH);
        this.setVisible(true);
    }


    public static void main(String[] args) {
        new Client(args[0]);
    }
}

class RecievedMessagesHandler implements Runnable {
    private DataInputStream server;
    private String LoginName;
    private JTextArea messageArea;
    private DatagramSocket clientSocUDP;
    public RecievedMessagesHandler(DataInputStream server, String LoginName, JTextArea messageArea,DatagramSocket clientSocUDP) {
        this.server = server;
        this.LoginName = LoginName;
        this.messageArea = messageArea;
        this.clientSocUDP = clientSocUDP;
    }
    @Override
    public void run() {
//        Client cl = new Client();

        String inputLine=null;
        while(true)
        {
            try {
                inputLine=server.readUTF();
                StringTokenizer st = new StringTokenizer(inputLine);
                String message_type = st.nextToken();
                if(message_type.equals("FILE"))
                {
                    //File recieve
                    String fileName=st.nextToken();
                    String typ=st.nextToken();
                    st.nextToken();
                    int fileLength = Integer.parseInt(st.nextToken());
                    byte[] file_contents = new byte[1000];
                    Path base_dir = Paths.get(System.getProperty("user.dir"));
                    Path dir = Paths.get(base_dir.toString(), "temp",LoginName);
                    File dir_ = new File(dir.toString());
                    if(!dir_.exists()) dir_.mkdirs();
                    dir = Paths.get(dir_.toString(),fileName);
                    FileOutputStream fpout = new FileOutputStream(dir.toString());
                    //System.out.println(dir.toString());
                    //System.out.println(fileLength);
                    BufferedOutputStream bpout = new BufferedOutputStream(fpout);
                    DatagramPacket receivePacket;
                    if(typ.equals("TCP"))
                    {
                        int bytesRead=0,size=1000,current=0,total = fileLength;
                        if(size>fileLength)size=fileLength;
                        while((bytesRead=server.read(file_contents,0,size))!=-1 && fileLength>0)
                        {
                            bpout.write(file_contents,0,size);
                            if(total - current >= size) current+=size;
                            else current=total;
                            messageArea.append( "Recieving file..."+(current*100/total)+"% complete"+"\n");
                            System.out.println("Recieving file..."+(current*100/total)+"% complete");
                            fileLength-=size;
                            if(size>fileLength) size=fileLength;
                            //file_contents = new byte[1000];
                        }
                        bpout.flush();
                        fpout.close();
                        bpout.close();
                        messageArea.append("TCP: Recieved file \n");
                        System.out.println("TCP: Recieved file");
                    }
                    else
                    {
                        int size=1024,current=0,total = fileLength;
                        file_contents = new byte[size];
                        if(size>fileLength) size=fileLength;
                        System.out.println("DEBUG: UDP FILELENGTH ==> "+fileLength);
                        while(fileLength>0)
                        {
                            receivePacket  = new DatagramPacket(file_contents, size);
                            System.out.println("DEBUG: UDP start"); //start
                            clientSocUDP.setSoTimeout(3000);
                            clientSocUDP.receive(receivePacket);
                            System.out.println("DEBUG: UDP received"); //recieved chunk
                            bpout.write(file_contents,0,size);
                            System.out.println("DEBUG: UDP write"); //write chunk
                            if(total - current >= size) current+=size;
                            else current=total;
                            System.out.println("Recieving file..."+(current*100/total)+"% complete");
                            messageArea.append("Recieving file..."+(current*100/total)+"% complete\n");
                            fileLength-=size;
                            if(size>fileLength) size=fileLength;
                        }
                        bpout.flush();
                        fpout.close();
                        bpout.close();

                        System.out.println("UDP: Recieved file");
                    }
                }
                else
                {
                    System.out.println(inputLine);
                    messageArea.append(inputLine+"\n");
                }

            }
            catch(Exception e){
                e.printStackTrace(System.out);

            }
        }
    }
}
