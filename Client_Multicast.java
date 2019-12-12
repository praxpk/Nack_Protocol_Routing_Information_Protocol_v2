import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

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
    public byte [] poison_reverse(byte [] packet, InetAddress rcvr) {
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
            MulticastSocket msocket = new MulticastSocket(59999);
            InetAddress rover_group = InetAddress.getByName("224.0.0.9");
            msocket.joinGroup(rover_group);
            RoverEntry r1;
            byte[] temp = new byte[4];
            InetAddress dest;
            InetAddress via_ip;
            int command = 0;
            while (listen) {
                DatagramPacket incoming_multicast = new DatagramPacket(received_multicast, received_multicast.length);
                try {
                    msocket.receive(incoming_multicast);
                    if (incoming_multicast.getAddress().equals(InetAddress.getLocalHost())) {
                        continue;
                    }
                    command = (incoming_multicast.getData()[0] & 0xff);
                    if (command == 1){
                        System.arraycopy(incoming_multicast.getData(), 8, temp, 0, 4);
                    dest = InetAddress.getByAddress(temp);
                    System.arraycopy(incoming_multicast.getData(), 16, temp, 0, 4);
                    via_ip = InetAddress.getByAddress(temp);
//                    System.out.println("CM.java line 27: Multicast received from "+incoming_multicast.getAddress());
//                    System.out.println("CM.java line 28: local host "+InetAddress.getLocalHost());
//                    print_packet(incoming_multicast.getData(),incoming_multicast.getAddress());
                    route_table_object.deposit_packet(incoming_multicast.getAddress(), incoming_multicast.getData());
                    if (route_table_object.containsRoverEntry(dest)) {
                        r1 = route_table_object.getRoverEntry(dest);
                        r1.change_metric_and_via(1, incoming_multicast.getAddress());
                    } else {
                        route_table_object.addRoverEntry(via_ip, dest);
//                        r1.change_metric_and_via(1, InetAddress.getLocalHost());
                    }
                } else if (command==2){
                        byte[] data = incoming_multicast.getData();
                        data=poison_reverse(data,incoming_multicast.getAddress());
                        route_table_object.deposit_packet(incoming_multicast.getAddress(),data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
