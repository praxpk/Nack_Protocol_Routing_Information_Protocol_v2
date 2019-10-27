import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class Rover {

    public static void main(String[] args) {
        // write your code here
        //create objects of each class and assign them objects of each other
        //handle assigned Ip adderss
        //handle join multicast
        //multicast thread will be a while true loop in main and it will use routing table object to give a packet to deposit packet.
        //how to send first message
        //how to react to first message(in Routing_table) how to add a new incoming Inetaddress them to routing table.
        //modify code to use assigned Ip address
        //modify code to use subnets
        //how to print routing table(thread)
        if (args.length!=1){
            System.out.println("Please enter rover number as shown below");
            System.out.println("java Rover 5");
            System.out.println("Where 5 is the assigned number to the Rover");
            System.exit(0);

        }
        int assigned_number=0;
        try {
             assigned_number = Integer.parseInt(args[0]);
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Please enter rover number as shown below");
            System.out.println("java Rover 5");
            System.out.println("Where 5 is the assigned number to the Rover");
            System.exit(0);
        }
        String assigned_ip = "10.0."+assigned_number+".0";

        try {
            System.out.println("R.java line 39: My assigned ip address : "+assigned_ip);
            System.out.println("R.java line 40: My real ip "+InetAddress.getLocalHost());
            DatagramSocket ds = new DatagramSocket(60000);
            Routing_table routing_table = new Routing_table(InetAddress.getByName(assigned_ip),ds);
            Client client = new Client(routing_table,ds);
            Client_Multicast client_multicast= new Client_Multicast(routing_table);
            client.start();
            client_multicast.start();
            routing_table.start();
            DatagramSocket multicast_socket = new DatagramSocket();
            InetAddress multicast_group = InetAddress.getByName("224.0.0.9");
            while(true){
                try {
                    byte[] hello = routing_table.multicast_hello();

                    DatagramPacket hello_packet = new DatagramPacket(hello, hello.length,
                            multicast_group, 59999);
                    multicast_socket.send(hello_packet);
//                System.out.println(Arrays.toString(hello_packet.getData()));
                    Thread.sleep(5000);
                    routing_table.send_regular_update();
                    routing_table.print_routing_table();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }





    }
}
