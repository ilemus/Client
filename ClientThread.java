import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class ClientThread {
    private ArrayList<Byte[]> mPayload = new ArrayList<Byte[]>();
    private int mByteCount = 0;
    private int mNumPackets = 0;
    private String mServerName = null;
    private int mPort = 0;
    private byte HEADER = 0b01111110;
    private static int SNED_BYTES = 128;
    Scanner scanner = new Scanner(System.in);
    private static final String FILE_NAME = "umdlogo.jpg";
    private static final String FILE_LARGE = "bugsbunny1.wav";
    private static final String VERY_LARGE = "09 Rap God.mp3";
    private static final String LAPTOP = "C:/Users/yitzchak/Downloads/";
    private static final String DESKTOP = "D:/Downloads/";
    private static String ROOT_PATH = LAPTOP;
    private static final boolean largeFile = true;
    private static final boolean veryLargeFile = true;
    
    private void loadFile(String location) throws IOException {
        File fp = new File(location);
        byte[] temp = new byte[SNED_BYTES];
        Byte[] tempB;
        int numBytes = 0;
        
        if (fp.exists()) {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fp));
            
            while (true) {
                int count = bis.read(temp, 0, SNED_BYTES);
                
                if (count == -1) {
                    break;
                } else {
                    numBytes += count;
                    
                    // Have to create new element
                    tempB = new Byte[SNED_BYTES];
                    
                    for (int i = 0; i < SNED_BYTES; i++) {
                        tempB[i] = temp[i];
                    }
                    
                    //System.out.println("[127]" + tempB[127] + "[126]" + tempB[126] + "[125]" + tempB[125]);
                    
                    mPayload.add(tempB);
                    mNumPackets++;
                }
            }
            
            //System.out.println("mPayload: " + mPayload);
            
            mByteCount = numBytes;
            
            bis.close();
        } else {
            System.err.println("File to upload is not available");
        }
    }
    
    public ClientThread(String name, int port) {
        mServerName = name;
        mPort = port;
    }
    
    public void sendFile() {
        //System.out.println("Connecting to " + mServerName + " on port " + mPort);
        Socket client = null;
        DataOutputStream dos = null;
        DataInputStream dis = null;
        byte[] temp;
        Byte[] tempB;
        byte[] response = (!largeFile) ? new byte[3] : new byte[4];
        Random rand = new Random();
        
        int loc = rand.nextInt(mNumPackets);
        
        try {
            client = new Socket(mServerName, mPort);
            // 1 second initial timeout
            client.setSoTimeout(1000);
            
            dos = new DataOutputStream(client.getOutputStream());
            dis = new DataInputStream(client.getInputStream());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        //System.out.println("Num Packets: " + mNumPackets);
        
        for (int i = 0; i < mNumPackets; i++) {
            tempB = mPayload.get(i);
            
            temp = (!largeFile) ? new byte[tempB.length + 2] : new byte[tempB.length + 3];
            
            for (int j = 0; j < tempB.length; j++) {
                temp[j] = tempB[j];
            }
            
            if (!largeFile) {
                temp[tempB.length + 1] = HEADER;
                temp[tempB.length + 0] = (byte) (0xFF & i);
            } else {
                temp[tempB.length + 2] = HEADER;
                temp[tempB.length + 1] = (byte) ((0xFF00 & i) >> 8);
                temp[tempB.length + 0] = (byte) (0xFF & i);
            }
            
            CRC16 crc = new CRC16(0x1D, temp);
            //System.out.println(Byte.toString(temp[2]) + ", " + Byte.toString(temp[1]) + ", " + Byte.toString(temp[0]));
            temp = crc.getCrcFrame();
            //System.out.println(Byte.toString(temp[2]) + ", " + Byte.toString(temp[1]) + ", " + Byte.toString(temp[0]));
            
            try {
                // Create one time fault in sending
                if (loc == i) {
                    int pos = rand.nextInt(temp.length);
                    int bit = rand.nextInt(8);
                    
                    int hex = (int)Math.pow(2, bit);
                    
                    byte hexInv = (byte) (hex ^ 0xFF);
                    
                    temp[pos] = (byte) ((temp[pos] & hexInv) + (hex & (temp[pos] ^ 0xFF)));
                    //System.out.println("Rand Num: " + loc);
                    loc = loc + rand.nextInt(mNumPackets);
                }
                //System.out.println("[128] " + temp[128] + " [127] " + temp[127] + " [126] " + temp[126]);
                // Send Data
                dos.write(temp);
                
                // Wait for response
                if (!largeFile) {
                    dis.read(response, 0, 3);
                } else {
                    dis.read(response, 0, 4);
                }
                
                //System.out.println("res[2]: " + Byte.toString(response[2]) + ", res[1]: " + Byte.toString(response[1]) + ", res[0]: " + Byte.toString(response[0]));
                if (Byte.compare(response[response.length - 1], HEADER) == 0
                        && Byte.compare(response[0], (byte) 0x01) == 0) {
                    // Will increment i...
                } else {
                    i = (!largeFile) ? (0xFF & response[1]) : (0xFF & ((0xFF & response[2]) << 8)) + (0xFF & response[1]);
                    System.err.println("Retrying to send packet: " + i);
                    //System.err.println("HEADER: " + (Byte.compare(response[response.length - 1], HEADER) == 0) + " ACK: " + (Byte.compare(response[0], (byte) 1) == 0));
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                System.out.println("Lost Connection to server");
                break;
            }
            //scanner.nextLine();
        }
        
        try {
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public boolean sendMeta() throws IOException {
        boolean success = false;
        System.out.println("Connecting to " + mServerName + " on port " + mPort);
        Socket client = new Socket(mServerName, mPort);
        // 60 second initial timeout
        client.setSoTimeout(60 * 1000);
        
        System.out.println("Connected to " + client.getRemoteSocketAddress());
        OutputStream outToServer = client.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);
        
        byte[] meta = (!largeFile) ? new byte[4] : (!veryLargeFile) ? new byte[5] : new byte[6];
        
        if (!largeFile) {
            meta[3] = HEADER;
            meta[2] = (byte) mNumPackets;
            meta[1] = (byte) ((0xFF00 & mByteCount) >> 8);
            meta[0] = (byte) (0xFF & mByteCount);
        } else {
            if (!veryLargeFile) {
                meta[4] = HEADER;
                meta[3] = (byte) ((0xFF00 & mNumPackets) >> 8);
                meta[2] = (byte) (0xFF & mNumPackets);
                meta[1] = (byte) ((0xFF00 & mByteCount) >> 8);
                meta[0] = (byte) (0xFF & mByteCount);
            } else {
                meta[5] = HEADER;
                meta[4] = (byte) ((0xFF00 & mNumPackets) >> 8);
                meta[3] = (byte) (0xFF & mNumPackets);
                meta[2] = (byte) ((0xFF0000 & mByteCount) >> 16);
                meta[1] = (byte) ((0xFF00 & mByteCount) >> 8);
                meta[0] = (byte) (0xFF & mByteCount);
                
                //System.out.println("[4]: " + meta[4] + " [3]: " + meta[3]);
            }
        }
        
        System.out.println("NUM_PACKETS: " + mNumPackets + " NUM_BYTES: " + mByteCount);
        
        out.write(meta);
        
        InputStream inFromServer = client.getInputStream();
        DataInputStream in = new DataInputStream(inFromServer);
        
        if (!largeFile) {
            in.read(meta, 0, 3);
        } else {
            in.read(meta, 0, 4);
        }
        
        // System.out.print("Header: " + Byte.toString(meta[2]) + ", Count: " + Byte.toString(meta[1]) + ", ACK: " + Byte.toString(meta[0]));
        
        boolean h = ((!largeFile) ? Byte.compare(meta[2], HEADER) == 0 : Byte.compare(meta[3], HEADER) == 0);
        boolean c = Byte.compare(meta[1], (byte) 0xFF) == 0;
        boolean a = Byte.compare(meta[0], (byte) 0x01) == 0;
        
        //System.out.print("h: " + h + ", c: " + c + ", a: " + a);
        
        if (h && c && a) {
            success = true;
        }
        
        client.close();
        return success;
    }
    
    public static void main(String [] args) {
        String serverName = args[0];
        int port = Integer.parseInt(args[1]);
        if (veryLargeFile && largeFile) SNED_BYTES = 1024;
        
        ClientThread ct = new ClientThread(serverName, port);
        
        try {
            ct.loadFile(ROOT_PATH + ((!largeFile) ? FILE_NAME : ((!veryLargeFile) ? FILE_LARGE : VERY_LARGE)));
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        try {
            System.out.println("Send meta data");
            if (ct.sendMeta()) {
                System.out.println("Send file to server");
                ct.sendFile();
            }
        } catch(IOException e) {
           e.printStackTrace();
        }
     }
}
