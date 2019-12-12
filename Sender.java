import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Sender extends Thread {
    private DatagramSocket sender;
    private byte[] packet_data;
    private InetAddress destination_address;
    private int destination_port = 60000;
    private static int packet_num = 0;
    private String id;
    DatagramPacket message;


    public Sender(byte[] packet_data, InetAddress destination_address,DatagramSocket datagramSocket) {
        this.packet_data = packet_data;
        this.destination_address = destination_address;
        message = new DatagramPacket
                (packet_data,packet_data.length,
                        destination_address,destination_port);
        this.id = id;
        try {
            this.sender = datagramSocket;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Sender(byte[] msg, InetAddress dest_address, int dest_port, DatagramSocket ds1){
        this.message = new DatagramPacket(msg,msg.length,dest_address,dest_port);
        this.sender = ds1;
    }

    public void print_packet(byte [] packet, InetAddress sender){
        try {
            byte[] temp = new byte[4];
            InetAddress dest;
            InetAddress next_hop;
            int metric = 0;
            for (int i = 4; i < 504; i = i + 20) {
                System.arraycopy(packet, i+4, temp, 0, 4);
                dest = InetAddress.getByAddress(temp);
                System.arraycopy(packet, i+12, temp, 0, 4);
                next_hop = InetAddress.getByAddress(temp);
                if(dest.equals(InetAddress.getByName("0.0.0.0"))){
                    break;
                }
                System.out.println(id+","+"Pktnum= "+packet_num+", send2dest= "+sender+"," +
                        " DestAdd= "+dest+", Nxthop= "+next_hop+
                        ", met= "+(packet[i+19]&0xff)+", cmd= "+(packet[0]&0xff));

            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void run() {
        try{
//        System.out.println("S.java line 22: In sender");
//        if(destination_address.equals(InetAddress.getByName("0.0.0.0"))){
//            return;
//        }
//        packet_num++;
            sender.send(message);
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
