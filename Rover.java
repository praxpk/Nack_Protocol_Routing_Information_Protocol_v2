import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Rover extends Thread {
    private static Server server;
    private static BufferedInputStream in;

    public static void main(String[] args) {
        //create objects of each class and assign them objects of each other
        //handle assigned Ip adderss
        //handle join multicast
        //multicast thread will be a while true loop in main and it will use routing table object to give a packet to deposit packet.
        //how to send first message
        //how to react to first message(in Routing_table) how to add a new incoming Inetaddress them to routing table.
        //modify code to use assigned Ip address
        //modify code to use subnets
        //how to print routing table(thread)
        int assigned_number = 0;
        System.out.println("number ofarguments = " + args.length);
        try {
            assigned_number = Integer.parseInt(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Please enter rover number as shown below");
            System.out.println("java Rover 5");
            System.out.println("Where 5 is the assigned number to the Rover");
            System.exit(0);
        }
        String assigned_ip = "10.0." + assigned_number + ".0";
        if (args.length == 0) {
            System.out.println("Please enter rover number as shown below");
            System.out.println("java Rover 5");
            System.out.println("Where 5 is the assigned number to the Rover");
            System.exit(0);

        } else if (args.length == 1) {


            try {
                System.out.println("My assigned ip address : " + assigned_ip);
                System.out.println("My real ip " + InetAddress.getLocalHost());
                DatagramSocket ds = new DatagramSocket(60000);
                Routing_table routing_table = new Routing_table(InetAddress.getByName(assigned_ip), InetAddress.getLocalHost(), ds);
                Client client = new Client(routing_table, ds, InetAddress.getByName(assigned_ip));
                Client_Multicast client_multicast = new Client_Multicast(routing_table);
                client.start();
                client_multicast.start();
                routing_table.start();
                DatagramSocket multicast_socket = new DatagramSocket();
                InetAddress multicast_group = InetAddress.getByName("224.0.0.9");
                while (true) {
                    try {
                        byte[] hello = routing_table.multicast_hello();
                        byte[] update_packet = new byte[504];
                        update_packet = routing_table.prepare_entire_ripPacket(2, update_packet);
                        DatagramPacket hello_packet = new DatagramPacket(hello, hello.length,
                                multicast_group, 59999);
                        multicast_socket.send(hello_packet);
                        DatagramPacket update = new DatagramPacket(update_packet, update_packet.length,
                                multicast_group, 59999);
                        multicast_socket.send(update);
                        Thread.sleep(1000);
                        routing_table.send_regular_update();
                        routing_table.print_routing_table();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


        } else if (args.length == 3) {
            try {
                System.out.println("My assigned ip address : " + assigned_ip);
                System.out.println("My real ip " + InetAddress.getLocalHost());
                DatagramSocket ds = new DatagramSocket(60000);
                InetAddress dest_address = InetAddress.getByName(args[1]);
                in = new BufferedInputStream(new FileInputStream(args[2]));
                Routing_table routing_table = new Routing_table(InetAddress.getByName(assigned_ip), InetAddress.getLocalHost(), ds);
                Client_Multicast client_multicast = new Client_Multicast(routing_table);
                Client client = new Client(routing_table, ds, server, InetAddress.getByName(assigned_ip));
                client.start();
                client_multicast.start();
                routing_table.start();
                DatagramSocket multicast_socket = new DatagramSocket();
                InetAddress multicast_group = InetAddress.getByName("224.0.0.9");
                new Thread(() -> {
                    try {
                        byte[] buff = new byte[1000 * 1024];
                        int n = 0;
                        int session_id = 1000;
                        Thread.sleep(30000);//30 second sleep to allow network to transfer file
                        System.out.println("entering thread, finishing sleep");
                        while ((n = in.read(buff)) >= 0) {
                            System.out.println("buff length = " + buff.length);
                            server = new Server(dest_address, InetAddress.getByName(assigned_ip), ds, 60000, routing_table);
                            client.setServer(server);
                            server.setFile_to_send(buff, session_id);
                            server.start();
                            while (!server.isAll_received()) {
                                Thread.sleep(100);
                            }
                            session_id++;

                            Thread.sleep(1000);
                        }
                        in.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                while (true) {
                    try {
                        byte[] hello = routing_table.multicast_hello();
                        byte[] update_packet = new byte[504];
                        update_packet = routing_table.prepare_entire_ripPacket(2, update_packet);
                        DatagramPacket hello_packet = new DatagramPacket(hello, hello.length,
                                multicast_group, 59999);
                        multicast_socket.send(hello_packet);
                        DatagramPacket update = new DatagramPacket(update_packet, update_packet.length,
                                multicast_group, 59999);
                        multicast_socket.send(update);
                        Thread.sleep(1000);
                        routing_table.send_regular_update();
                        routing_table.print_routing_table();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

        }


    }
}
