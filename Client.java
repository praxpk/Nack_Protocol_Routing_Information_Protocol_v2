import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Client extends Thread{
    private int packet_size = 504;
    private int listen_port = 60000;
    private byte [] received_msg = new byte[packet_size];
    private DatagramSocket socket_listen;
    boolean listen;
    private Routing_table route_table_object;
    int packet_num;
    public Client(Routing_table r1, DatagramSocket ds){
        System.out.println("C.java line 15: client initialized");
        this.route_table_object = r1;
        this.listen = true;
        this.socket_listen = ds;
        packet_num = 0;
    }

    public byte [] poison_reverse(byte [] packet) {
        try {
            byte[] temp = new byte[4];
            InetAddress dest;
            InetAddress next_hop;
            int metric = 0;
            for (int i = 4; i < 504; i = i + 20) {
                System.arraycopy(packet, i + 4, temp, 0, 4);
                dest = InetAddress.getByAddress(temp);
                System.arraycopy(packet, i + 12, temp, 0, 4);
                next_hop = InetAddress.getByAddress(temp);
                if (dest.equals(InetAddress.getByName("0.0.0.0"))) {
                    continue;
                }
                if(next_hop.equals(InetAddress.getLocalHost())){
                    packet[i+19]=16;
                }
//                System.out.println("Rcvrip = " + rcvr + "," +
//                        " Destination address = " + dest + ", Next hop = " + next_hop +
//                        ", metric = " + (packet[i + 19] & 0xff));

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return packet;
    }

    public void run() {
        try{
        while (listen) {
            DatagramPacket incoming_message = new DatagramPacket(received_msg, received_msg.length);
            try {
                packet_num++;
                socket_listen.receive(incoming_message);
                if(incoming_message.getAddress().equals(InetAddress.getLocalHost())){
//                    System.out.println("received message from "+incoming_message.getAddress()+", now continue");
                    continue;
                }
                byte data[] = poison_reverse(incoming_message.getData());

                route_table_object.deposit_packet(incoming_message.getAddress()
                        , data);
//                print_packet(incoming_message.getData(),incoming_message.getAddress());

            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }catch (Exception e){
            e.printStackTrace();
        }
    }
}
