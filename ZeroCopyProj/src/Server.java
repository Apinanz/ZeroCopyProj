
import java.io.*;
import java.nio.channels.*;
import java.net.*;

public class Server {

    private ServerSocket serverSocket; //เชื่อม server กับ client ตัวนี้จะเป็นของ copy
    private ServerSocketChannel serverChannel;//เชื่อม server กับ client ตัวนี้จะเป็นของ ZeroCopy
    private final String IP = "172.20.10.8"; //ip standart vm
    private final int PORT_SERVER = 8040; //port ของ Socket
    private final int PORT_SOCKET = 4070;//port ของ SocketChannel
    private int countClient = 0; //นับclientที่เชื่อมเข้ามา
    private final String folder = "C:/Users/api_q/OneDrive/เดสก์ท็อป/ZeroCopyProj/server"; //path ไฟล์ที่ทำหน้าที่เก็บไฟล์ที่ต้องการโหลด

    public static void main(String[] args) {
        Server server = new Server();
    }

    public Server() {
        connected();
    }

    private File[] fetchFile() {
        return new File(folder).listFiles((File file) -> file.isFile()); //ตรวจสอบว่าเป็นไฟล์จริงๆไหม?
    }

    private void connected() {
        try {
            serverSocket = new ServerSocket(PORT_SERVER); //ทำการเชื่อมโดยใส่ port กำหนดไว้
            serverChannel = ServerSocketChannel.open();//ทำการเชื่อมโดยใส่ port กำหนดไว้
            serverChannel.bind(new InetSocketAddress(IP, PORT_SOCKET));
            System.out.println("Server waiting for client on port : " + serverSocket.getLocalPort());
            System.out.println("Server waiting ...");
            while (true) {
                Socket client = serverSocket.accept(); //จุดยอมรับการเชื่อมต่อ
                SocketChannel clientChannel = serverChannel.accept();//จุดยอมรับการเชื่อมต่อ
                new Thread(new HandleClient(client, clientChannel, ++countClient, fetchFile())).start();
                System.out.println("New connection accepted : " + client.getInetAddress() + " : " + client.getLocalPort() + " --> Cilent No. [" + countClient + "]");
            } //เหตุผลที่เป็น loop while เพราะว่าเราไม่รู้จำนวน client ที่จะเชื่อมต่อ
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    class HandleClient implements Runnable {

        private final Socket clientSocket; //เป็นสิ่งที่ยืนยันตัวตน
        private final SocketChannel clientChannel;//เป็นสิ่งที่ยืนยันตัวตน
        private DataInputStream din; //รับ
        private DataOutputStream dout; //ส่ง
        private final int clientNo; //ทำหน้าที่จำแนกว่าเป็นคนที่เท่าไหร่
        private final File[] file; //ทำหน้าที่เก็บลิสต์ไฟล์

        public HandleClient(Socket clientSocket, SocketChannel clientChannel, int clientNo, File[] file) {
            this.clientSocket = clientSocket;
            this.clientChannel = clientChannel;
            this.clientNo = clientNo;
            this.file = file;
        }

        @Override
        public void run() {
            try {
                din = new DataInputStream(clientSocket.getInputStream()); //รับข้อมูล
                dout = new DataOutputStream(clientSocket.getOutputStream()); //ส่งข้อมูล

                dout.writeInt(file.length); //ส่งขนาดไฟล์ทั้งหมดไปหา client
                for (int i = 0; i < file.length; i++) {

                    dout.writeUTF(file[i].getName());//ส่งชื่อไฟล์ไปหาclientทั้งหมด
                }
                int select = din.readInt(); //อ่านไฟล์ที่ต้องการจาก clinet
                System.out.println("Cilent No. [" + countClient + "] : Reques File : " + file[select - 1].getName());
                int solution = din.readInt(); //อ่านวิธีที่ต้องการโหลด
                dout.writeLong(file[select - 1].length()); //ส่งขนาดไฟล์ทั้งหมดไป
                if (solution == 1) {
                    System.out.println("Cilent No. [" + countClient + "] : Solution : Copy");
                    int start = (int) System.currentTimeMillis(); 
                    System.out.println("Sending...");
                    copy(file[select - 1].getAbsolutePath(), file[select - 1].length());
                    int end = (int) System.currentTimeMillis();
                   
                    System.out.println("Finish time : " + (end - start) + " Milliseconde");
                } else {
                    int start = (int) System.currentTimeMillis();
                    System.out.println("Cilent No. [" + countClient + "] : Solution : ZeroCopy");
                    System.out.println("Sending...");
                    zeroCopy(file[select - 1].getAbsolutePath(), file[select - 1].length());
                    int end = (int) System.currentTimeMillis();
                    
                    System.out.println("Finish time : " + (end - start) + " Milliseconde");
                }

            } catch (IOException e) {

            }
        }

        private void copy(String pathFile, long sizeFile) {
            try {
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(pathFile));  //เก็บลงที่ buffer
                byte[] buffer = new byte[1024]; //เขียนทีละ 1024
                long currentRead = 0;
                while (currentRead < sizeFile) {
                    int read = bis.read(buffer);//อ่านที่ละ 1024
                    currentRead += read; //ทำการบวกค่าข้อมูลที่อ่านแล้ว
                    dout.write(buffer, 0, read); //เขียนข้อมูลตั้งแต่เริ่มต้นจนถึง read
                    System.out.println("Downloading : "+ currentRead + " From : "+ sizeFile );
                }
                bis.close();
            } catch (IOException e) {

            }
        }

        private void zeroCopy(String pathFile, long sizeFile) {
            try {
                FileChannel fileChannel = new FileInputStream(pathFile).getChannel();
                int currentRead = 0;
                while (currentRead < sizeFile) {
                    long read = fileChannel.transferTo(currentRead, sizeFile - currentRead, clientChannel); //ทำการโอนถ่ายข้อมูลไปหา client
                    currentRead += read;
                    System.out.println("Downloading : "+currentRead);
                }
                fileChannel.close();
            } catch (IOException e) {

            }
        }

    }
}
