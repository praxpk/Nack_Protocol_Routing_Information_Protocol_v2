import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

public class Client_Multicast extends Thread{
    private int packet_size = 504;
    private byte [] received_msg = new byte[packet_size];
    private byte [] received_multicast = new byte[packet_size];
    boolean listen;
    private Routing_table route_table_object;
    public Client_Multicast(Routing_table r1){
        route_table_object=r1;
        System.out.println("CM.java line 15: client_multicast initialized");
        this.listen = true;
    }
    public void run() {
        try{
            MulticastSocket msocket = new MulticastSocket(59999);
            InetAddress rover_group = InetAddress.getByName("224.0.0.9");
            msocket.joinGroup(rover_group);
            while (listen) {
                DatagramPacket incoming_multicast = new DatagramPacket(received_multicast, received_multicast.length);
                try {
                    msocket.receive(incoming_multicast);
                    System.out.println("CM.java line 27: Multicast received from "+incoming_multicast.getAddress());
                    System.out.println("CM.java line 28: local host "+InetAddress.getLocalHost());
                    route_table_object.deposit_packet(incoming_multicast.getAddress(),incoming_multicast.getData());
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
