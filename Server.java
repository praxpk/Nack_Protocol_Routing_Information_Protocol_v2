
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server extends Thread {

    private byte[] file_to_send;
    private static ConcurrentLinkedQueue<Integer> segments;
    private int data_in_packet = 1024;
    private InetAddress dest_ip;
    private int segment_step = 1;
    private DatagramSocket ds1;
    private volatile boolean syn = false;
    private boolean syn_ack = false;
    private boolean ack = false;
    private int dest_port;
    private boolean all_received;
    private int client_start_seg;
    private int client_end_seg;
    private int self_start_seg;
    private int self_end_seg;
    private int session_id;
    private InetAddress this_ip;
    private Routing_table rt;
    private InetAddress assigned_ip;


    public Server(InetAddress dest_ip, InetAddress assigned_ip ,DatagramSocket ds1, int dest_port, Routing_table routing_table) {
        this.dest_ip = dest_ip;
        this.ds1 = ds1;
        this.dest_port = dest_port;
        all_received = false;
        try {
            this_ip = InetAddress.getLocalHost();
        } catch (Exception e) {
            e.printStackTrace();
        }
        client_end_seg = 0;
        client_start_seg = 0;
        rt = routing_table;
        this.assigned_ip = assigned_ip;
    }

    public void setFile_to_send(byte[] file_to_send, int session_id) {
        segments = new ConcurrentLinkedQueue<>();
        this.session_id = session_id;
        this.file_to_send = file_to_send;
        define_segment_numbers(this.file_to_send.length);

    }

    public void define_segment_numbers(int size_of_file) {
        self_start_seg = 0;
        self_end_seg = file_to_send.length / data_in_packet;
        for (int i = 0; i <= (file_to_send.length / data_in_packet); i = i + 1) {
            segments.add(i);
        }

    }

    public InetAddress obtain_real_ip(InetAddress dest) {
        InetAddress result_ip = rt.getRoverEntry(dest).getVia_router();
        return result_ip;
    }


    public void setSyn(boolean syn) {
        this.syn = syn;
    }


    public byte set_bit_at(byte packet, int pos) {
        return (byte) ((packet | (1 << pos)));
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

    public static byte[] intToByte(int num) {
        byte[] b1 = new byte[2];

        ByteBuffer.wrap(b1).putShort((short) num);

        return b1;
    }

    public static int byte_to_int(byte[] data) {
        int num = ByteBuffer.wrap(data).getShort();
        return num;
    }


    public void threeway_handshake() {
        try {
            byte[] packet = new byte[25];
            packet = add_ip_address(packet, dest_ip, assigned_ip);
            packet = create_syn_packet(packet, self_start_seg, self_end_seg);
            long time1 = System.currentTimeMillis();//this is for the time out
            long time2;
            System.out.println("SYN sent");
            while (!syn) {
                InetAddress dest = obtain_real_ip(dest_ip);
                new Sender(packet, dest, dest_port, ds1).start();
                Thread.sleep(10);
                time2 = System.currentTimeMillis();
                if (time2 - time1 > 30000) {
                    System.out.println("Client unreachable"); //30 second timeout
                    System.exit(0);
                }
            }
            //out of while loop syn-ack received. now send ack
            System.out.println("SYN-ACK received");
            packet = new byte[25];
            packet = create_ack_packet(packet);
            InetAddress dest = obtain_real_ip(dest_ip);
            for (int i = 0; i < 5; i++) {
                new Sender(packet, dest, dest_port, ds1).start();
                Thread.sleep(200);
            }
            System.out.println("ACK sent");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] add_ip_address(byte[] packet, InetAddress dest, InetAddress source) {
        byte[] temp;
        temp = dest.getAddress();
        System.arraycopy(temp, 0, packet, 2, 4);
        temp = source.getAddress();
        System.arraycopy(temp, 0, packet, 6, 4);
        return packet;
    }

    public byte[] add_session_number(byte[] packet, int session) {
        byte[] temp = intToByte(session);
        System.arraycopy(temp, 0, packet, 10, 2);
        return packet;
    }

    public byte[] create_syn_packet(byte[] packet, int start_seg, int end_seg) {
        try {
            packet[0] = set_bit_at(packet[0], 7);//set the first bit to 1 for SYN
            packet[1] = intToByte(2)[1];//header length of 2
            byte[] temp;
            packet = add_ip_address(packet, dest_ip, assigned_ip);
            packet = add_session_number(packet, session_id);
            temp = intToByte(start_seg);
            System.arraycopy(temp, 0, packet, 12, 2);
            temp = intToByte(end_seg);
            System.arraycopy(temp, 0, packet, 14, 2);
            System.arraycopy(packet, 10, temp, 0, 2);
//            System.out.println("session id in create syn packet, testing bytetoint session id "+byte_to_int(temp));
            System.arraycopy(packet, 12, temp, 0, 2);
//            System.out.println("session id in create syn packet, testing bytetoint start_seg "+byte_to_int(temp));
            System.arraycopy(packet, 14, temp, 0, 2);
//            System.out.println("session id in create syn packet, testing bytetoint end_seg "+byte_to_int(temp));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return packet;
    }

    public byte[] create_ack_packet(byte[] packet) {
        packet[0] = set_bit_at(packet[0], 4);//set the fourth bit from left to 1 for ACK
        packet[1] = intToByte(2)[1];//header length of 2
        packet = add_ip_address(packet, dest_ip, this_ip);
        packet = add_session_number(packet, session_id);
        byte[] temp;
        temp = intToByte(client_start_seg);
        System.arraycopy(temp, 0, packet, 12, 2);
        temp = intToByte(client_end_seg);
        System.arraycopy(temp, 0, packet, 14, 2);
        return packet;
    }

    public byte[] create_data_packet(int start_seg, int end_seg, int segment_number, int packet_size) {

        byte[] packet = new byte[packet_size];
        packet[0] = set_bit_at(packet[0], 6);
        packet = add_ip_address(packet, dest_ip, this_ip);
        packet = add_session_number(packet, session_id);
        byte[] start_seg_bytes = intToByte(start_seg);
        byte[] end_seg_bytes = intToByte(end_seg);
        byte[] seg_bytes = intToByte(segment_number);
        System.arraycopy(start_seg_bytes, 0, packet, 12, 2);
        System.arraycopy(end_seg_bytes, 0, packet, 14, 2);
        System.arraycopy(seg_bytes, 0, packet, 16, 2);
        System.arraycopy(file_to_send, segment_number * 1024, packet, 18, packet_size - 18);

        return packet;

    }

    public void process_syn_ack(byte[] packet) {
        setSyn(true);
        byte[] temp = new byte[2];
        System.arraycopy(packet, 14, temp, 0, 2);
        client_start_seg = byte_to_int(temp);
        System.arraycopy(packet, 16, temp, 0, 2);
        client_end_seg = byte_to_int(temp);
    }

    public void process_nack(byte[] packet) {
        byte[] temp = new byte[2];
        System.arraycopy(packet, 12, temp, 0, 2);
        int missing_segment = byte_to_int(temp);
        segments.add(missing_segment);
        System.out.println("Received nack for segment number " + missing_segment);
    }


    public void process_all_received() {
        all_received = true;
        System.out.println("all received true");
    }

    public boolean isAll_received() {
        return all_received;
    }

    public void deposit_packet(byte[] packet) {
        byte[] temp = new byte[2];
        if ((isSet(packet[0], 8)) && (isSet(packet[0], 4))) {
            System.arraycopy(packet, 10, temp, 0, 2);
            if (byte_to_int(temp) == session_id) {
                process_syn_ack(packet);
            }
        } else if (isSet(packet[0], 7)) {
            System.arraycopy(packet, 10, temp, 0, 2);
            if (byte_to_int(temp) == session_id) {
//                process_data(packet);
            }
        } else if (isSet(packet[0], 5)) {
            System.arraycopy(packet, 10, temp, 0, 2);
            if (byte_to_int(temp) == session_id) {
                process_nack(packet);
            }
        } else if (isSet(packet[0], 4)) {
            System.arraycopy(packet, 10, temp, 0, 2);
            if (byte_to_int(temp) == session_id) {
                System.out.println("entering all received");
                process_all_received();
            }
        }


    }

    public void run() {
        int start_seg = 0;
        int end_seg = (file_to_send.length / data_in_packet);
        int packet_size = 0;
        int header_size = 18;
        System.out.println("==========Current Session ID = " + session_id + " =======================");
        System.out.println("sending file size of length = " + file_to_send.length);
        System.out.println("end seg = " + end_seg);
        threeway_handshake();
        InetAddress dest = obtain_real_ip(dest_ip);
        try {
            while (!all_received) {
                int segment = 0;
                if (!segments.isEmpty()) {
                    segment = segments.poll();

                    if (segment != end_seg) {
                        packet_size = data_in_packet + header_size;
                    } else {
                        packet_size = (file_to_send.length) - (end_seg * data_in_packet) + header_size;
                    }
                    byte[] packet = create_data_packet(start_seg, end_seg, segment, packet_size);
                    packet = add_ip_address(packet, dest_ip, assigned_ip);

                    new Sender(packet, dest, dest_port, ds1).start();
                    Thread.sleep(1);
                }

            }

            System.out.println("client successfully received chuck");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
