
import java.io.*;
import java.nio.channels.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    private Socket clientSocket; //ตัวระบุยืนยันตัวตน
    private SocketChannel clientChannel;//ตัวระบุยืนยันตัวตน
    private DataInputStream din; //รับข้อมูล
    private DataOutputStream dout; //ส่งข้อมูล
    private final String IP = "172.20.10.8"; //ip standart vm
    private final int PORT_SERVER = 8040;
    private final int PORT_SOCKET = 4070;
    private final String folder = "C:/Users/api_q/OneDrive/เดสก์ท็อป/ZeroCopyProj/client"; //ที่เก็บไฟล์เมื่อทำการกาวน์โหลด
    private String[] file;

    public static void main(String[] args) {
        Client client = new Client();
        client.connectionToServer();
    }

    private void connectionToServer() {
        try {
            clientSocket = new Socket(IP, PORT_SERVER); //สิ่งที่ระบุตัวตนของ client
            clientChannel = SocketChannel.open(new InetSocketAddress(IP, PORT_SOCKET));//สิ่งที่ระบุตัวตนของ client
            din = new DataInputStream(clientSocket.getInputStream()); //รับข้อมูล
            dout = new DataOutputStream(clientSocket.getOutputStream()); //ส่งข้อมูล
            Scanner sc = new Scanner(System.in);

            int listFile = din.readInt(); //รับขนาดไฟล์
            file = new String[listFile];
            System.out.println("-----------------------File------------------------");
            for (int i = 0; i < listFile; i++) {
                file[i] = din.readUTF();
                System.out.println("(" + (i + 1) + ")" + file[i]);
            }//รับชื่อไฟลืทั้งหมด
            System.out.println("---------------------------------------------------");
            System.out.println("Which file do you want to download?");
            System.out.print("Press your number : ");
            int select = sc.nextInt(); //ใส่เลขไฟล์ที่ต้องการ
            dout.writeInt(select); //ส่งไฟล์ที่ต้องการ
            System.out.println("Which solution do you want to download?");
            System.out.println("(1)" + " Copy");
            System.out.println("(2)" + " ZeroCopy");
            System.out.print("Press your number : ");
            int solution = sc.nextInt();
            dout.writeInt(solution); //ส่งค่าวิธีที่ต้องการโหลดไปหา server
            String path = folder + "/" + file[select - 1];
            long size = din.readLong();
            if (solution == 1) {
                int start = (int) System.currentTimeMillis();
                System.out.println("Receiv...");
                copy(path, size);
                int end = (int) System.currentTimeMillis();
                System.out.println("Finish time : " + (end - start) + " Milliseconde");
            } else {
                int start = (int) System.currentTimeMillis();
                System.out.println("Receiv...");
                zeroCopy(path, size);
                int end = (int) System.currentTimeMillis();
                System.out.println("Finish time : " + (end - start) + " Milliseconde");
            }

        } catch (IOException e) {

        }
    }

    private void copy(String pathFile, long sizeFile) {
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(pathFile)); //ข้อมูลไบต์แปลงเป็น buffer อีกที
            byte[] buffer = new byte[1024];
            long currentRead = 0;
            while (currentRead < sizeFile) {
                int read = din.read(buffer); //อ่านข้อมูลเข้ามาทีละ 1024
                currentRead += read;
                bos.write(buffer, 0, read);
                System.out.println("Receiving : "+read);
            }
            bos.close(); //คืนสิทธิการครอบครองไฟล์
        } catch (IOException e) {

        }
    }

    private void zeroCopy(String pathFile, long sizeFile) {
        try {
            FileChannel fileChannel = new FileOutputStream(pathFile).getChannel();
            int currentRead = 0;
            while (currentRead < sizeFile) {
                long read = fileChannel.transferFrom(clientChannel, currentRead, sizeFile - currentRead);//อ่านข้อมูลไฟล์ที่ส่งมา
                currentRead += read;
                System.out.println("Receiving : "+read);
            }
            fileChannel.close();
        } catch (IOException e) {

        }
    }

}
