import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Client extends Thread{
    private int packet_size = 504;
    private int listen_port = 60000;
    private byte [] received_msg = new byte[packet_size];
    private byte [] received_multicast = new byte[packet_size];
    private DatagramSocket socket_listen;
    boolean listen;
    private Routing_table route_table_object;
    public Client(Routing_table r1, DatagramSocket ds){
        System.out.println("C.java line 15: client initialized");
        this.route_table_object = r1;
        this.listen = true;
        this.socket_listen = ds;
    }
    public void run() {
        try{
        while (listen) {
            DatagramPacket incoming_message = new DatagramPacket(received_msg, received_msg.length);
            try {
                socket_listen.receive(incoming_message);
                route_table_object.deposit_packet(incoming_message.getAddress()
                        , incoming_message.getData());
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }catch (Exception e){
            e.printStackTrace();
        }
    }
}
