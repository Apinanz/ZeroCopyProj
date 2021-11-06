
import java.io.*;
import java.nio.channels.*;
import java.net.*;

public class Server {

    private ServerSocket serverSocket;
    private ServerSocketChannel serverChannel;
    private final String IP = "172.20.10.8"; //ip standart vm
    private final int PORT_SERVER = 8040;
    private final int PORT_SOCKET = 4070;
    private int countClient = 0;
    private final String folder = "C:/Users/api_q/OneDrive/เดสก์ท็อป/ZeroCopyProj/server";

    public static void main(String[] args) {
        Server server = new Server();
    }

    public Server() {
        connected();
    }

    private File[] fetchFile() {
        return new File(folder).listFiles((File file) -> file.isFile()); //judge file?
    }

    private void connected() {
        try {
            serverSocket = new ServerSocket(PORT_SERVER);
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(IP, PORT_SOCKET));
            System.out.println("Server waiting for client on port : " + serverSocket.getLocalPort());
            System.out.println("Server waiting ...");
            while (true) {
                Socket client = serverSocket.accept();
                SocketChannel clientChannel = serverChannel.accept();
                new Thread(new HandleClient(client, clientChannel, ++countClient, fetchFile())).start();
                System.out.println("New connection accepted : " + client.getInetAddress() + " : " + client.getLocalPort() + " --> Cilent No. [" + countClient + "]");
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    class HandleClient implements Runnable {

        private final Socket clientSocket;
        private final SocketChannel clientChannel;
        private DataInputStream din;
        private DataOutputStream dout;
        private final int clientNo;
        private final File[] file;

        public HandleClient(Socket clientSocket, SocketChannel clientChannel, int clientNo, File[] file) {
            this.clientSocket = clientSocket;
            this.clientChannel = clientChannel;
            this.clientNo = clientNo;
            this.file = file;
        }

        @Override
        public void run() {
            try {
                din = new DataInputStream(clientSocket.getInputStream());
                dout = new DataOutputStream(clientSocket.getOutputStream());

                dout.writeInt(file.length); //ขนาดไฟล์ทั้งหมด
                for (int i = 0; i < file.length; i++) {

                    dout.writeUTF(file[i].getName());//send name all file
                }
                int select = din.readInt();
                System.out.println("Cilent No. [" + countClient + "] : Reques File : " + file[select - 1].getName());
                int solution = din.readInt();
                dout.writeLong(file[select - 1].length());
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
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(pathFile)); 
                byte[] buffer = new byte[1024];
                long currentRead = 0;
                while (currentRead < sizeFile) {
                    int read = bis.read(buffer);
                    currentRead += read;
                    dout.write(buffer, 0, read);
                    System.out.println("Downloading : "+currentRead);
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
                    long read = fileChannel.transferTo(currentRead, sizeFile - currentRead, clientChannel);
                    currentRead += read;
                    System.out.println("Downloading : "+currentRead);
                }
                fileChannel.close();
            } catch (IOException e) {

            }
        }

    }
}
