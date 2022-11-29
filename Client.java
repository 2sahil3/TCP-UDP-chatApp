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
import java.util.StringTokenizer;

public class Client extends JFrame
{
    public  String ip="192.168.1.172";
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
            clientSoc = new Socket(ip,6666) ;
            System.out.println("Connected to Server at localhost Port-6666(TCP)");
            din = new DataInputStream(clientSoc.getInputStream());
            dout = new DataOutputStream(clientSoc.getOutputStream());
            String a="HELLO SERVER!!";

            file_contents = a.getBytes();
            DatagramPacket initial = new DatagramPacket(file_contents,file_contents.length, InetAddress.getByName(ip),port); /*For sending a packet via UDP, we should know 4 things, the message to send, its length, ipaddress of destination, port at which destination is listening.*/
            //System.out.println(initial);
            clientSocUDP.send(initial); //send for testing
            dout.writeUTF(LoginName);

            createGUI();
            handleEvents();
            //Recieve messages
            new Thread(new RecievedMessagesHandler (din,LoginName)).start();

            //Send messages

            String inputLine=null;

            while(true)
            {
                try
                {
                    inputLine=bufferedReader.readLine();
                    dout.writeUTF(inputLine);
                    if(inputLine.equals("LOGOUT"))
                    {
                        clientSoc.close();
                        din.close();
                        dout.close();
                        System.out.println("Logged Out");
                        System.exit(0);
                    }
                    StringTokenizer tokenedcommand = new StringTokenizer(inputLine);
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
                                    }
                                    fpin.close();
                                    bpin.close();
                                    System.out.println("TCP: Sent file");
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
                                    }
                                    fpin.close();
                                    bpin.close();
                                    System.out.println("UDP: Sent file");
                                }
                            }
                        }
                    }
                } catch(Exception e){
                    System.out.println(e);
//                    break;
                }
            }
        }
        catch(Exception e) {
            System.out.println(e);
            System.exit(0);
        }
    }

    public Client() {

    }

    private void sayServer()
    {
        try
        {
            String inputLine=bufferedReader.readLine();
            dout.writeUTF(inputLine);
            if(inputLine.equals("LOGOUT"))
            {
                clientSoc.close();
                din.close();
                dout.close();
                System.out.println("Logged Out");
                System.exit(0);
            }
            StringTokenizer tokenedcommand = new StringTokenizer(inputLine);
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
                            }
                            fpin.close();
                            bpin.close();
                            System.out.println("TCP: Sent file");
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
                            }
                            fpin.close();
                            bpin.close();
                            System.out.println("UDP: Sent file");
                        }
                    }
                }
            }
        }
        catch(Exception e){
            System.out.println(e);
//                    break;
        }
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
                System.out.println("key released" + e.getKeyCode());
                // enter key code is 10
                if (e.getKeyCode() == 10) {
                    sayServer();
//                    String msgToSend = messageInp.getText();
//                    messageArea.append("You:" + msgToSend + "\n");
//                    dout.writeUTF
//
//                    out.println(msgToSend);
//                    out.flush();
//                    messageInp.setText("");
//                    messageInp.requestFocus();
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
    public RecievedMessagesHandler(DataInputStream server,String LoginName) {
        this.server = server;
        this.LoginName = LoginName;
    }
    @Override
    public void run() {
        Client cl = new Client();

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
                            System.out.println("Recieving file..."+(current*100/total)+"% complete");
                            fileLength-=size;
                            if(size>fileLength) size=fileLength;
                            //file_contents = new byte[1000];
                        }
                        bpout.flush();
                        fpout.close();
                        bpout.close();
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
                            cl.clientSocUDP.receive(receivePacket);
                            System.out.println("DEBUG: UDP received"); //recieved chunk
                            bpout.write(file_contents,0,size);
                            System.out.println("DEBUG: UDP write"); //write chunk
                            if(total - current >= size) current+=size;
                            else current=total;
                            System.out.println("Recieving file..."+(current*100/total)+"% complete");

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
                    System.out.println(inputLine);
            }
            catch(Exception e){
                e.printStackTrace(System.out);
                break;
            }
        }
    }
}
