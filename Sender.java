import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Sender extends Thread {
    private DatagramSocket sender;
    private byte[] packet_data;
    private InetAddress destination_address;
    private int destination_port = 60000;

    public Sender(byte[] packet_data, InetAddress destination_address,DatagramSocket datagramSocket) {
        this.packet_data = packet_data;
        this.destination_address = destination_address;
        try {
            this.sender = datagramSocket;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try{
        System.out.println("S.java line 22: In sender");
        if(destination_address.equals(InetAddress.getByName("0.0.0.0"))){
            return;
        }
        DatagramPacket message = new DatagramPacket
                (packet_data,packet_data.length,
                        destination_address,destination_port);
        System.out.println("S.java line 26: sending to "+ destination_address);
            sender.send(message);
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
