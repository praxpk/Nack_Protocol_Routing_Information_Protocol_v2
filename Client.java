import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class Client extends Thread {
    private int packet_size = 504;
    private int listen_port = 60000;
    private byte[] received_msg = new byte[packet_size];
    private DatagramSocket socket_listen;
    boolean listen;
    private Routing_table route_table_object;
    int packet_num;
    DataClient dc1;
    InetAddress assigned_ip;
    boolean isServer;
    boolean isClient;
    Server server;
    private int current_session_id;
    private OutputStream output;

    public Client(Routing_table r1, DatagramSocket ds, InetAddress assigned_ip) {
        System.out.println("C.java line 15: client initialized");
        this.route_table_object = r1;
        this.listen = true;
        this.socket_listen = ds;
        packet_num = 0;
        this.assigned_ip = assigned_ip;


        this.isServer = false;
        this.isClient = true;
        current_session_id = 0;
        try {
            output = new FileOutputStream("file_name");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public Client(Routing_table r1, DatagramSocket ds, Server server, InetAddress assigned_ip) {
        System.out.println("C.java line 15: client initialized");
        this.route_table_object = r1;
        this.listen = true;
        this.socket_listen = ds;
        packet_num = 0;
        this.dc1 = dc1;
        this.assigned_ip = assigned_ip;
        this.isServer = false;
        this.isClient = true;
        this.isClient = false;
        this.isServer = true;
        this.server = server;
        current_session_id = 0;

    }

    public void setServer(Server s1) {
        this.server = s1;
    }

    public byte[] poison_reverse(byte[] packet) {
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
                if (next_hop.equals(InetAddress.getLocalHost())) {
                    packet[i + 19] = 16;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return packet;
    }

    public boolean isSet(byte b, int pos) {
        boolean result;
        if ((b & (1 << pos)) != 0) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    public static int byte_to_int(byte[] data) {
        int num = ByteBuffer.wrap(data).getShort();
        return num;
    }

    public void run() {
        try {
            while (listen) {
                DatagramPacket incoming_message = new DatagramPacket(received_msg, received_msg.length);
                try {
                    packet_num++;
                    socket_listen.receive(incoming_message);
                    byte[] packet = incoming_message.getData();
                    InetAddress incoming_ip = incoming_message.getAddress();
                    if (incoming_ip.equals(InetAddress.getLocalHost())) {
                        continue;
                    }
                    if ((packet[0] & 0xff) == 2) {
                        byte data[] = poison_reverse(packet);
                    } else {
                        byte[] temp = new byte[4];
                        System.arraycopy(packet, 2, temp, 0, 4);
                        InetAddress dest = InetAddress.getByAddress(temp);
                        System.arraycopy(packet, 6, temp, 0, 4);
                        InetAddress source = InetAddress.getByAddress(temp);
                        if (dest.equals(assigned_ip)) {
                            if (isClient) {
                                if (isSet(packet[0], 7)) {
                                    System.arraycopy(packet, 10, temp, 0, 2);
                                    int session_id = byte_to_int(temp);
                                    if (session_id != current_session_id) {
                                        current_session_id = session_id;
                                        dc1 = new DataClient(socket_listen, assigned_ip, route_table_object, output);
                                        dc1.setSender_ip(source);
                                        dc1.setSender_port(incoming_message.getPort());
                                        Thread.sleep(1000);
                                        dc1.start();
                                        dc1.deposit_packet(packet);
                                    }
                                } else {
                                    dc1.setSender_ip(source);
                                    dc1.setSender_port(incoming_message.getPort());
                                    dc1.deposit_packet(packet);
                                }

                            } else if (isServer) {
                                server.deposit_packet(packet);
                            }
                        } else {
                            if (route_table_object.containsRoverEntry(assigned_ip)) {
                                InetAddress send_to = route_table_object.getRoverEntry(dest).getVia_router();
                                new Sender(packet, send_to, socket_listen).start();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
